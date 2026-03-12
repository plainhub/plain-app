# Crowdin Automation with GitHub Actions

This document describes the fully-automated translation workflow used by this project.
No manual operations on the Crowdin web UI are required after the initial one-time setup.

---

## Overview

The workflow follows the pattern popularised by projects such as Element Android, Thunderbird, and
Signal — a tight loop between the source repository and Crowdin:

```
Developer pushes English strings
        ↓
[crowdin-upload.yml] uploads sources to Crowdin automatically
        ↓
Community translates on Crowdin
        ↓
[crowdin-sync.yml] (weekly / manual) pulls translations → opens a GitHub PR
        ↓
Maintainer reviews & merges the PR
```

All orchestration is done via the official
[crowdin/github-action](https://github.com/crowdin/github-action) (v2).

---

## Repository layout

| Path | Role |
|------|------|
| `crowdin.yml` | Crowdin CLI configuration — file mappings & language codes |
| `.github/workflows/crowdin-upload.yml` | CI workflow that uploads English sources |
| `.github/workflows/crowdin-sync.yml` | CI workflow that pulls translations & opens a PR |

### crowdin.yml highlights

```yaml
project_id: "879016"
api_token_env: CROWDIN_PERSONAL_TOKEN   # resolved from the GitHub secret

files:
  - source: /app/src/main/res/values/strings.xml
    translation: /app/src/main/res/values-%two_letters_code%/strings.xml
    type: android
    languages_mapping:
      two_letters_code:
        zh-CN: zh-rCN
        zh-TW: zh-rTW
  - source: /app/src/main/res/values/plurals.xml
    translation: /app/src/main/res/values-%two_letters_code%/plurals.xml
    type: android
    languages_mapping:
      two_letters_code:
        zh-CN: zh-rCN
        zh-TW: zh-rTW
```

---

## One-time setup

### 1 · Create a Crowdin Personal Access Token

1. Log in to [crowdin.com](https://crowdin.com) → **Account settings → API → New token**.
2. Give it a descriptive name (e.g. `plain-app-ci`).
3. Grant the **Projects (source files & translations)** scope.
4. Copy the generated token — it is shown only once.

### 2 · Add the secret to GitHub

1. Open the repository on GitHub → **Settings → Secrets and variables → Actions**.
2. Click **New repository secret**.
3. Name: `CROWDIN_PERSONAL_TOKEN`
4. Value: paste the token from step 1.
5. Save.

> The `GITHUB_TOKEN` secret is provided automatically by GitHub and requires no extra setup.

### 3 · Configure branch protection (recommended)

If the default branch (`main`) has branch-protection rules, make sure:

- The `l10n/crowdin-translations` branch is **not** included in the protection rules, or
- The "Allow force pushes" option is enabled for that branch (the sync action force-resets it
  on every run to avoid stale history).

Alternatively, require a PR review for `l10n/crowdin-translations` PRs if you want human sign-off
before translations land.

### 4 · (Optional) Enable Crowdin's GitHub integration

You can also connect Crowdin's **GitHub integration** on the Crowdin project page
(Integrations → GitHub) for a real-time preview of pending PRs, but this is optional — the
workflows function without it.

---

## Workflow reference

### `crowdin-upload.yml` — Upload Sources

| Property | Value |
|----------|-------|
| Trigger | Push to `main` touching `strings.xml` or `plurals.xml`; manual dispatch |
| Action | `crowdin/github-action@v2` with `upload_sources: true` |
| Effect | Sends the latest English source files to Crowdin |

Crowdin calculates a diff internally, so only changed strings appear as new work for translators.

### `crowdin-sync.yml` — Sync Translations

| Property | Value |
|----------|-------|
| Trigger | Weekly cron (Monday 09:00 UTC); manual dispatch |
| Action | `crowdin/github-action@v2` with `download_translations: true` |
| Effect | Downloads translations → pushes `l10n/crowdin-translations` branch → opens or updates a PR |

Key options in the workflow:

```yaml
localization_branch_name: l10n/crowdin-translations
create_pull_request: true
pull_request_base_branch_name: main
export_only_approved: false   # change to true to ship only reviewer-approved strings
```

Setting `export_only_approved: true` is common in projects that have a dedicated translation review
team. Leave it `false` if you want the community's translations to ship as soon as they are
contributed.

---

## Day-to-day developer workflow

### Adding or changing English strings

1. Edit `app/src/main/res/values/strings.xml` (and/or `plurals.xml`) as usual.
2. Open a PR and merge to `main`.
3. The **Upload Sources** workflow fires automatically.
   - New/changed keys appear in Crowdin within seconds.
   - Deleted keys are automatically marked as "hidden" in Crowdin (not removed — history is
     preserved).

No further action is required from the developer.

### Shipping translations

1. Wait for the weekly **Sync Translations** workflow, **or** trigger it manually from
   **Actions → Crowdin — Sync Translations → Run workflow**.
2. Review the automatically-opened PR (`chore(i18n): sync translations from Crowdin`).
3. Merge the PR — done.

---

## Troubleshooting

### The upload workflow fails with "Unauthorized"

- Verify the `CROWDIN_PERSONAL_TOKEN` secret is set in GitHub repository secrets.
- Check that the token has not expired (Crowdin tokens do not expire by default, but they can be
  revoked manually).

### The sync workflow opens an empty PR / no changes

- It is normal if no new translations have been submitted since the last sync.
- Run the workflow manually after a period of active translation activity.

### Merge conflicts on the `l10n/crowdin-translations` branch

The sync action force-pushes the localization branch on every run, so conflicts on that branch
resolve themselves automatically on the next trigger. If a conflict exists on the target PR,
re-run the **Sync Translations** workflow and the PR will be updated.

### You want only reviewed/approved strings

Change `export_only_approved: false` to `export_only_approved: true` in
`.github/workflows/crowdin-sync.yml`.

---

## Supported locales

The locales in scope are defined by the Crowdin project settings. The `crowdin.yml`
`languages_mapping` block handles the Android-specific folder naming:

| Crowdin code | Android folder |
|---|---|
| `de` | `values-de` |
| `es` | `values-es` |
| `fr` | `values-fr` |
| `hi` | `values-hi` |
| `it` | `values-it` |
| `ja` | `values-ja` |
| `ko` | `values-ko` |
| `nl` | `values-nl` |
| `pt` | `values-pt` |
| `ru` | `values-ru` |
| `ta` | `values-ta` |
| `tr` | `values-tr` |
| `vi` | `values-vi` |
| `bn` | `values-bn` |
| `zh-CN` | `values-zh-rCN` |
| `zh-TW` | `values-zh-rTW` |

---

## References

- [crowdin/github-action — official action](https://github.com/crowdin/github-action)
- [Crowdin CLI configuration docs](https://developer.crowdin.com/configuration-file/)
- [Crowdin API — Personal Access Tokens](https://support.crowdin.com/personal-access-tokens/)
