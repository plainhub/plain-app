# Copilot Instructions for plain-app

## i18n Translation Workflow

### When to use
Run this workflow any time new strings are added to `app/src/main/res/values/strings.xml`, or when you suspect other locales have untranslated (still-English) strings.

### How to trigger
Tell Copilot:
> **"同步翻译"** or **"sync i18n translations"** or **"检查并补全多语言翻译"**

Copilot will run the three-step pipeline below **from the `plain-app` project root**.

### Three-step pipeline

```bash
# Step 1 – detect missing keys and untranslated (English) values
node scripts/i18n-find-untranslated.mjs
# → writes scripts/i18n-todo.json  (grouped by locale folder, e.g. values-de)

# Step 2 – translate only the affected keys via Google Translate (free, no API key)
node scripts/i18n-translate-todo.mjs
# → writes scripts/i18n-translated.json
# → writes scripts/i18n-stable.json  (loanwords / brand names intentionally same as English)

# Step 3 – apply translations back into each locale strings.xml
node scripts/i18n-apply-todo.mjs

# Verify clean
node scripts/i18n-find-untranslated.mjs
# → should print "Total: 0 missing, 0 untranslated"
```

### Key design decisions
- Base locale is `app/src/main/res/values/strings.xml` (English).
- Only the **delta** (missing/untranslated keys) is sent for translation — never the whole file.
- Android-style placeholders (`%1$s`, `%s`, `{{path}}`) are protected before translation and restored afterwards.
- Keys where Google Translate returns the same value as English (loanwords, brand names, tech terms) are recorded in `scripts/i18n-stable.json` and skipped in future runs.
- Intermediate files (`i18n-todo.json`, `i18n-translated.json`) are gitignored; `i18n-stable.json` **must be committed**.

### Scripts location
| Script | Purpose |
|--------|---------|
| `scripts/i18n-find-untranslated.mjs` | Detect missing / English-value keys → `i18n-todo.json` |
| `scripts/i18n-translate-todo.mjs` | Translate via Google Translate → `i18n-translated.json` |
| `scripts/i18n-apply-todo.mjs` | Patch locale `strings.xml` files |
| `scripts/i18n-stable.json` | Cache of keys correctly staying as English (auto-managed, commit this) |

### Locale directory mapping
| Folder | Language |
|--------|----------|
| `values-zh-rCN` | Chinese Simplified |
| `values-zh-rTW` | Chinese Traditional |
| `values-de` | German |
| `values-fr` | French |
| `values-es` | Spanish |
| `values-it` | Italian |
| `values-pt` | Portuguese |
| `values-ru` | Russian |
| `values-ja` | Japanese |
| `values-ko` | Korean |
| `values-nl` | Dutch |
| `values-tr` | Turkish |
| `values-vi` | Vietnamese |
| `values-hi` | Hindi |
| `values-ta` | Tamil |
| `values-bn` | Bengali |
