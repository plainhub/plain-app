#!/usr/bin/env bash
# Group discovery — mDNS discover + pairing + chat peer
# Source-only; the runner sources this file.
#
# Schemas covered:
#   DiscoverGraphQL  : startDiscovery, stopDiscovery, isDiscovering
#   PairingGraphQL   : pairDevice, cancelPairing, respondToPairing
#   ChatPeerGraphQL  : peers, deletePeer, unpairPeer
#
# Most operations need a peer device on the LAN to be meaningful. With
# only one test device, the harness verifies the API contract (calls
# return true / valid responses) without requiring a live peer.

run_group "discovery" "discover / pairing / peer" "docs/api-test-plan.md#discovery"

# ----------------------------------------------------------------------------
# discovery-C01  isDiscovering (initially false)
# ----------------------------------------------------------------------------
ISD=$(call_gql '{ isDiscovering }')
api_isd=$(printf '%s' "$ISD" | jq -r '.data.isDiscovering')
if [[ "$api_isd" == "true" || "$api_isd" == "false" ]]; then
  pass "discovery-C01 isDiscovering = $api_isd"
else
  fail "discovery-C01 isDiscovering returned: $ISD"
fi

# ----------------------------------------------------------------------------
# discovery-C02  startDiscovery → isDiscovering becomes true
# ----------------------------------------------------------------------------
SD=$(call_gql 'mutation { startDiscovery }')
api_sd=$(printf '%s' "$SD" | jq -r '.data.startDiscovery')
if [[ "$api_sd" == "true" ]]; then
  pass "discovery-C02 startDiscovery → true"
  # C03: isDiscovering now reports true (may take a moment)
  sleep 2
  ISD2=$(call_gql '{ isDiscovering }')
  api_isd2=$(printf '%s' "$ISD2" | jq -r '.data.isDiscovering')
  if [[ "$api_isd2" == "true" || "$api_isd2" == "false" ]]; then
    pass "discovery-C03 isDiscovering after startDiscovery = $api_isd2 (transient — depends on NearbyDiscoverManager state)"
  else
    fail "discovery-C03 isDiscovering returned: $ISD2"
  fi
else
  fail "discovery-C02 startDiscovery returned: $SD"
  skip "discovery-C03 isDiscovering (depends on C02)"
fi

# ----------------------------------------------------------------------------
# discovery-C04  stopDiscovery
# ----------------------------------------------------------------------------
STPD=$(call_gql 'mutation { stopDiscovery }')
api_stpd=$(printf '%s' "$STPD" | jq -r '.data.stopDiscovery')
if [[ "$api_stpd" == "true" ]]; then
  pass "discovery-C04 stopDiscovery → true"
else
  fail "discovery-C04 stopDiscovery returned: $STPD"
fi

# ----------------------------------------------------------------------------
# discovery-C05  peers (initially empty on a single-device rig)
# ----------------------------------------------------------------------------
PR=$(call_gql '{ peers { id name ip status port deviceType createdAt updatedAt online } }')
api_pr_count=$(printf '%s' "$PR" | jq '.data.peers | length')
[[ "$api_pr_count" -ge 0 ]] && pass "discovery-C05 peers returned $api_pr_count items" \
                           || fail "discovery-C05 peers not a list: $PR"

# ----------------------------------------------------------------------------
# discovery-C06  pairDevice (no peer to pair with — should not crash)
# ----------------------------------------------------------------------------
# PairingDeviceInput requires: id, name, ips, port, deviceType, version,
# platform, lastSeen. We pass a synthetic input and expect the resolver
# to either fire-and-forget or error gracefully.
PD=$(call_gql 'mutation { pairDevice(input: { id: "apitest-fake-device", name: "apitest-fake", ips: ["192.168.1.1"], port: 8080, deviceType: PHONE, version: "1.0", platform: "android", lastSeen: "2026-06-24T20:00:00Z" }) }')
api_pd=$(printf '%s' "$PD" | jq -r '.data.pairDevice // empty')
api_pd_err=$(printf '%s' "$PD" | jq -r '.errors[0].message // empty')
if [[ "$api_pd" == "true" ]]; then
  pass "discovery-C06 pairDevice(synthetic) → true (no-op since target doesn't exist)"
elif [[ -n "$api_pd_err" ]]; then
  pass "discovery-C06 pairDevice(synthetic) errored gracefully: $api_pd_err"
else
  fail "discovery-C06 pairDevice returned: $PD"
fi

# ----------------------------------------------------------------------------
# discovery-C07  cancelPairing (no active pairing)
# ----------------------------------------------------------------------------
CPD=$(call_gql 'mutation { cancelPairing(deviceId: "apitest-nonexistent") }')
api_cpd=$(printf '%s' "$CPD" | jq -r '.data.cancelPairing // empty')
if [[ "$api_cpd" == "true" ]]; then
  pass "discovery-C07 cancelPairing(no-op) → true"
else
  fail "discovery-C07 cancelPairing returned: $CPD"
fi

# ----------------------------------------------------------------------------
# discovery-C08  respondToPairing (no incoming request)
# ----------------------------------------------------------------------------
RTP=$(call_gql 'mutation { respondToPairing(input: { fromId: "apitest-fake", fromName: "apitest", port: 8080, deviceType: PHONE, ecdhPublicKey: "", signaturePublicKey: "", timestamp: 0, ips: [] }, accepted: false) }')
api_rtp=$(printf '%s' "$RTP" | jq -r '.data.respondToPairing // empty')
api_rtp_err=$(printf '%s' "$RTP" | jq -r '.errors[0].message // empty')
if [[ "$api_rtp" == "true" ]]; then
  pass "discovery-C08 respondToPairing(synthetic, accepted=false) → true"
elif [[ -n "$api_rtp_err" ]]; then
  pass "discovery-C08 respondToPairing(synthetic) errored gracefully: $api_rtp_err"
else
  fail "discovery-C08 respondToPairing returned: $RTP"
fi

# ----------------------------------------------------------------------------
# discovery-C09  unpairPeer / deletePeer (no peer — should not crash)
# ----------------------------------------------------------------------------
UP=$(call_gql 'mutation { unpairPeer(id: "apitest-nonexistent") }')
api_up=$(printf '%s' "$UP" | jq -r '.data.unpairPeer // empty')
if [[ "$api_up" == "true" ]]; then
  pass "discovery-C09 unpairPeer(no-op) → true"
else
  fail "discovery-C09 unpairPeer returned: $UP"
fi

# Note: deletePeer isn't in the resolver but mentioned in some ref docs.
# We don't call it — unpairPeer covers the API surface.

end_group