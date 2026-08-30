# SMS/MMS state does not synchronize reliably between Android and web clients

## Tracking

- Issue: https://github.com/plainhub/plain-app/issues/345
- Android/backend PR: https://github.com/plainhub/plain-app/pull/347
- Shared browser/Tauri frontend PR: https://github.com/plainhub/plain-desktop/pull/16

## Describe the bug

SMS/MMS state can diverge between the Android provider and PlainApp's browser/desktop UI. The visible failures are not one isolated rendering defect: the current implementation has no provider-level SMS/MMS change signal, and several independent request, optimistic-state, identity, and MMS parsing defects can overwrite or conceal fresh data.

Observed symptoms include:

1. A message sent from the web UI appears optimistically, then disappears almost immediately.
2. An incoming-message notification arrives in the web UI, but the message does not appear until a full page reload.
3. A message sent from the web UI does not appear on the phone.
4. A thread created on the phone does not appear in the browser/desktop conversation list.
5. Group conversations can be labeled as the current user and display the current user's own phone number.
6. Incoming MMS attachments can appear as `-` or not appear at all.

The browser UI and Tauri desktop application share the `plain-desktop` Vue frontend. The Android app in `plain-app` hosts the GraphQL/WebSocket APIs and packages a built copy of that frontend, so a complete correction spans both repositories.

## Steps to reproduce

### Incoming SMS

1. Open Messages in the PlainApp browser or desktop client.
2. Keep an existing thread open.
3. Receive a new SMS on the connected phone.
4. Observe that a notification can arrive while the thread contents, sidebar snippet, timestamp, ordering, and unread/count state remain stale.
5. Reload the page and observe the missing message appear.

### Phone-created conversation

1. Keep the PlainApp Messages page open.
2. Create and send a new SMS thread from the phone's native messaging application.
3. Wait or switch between existing threads.
4. Observe that the new thread does not appear until a page reload.

### Web-sent SMS

1. Send an SMS from an open PlainApp thread.
2. Observe that the optimistic bubble can disappear after an unchanged provider query.
3. If Android later reports a send failure, observe that the web client still clears the draft and provides no durable failure state.

### Group/MMS identity

1. Open a multi-party MMS/SMS conversation.
2. Observe that only one canonical recipient is returned and used as the conversation identity.
3. If the first address is the active SIM's number, observe that the thread is labeled as the current user.
4. Receive an MMS containing an attachment and observe that downloaded MMS content may be absent.

## Expected behavior

- SMS/MMS provider changes should update the active thread, conversation list, ordering, snippets, counts, contact display, and read state without a reload.
- A WebSocket reconnect should reconcile potentially missed provider changes.
- An optimistic outgoing message should remain visible until it is matched to a provider row or reaches an explicit failure state.
- Android send failures should be reported to the web client; drafts should not be silently lost.
- Group threads should preserve all participants and exclude the user's own number from the display identity when possible.
- Sent MMS and downloaded incoming MMS should both be queryable and render their attachments.
- Concurrent queries, pagination, filters, and delayed refreshes should not allow an older response to overwrite newer state.

## Investigation findings

### 1. There is no provider-driven SMS/MMS synchronization event

This is the primary cross-client synchronization gap.

- `plain-app` does not register a `ContentObserver` for SMS, MMS, MMS parts/addresses, or conversations. Its existing real-time path is based on Android notification-listener events.
- Notification events are not an authoritative change feed: phone-originated sends normally create no qualifying notification, group summaries/ongoing/local notifications are filtered, packages can be absent from the allowlist, and notification delivery can precede the provider commit.
- `WebSocketEvents` has notification events and `MMS_SENT`, but no event representing an external SMS/MMS provider change.
- `plain-desktop` therefore cannot refresh for phone-originated sends/new threads, and its SMS views do not reconcile on WebSocket reconnect.

Relevant code:

- `plain-app/shared/src/commonMain/kotlin/com/ismartcoding/plain/events/WebSocketEvents.kt`
- `plain-app/app/src/main/java/com/ismartcoding/plain/services/PNotificationListenerService.kt`
- `plain-desktop/src/hooks/app-socket.ts`
- `plain-desktop/src/hooks/sms-notification-refresh.ts`

### 2. Optimistic outgoing messages are cleared by unrelated or stale queries

- `plain-desktop/src/hooks/message-thread.ts` stores only one module-level pending SMS.
- Any successful full fetch clears that pending item, even when the provider returns the same list and has not persisted the send.
- Retry logic compares list length rather than message identity, uses uncancelled timers, and is not scoped to the thread that initiated the send.
- Rapid sends overwrite the singleton pending slot.
- Switching threads while a retry is pending can make the old timer query the newly selected thread.

This directly explains a sent bubble appearing and then disappearing.

### 3. Android reports send success before receiving the platform result

- `SmsHelper.sendText()` passes no sent/delivery `PendingIntent` callbacks.
- The GraphQL mutation returns `true` immediately after invoking `SmsManager`.
- Radio, service, carrier, rate-limit, and SIM-selection failures are therefore hidden from the web client.
- The composer clears the draft without awaiting the mutation and has no failure restoration path.

Relevant code:

- `plain-app/shared/src/androidMain/kotlin/com/ismartcoding/plain/features/sms/SmsHelper.kt`
- `plain-app/shared/src/commonMain/kotlin/com/ismartcoding/plain/features/sms/SmsGraphQL.kt`
- `plain-desktop/src/hooks/message-send.ts`

### 4. In-flight request deduplication and mutable response state can restore stale data

- `plain-desktop/src/lib/api/gql-client.ts` deduplicates identical in-flight queries. A refresh triggered after a provider notification can join a request that began before the provider commit, so no post-change request occurs.
- `initLazyQuery` has no request generation/cancellation guard; an older request can complete last and overwrite a newer result.
- Thread pagination decides append/replace using mutable global state at response time rather than immutable request variables.
- The SMS sidebar cache likewise applies responses using the current page/filter rather than the page/filter that produced the response.

These races explain cases where waiting or switching threads still does not reveal a received message.

### 5. Send and refresh paths update different parts of the UI

- The in-thread composer refreshes the open thread but does not emit the same event as the new-message modal.
- The sidebar's local `sms_sent` listener therefore misses normal composer sends.
- Counts, sidebar ordering, and snippets are not consistently refreshed with the active thread.
- New-thread creation relies on one fixed delayed refresh instead of provider confirmation.

### 6. Group participants are truncated to one address

- Both conversation queries split Android's `recipient_ids` and take only the first canonical address.
- The domain and GraphQL models expose only singular `address` data.
- MMS address parsing likewise returns only one candidate.
- The frontend derives conversation display and sometimes send targets from that singular address or the first loaded message.

This makes group identity arbitrary and can select the active SIM's own number. It also risks targeting the wrong participant.

Relevant code:

- `plain-app/shared/src/androidMain/kotlin/com/ismartcoding/plain/features/sms/SmsConversationHelper.kt`
- `plain-app/shared/src/androidMain/kotlin/com/ismartcoding/plain/features/sms/SmsHelper.kt`
- `plain-app/shared/src/commonMain/kotlin/com/ismartcoding/plain/features/sms/DMessageConversation.kt`
- `plain-desktop/src/lib/interfaces.ts`
- `plain-desktop/src/views/messages/MessagesSidebar2.vue`

### 7. Incoming MMS content uses the wrong PDU type filter

The backend uses `m_type IN (128, 130)` and documents `130` as retrieve-conf. Android/AOSP defines:

- `128`: `SEND_REQ`
- `130`: `NOTIFICATION_IND`
- `132`: `RETRIEVE_CONF`

The current predicate includes the notification placeholder and excludes downloaded incoming MMS content. This affects message queries, conversation queries, and counts, and directly explains missing MMS bodies/attachments.

The predicate should be centralized using named constants and include `(128, 132)`. Reference: [AOSP `PduHeaders`](https://android.googlesource.com/platform/packages/apps/Messaging/+/master/src/com/android/messaging/mmslib/pdu/PduHeaders.java).

### 8. MMS completion polling is not correlated to the send

- The Android poller treats any sent MMS row newer than a loose timestamp cutoff as completion.
- Concurrent pending sends can all be completed by one unrelated provider row.
- Timeout emits no terminal failure.
- `MMS_SENT` is mapped by the frontend but is not consumed by the SMS views.

The poller should snapshot the provider ID before launch, match a new row to the operation, and emit exactly one success/failure/cancel/timeout result.

### 9. Contact resolution is lossy and never invalidated

- Phone numbers are reduced to a ten-digit suffix, with the last contact silently winning collisions.
- International numbers, short codes, ambiguous suffixes, and participant lists are not represented safely.
- The module-level contact map is loaded once, so edits on the phone remain stale until reload.

Resolution should prefer an exact normalized number, use a suffix only when unique, support participant arrays, and invalidate on relevant provider/reconnect events.

### 10. Secondary list/cache consistency failures

- Failed sidebar load-more increments the page permanently, so retry skips a page.
- Offset pagination appends without ID deduplication and can duplicate/skip conversations when new rows arrive.
- Archive/unarchive is optimistic without rollback and invalidates only the current cache key.
- Refresh and delayed-send timers outlive view deactivation.
- Parsed text/ID filters exclude MMS; SMS count uses raw query-string checks and can disagree with the returned list.
- Filtered conversation IDs are paginated before the combined SMS/MMS results are chronologically sorted.
- Archived snippets inspect SMS only, so an MMS-last thread can show an older snippet.
- SMS ID collection omits MMS, breaking some bulk/tag operations.

## Existing issue review

- `plainhub/plain-desktop#14` covered the narrower incoming-notification/sidebar refresh defect and was fixed by `plainhub/plain-desktop#15`. It did not add an authoritative Android provider change feed or resolve outgoing, reconnect, group identity, MMS, and concurrency failures.
- `plainhub/plain-app#274` requests broader MMS/group-text support. It overlaps group presentation and attachments, but it does not describe the provider synchronization, optimistic-message disappearance, stale response, or send-acknowledgement defects here.
- No open issue was found that covers this full failure set.

## Implemented correction

### `plain-app`

1. Added a lifecycle-owned Android `ContentObserver` for SMS, MMS, MMS parts/addresses, and conversations. Provider write bursts are coalesced and published as a new additive WebSocket provider-change event.
2. Added correlated, multipart-aware SMS sent callbacks with unique `PendingIntent` identities. Terminal results are persisted, bounded, timestamped, and replayed after a WebSocket reconnect so a disconnect cannot silently lose success or failure.
3. Added equivalent durable MMS terminal-result tracking and replay. MMS sends are serialized around duplicate/overlap detection, correlated to a new provider row by immutable send characteristics, and produce explicit success, timeout, cancellation, or failure results.
4. Corrected MMS content selection to use `SEND_REQ` (`128`) and `RETRIEVE_CONF` (`132`), excluding the notification placeholder (`130`). The same predicate now drives lists, counts, searches, conversation summaries, and archived data.
5. Preserved participant arrays through the Android/domain/GraphQL layers, normalized and deduplicated them, and excluded the active SIM/self number when deriving conversation identity where possible. Existing singular address fields remain for compatibility.
6. Made large SMS/MMS ID filtering safe by validating numeric IDs and generating chunked literal predicates rather than exceeding SQLite bind limits.
7. Added backward-compatible persistent outboxes, bounded retention, terminal timestamps, one-replay-per-WebSocket-session behavior, and cleanup for expired send results.

### `plain-desktop`

1. Consumed the provider-change and SMS/MMS result events across the active thread, conversation list, counts, contact identity, and reconnect reconciliation paths.
2. Replaced the singleton optimistic message with per-client/per-thread pending state and a correlated result ledger. Pending messages survive ordinary refreshes until provider reconciliation or an explicit terminal result; rapid sends and thread switches remain isolated.
3. Started failure deadlines only after mutation acceptance, handled the result-before-HTTP-response race, prevented duplicate submission, validated send targets/SIM selection, and restored SMS/MMS drafts on terminal failure.
4. Added forced post-invalidation fetches, latest-generation guards, and immutable request context so an older in-flight response cannot overwrite a newer provider refresh.
5. Repaired conversation pagination retry/deduplication, cache invalidation, archive rollback, and sidebar/thread refresh parity.
6. Switched conversation display and conservative group-send targeting to authoritative participant metadata, with exact-first and ambiguity-safe contact matching plus invalidation.

Because the backend and shared web client live in separate repositories, this issue will require two linked pull requests rather than one cross-repository PR.

## Regression and validation plan

### Automated validation completed

- `plain-app`: `:shared:testAndroidHostTest --tests com.ismartcoding.plain.features.sms.SmsSyncContractTest` passes. The contract suite covers provider burst coalescing; durable/bounded SMS and MMS result state; replay expiry; multipart and terminal states; PDU filtering; concurrent/overlapping MMS correlation; participant/self resolution; MIME-category matching; cancellation; and predicates containing more than 1,000 IDs.
- `plain-app`: the repository-required `PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH ./gradlew :app:assembleDebug` passes for the F-Droid, GitHub, and Google debug variants.
- `plain-desktop`: 13 focused unit-test files pass with 119 tests passed and 1 skipped. They cover provider/reconnect refresh, newest-query wins, cache behavior, per-thread pending reconciliation, result ordering, send deadlines, draft restoration, pagination, participant/contact resolution, and the result-before-mutation-response race.
- `plain-desktop`: `vue-tsc --noEmit`, ESLint over every changed/new TypeScript and Vue file, and the Vite production build all pass.
- `plain-desktop`: the complete repository run finishes with 479 passed and 52 skipped tests, plus 4 failures in existing unchanged local/window-mode suites (`cross-window-store`, `local-mode`, and `window-client`). The cross-window failure passes in isolation; the three local/window assertions are repeatable environment/configuration failures outside the SMS/MMS changes.

### Real-device validation completed

- Installed PlainApp debug 3.3.10 (`com.ismartcoding.plain.debug`) alongside the existing release 3.3.5 on a Pixel 9 Pro. The release package and its data were left untouched.
- Granted the debug package the SMS, phone-number/state, contacts, notification, and local-network permissions needed by this flow, then enabled PlainApp's read/send-message API access.
- Ran the corrected Android backend on separate debug-only HTTP/HTTPS ports so it could coexist with the release service.
- Completed the browser-to-phone access approval and authenticated the corrected `plain-desktop` development build against the corrected Android backend.
- Queried a real provider dataset containing 145,089 messages and 1,117 conversations. The corrected frontend displayed all 1,117 conversations (50 virtualized rows initially rendered) with no skeleton fallback, console errors, page errors, or failed requests.

### Remaining carrier/manual matrix

Automated validation deliberately did not send a real carrier SMS/MMS or create a conversation in the user's live message history. The following external-stimulus checks remain appropriate before release: receive SMS while a thread is open/closed, originate a new phone-side thread, send browser-to-phone SMS/MMS, reconnect during an in-flight result, verify a known multi-party thread's display identity, exercise multi-SIM selection, and force a carrier send failure. The new unit/contract coverage exercises the corresponding state transitions without mutating live data.

## Environment

- Client: PlainApp browser UI and the shared `plain-desktop` frontend
- Android backend audited: `plainhub/plain-app` `upstream/main` at `78b528156d85a51d2fc064031a4fc00c8df64638`
- Web frontend audited: `plainhub/plain-desktop` `upstream/main` at `26e28a89cbc4c57e68add46770c4c7a5c071e27f`
- Device: Pixel 9 Pro (`caiman`), GrapheneOS on Android 17 / API 37, build `CP2A.260805.005`
