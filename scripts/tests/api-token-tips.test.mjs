import { readFileSync, readdirSync } from 'node:fs'
import { strict as assert } from 'node:assert'
import { test } from 'node:test'

const root = new URL('../../', import.meta.url)

test('API token curl uses the HTTPS port and does not bypass certificate validation', () => {
  const source = readFileSync(new URL('shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/connections/ApiTokenTipsPage.kt', root), 'utf8')
  assert.match(source, /Prefs\.httpsPort\.collectAsState\(\)/)
  assert.match(source, /buildUrl\("https", hostname, httpsPort\.value, "\/graphql"\)/)
  assert.doesNotMatch(source, /buildUrl\("http"|Prefs\.httpPort|--insecure|curl -k/)
})

test('every translated API token tip warns about HTTPS', () => {
  const resources = new URL('shared/src/commonMain/composeResources/', root)
  for (const directory of readdirSync(resources).filter(name => name === 'values' || name.startsWith('values-'))) {
    const source = readFileSync(new URL(`${directory}/strings_network.xml`, resources), 'utf8')
    const tip = source.match(/<string name="auth_dev_token_tips">(.*?)<\/string>/s)
    assert.ok(tip, `${directory}: missing API token guidance`)
    assert.match(tip[1], /HTTPS/, directory)
  }
})
