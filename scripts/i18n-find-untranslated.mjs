/**
 * i18n-find-untranslated.mjs  (plain-app / Android)
 *
 * Compares every locale strings_*.xml set against the base English strings
 * in app/src/main/res/values/strings_*.xml and writes scripts/i18n-todo.json:
 *   - missing  : names present in base but absent in locale
 *   - english  : names whose value equals the English one and looks like
 *                real English text (not loanwords / unchanged brand names)
 *
 * Usage (run from plain-app root):
 *   node scripts/i18n-find-untranslated.mjs
 */
import fs from 'node:fs'
import path from 'node:path'

// ── XML helpers ────────────────────────────────────────────────────────────────
function parseStrings(file) {
  const xml = fs.readFileSync(file, 'utf8')
  const map = new Map()
  // Match <string name="key">value</string>  (single-line; Android format is always single-line)
  const re = /<string\s+name="([^"]+)"[^>]*>([^<]*(?:<!\[CDATA\[[^\]]*\]\]>[^<]*)*)<\/string>/g
  let m
  while ((m = re.exec(xml)) !== null) {
    map.set(m[1], m[2])
  }
  return map
}

/** Merge all strings_*.xml (and legacy strings.xml) in a directory into one Map. */
function parseAllStringsInDir(dir) {
  const merged = new Map()
  if (!fs.existsSync(dir)) return merged
  for (const f of fs.readdirSync(dir)) {
    if (!/^strings.*\.xml$/.test(f)) continue
    const filePath = path.join(dir, f)
    for (const [k, v] of parseStrings(filePath)) merged.set(k, v)
  }
  return merged
}

// Android XML entity / CDATA decode (best-effort for comparison only)
function decode(s) {
  return s
    .replace(/<!\[CDATA\[(.*?)\]\]>/gs, (_, c) => c)
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&apos;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/\\n/g, '\n')
    .replace(/\\'/g, "'")
}

// ── "Looks like untranslated English text" heuristic ──────────────────────────
function looksLikeUntranslatedEnglish(value) {
  if (typeof value !== 'string') return false
  const v = decode(value).trim()
  if (v.length <= 2) return false
  if (!/[a-zA-Z]/.test(v)) return false
  // All-caps short token → acronym/abbreviation
  if (/^[A-Z0-9\s\/\-_\.]+$/.test(v) && v.length <= 8) return false
  // URL
  if (/^https?:\/\//.test(v)) return false
  // Android/printf placeholders only
  if (/^[\s%\{\}0-9\$a-z_]+$/.test(v)) return false
  // At least one 3-letter lowercase run → real English word
  return /[a-z]{3}/.test(v)
}

// ── Locale directory → Google Translate lang code ────────────────────────────
const langMap = {
  'values-bn': 'bn',
  'values-de': 'de',
  'values-es': 'es',
  'values-fr': 'fr',
  'values-hi': 'hi',
  'values-it': 'it',
  'values-ja': 'ja',
  'values-ko': 'ko',
  'values-nl': 'nl',
  'values-pt': 'pt',
  'values-ru': 'ru',
  'values-ta': 'ta',
  'values-tr': 'tr',
  'values-vi': 'vi',
  'values-zh-rCN': 'zh-CN',
  'values-zh-rTW': 'zh-TW',
}

// ── Main ──────────────────────────────────────────────────────────────────────
const resDir = path.resolve('app/src/main/res')
const baseMap = parseAllStringsInDir(path.join(resDir, 'values'))

// Load stable cache
const stableFile = path.resolve('scripts/i18n-stable.json')
const stable = fs.existsSync(stableFile)
  ? JSON.parse(fs.readFileSync(stableFile, 'utf8'))
  : {}

const todo = {}
let totalMissing = 0
let totalEnglish = 0

for (const [dir, lang] of Object.entries(langMap)) {
  const locDir = path.join(resDir, dir)
  if (!fs.existsSync(locDir)) continue

  const locMap = parseAllStringsInDir(locDir)
  const stableKeys = stable[dir] ?? []
  const missing = []
  const english = []

  for (const [name, enVal] of baseMap) {
    if (stableKeys.includes(name)) continue
    const locVal = locMap.get(name)
    if (locVal === undefined) {
      missing.push({ key: name, en: decode(enVal) })
    } else if (
      decode(locVal).trim() === decode(enVal).trim() &&
      looksLikeUntranslatedEnglish(enVal)
    ) {
      english.push({ key: name, en: decode(enVal) })
    }
  }

  if (missing.length + english.length === 0) continue
  todo[dir] = { lang, missing, english }
  totalMissing += missing.length
  totalEnglish += english.length
  console.log(`${dir}: ${missing.length} missing, ${english.length} untranslated (English)`)
}

const outFile = path.resolve('scripts/i18n-todo.json')
fs.writeFileSync(outFile, JSON.stringify(todo, null, 2), 'utf8')
console.log(`\nTotal: ${totalMissing} missing, ${totalEnglish} untranslated`)
console.log(`Written to ${outFile}`)
