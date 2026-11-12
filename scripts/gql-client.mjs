#!/usr/bin/env node
// OPAQUE password login and encrypted GraphQL using the shared Rust WASM client.
// Run plain-core/scripts/build-auth-wasm.sh in the sibling checkout first.
// Set PLAIN_TEST_PASSWORD_FILE (raw password bytes, no trailing newline) or PLAIN_TEST_PASSWORD.
// --base-url preserves HTTP/HTTPS; HTTPS always uses Node's normal certificate validation.
// HTTP authenticates the API through OPAQUE but does not authenticate downloaded webpage code.

import crypto from 'node:crypto'
import { execSync } from 'node:child_process'
import { readFileSync } from 'node:fs'

// ---- XChaCha20-Poly1305 ----------------------------------------------------

const rotl = (v, c) => ((v << c) | (v >>> (32 - c))) >>> 0

function quarterRound(x, a, b, c, d) {
  x[a] = (x[a] + x[b]) >>> 0
  x[d] = rotl(x[d] ^ x[a], 16)
  x[c] = (x[c] + x[d]) >>> 0
  x[b] = rotl(x[b] ^ x[c], 12)
  x[a] = (x[a] + x[b]) >>> 0
  x[d] = rotl(x[d] ^ x[a], 8)
  x[c] = (x[c] + x[d]) >>> 0
  x[b] = rotl(x[b] ^ x[c], 7)
}

function hchacha20(key, nonce16) {
  const s = new Uint32Array(16)
  s[0] = 0x61707865
  s[1] = 0x3320646e
  s[2] = 0x79622d32
  s[3] = 0x6b206574
  for (let i = 0; i < 8; i++) s[4 + i] = key.readUInt32LE(i * 4)
  for (let i = 0; i < 4; i++) s[12 + i] = nonce16.readUInt32LE(i * 4)
  for (let r = 0; r < 10; r++) {
    quarterRound(s, 0, 4, 8, 12)
    quarterRound(s, 1, 5, 9, 13)
    quarterRound(s, 2, 6, 10, 14)
    quarterRound(s, 3, 7, 11, 15)
    quarterRound(s, 0, 5, 10, 15)
    quarterRound(s, 1, 6, 11, 12)
    quarterRound(s, 2, 7, 8, 13)
    quarterRound(s, 3, 4, 9, 14)
  }
  const out = Buffer.alloc(32)
  ;[0, 1, 2, 3, 12, 13, 14, 15].forEach((v, i) => out.writeUInt32LE(s[v], i * 4))
  return out
}

function xchachaSeal(key32, plaintext, aad) {
  const nonce = crypto.randomBytes(24)
  const cipher = crypto.createCipheriv('chacha20-poly1305', hchacha20(key32, nonce.subarray(0, 16)), Buffer.concat([Buffer.alloc(4), nonce.subarray(16)]), { authTagLength: 16 })
  if (aad) cipher.setAAD(aad, { plaintextLength: plaintext.length })
  const body = Buffer.concat([cipher.update(plaintext), cipher.final(), cipher.getAuthTag()])
  return Buffer.concat([nonce, body])
}

function xchachaOpen(key32, blob, aad) {
  if (blob.length < 40) throw new Error(`ciphertext too short (${blob.length})`)
  const nonce = blob.subarray(0, 24)
  const decipher = crypto.createDecipheriv('chacha20-poly1305', hchacha20(key32, nonce.subarray(0, 16)), Buffer.concat([Buffer.alloc(4), nonce.subarray(16)]), { authTagLength: 16 })
  if (aad) decipher.setAAD(aad, { plaintextLength: blob.length - 40 })
  decipher.setAuthTag(blob.subarray(blob.length - 16))
  return Buffer.concat([decipher.update(blob.subarray(24, blob.length - 16)), decipher.final()])
}

// ---- login -----------------------------------------------------------------

async function login(clientId) {
  const initRes = await fetch(new URL('/init', baseUrl), {
    method: 'POST', headers: { 'c-id': clientId },
  })
  if (!initRes.ok) throw new Error(`/init -> HTTP ${initRes.status}`)
  const init = await initRes.json()
  if (init.passwordRequired !== true) throw new Error('This test client requires password login enabled on the server')
  const publicKey = (value) => {
    const key = Buffer.from(value ?? '', 'base64')
    if (key.length !== 32 || key.toString('base64') !== value) throw new Error('Invalid /init public key')
    return key
  }
  const signingKey = publicKey(init.signaturePublicKey)
  const opaqueKey = publicKey(init.opaquePublicKey)
  const passwordFile = opts['password-file'] || process.env.PLAIN_TEST_PASSWORD_FILE
  const password = passwordFile ? readFileSync(passwordFile) : Buffer.from(process.env.PLAIN_TEST_PASSWORD ?? '', 'utf8')
  delete process.env.PLAIN_TEST_PASSWORD
  if (!password.length) throw new Error('Set PLAIN_TEST_PASSWORD_FILE or PLAIN_TEST_PASSWORD; /init never supplies a password')
  const moduleUrl = new URL('../../plain-desktop/src/lib/opaque/pkg/plain_auth.js', import.meta.url)
  let module
  try {
    module = await import(moduleUrl.href)
    module.initSync({ module: readFileSync(new URL('plain_auth_bg.wasm', moduleUrl)) })
  } catch (error) {
    password.fill(0)
    throw new Error('OPAQUE client artifacts unavailable; run plain-core/scripts/build-auth-wasm.sh', { cause: error })
  }
  let login
  try { login = module.OpaqueLogin.start(password) }
  finally { password.fill(0) }
  let key
  let responseAad
  let approved = false
  try {
    const result = await new Promise((resolve, reject) => {
      const url = new URL('/auth', baseUrl)
      url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
      url.searchParams.set('cid', clientId)
      const ws = new WebSocket(url)
      let settled = false
      const finish = (error, result) => {
        if (settled) return
        settled = true
        clearTimeout(timer)
        ws.close()
        if (error) reject(error)
        else resolve(result)
      }
      const timer = setTimeout(() => finish(new Error('Login timed out')), 90000)
      ws.binaryType = 'arraybuffer'
      ws.onopen = () => ws.send(login.request())
      ws.onmessage = (event) => {
        try {
          if (!(event.data instanceof ArrayBuffer)) throw new Error('Expected binary login frame')
          const frame = Buffer.from(event.data)
          if (login) {
            const state = login
            login = undefined
            const completed = state.finish(frame, clientId, signingKey)
            let requestAad
            let ke3
            try {
              if (!crypto.timingSafeEqual(Buffer.from(completed.serverPublicKey), opaqueKey)) throw new Error('Server OPAQUE key changed')
              const apiKey = completed.apiKey
              key = Buffer.from(apiKey.buffer, apiKey.byteOffset, apiKey.byteLength)
              requestAad = completed.requestAad
              responseAad = completed.responseAad
              ke3 = completed.ke3
            } finally { completed.free() }
            const details = Buffer.from(JSON.stringify({
              appName: 'plain-api-test', appVersion: '1', osName: process.platform,
              osVersion: process.version, isMobile: false,
            }))
            ws.send(Buffer.concat([Buffer.from(ke3), xchachaSeal(key, details, requestAad)]))
            return
          }
          const reply = JSON.parse(xchachaOpen(key, frame, responseAad).toString('utf8'))
          if (reply.status === 'PENDING') {
            if (!approved && opts['on-pending']) {
              approved = true
              try { execSync(opts['on-pending'], { stdio: ['inherit', 2, 2] }) }
              catch (error) { console.error(`approval command failed: ${error.message.split('\n')[0]}`) }
            }
            return
          }
          if (reply.status !== 'COMPLETED' || typeof reply.clientId !== 'string' || !reply.clientId) throw new Error('Invalid completed login response')
          finish(undefined, reply)
        } catch (error) { finish(error) }
      }
      ws.onerror = () => finish(new Error('Login websocket failed; verify server availability and trusted TLS certificate'))
      ws.onclose = event => finish(new Error(`Login closed: ${event.code} ${event.reason}`))
    })
    // The reply clientId identifies the server device; API requests retain our session cid.
    return { clientId, token: key.toString('base64') }
  } finally {
    login?.free()
    key?.fill(0)
  }
}

function graphqlRequest(key, query) {
  const id = crypto.randomBytes(16)
  id.writeUIntBE(Date.now(), 0, 6)
  id[6] = (id[6] & 15) | 0x70
  id[8] = (id[8] & 63) | 0x80
  const hex = id.toString('hex')
  const requestId = `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  return xchachaSeal(key, Buffer.from(JSON.stringify({ requestId, query })))
}

function graphqlResponse(key, request, response) {
  const plain = xchachaOpen(key, response)
  const binding = Buffer.concat([Buffer.from([0]), crypto.createHash('sha256').update(request).digest()])
  if (plain.length < binding.length || !plain.subarray(0, binding.length).equals(binding)) throw new Error('Invalid GraphQL response binding')
  return plain.subarray(binding.length).toString('utf8')
}

// ---- graphql ---------------------------------------------------------------

async function graphql(clientId, token, query) {
  const body = graphqlRequest(Buffer.from(token, 'base64'), query)
  const res = await fetch(new URL('/graphql', baseUrl), {
    method: 'POST',
    headers: { 'c-id': clientId, 'content-type': 'application/octet-stream' },
    body,
  })
  if (res.status !== 200) return { ok: false, detail: `HTTP ${res.status}` }
  const text = graphqlResponse(Buffer.from(token, 'base64'), body, Buffer.from(await res.arrayBuffer()))
  if (text.includes('"errors"')) {
    let detail = text.slice(0, 200)
    try {
      detail = JSON.parse(text).errors.map((e) => e.message).join('; ').slice(0, 200)
    } catch {
      /* keep the raw prefix */
    }
    return { ok: false, detail: `GraphQL errors: ${detail}` }
  }
  return { ok: true, detail: '200 OK' }
}

// Prints the decrypted body verbatim — for introspection and other queries
// whose payload is the point.
async function rawQuery(clientId, token, query) {
  const body = graphqlRequest(Buffer.from(token, 'base64'), query)
  const res = await fetch(new URL('/graphql', baseUrl), {
    method: 'POST',
    headers: { 'c-id': clientId, 'content-type': 'application/octet-stream' },
    body,
  })
  if (res.status !== 200) return `HTTP ${res.status}`
  return graphqlResponse(Buffer.from(token, 'base64'), body, Buffer.from(await res.arrayBuffer()))
}

// ---- cli -------------------------------------------------------------------

function parseArgs(argv) {
  const opts = {}
  for (let i = 0; i < argv.length; i += 2) opts[argv[i].replace(/^--/, '')] = argv[i + 1]
  return opts
}

const opts = parseArgs(process.argv.slice(2))
const host = opts.host
// No default: the ports are a stored preference, so guessing one is how a
// request ends up on the other build that shares the phone.
const port = opts.port
const clientId = opts['client-id'] || `plain-api-test-${crypto.randomBytes(4).toString('hex')}`

if ((!opts['base-url'] && (!host || !port)) || (!opts.queries && !opts.raw)) {
  console.error('usage: gql-client.mjs --base-url <http[s]://host:port> (--queries <tsv> | --raw <query-file>) [--client-id <id>] [--password-file <path>] [--on-pending <cmd>]')
  process.exit(2)
}

const baseUrl = new URL(opts['base-url'] || `http://${host}:${port}`)
if (!['http:', 'https:'].includes(baseUrl.protocol) || baseUrl.username || baseUrl.password || baseUrl.pathname !== '/' || baseUrl.search || baseUrl.hash) throw new Error('Expected an HTTP(S) origin without credentials or a path')
if (process.env.NODE_TLS_REJECT_UNAUTHORIZED === '0') throw new Error('TLS certificate validation must not be disabled')

let session
try {
  session = await login(clientId)
} catch (e) {
  console.error(`LOGIN FAILED: ${e instanceof Error ? e.message : String(e)}`)
  process.exit(1)
}
console.error(`logged in as ${session.clientId}`)

if (opts.raw) {
  const body = await rawQuery(session.clientId, session.token, readFileSync(opts.raw, 'utf8'))
  process.stdout.write(body)
  process.exit(0)
}

const queries = readFileSync(opts.queries, 'utf8')
  .split('\n')
  .map((line) => line.trim())
  .filter(Boolean)
  .map((line) => line.split('\t'))

for (const [name, query] of queries) {
  const raw = name.startsWith('raw:')
  let result
  try {
    // A `raw:` case exists for its payload, not for its status line, and it
    // must be sent ONCE. Sending it twice would replay the document, which is
    // harmless for a query and unacceptable for a mutation.
    if (raw) {
      const detail = await rawQuery(session.clientId, session.token, query)
      const failed = /"errors"/.test(detail)
      result = failed
        ? { ok: false, detail: `GraphQL errors: ${detail.slice(0, 200)}` }
        : { ok: true, detail }
    } else {
      result = await graphql(session.clientId, session.token, query)
    }
  } catch (e) {
    result = { ok: false, detail: e instanceof Error ? e.message : String(e) }
  }
  console.log([name, result.ok ? 'ok' : 'fail', result.detail.replace(/[\t\n]/g, ' ')].join('\t'))
}