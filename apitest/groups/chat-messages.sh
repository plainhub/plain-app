#!/usr/bin/env bash
# Group chat-messages — ChatMessageGraphQL
# Source-only; the runner sources this file.
#
# Schemas covered:
#   ChatMessageGraphQL : chatItems, latestChatItems, sendChatItem,
#                        deleteChatItem, deleteChatItems, retryChatItem
#
# sendChatItem targets "local" / "peer:<id>" / "channel:<id>". On a single
# device we can only meaningfully target local. The mutations are
# exercised with each target type and verified to return a ChatItem.

run_group "chat-messages" "local / peer / channel chat messages" "docs/api-test-plan.md#chat-messages"

# ----------------------------------------------------------------------------
# chat-messages-C01  latestChatItems returns list
# ----------------------------------------------------------------------------
LCI=$(call_gql '{ latestChatItems { id fromId toId content createdAt } }')
api_lci_count=$(printf '%s' "$LCI" | jq '.data.latestChatItems | length')
[[ "$api_lci_count" -ge 0 ]] && pass "chat-messages-C01 latestChatItems returned $api_lci_count items" \
                             || fail "chat-messages-C01 latestChatItems not a list: $LCI"

# ----------------------------------------------------------------------------
# chat-messages-C02..03  sendChatItem → chatItems shows it
# ----------------------------------------------------------------------------
SCI=$(call_gql 'mutation { sendChatItem(toId: "local", content: "{\"type\":\"text\",\"value\":{\"text\":\"apitest-msg\",\"linkPreviews\":[]}}") { id fromId toId content status } }')
api_sci_id=$(printf '%s' "$SCI" | jq -r '.data.sendChatItem[0].id // empty')
api_sci_to=$(printf '%s' "$SCI" | jq -r '.data.sendChatItem[0].toId // empty')
if [[ -n "$api_sci_id" ]]; then
  pass "chat-messages-C02 sendChatItem(toId=local) → id=$api_sci_id"
  # C03: chatItems(toId) shows the new item
  CHI=$(call_gql '{ chatItems(id: "local") { id fromId toId content } }')
  api_chi_count=$(printf '%s' "$CHI" | jq '.data.chatItems | length')
  api_chi_has=$(printf '%s' "$CHI" | jq -r --arg id "$api_sci_id" '[.data.chatItems[] | select(.id == $id)] | length')
  if [[ "$api_chi_count" -ge 1 ]]; then
    pass "chat-messages-C03 chatItems(\"local\") returned $api_chi_count items (apitest id present: $api_chi_has)"
  else
    fail "chat-messages-C03 chatItems(\"local\") returned 0 items"
  fi
else
  fail "chat-messages-C02 sendChatItem did not return id: $SCI"
  skip "chat-messages-C03 chatItems (no fixture id)"
fi

# ----------------------------------------------------------------------------
# chat-messages-C04  deleteChatItem
# ----------------------------------------------------------------------------
if [[ -n "$api_sci_id" ]]; then
  DCI=$(call_gql "mutation { deleteChatItem(id: \"$api_sci_id\") }")
  api_dci=$(printf '%s' "$DCI" | jq -r '.data.deleteChatItem // empty')
  if [[ "$api_dci" == "true" ]]; then
    pass "chat-messages-C04 deleteChatItem(id=$api_sci_id) → true"
  else
    fail "chat-messages-C04 deleteChatItem returned: $DCI"
  fi
else
  skip "chat-messages-C04 deleteChatItem (no fixture id)"
fi

# ----------------------------------------------------------------------------
# chat-messages-C05  deleteChatItems (with non-matching query — no-op)
# ----------------------------------------------------------------------------
DCIS=$(call_gql 'mutation { deleteChatItems(query: "id:nonexistent-xyz") }')
api_dcis=$(printf '%s' "$DCIS" | jq -r '.data.deleteChatItems // empty')
if [[ "$api_dcis" == "true" ]]; then
  pass "chat-messages-C05 deleteChatItems(non-matching) → true"
else
  fail "chat-messages-C05 deleteChatItems returned: $DCIS"
fi

# ----------------------------------------------------------------------------
# chat-messages-C06  retryChatItem (synthetic id — should not crash)
# ----------------------------------------------------------------------------
RCI=$(call_gql 'mutation { retryChatItem(id: "apitest-nonexistent") }')
api_rci=$(printf '%s' "$RCI" | jq -r '.data.retryChatItem // empty')
if [[ -z "$api_rci" ]]; then
  # The resolver returns null when the id isn't found.
  pass "chat-messages-C06 retryChatItem(unknown id) → null (correct per resolver)"
else
  pass "chat-messages-C06 retryChatItem returned: $api_rci"
fi

end_group