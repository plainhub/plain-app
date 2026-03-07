/**
 * i18n-translate-todo.mjs  (plain-app / Android)
 *
 * Reads scripts/i18n-todo.json, translates each item via Google Translate
 * (unofficial free endpoint – no API key), and writes
 * scripts/i18n-translated.json.
 *
 * Batching: up to BATCH_SIZE strings per request to reduce round-trips.
 * Android-style placeholders (%1$s, %s, {{x}}) are shielded before
 * translation and restored afterwards.
 *
 * Usage (run from plain-app root):
 *   node scripts/i18n-translate-todo.mjs
 *
 * Env vars:
 *   BATCH_SIZE  – strings per request (default 20)
 *   DELAY_MS    – ms between requests  (default 300)
 */
const BATCH_SIZE = parseInt(process.env.BATCH_SIZE ?? '20', 10)
const DELAY_MS = parseInt(process.env.DELAY_MS ?? '300', 10)
// Separator: special Unicode symbols that won't be translated as words
const SEP = ' ◆◇◆ '

import fs from 'node:fs'
import path from 'node:path'

const todoFile = path.resolve('scripts/i18n-todo.json')
if (!fs.existsSync(todoFile)) {
  console.error('scripts/i18n-todo.json not found – run i18n-find-untranslated.mjs first')
  process.exit(1)
}
const todo = JSON.parse(fs.readFileSync(todoFile, 'utf8'))

const stableFile = path.resolve('scripts/i18n-stable.json')
const stable = fs.existsSync(stableFile)
  ? JSON.parse(fs.readFileSync(stableFile, 'utf8'))
  : {}

// ── Protect Android placeholders (%1$s, %s, %d, {{path}}, \n, \') ─────────────
function protect(s, map) {
  // Android printf-style: %1$s, %2$d, %s, %d, etc.
  s = s.replace(/%\d+\$[a-zA-Z]|%[a-zA-Z]/g, (m) => {
    const i = map.length; map.push(m); return `⟨${i}⟩`
  })
  // Mustache-style: {{path}}, {{ version_name }}
  s = s.replace(/\{\{[^}]+\}\}/g, (m) => {
    const i = map.length; map.push(m); return `⟨${i}⟩`
  })
  return s
}
function restore(s, map) {
  return s.replace(/⟨(\d+)⟩/g, (_, i) => map[+i] ?? _)
}

// ── Google Translate (free, no key) ──────────────────────────────────────────
async function translateBatch(strings, targetLang) {
  const placeholderMap = []
  const protected_ = strings.map((s) => protect(s, placeholderMap))
  const joined = protected_.join(SEP)

  const url =
    `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=` +
    `${encodeURIComponent(targetLang)}&dt=t&q=${encodeURIComponent(joined)}`

  const res = await fetch(url)
  if (!res.ok) throw new Error(`HTTP ${res.status} for lang=${targetLang}`)

  const data = await res.json()
  const raw = data[0].map((x) => x[0]).join('')
  const parts = raw.split(/\s*◆◇◆\s*/)
  return parts.map((p) => restore(p.trim(), placeholderMap))
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

// ── Main ──────────────────────────────────────────────────────────────────────
async function main() {
  const translated = {}
  let totalDone = 0
  let totalFailed = 0
  let totalStable = 0

  for (const [dir, { lang, missing, english }] of Object.entries(todo)) {
    const allItems = [
      ...missing.map((i) => ({ ...i, src: 'missing' })),
      ...english.map((i) => ({ ...i, src: 'english' })),
    ]
    if (allItems.length === 0) continue
    console.log(`\n[${dir}] translating ${allItems.length} items → ${lang}`)

    const results = []

    for (let i = 0; i < allItems.length; i += BATCH_SIZE) {
      const batch = allItems.slice(i, i + BATCH_SIZE)
      const enTexts = batch.map((it) => it.en)
      let translations

      try {
        translations = await translateBatch(enTexts, lang)
      } catch (e) {
        console.error(`  Batch [${i}..${i + batch.length - 1}] failed: ${e.message}`)
        translations = enTexts
        totalFailed += batch.length
      }

      for (let j = 0; j < batch.length; j++) {
        const t = translations[j] ?? enTexts[j]
        results.push({ key: batch[j].key, en: batch[j].en, translated: t, src: batch[j].src })
        if (t.trim() === batch[j].en.trim()) {
          stable[dir] = stable[dir] ?? []
          if (!stable[dir].includes(batch[j].key)) {
            stable[dir].push(batch[j].key)
            totalStable++
          }
        }
        process.stdout.write('.')
      }

      if (i + BATCH_SIZE < allItems.length) await sleep(DELAY_MS)
    }

    translated[dir] = { lang, items: results }
    totalDone += results.length
    console.log()
  }

  fs.writeFileSync(path.resolve('scripts/i18n-translated.json'), JSON.stringify(translated, null, 2), 'utf8')
  fs.writeFileSync(stableFile, JSON.stringify(stable, null, 2), 'utf8')
  console.log(`\n✓ ${totalDone} translated, ${totalFailed} failed, ${totalStable} new stable entries`)
  console.log(`Written to scripts/i18n-translated.json`)
}

main()
