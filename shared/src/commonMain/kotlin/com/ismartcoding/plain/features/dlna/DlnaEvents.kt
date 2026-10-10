package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.events.HEvent

/**
 * A DLNA cast has arrived and is being handled — auto-accepted for known
 * senders, or promoted to the confirmation dialog for unknown senders.
 *
 * The Android main activity listens for this event to bring the app to the
 * foreground when a cast arrives while the app is in the background (the same
 * pattern [com.ismartcoding.plain.events.HConfirmToAcceptLoginEvent] uses), so
 * playback starts immediately instead of waiting for the user to reopen the
 * app.
 */
class HDlnaCastRequestEvent : HEvent()
