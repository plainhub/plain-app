/**
 * i18n-apply-todo.mjs  (plain-app / Android)
 *
 * Reads scripts/i18n-translated.json and patches each locale strings_*.xml:
 *   - Missing keys are inserted into the correct category file
 *   - Keys that were still English are replaced in-place
 *
 * Android XML escaping rules are applied to the translated values:
 *   apostrophes → \'   ampersands → &amp;   newlines → \n
 *
 * Usage (run from plain-app root):
 *   node scripts/i18n-apply-todo.mjs
 */
import fs from 'node:fs'
import path from 'node:path'

// ── Category rules (mirrors split-strings.mjs) ────────────────────────────────
// Used to determine which strings_*.xml file a new (missing) key belongs to.
const RULES = [
  { file: 'strings_timeago.xml',       re: /^timeago_/ },
  { file: 'strings_photo_exif.xml',    re: /^(flash(_|$)|f_number|exposure_(time|program(_.*)?)|aperture_value|focal_length|iso_speed|metering_mode(_.*)?|white_balance(_.*)?|color_(space|profile)|device_(make|model)|creator|resolution|dimensions|taken_at|bitrate|duration|description)$/ },
  { file: 'strings_apps.xml',          re: /^(apps|launch|uninstall|view_in_settings|source_directory|data_directory|more_info|sdk|installed_at|app_size|user_app|system_app|large_heap|app_type_(user|system))$/ },
  { file: 'strings_tools.xml',         re: /^(sound_meter|decibel_values|pomodoro_timer|short_break(_duration)?|long_break(_duration|_complete)?|tomatoes_today|current_round|work_(duration|time|session_complete)|pomodoros_before_long_break|total_pomodoros|session_complete|time_for_break|back_to_work|pomodoro_(stats|notification_prompt)|round_counter|drag_to_adjust_progress|today_completed|great_job_(short|long)_break|break_complete|time_to_work|ready_for_work|grant_permission_button|sleep_timer_permission_message|play_sound_on_complete|custom_sound|select_sound|checked|min|avg|max)$/ },
  { file: 'strings_permissions.xml',   re: /^(feature_|notification_filter|filter_mode|notification_filter_mode_desc|allowlist_mode|blacklist_mode|allowed_apps|blocked_apps|add_app|system_permission_(granted|not_granted)|open_permission_settings|accessibility_service_(description|label)|storage_permission_confirm|system_alert_window_warning|need_(storage|sms|contact|call)_permission|grant_access|call_phone_permission_required|audio_notification_prompt|foreground_service_notification_prompt|scan_needs_camera_warning|permissions|show_notification)/ },
  { file: 'strings_notes.xml',         re: /^(save_to_notes|note$|editor_settings|show_line_numbers|wrap_content|syntax_highlight|insert_image|image_(url|description)|width|browse|move_to_trash|trash|todo|children_songs|light_music|movie|family|important|inspirations|personal|work|note_sample1)$/ },
  { file: 'strings_feeds.xml',         re: /^(feeds|books_title|subscriptions_title|books|subscriptions|url|rss_url|import_opml_file|export_opml_file|auto_(refresh_feeds|refresh_interval|refresh_only_over_wifi|fetch_full_content(_tips)?)|add_subscription|update_subscription|already_added|sync(ing(_all_feeds)?|ed|_failed)|exported_to|invalid_file|pull_down_to_(fetch_content|sync_(all|current)_feeds)|fetching_content|release_to_(fetch|sync_(all|current)_feeds)|fetch(ed|_failed)|older_than_(7|30)days_feed_items|feed_items_cleared|clear_feed_items|load_full_content|published_at)$/ },
  { file: 'strings_files.xml',         re: /^(files_title|trash_title|file(_size|_name|_path|_save_to)?|files|delete_file(s)?|copy_path|show_hidden_files|add_new_folder|newest_date_first|oldest_date_first|group_by_taken_at|largest_first|smallest_first|create_(folder|file)|open_with(_(other_app))?|get_info|compress|decompress|cannot_(share|open)_|some_files_cannot_be_shared|file_picker_not_available|cannot_(move|copy)_folder_into_itself|text_file_size_limit|(image|video)_save_to(_failed)?|sdcard|internal_storage|usb_storage|folder(s)?|create$)/ },
  { file: 'strings_network.xml',       re: /^(vpn_web_conflict_warning|ssl_certificate_reset|the_token_is_reset|reset_ssl_certificate|device_name|http_(port(_conflict_error(s)?)?|server_(state_(on|starting|stopping|error)|ok|error|diagnostics|failed|stopped))|https_(port|certificate_signature)|change_port|start_service|stop_service|run|troubleshoot|allow_(remote_access_from_pc|new_login)|fix|security|url_token(_tips)?|api_(url|service_is_running)|web_(url|console|service_required_for_chat|how_to|dig)|enable_web_service|require_(password|confirmation)|auth_dev_token_tips|browser_https_error_tips|screen_mirror(_service_is_running|_audio_permission_settings_message)?|sessions|reset_(password|token)|testing_token|token|mdns_hostname(_invalid)?|last_visit_at|enter_this_address_tips|access_phone_web|scan_qrcode_to_access_web|open_in_web|two_factor_auth_tips|instruction_for_use|restart_app_(title|message)|relaunch_app|http_server_failed|http_server_stopped|learn_more|http_port_conflict_error(s)?)$/ },
  { file: 'strings_media.xml',         re: /^(audios_title|videos_title|images_title|pause|play$|change_playback_speed|toggle_audio|picture_in_picture|fullscreen|exit_cast_mode|rotate|select_a_device|cast(_mode|_playlist(_empty)?|$)|casting|keep_screen_on(_confirm)?|add_to_cast_queue|remove_from_cast_queue|sleep_timer$|finish_last_audio|start|stop|video_playlist_no_data|add_videos_to_playlist|pdf_open_error|audio_play_error|image$|images$|video$|audios$|videos$|recents|playlist(_title)?|clear_list|add_to_playlist|play_(selected|all)|favorites|in_playlist|empty_playlist|drag_number_to_reorder|remove_from_playlist|send_message|my_phone|items|file_name|remaining|cancel_timer|start_timer|clear_all(_confirm)?)$/ },
  { file: 'strings_chat.xml',          re: /^(chat(_settings|_files_save_directory|_info|_input_hint)?$|peer(_chat(_reply|_type_reply)?|_id)?$|group_chat|channel(s|_name(_hint)?|_created|_left_notice|_kicked_notice|_invite(_message)?|_members|_delete)?|new_channel|no_channels|rename_channel|manage_members|members$|add_member|remove_member|no_paired_peers_available|delete_channel|leave_channel(_warning)?|decline|pending_(invite|members)|resend_invite|kick_member|delete_(device|peer_warning)|add_device|pair(ing(_request(_message)?)?|_via_qr_(title|desc)|_with_device|_devices_hint$|$)|confirm_pair_with_device|unpair|paired(_devices)?|unpaired(_devices)?|discovered_devices|no_paired_devices|allow$|deny|computer|phone$|tablet$|tv$|forward|sent$|device_type|port$|delivery_status(_summary)?|resend_selected|messages_cleared|clear_messages(_confirm)?|me$|share_message|delete_message|request_to_connect|client_(ua|id)|os$|browser$|confirm_to_send_(file|files)_to_file_assistant|local_chat(_desc)?|bluetooth(_scan_gps_enable_(title|description|confirm))?|qrcode|scanner$|nearby(_devices)?|searching_nearby_devices|no_nearby_devices_found|make_discoverable(_desc)?|discover_nearby_devices|stop_discovering|tap_discover_to_start|make_sure_devices_same_network|show_qr_code|last_seen|device_discovered|link_preview_(image|error)|loading_link_preview|location_permission_should_be_enabled|default_app_directory|connect(ing)?$|password$|confirm$|invalid_password|empty_password|unknown_ip|try_again|added_at|copy_text|edit_text|accept$|reject$|online$|offline$)$/ },
  { file: 'strings_settings.xml',      re: /^(developer_(mode|settings)|dark(_theme)?|amoled_dark_theme|use_device_(theme|language)|primary_color_hint|style|language(_desc)?|about(_desc)?|backup(_restore|_desc)?|local_cache(_cleared)?|clear_cache|checking_updates|rate_limit|check_(failure|update)|auto_check_update(_desc)?|is_latest_version|change_log|update$|get_new_updates(_desc)?|skip_this_version|usb_connect_recommendation|performance|disable_battery_optimization|battery_optimization_disabled|keep_awake(_tips)?|pick_color|generate_password|privacy_policy|terms_of_use|version_name_with_code|optimized_batter_usage_warning|app_settings|settings|keywords|name$|sort(_by_random)?|name_(asc|desc)|ip_address_asc|last_active_desc|app_(name|data|version|restored)|android_version|clear_(history|logs)|no_logs_error|share_logs|features$|customize_home_features|donation(_desc)?|logs$|welcome_text|restart_app_title|restart_app_message|system$|light$|attention$|recommendation$|developer_settings)$/ },
  { file: 'strings_common.xml',        re: /.*/ },
]

function categoryFileForKey(key) {
  for (const { file, re } of RULES) if (re.test(key)) return file
  return 'strings_common.xml'
}
// ── XML encode for Android string values ──────────────────────────────────────
function escapeAndroid(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/'/g, "\\'")
    .replace(/\n/g, '\\n')
}

// ── Apply to a locale directory ─────────────────────────────────────────────
/**
 * Find which strings_*.xml file in `locDir` contains `key`.  Returns the
 * file path if found, otherwise null.
 */
function findFileForKey(locDir, key) {
  for (const f of fs.readdirSync(locDir)) {
    if (!/^strings.*\.xml$/.test(f)) continue
    const filePath = path.join(locDir, f)
    const xml = fs.readFileSync(filePath, 'utf8')
    if (new RegExp(`name="${key}"`).test(xml)) return filePath
  }
  return null
}

function applyToDir(locDir, items) {
  let changed = 0

  const toReplace = items.filter((i) => i.src === 'english')
  const toInsert  = items.filter((i) => i.src === 'missing')

  // 1. Replace existing (English-value) entries in whichever file holds the key
  for (const { key, translated: t } of toReplace) {
    const filePath = findFileForKey(locDir, key)
    if (!filePath) { console.warn(`  [warn] key "${key}" not found in ${locDir}`) ; continue }
    let xml = fs.readFileSync(filePath, 'utf8')
    const escaped = escapeAndroid(t)
    const re = new RegExp(`([ \\t]*<string\\s+name="${key}"[^>]*>)[^<]*(</string>)`, 'g')
    const updated = xml.replace(re, `$1${escaped}$2`)
    if (updated !== xml) { fs.writeFileSync(filePath, updated, 'utf8') ; changed++ }
  }

  // 2. Insert missing keys into the appropriate category file
  //    Group by target file to minimise file reads/writes
  const byFile = new Map()
  for (const item of toInsert) {
    const targetFile = path.join(locDir, categoryFileForKey(item.key))
    if (!byFile.has(targetFile)) byFile.set(targetFile, [])
    byFile.get(targetFile).push(item)
  }

  for (const [filePath, fileItems] of byFile) {
    // Create the file with a minimal skeleton if it doesn't exist yet
    if (!fs.existsSync(filePath)) {
      fs.writeFileSync(filePath, '<?xml version="1.0" encoding="utf-8"?>\n<resources xmlns:tools="http://schemas.android.com/tools">\n</resources>\n', 'utf8')
    }
    let xml = fs.readFileSync(filePath, 'utf8')
    const insertLines = fileItems
      .map(({ key, translated: t }) => `    <string name="${key}">${escapeAndroid(t)}</string>`)
      .join('\n')
    xml = xml.replace(/(\s*<\/resources>)/, `\n${insertLines}\n$1`)
    fs.writeFileSync(filePath, xml, 'utf8')
    changed += fileItems.length
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
  const locDir = path.join(resDir, dir)
  if (!fs.existsSync(locDir)) { console.warn(`  Skip ${dir} – directory not found`) ; continue }

  const applied = applyToDir(locDir, items)
  if (applied > 0) {
    console.log(`[${dir}] applied ${applied} translations`)
    totalApplied += applied
    totalFiles++
  }
}

console.log(`\n✓ ${totalApplied} keys applied across ${totalFiles} files`)
