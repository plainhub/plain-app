/**
 * i18n-apply-todo.mjs  (plain-app / Android)
 *
 * Reads scripts/i18n-translated.json and patches each locale strings.xml:
 *   - Missing keys are inserted before </resources>
 *   - Keys that were still English are replaced in-place (line-level rewrite)
 *
 * Android XML escaping rules are applied to the translated values:
 *   apostrophes → \'   ampersands → &amp;   newlines → \n
 *
 * Usage (run from plain-app root):
 *   node scripts/i18n-apply-todo.mjs
 */
import fs from 'node:fs'
import path from 'node:path'

// ── XML encode for Android string values ──────────────────────────────────────
function escapeAndroid(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/'/g, "\\'")
    .replace(/\n/g, '\\n')
}

// ── Apply to a single strings.xml ────────────────────────────────────────────
function applyToFile(filePath, items) {
  let xml = fs.readFileSync(filePath, 'utf8')
  let changed = 0

  // Separate into two groups: replacements and insertions
  const toReplace = items.filter((i) => i.src === 'english')
  const toInsert = items.filter((i) => i.src === 'missing')

  // 1. Replace existing (English-value) lines
  for (const { key, translated: t } of toReplace) {
    const escaped = escapeAndroid(t)
    // Match the entire line for this key
    const re = new RegExp(
      `([ \\t]*<string\\s+name="${key}"[^>]*>)[^<]*(</string>)`,
      'g',
    )
    const updated = xml.replace(re, `$1${escaped}$2`)
    if (updated !== xml) { xml = updated; changed++ }
  }

  // 2. Insert missing keys before </resources>
  if (toInsert.length > 0) {
    const insertLines = toInsert
      .map(({ key, translated: t }) => `    <string name="${key}">${escapeAndroid(t)}</string>`)
      .join('\n')
    xml = xml.replace(/(\s*<\/resources>)/, `\n${insertLines}\n$1`)
    changed += toInsert.length
  }

  if (changed > 0) {
    fs.writeFileSync(filePath, xml, 'utf8')
  }
  return changed
}

// ── Main ──────────────────────────────────────────────────────────────────────
const translatedFile = path.resolve('scripts/i18n-translated.json')
if (!fs.existsSync(translatedFile)) {
  console.error('scripts/i18n-translated.json not found – run i18n-translate-todo.mjs first')
  process.exit(1)
}

const translated = JSON.parse(fs.readFileSync(translatedFile, 'utf8'))
const resDir = path.resolve('app/src/main/res')

let totalApplied = 0
let totalFiles = 0

for (const [dir, { items }] of Object.entries(translated)) {
  if (!items || items.length === 0) continue
  const filePath = path.join(resDir, dir, 'strings.xml')
  if (!fs.existsSync(filePath)) { console.warn(`  Skip ${dir} – file not found`); continue }

  const applied = applyToFile(filePath, items)
  if (applied > 0) {
    console.log(`[${dir}] applied ${applied} translations`)
    totalApplied += applied
    totalFiles++
  }
}

console.log(`\n✓ ${totalApplied} keys applied across ${totalFiles} files`)
