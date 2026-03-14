#!/usr/bin/env node
/**
 * split-strings.mjs  (plain-app / Android)
 *
 * Splits the monolithic strings.xml into focused category files,
 * each ≤ 150 lines, to reduce AI token usage and improve readability.
 *
 * Usage (run from plain-app root):
 *   node scripts/split-strings.mjs           # apply changes
 *   node scripts/split-strings.mjs --dry-run # preview only, no writes
 *
 * Generated files (in values/ and every values-XX/ locale dir):
 *   strings_common.xml       – Generic shared UI strings
 *   strings_timeago.xml      – Relative time strings (timeago_*)
 *   strings_photo_exif.xml   – Camera EXIF metadata
 *   strings_apps.xml         – App management (install/uninstall/info)
 *   strings_tools.xml        – Pomodoro, sound meter, sleep timer
 *   strings_permissions.xml  – Permission & notification feature strings
 *   strings_notes.xml        – Notes & markdown editor
 *   strings_feeds.xml        – RSS feeds & subscriptions
 *   strings_files.xml        – File management & storage
 *   strings_network.xml      – HTTP server, ports, web interface
 *   strings_media.xml        – Audio/video player, cast, playlist
 *   strings_chat.xml         – Chat, channels, pairing, Bluetooth/nearby
 *   strings_settings.xml     – App settings, theme, updates, about
 *
 * The original strings.xml is removed after successful split.
 */

import fs from 'node:fs'
import path from 'node:path'

const DRY_RUN = process.argv.includes('--dry-run')
const __dirname = path.dirname(new URL(import.meta.url).pathname)
const RES_DIR = path.join(__dirname, '../app/src/main/res')

// ─── Category definitions (ordered; first match wins) ────────────────────────
// Each rule has a file name and a regex tested against the resource key.
// Keys not matched by any rule fall through to strings_common.xml.

const RULES = [
  // Relative time strings
  {
    file: 'strings_timeago.xml',
    title: 'Relative time (timeago)',
    re: /^timeago_/,
  },

  // Camera / photo EXIF metadata
  {
    file: 'strings_photo_exif.xml',
    title: 'Photo EXIF metadata',
    re: /^(flash(_|$)|f_number|exposure_(time|program(_.*)?)|aperture_value|focal_length|iso_speed|metering_mode(_.*)?|white_balance(_.*)?|color_(space|profile)|device_(make|model)|creator|resolution|dimensions|taken_at|bitrate|duration|description)$/,
  },

  // Installed-app management
  {
    file: 'strings_apps.xml',
    title: 'Apps management',
    re: /^(apps|launch|uninstall|view_in_settings|source_directory|data_directory|more_info|sdk|installed_at|app_size|user_app|system_app|large_heap|app_type_(user|system))$/,
  },

  // Productivity tools: Pomodoro, sound meter, sleep timer
  {
    file: 'strings_tools.xml',
    title: 'Productivity tools (Pomodoro / sound meter / sleep timer)',
    re: /^(sound_meter|decibel_values|pomodoro_timer|short_break(_duration)?|long_break(_duration|_complete)?|tomatoes_today|current_round|work_(duration|time|session_complete)|pomodoros_before_long_break|total_pomodoros|session_complete|time_for_break|back_to_work|pomodoro_(stats|notification_prompt)|round_counter|drag_to_adjust_progress|today_completed|great_job_(short|long)_break|break_complete|time_to_work|ready_for_work|grant_permission_button|sleep_timer_permission_message|play_sound_on_complete|custom_sound|select_sound|checked|min|avg|max)$/,
  },

  // Android permission & notification strings
  {
    file: 'strings_permissions.xml',
    title: 'Permissions & notifications',
    re: /^(feature_|notification_filter|filter_mode|notification_filter_mode_desc|allowlist_mode|blacklist_mode|allowed_apps|blocked_apps|add_app|system_permission_(granted|not_granted)|open_permission_settings|accessibility_service_(description|label)|storage_permission_confirm|system_alert_window_warning|need_(storage|sms|contact|call)_permission|grant_access|call_phone_permission_required|audio_notification_prompt|foreground_service_notification_prompt|scan_needs_camera_warning|permissions|show_notification)/,
  },

  // Notes & markdown editor
  {
    file: 'strings_notes.xml',
    title: 'Notes & markdown editor',
    re: /^(save_to_notes|note$|editor_settings|show_line_numbers|wrap_content|syntax_highlight|insert_image|image_(url|description)|width|browse|move_to_trash|trash|todo|children_songs|light_music|movie|family|important|inspirations|personal|work|note_sample1)$/,
  },

  // RSS feeds & subscriptions
  {
    file: 'strings_feeds.xml',
    title: 'RSS feeds & subscriptions',
    re: /^(feeds|books_title|subscriptions_title|books|subscriptions|url|rss_url|import_opml_file|export_opml_file|auto_(refresh_feeds|refresh_interval|refresh_only_over_wifi|fetch_full_content(_tips)?)|add_subscription|update_subscription|already_added|sync(ing(_all_feeds)?|ed|_failed)|exported_to|invalid_file|pull_down_to_(fetch_content|sync_(all|current)_feeds)|fetching_content|release_to_(fetch|sync_(all|current)_feeds)|fetch(ed|_failed)|older_than_(7|30)days_feed_items|feed_items_cleared|clear_feed_items|load_full_content|published_at)$/,
  },

  // File management & storage
  {
    file: 'strings_files.xml',
    title: 'File management & storage',
    re: /^(files_title|trash_title|file(_size|_name|_path|_save_to)?|files|delete_file(s)?|copy_path|show_hidden_files|add_new_folder|newest_date_first|oldest_date_first|group_by_taken_at|largest_first|smallest_first|create_(folder|file)|open_with(_(other_app))?|get_info|compress|decompress|cannot_(share|open)_|some_files_cannot_be_shared|file_picker_not_available|cannot_(move|copy)_folder_into_itself|text_file_size_limit|(image|video)_save_to(_failed)?|sdcard|internal_storage|usb_storage|folder(s)?|create$)/,
  },

  // HTTP server, ports, web console, network
  {
    file: 'strings_network.xml',
    title: 'HTTP server & network',
    re: /^(vpn_web_conflict_warning|ssl_certificate_reset|the_token_is_reset|reset_ssl_certificate|device_name|http_(port(_conflict_error(s)?)?|server_(state_(on|starting|stopping|error)|ok|error|diagnostics|failed|stopped))|https_(port|certificate_signature)|change_port|start_service|stop_service|run|troubleshoot|allow_(remote_access_from_pc|new_login)|fix|security|url_token(_tips)?|api_(url|service_is_running)|web_(url|console|service_required_for_chat|how_to|dig)|enable_web_service|require_(password|confirmation)|auth_dev_token_tips|browser_https_error_tips|screen_mirror(_service_is_running|_audio_permission_settings_message)?|sessions|reset_(password|token)|testing_token|token|mdns_hostname(_invalid)?|last_visit_at|enter_this_address_tips|access_phone_web|scan_qrcode_to_access_web|open_in_web|two_factor_auth_tips|instruction_for_use|restart_app_(title|message)|relaunch_app|http_server_failed|http_server_stopped|learn_more|http_port_conflict_error(s)?)$/,
  },

  // Audio / video player, cast, playlist
  {
    file: 'strings_media.xml',
    title: 'Media player, cast & playlist',
    re: /^(audios_title|videos_title|images_title|pause|play$|change_playback_speed|toggle_audio|picture_in_picture|fullscreen|exit_cast_mode|rotate|select_a_device|cast(_mode|_playlist(_empty)?|$)|casting|keep_screen_on(_confirm)?|add_to_cast_queue|remove_from_cast_queue|sleep_timer$|finish_last_audio|start|stop|video_playlist_no_data|add_videos_to_playlist|pdf_open_error|audio_play_error|image$|images$|video$|audios$|videos$|recents|playlist(_title)?|clear_list|add_to_playlist|play_(selected|all)|favorites|in_playlist|empty_playlist|drag_number_to_reorder|remove_from_playlist|send_message|my_phone|items|in_playlist|empty_playlist|drag_number_to_reorder|remove_from_playlist|file_name|remaining|cancel_timer|start_timer|clear_all(_confirm)?)$/,
  },

  // Chat, peer chat, channels, pairing, Bluetooth/nearby
  {
    file: 'strings_chat.xml',
    title: 'Chat, channels, pairing & nearby devices',
    re: /^(chat(_settings|_files_save_directory|_info|_input_hint)?$|peer(_chat(_reply|_type_reply)?|_id)?$|group_chat|channel(s|_name(_hint)?|_created|_left_notice|_kicked_notice|_invite(_message)?|_members|_delete)?|new_channel|no_channels|rename_channel|manage_members|members$|add_member|remove_member|no_paired_peers_available|delete_channel|leave_channel(_warning)?|decline|pending_(invite|members)|resend_invite|kick_member|delete_(device|peer_warning)|add_device|pair(ing(_request(_message)?)?|_via_qr_(title|desc)|_with_device|_devices_hint$|$)|confirm_pair_with_device|unpair|paired(_devices)?|unpaired(_devices)?|discovered_devices|no_paired_devices|allow$|deny|computer|phone$|tablet$|tv$|forward|sent$|device_type|port$|delivery_status(_summary)?|resend_selected|messages_cleared|clear_messages(_confirm)?|me$|share_message|delete_message|request_to_connect|client_(ua|id)|os$|browser$|confirm_to_send_(file|files)_to_file_assistant|local_chat(_desc)?|bluetooth(_scan_gps_enable_(title|description|confirm))?|qrcode|scanner$|nearby(_devices)?|searching_nearby_devices|no_nearby_devices_found|make_discoverable(_desc)?|discover_nearby_devices|stop_discovering|tap_discover_to_start|make_sure_devices_same_network|show_qr_code|last_seen|device_discovered|link_preview_(image|error)|loading_link_preview|location_permission_should_be_enabled|default_app_directory|connect(ing)?$|password$|confirm$|invalid_password|empty_password|unknown_ip|try_again|added_at|copy_text|edit_text|accept$|reject$|online$|offline$)$/,
  },

  // App settings, theme, language, about, update checker
  {
    file: 'strings_settings.xml',
    title: 'App settings, theme, language & about',
    re: /^(developer_(mode|settings)|dark(_theme)?|amoled_dark_theme|use_device_(theme|language)|primary_color_hint|style|language(_desc)?|about(_desc)?|backup(_restore|_desc)?|local_cache(_cleared)?|clear_cache|checking_updates|rate_limit|check_(failure|update)|auto_check_update(_desc)?|is_latest_version|change_log|update$|get_new_updates(_desc)?|skip_this_version|usb_connect_recommendation|performance|disable_battery_optimization|battery_optimization_disabled|keep_awake(_tips)?|pick_color|generate_password|privacy_policy|terms_of_use|version_name_with_code|optimized_batter_usage_warning|app_settings|settings|keywords|name$|sort(_by_random)?|name_(asc|desc)|ip_address_asc|last_active_desc|app_(name|data|version|restored)|android_version|clear_(history|logs)|no_logs_error|share_logs|features$|customize_home_features|donation(_desc)?|logs$|welcome_text|restart_app_title|restart_app_message|system$|light$|attention$|recommendation$|app_size|developer_settings)$/,
  },

  // Everything else → common
  { file: 'strings_common.xml', title: 'Common / shared UI strings', re: /.*/ },
]

// ─── XML parsing ─────────────────────────────────────────────────────────────

/**
 * Parse a strings.xml file into an ordered list of resource entries.
 * Handles <string>, <string-array>, <plurals>, and XML comments.
 */
function parseResourceFile(filePath) {
  const xml = fs.readFileSync(filePath, 'utf8')
  const entries = [] // { name, raw, type }

  // We do a streaming character scan to correctly handle multi-line elements.
  let i = 0
  const len = xml.length

  while (i < len) {
    // Skip whitespace / newlines between top-level elements
    if (/\s/.test(xml[i])) { i++; continue }

    // Match XML declaration or <resources> opening tag
    if (xml.startsWith('<?xml', i) || xml.startsWith('<resources', i)) {
      const end = xml.indexOf('>', i)
      i = end + 1
      continue
    }

    // Skip closing </resources>
    if (xml.startsWith('</resources>', i)) {
      i += '</resources>'.length
      continue
    }

    // XML comment
    if (xml.startsWith('<!--', i)) {
      const end = xml.indexOf('-->', i)
      if (end === -1) break
      i = end + 3
      continue
    }

    // Resource element: collect the full raw element
    if (xml[i] === '<') {
      // Determine element type and name
      const tagMatch = xml.slice(i).match(/^<(string|string-array|plurals)\s+[^>]*name="([^"]+)"/)
      if (!tagMatch) {
        // Unknown tag — skip to end of tag
        const end = xml.indexOf('>', i)
        if (end === -1) break
        i = end + 1
        continue
      }

      const type = tagMatch[1]
      const name = tagMatch[2]

      let raw
      if (type === 'string') {
        // Self-closing or single-line: try on same "line" first
        const closeTag = '</string>'
        const closeIdx = xml.indexOf(closeTag, i)
        if (closeIdx === -1) break
        raw = xml.slice(i, closeIdx + closeTag.length)
        i = closeIdx + closeTag.length
      } else {
        // Multi-line element (string-array, plurals): find closing tag
        const closeTag = `</${type}>`
        const closeIdx = xml.indexOf(closeTag, i)
        if (closeIdx === -1) break
        raw = xml.slice(i, closeIdx + closeTag.length)
        i = closeIdx + closeTag.length
      }

      entries.push({ name, type, raw: raw.trim() })
      continue
    }

    i++
  }

  return entries
}

// ─── Category assignment ─────────────────────────────────────────────────────

function assignCategory(name) {
  for (const rule of RULES) {
    if (rule.re.test(name)) return rule.file
  }
  return 'strings_common.xml' // should never reach here (last rule is /.*/)
}

// ─── XML file builder ─────────────────────────────────────────────────────────

const FILE_HEADER = `<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">`
const FILE_FOOTER = `</resources>`

function buildXml(entries) {
  if (entries.length === 0) return null
  const body = entries.map(e => `    ${e.raw}`).join('\n')
  return `${FILE_HEADER}\n${body}\n${FILE_FOOTER}\n`
}

// ─── Process a single values directory ───────────────────────────────────────

function processValuesDir(valuesDir) {
  const srcFile = path.join(valuesDir, 'strings.xml')
  if (!fs.existsSync(srcFile)) return

  const entries = parseResourceFile(srcFile)
  if (entries.length === 0) {
    console.warn(`  ⚠  No string entries found in ${srcFile}`)
    return
  }

  // Group entries by category file
  const groups = new Map()
  for (const rule of RULES) groups.set(rule.file, [])

  for (const entry of entries) {
    const cat = assignCategory(entry.name)
    groups.get(cat).push(entry)
  }

  let totalWritten = 0
  const summary = []

  for (const [file, fileEntries] of groups) {
    if (fileEntries.length === 0) continue
    const xml = buildXml(fileEntries)
    if (!xml) continue

    const destPath = path.join(valuesDir, file)
    const lineCount = xml.split('\n').length

    summary.push(`    ${file.padEnd(30)} ${fileEntries.length} strings  (${lineCount} lines)`)

    if (lineCount > 150) {
      console.warn(`  ⚠  ${file} has ${lineCount} lines (> 150). Consider splitting further.`)
    }

    if (!DRY_RUN) {
      fs.writeFileSync(destPath, xml, 'utf8')
    }
    totalWritten += fileEntries.length
  }

  // Verify all entries were placed
  if (totalWritten !== entries.length) {
    console.error(`  ✗  Entry count mismatch: input=${entries.length} output=${totalWritten}`)
    return
  }

  // Remove original strings.xml
  if (!DRY_RUN) {
    fs.unlinkSync(srcFile)
  }

  const dirName = path.relative(RES_DIR, valuesDir)
  console.log(`\n  ${dirName}/  (${entries.length} total strings)`)
  for (const line of summary) console.log(line)
  if (DRY_RUN) console.log('  [dry-run: no files written]')
}

// ─── Main ────────────────────────────────────────────────────────────────────

function main() {
  if (DRY_RUN) console.log('=== DRY RUN – no files will be written ===\n')

  const valuesDirs = [path.join(RES_DIR, 'values')]

  // Add all locale directories
  for (const entry of fs.readdirSync(RES_DIR, { withFileTypes: true })) {
    if (entry.isDirectory() && entry.name.startsWith('values-')) {
      valuesDirs.push(path.join(RES_DIR, entry.name))
    }
  }

  let processed = 0
  for (const dir of valuesDirs) {
    const srcFile = path.join(dir, 'strings.xml')
    if (!fs.existsSync(srcFile)) continue
    processValuesDir(dir)
    processed++
  }

  console.log(`\n✓  Processed ${processed} directories.`)

  if (!DRY_RUN) {
    console.log('\nNext step: run the i18n pipeline to verify all locales are consistent:')
    console.log('  node scripts/i18n-find-untranslated.mjs')
  }
}

main()
