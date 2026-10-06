#!/usr/bin/env node
// Wire-protocol client for the phone's main listener: logs in over the
// WebSocket `auth=1` handshake and speaks token-mode GraphQL. This is the
// same exchange the web client performs in `login-handshake.ts` /
// `gql-client.ts`, written out in Node so the API suite can drive it from a
// shell without a browser.
//
// Zero dependencies on purpose: Node's built-in crypto already covers P-256
// ECDH, Ed25519 and ChaCha20-Poly1305. The only piece missing is HChaCha20,
// which XChaCha20 needs to stretch the 24-byte nonce down to the 12 bytes
// ChaCha20-Poly1305 takes.
//
//   node scripts/gql-client.mjs --host <ip> --port <port> --queries <tsv>
//
// Queries file is `name<TAB>query` per line; one TSV result line comes back
// per query: `name<TAB>ok|http<TAB>detail`.

import crypto from 'node:crypto'
import { execSync } from 'node:child_process'

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

function xchachaSeal(key32, plaintext) {
  const nonce = crypto.randomBytes(24)
  const cipher = crypto.createCipheriv('chacha20-poly1305', hchacha20(key32, nonce.subarray(0, 16)), Buffer.concat([Buffer.alloc(4), nonce.subarray(16)]), { authTagLength: 16 })
  const body = Buffer.concat([cipher.update(plaintext), cipher.final(), cipher.getAuthTag()])
  return Buffer.concat([nonce, body])
}

function xchachaOpen(key32, blob) {
  if (blob.length < 24) throw new Error(`ciphertext too short (${blob.length})`)
  const nonce = blob.subarray(0, 24)
  const decipher = crypto.createDecipheriv('chacha20-poly1305', hchacha20(key32, nonce.subarray(0, 16)), Buffer.concat([Buffer.alloc(4), nonce.subarray(16)]), { authTagLength: 16 })
  decipher.setAuthTag(blob.subarray(blob.length - 16))
  return Buffer.concat([decipher.update(blob.subarray(24, blob.length - 16)), decipher.final()])
}

// ---- handshake primitives --------------------------------------------------

const sha512hex = (text) => crypto.createHash('sha512').update(text, 'utf8').digest('hex')

// DER SPKI prefix for a raw 32-byte Ed25519 public key.
const ED25519_SPKI = Buffer.from('302a300506032b6570032100', 'hex')

function ed25519Verify(publicKeyB64, message, signatureB64) {
  const key = crypto.createPublicKey({ key: Buffer.concat([ED25519_SPKI, Buffer.from(publicKeyB64, 'base64')]), format: 'der', type: 'spki' })
  return crypto.verify(null, Buffer.from(message, 'utf8'), key, Buffer.from(signatureB64, 'base64'))
}

// ---- login -----------------------------------------------------------------

async function login(host, port, clientId) {
  const initRes = await fetch(`http://${host}:${port}/init`, { method: 'POST', headers: { 'c-id': clientId } })
  if (!initRes.ok) throw new Error(`/init -> HTTP ${initRes.status}`)
  const { password, signaturePublicKey } = await initRes.json()
  if (!password) throw new Error('/init returned no password — is a password configured on the device?')

  const passwordHash = sha512hex(password)
  const key = Buffer.from(passwordHash.slice(0, 32), 'ascii')

  const ecdh = crypto.createECDH('prime256v1')
  ecdh.generateKeys()
  const clientPub = ecdh.getPublicKey() // uncompressed X9.63, what the server decodes

  const request = Buffer.from(
    JSON.stringify({
      password: passwordHash,
      browserName: 'plain-api-test',
      browserVersion: '1',
      osName: process.platform,
      osVersion: process.version,
      isMobile: false,
      ecdhPublicKey: clientPub.toString('base64'),
    }),
    'utf8',
  )

  // The server answers PENDING first whenever 2FA is on (the Rust default).
  // It then delivers the real response on this same socket, but only after the
  // app's confirmation prompt is accepted — so `onPending` is where the caller
  // taps that prompt, and the socket has to stay open until then.
  let approved = false
  const response = await new Promise((resolve, reject) => {
    const ws = new WebSocket(`ws://${host}:${port}/?cid=${encodeURIComponent(clientId)}&auth=1`)
    const timer = setTimeout(() => {
      ws.close()
      reject(new Error(approved ? 'the device never answered after the approval — nobody accepted the prompt' : 'login handshake timed out'))
    }, 90000)
    ws.binaryType = 'arraybuffer'
    ws.onopen = () => ws.send(xchachaSeal(key, request))
    ws.onmessage = async (event) => {
      const frame = xchachaOpen(key, Buffer.from(new Uint8Array(event.data)))
      if (JSON.parse(frame.toString('utf8')).status === 'PENDING' && !approved) {
        approved = true
        console.error('PENDING — run the on-pending command, waiting for the app to confirm')
        // Its stdout goes to our stderr so it cannot corrupt the result stream.
        // A failing approval is not fatal to the process: the socket stays
        // open and the dialog can still be accepted, so report it and keep
        // waiting rather than throwing out of the message handler.
        if (opts['on-pending']) {
          try {
            execSync(opts['on-pending'], { stdio: ['inherit', 2, 2] })
          } catch (e) {
            console.error(`approval command failed: ${e.message.split('\n')[0]}`)
          }
        }
        return
      }
      clearTimeout(timer)
      ws.close()
      resolve(frame)
    }
    ws.onerror = () => {
      clearTimeout(timer)
      reject(new Error('login websocket error — wrong password or desktop access disabled?'))
    }
    ws.onclose = (event) => {
      clearTimeout(timer)
      if (!approved) reject(new Error(`login closed: ${event.code} ${event.reason || '(no reason)'}`))
    }
  })

  const result = JSON.parse(response.toString('utf8'))
  if (result.status !== 'COMPLETED') throw new Error(`unexpected login status: ${result.status}`)

  const signed = `${result.clientId}|${result.status}|${result.ecdhPublicKey}|${result.timestamp}`
  if (!ed25519Verify(signaturePublicKey, signed, result.signature)) {
    throw new Error('login response signature did not verify against the /init key')
  }

  const shared = ecdh.computeSecret(Buffer.from(result.ecdhPublicKey, 'base64'))
  const token = crypto.createHash('sha256').update(shared).digest('base64')
  return { clientId, token }
}

// ---- graphql ---------------------------------------------------------------

async function graphql(host, port, clientId, token, query) {
  const body = xchachaSeal(Buffer.from(token, 'base64'), Buffer.from(`${Date.now()}|${crypto.randomBytes(16).toString('hex')}|${JSON.stringify({ query })}`, 'utf8'))
  const res = await fetch(`http://${host}:${port}/graphql`, {
    method: 'POST',
    headers: { 'c-id': clientId, 'content-type': 'application/octet-stream' },
    body,
  })
  if (res.status !== 200) return { ok: false, detail: `HTTP ${res.status}` }
  const text = xchachaOpen(Buffer.from(token, 'base64'), Buffer.from(await res.arrayBuffer())).toString('utf8')
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
async function rawQuery(host, port, clientId, token, query) {
  const body = xchachaSeal(Buffer.from(token, 'base64'), Buffer.from(`${Date.now()}|${crypto.randomBytes(16).toString('hex')}|${JSON.stringify({ query })}`, 'utf8'))
  const res = await fetch(`http://${host}:${port}/graphql`, {
    method: 'POST',
    headers: { 'c-id': clientId, 'content-type': 'application/octet-stream' },
    body,
  })
  if (res.status !== 200) return `HTTP ${res.status}`
  return xchachaOpen(Buffer.from(token, 'base64'), Buffer.from(await res.arrayBuffer())).toString('utf8')
}

// ---- cli -------------------------------------------------------------------

function parseArgs(argv) {
  const opts = {}
  for (let i = 0; i < argv.length; i += 2) opts[argv[i].replace(/^--/, '')] = argv[i + 1]
  return opts
}

const opts = parseArgs(process.argv.slice(2))
const host = opts.host
const port = opts.port || '8080'
const clientId = opts['client-id'] || `plain-api-test-${crypto.randomBytes(4).toString('hex')}`

if (!host || (!opts.queries && !opts.raw)) {
  console.error('usage: gql-client.mjs --host <ip> --port <port> (--queries <tsv> | --raw <query-file>) [--client-id <id>] [--on-pending <cmd>]')
  process.exit(2)
}

const { readFileSync } = await import('node:fs')

let session
try {
  session = await login(host, port, clientId)
} catch (e) {
  console.error(`LOGIN FAILED: ${e.message}`)
  process.exit(1)
}
console.error(`logged in as ${session.clientId}`)

if (opts.raw) {
  const body = await rawQuery(host, port, session.clientId, session.token, readFileSync(opts.raw, 'utf8'))
  process.stdout.write(body)
  process.exit(0)
}

const queries = readFileSync(opts.queries, 'utf8')
  .split('\n')
  .map((line) => line.trim())
  .filter(Boolean)
  .map((line) => line.split('\t'))

for (const [name, query] of queries) {
  let result
  try {
    result = await graphql(host, port, session.clientId, session.token, query)
  } catch (e) {
    result = { ok: false, detail: e.message }
  }
  console.log([name, result.ok ? 'ok' : 'fail', result.detail.replace(/[\t\n]/g, ' ')].join('\t'))
}