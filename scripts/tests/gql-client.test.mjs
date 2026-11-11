import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { createHash, randomBytes } from 'node:crypto'
import { mkdtemp, writeFile, rm } from 'node:fs/promises'
import { createServer } from 'node:http'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createInterface } from 'node:readline'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { xchacha20poly1305 } from '../../../plain-desktop/node_modules/@noble/ciphers/chacha.js'

const seal = (key, bytes, aad) => {
  const nonce = randomBytes(24)
  return Buffer.concat([nonce, xchacha20poly1305(key, nonce, aad).encrypt(bytes)])
}
const open = (key, bytes, aad) => xchacha20poly1305(key, bytes.subarray(0, 24), aad).decrypt(bytes.subarray(24))
const encodeFrame = bytes => {
  const header = bytes.length < 126 ? Buffer.from([0x82, bytes.length]) : Buffer.from([0x82, 126, bytes.length >> 8, bytes.length & 255])
  return Buffer.concat([header, bytes])
}

for (const wrongPassword of [false, true]) {
test(`actual GraphQL script ${wrongPassword ? 'rejects a wrong password' : 'interoperates with Rust OPAQUE and encrypted binary transport'}`, { timeout: 30000 }, async () => {
  const helper = spawn(fileURLToPath(new URL('../../../plain-core/target/auth-wasm/debug/examples/opaque-wasm-harness', import.meta.url)))
  const lines = createInterface({ input: helper.stdout })[Symbol.asyncIterator]()
  const next = async () => {
    const value = await lines.next()
    assert.equal(value.done, false)
    return Buffer.from(value.value, 'hex')
  }
  const opaqueKey = await next()
  const identity = Buffer.alloc(32, 7)
  let key
  let requestAad
  let responseAad
  let frames = 0
  let socket
  let serverError
  const server = createServer(async (req, res) => {
    try {
      if (req.url === '/init') {
        res.setHeader('content-type', 'application/json')
        res.end(JSON.stringify({ passwordRequired: true, signaturePublicKey: identity.toString('base64'), opaquePublicKey: opaqueKey.toString('base64') }))
        return
      }
      assert.equal(req.url, '/graphql')
      assert.equal(req.headers['c-id'], 'wasm-test-client')
      const chunks = []
      for await (const chunk of req) chunks.push(chunk)
      const request = Buffer.concat(chunks)
      const text = Buffer.from(open(key, request, Buffer.from('http-request\0POST\0/graphql'))).toString()
      const first = text.indexOf('|')
      const second = text.indexOf('|', first + 1)
      assert.equal(JSON.parse(text.slice(second + 1)).query, '{ __typename }')
      const aad = Buffer.concat([Buffer.from('r\0'), createHash('sha256').update(request).digest()])
      res.end(seal(key, Buffer.from(JSON.stringify({ data: { __typename: 'Query' } })), aad))
    } catch (error) { serverError = error; res.writeHead(500).end() }
  })
  server.on('upgrade', (req, stream) => {
    socket = stream
    const accept = createHash('sha1').update(`${req.headers['sec-websocket-key']}258EAFA5-E914-47DA-95CA-C5AB0DC85B11`).digest('base64')
    stream.write(`HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: ${accept}\r\n\r\n`)
    let pending = Buffer.alloc(0)
    let queue = Promise.resolve()
    stream.on('data', chunk => {
      pending = Buffer.concat([pending, chunk])
      while (pending.length >= 2) {
        const opcode = pending[0] & 15
        if (opcode === 8) { stream.end(); return }
        let length = pending[1] & 127
        let offset = 2
        if (length === 126) { if (pending.length < 4) return; length = pending.readUInt16BE(2); offset = 4 }
        assert.notEqual(length, 127)
        if (pending.length < offset + 4 + length) return
        const mask = pending.subarray(offset, offset + 4)
        const frame = Buffer.from(pending.subarray(offset + 4, offset + 4 + length))
        for (let i = 0; i < frame.length; i++) frame[i] ^= mask[i % 4]
        pending = pending.subarray(offset + 4 + length)
        queue = queue.then(async () => {
          if (frames++ === 0) {
            helper.stdin.write(`${frame.toString('hex')}\n`)
            stream.write(encodeFrame(await next()))
          } else {
            helper.stdin.write(`${frame.subarray(0, 64).toString('hex')}\n`)
            key = await next(); requestAad = await next(); responseAad = await next()
            const metadata = JSON.parse(Buffer.from(open(key, frame.subarray(64), requestAad)).toString())
            assert.equal(metadata.appName, 'plain-api-test')
            stream.write(encodeFrame(seal(key, Buffer.from(JSON.stringify({ status: 'COMPLETED', clientId: 'wasm-test-server' })), responseAad)))
          }
        }).catch(error => { serverError = error; stream.destroy() })
      }
    })
  })
  const dir = await mkdtemp(join(tmpdir(), 'plain-gql-'))
  try {
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
    const query = join(dir, 'query.graphql')
    await writeFile(query, '{ __typename }')
    const child = spawn(process.execPath, [fileURLToPath(new URL('../gql-client.mjs', import.meta.url)), '--base-url', `http://127.0.0.1:${server.address().port}`, '--client-id', 'wasm-test-client', '--raw', query], {
      env: { ...process.env, PLAIN_TEST_PASSWORD: wrongPassword ? 'wrong password' : 'test password for OPAQUE', PLAIN_TEST_PASSWORD_FILE: '' },
    })
    let stdout = ''; let stderr = ''
    child.stdout.on('data', data => { stdout += data })
    child.stderr.on('data', data => { stderr += data })
    const status = await new Promise(resolve => child.on('close', resolve))
    assert.equal(serverError, undefined)
    if (wrongPassword) {
      assert.equal(status, 1)
      assert.match(stderr, /Password login failed/)
      assert.equal(stdout, '')
      assert.equal(frames, 1)
    } else {
      assert.equal(status, 0, stderr)
      assert.deepEqual(JSON.parse(stdout), { data: { __typename: 'Query' } })
      assert.equal(frames, 2)
    }
  } finally {
    socket?.destroy(); server.closeAllConnections(); server.close(); helper.kill()
    key?.fill(0)
    await rm(dir, { recursive: true, force: true })
  }
})

}
