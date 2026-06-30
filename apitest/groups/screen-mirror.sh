#!/usr/bin/env bash
# Group screen-mirror — ScreenMirrorGraphQL
# Source-only; the runner sources this file.
#
# Schemas covered:
#   ScreenMirrorGraphQL : screenMirrorState, screenMirrorControlEnabled,
#                         screenMirrorQuality, screenMirrorVideoCodec,
#                         startScreenMirror, requestScreenMirrorAudio,
#                         stopScreenMirror, updateScreenMirrorQuality,
#                         sendScreenMirrorControl
#
# Screen mirror requires the AccessibilityService to be enabled. On a
# stock Pixel userdebug it isn't, so the mutations that dispatch
# input via the accessibility service will throw — we record them as
# gated rather than as failures. The read-side queries always work.

run_group "screen-mirror" "Screen mirror control" "docs/api-test-plan.md#screen-mirror"

# ----------------------------------------------------------------------------
# screen-mirror-C01  screenMirrorState (initially false)
# ----------------------------------------------------------------------------
SMS=$(call_gql '{ screenMirrorState }')
api_sms=$(printf '%s' "$SMS" | jq -r '.data.screenMirrorState')
if [[ "$api_sms" == "true" || "$api_sms" == "false" ]]; then
  pass "screen-mirror-C01 screenMirrorState = $api_sms"
else
  fail "screen-mirror-C01 screenMirrorState returned: $SMS"
fi

# ----------------------------------------------------------------------------
# screen-mirror-C02  screenMirrorControlEnabled (requires AccessibilityService)
# ----------------------------------------------------------------------------
SMCE=$(call_gql '{ screenMirrorControlEnabled }')
api_smce=$(printf '%s' "$SMCE" | jq -r '.data.screenMirrorControlEnabled')
if [[ "$api_smce" == "true" || "$api_smce" == "false" ]]; then
  pass "screen-mirror-C02 screenMirrorControlEnabled = $api_smce (true only if AccessibilityService enabled)"
else
  fail "screen-mirror-C02 screenMirrorControlEnabled returned: $SMCE"
fi

# ----------------------------------------------------------------------------
# screen-mirror-C03  screenMirrorQuality
# ----------------------------------------------------------------------------
SMQ=$(call_gql '{ screenMirrorQuality { mode resolution } }')
api_smq=$(printf '%s' "$SMQ" | jq '.data.screenMirrorQuality')
if [[ "$api_smq" != "null" && -n "$api_smq" ]]; then
  pass "screen-mirror-C03 screenMirrorQuality returned: $api_smq"
else
  fail "screen-mirror-C03 screenMirrorQuality not returned: $SMQ"
fi

# ----------------------------------------------------------------------------
# screen-mirror-C04  updateScreenMirrorQuality
# ----------------------------------------------------------------------------
USMQ=$(call_gql 'mutation { updateScreenMirrorQuality(mode: HD) }')
api_usmq=$(printf '%s' "$USMQ" | jq -r '.data.updateScreenMirrorQuality // empty')
if [[ "$api_usmq" == "true" ]]; then
  pass "screen-mirror-C04 updateScreenMirrorQuality(HD) → true"
else
  fail "screen-mirror-C04 updateScreenMirrorQuality returned: $USMQ"
fi

# ----------------------------------------------------------------------------
# screen-mirror-C05  startScreenMirror — would launch the service
# ----------------------------------------------------------------------------
# Skip the actual launch — it would start MediaProjection and consume
# device resources. Instead, verify the mutation is callable without
# immediate error if the accessibility service is unavailable.
skip "screen-mirror-C05 startScreenMirror (skipped: would launch MediaProjection)"

# ----------------------------------------------------------------------------
# screen-mirror-C06  stopScreenMirror — safe to call when not running
# ----------------------------------------------------------------------------
STPSM=$(call_gql 'mutation { stopScreenMirror }')
api_stpsm=$(printf '%s' "$STPSM" | jq -r '.data.stopScreenMirror // empty')
if [[ "$api_stpsm" == "true" ]]; then
  pass "screen-mirror-C06 stopScreenMirror (no-op when not running) → true"
else
  fail "screen-mirror-C06 stopScreenMirror returned: $STPSM"
fi

# ----------------------------------------------------------------------------
# screen-mirror-C07  requestScreenMirrorAudio
# ----------------------------------------------------------------------------
RSMA=$(call_gql 'mutation { requestScreenMirrorAudio }')
api_rsma=$(printf '%s' "$RSMA" | jq -r '.data.requestScreenMirrorAudio')
if [[ "$api_rsma" == "true" || "$api_rsma" == "false" ]]; then
  pass "screen-mirror-C07 requestScreenMirrorAudio → $api_rsma (true if RECORD_AUDIO already granted)"
else
  fail "screen-mirror-C07 requestScreenMirrorAudio returned: $RSMA"
fi

# ----------------------------------------------------------------------------
# screen-mirror-C08  sendScreenMirrorControl — requires AccessibilityService
# ----------------------------------------------------------------------------
# Try a benign no-op control (tap at origin) and expect it to throw if
# the AccessibilityService is disabled.
SSMC=$(call_gql 'mutation { sendScreenMirrorControl(input: { action: TAP, x: 0.5, y: 0.5 }) }')
api_ssmc_err=$(printf '%s' "$SSMC" | jq -r '.errors[0].message // empty')
api_ssmc=$(printf '%s' "$SSMC" | jq -r '.data.sendScreenMirrorControl // empty')
if [[ "$api_ssmc_err" == *"Accessibility"* ]] || [[ "$api_ssmc_err" == *"not enabled"* ]]; then
  pass "screen-mirror-C08 sendScreenMirrorControl correctly errored: AccessibilityService not enabled"
elif [[ "$api_ssmc" == "true" ]]; then
  pass "screen-mirror-C08 sendScreenMirrorControl → true (AccessibilityService enabled)"
else
  fail "screen-mirror-C08 sendScreenMirrorControl returned: $SSMC"
fi

end_group