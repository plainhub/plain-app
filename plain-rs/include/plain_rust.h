#ifndef PLAIN_RUST_H
#define PLAIN_RUST_H
#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>
typedef struct { uint8_t *data; size_t len; int32_t status; } PlainBleBuffer;
uint64_t plain_ble_new(void);
uint64_t plain_ble_encoder(uint32_t id, bool response, uint32_t limit, const uint8_t *data, size_t len);
void plain_ble_free(uint64_t handle);
uint64_t plain_ble_info(uint64_t handle);
PlainBleBuffer plain_ble_call(int32_t action, uint64_t handle, uint32_t id, uint32_t sequence, uint32_t limit, bool response, const uint8_t *data, size_t len);
void plain_ble_buffer_free(PlainBleBuffer buffer);

void plain_prefs_string_free(char *pointer);


char *plain_core_start(const char *data_dir, const char *token, const char *config_json);
void plain_core_stop(void);
char *plain_http_stop(void);
char *plain_graphql(const char *request);
char *plain_encrypt_http_request(const char *token, const char *payload);
char *plain_encrypt_resource_reference(const char *token, const char *payload);
char *plain_decrypt_http_response(const char *token, const char *endpoint, const char *request, const char *response);
char *plain_graphql_subscription_open(const char *target);
char *plain_graphql_subscription_send(uint64_t handle, const char *message);
char *plain_graphql_subscription_read(uint64_t handle);
char *plain_graphql_subscription_close(uint64_t handle);
char *plain_upload_chunk(const char *id, int64_t index, const uint8_t *bytes, size_t len);
char *plain_resource_open(const char *id);
char *plain_resource_file(const char *id, int32_t fd, int64_t offset, int64_t length);
char *plain_resource_write(const char *id, const uint8_t *bytes, size_t len);
char *plain_resource_close(const char *id, bool failed);
char *plain_report_pairing_response(const char *payload);
char *plain_report_notification(const char *payload);
char *plain_report_image_catalog_changed(const char *payload);
char *plain_report_network_interfaces_changed(const char *payload);
char *plain_report_ble_advertisement(const char *payload);
char *plain_report_ble_scan_state(const char *payload);
char *plain_report_ble_advertising_facts(const char *payload);
char *plain_report_screen_mirror_state(const char *payload);
char *plain_report_screen_mirror_codec(const char *payload);
char *plain_report_screen_mirror_audio_granted(const char *payload);
char *plain_report_sms_provider_changed(const char *payload);
char *plain_report_sms_part_sent(const char *payload);
char *plain_report_clipboard(const char *payload);
char *plain_report_permissions(const char *payload);
char *plain_report_dlna_position(const char *payload);
char *plain_report_dlna_playback(const char *payload);
char *plain_report_dlna_seek_consumed(const char *payload);
char *plain_report_dlna_stopped(const char *payload);
char *plain_report_audio_started(const char *payload);
char *plain_report_audio_completed(const char *payload);
char *plain_report_audio_progress(const char *payload);
char *plain_report_screen_mirror_video(const uint8_t *data, size_t len);
char *plain_report_screen_mirror_audio(const uint8_t *data, size_t len);
char *plain_host_next(void);
char *plain_host_reply(const char *message);
char *plain_submit_thumbnail(const char *token, const uint8_t *bytes, size_t len);
char *plain_grant_background_execution(const char *id, int64_t deadline_ms);
char *plain_revoke_background_execution(const char *id);
char *plain_execution_state_changed(void);

char *plain_restart_http_server(void);
// BLE bytes on success (status 1), UTF-8 error bytes on failure (status -1).
typedef struct { uint8_t *data; size_t len; int32_t status; } PlainBleIncomingBuffer;
PlainBleIncomingBuffer plain_ble_incoming(const char *remote_host, const char *characteristic_uuid, const uint8_t *bytes, size_t len);
void plain_ble_incoming_buffer_free(PlainBleIncomingBuffer buffer);
char *plain_check_http(void);
// Returns raw status JSON or NULL with an error. Free both strings with plain_prefs_string_free.
char *plain_http_status(char **error);
char *plain_lan_ipv4_addresses(void);
char *plain_tls_certificate_signature(void);
char *plain_regenerate_tls_certificate(void);
char *plain_import_tls_certificate_pem(const char *certificate_pem, const char *private_key_pem);
char *plain_import_tls_certificate_pkcs12(const uint8_t *data, size_t data_len, const char *password);
char *plain_create_session(const char *name);
char *plain_confirm_web_login(const char *request_id, bool allowed);
char *plain_stage_app_restore(const char *path);
char *plain_export_app_backup(void);
char *plain_create_crash_report(const char *report);
char *plain_file_icon(const char *extension);
char *plain_load_preferences(void);
char *plain_set_pref(const char *key, const char *value);
char *plain_complete_onboarding(void);
char *plain_toggle_notification_app(const char *package_name);
char *plain_verify_adb_token(const char *token);
char *plain_start_app_update_download(void);
char *plain_cancel_app_update_download(void);
char *plain_check_app_update(bool automatic);

char *plain_reset_password(void);
char *plain_reset_url_token(void);
char *plain_reset_adb_token(void);
char *plain_set_notification_filter_mode(const char *mode);
char *plain_clear_notification_filter_apps(void);
char *plain_is_pairing_request_current(const char *id, const char *signature);
char *plain_reconnect_peers(void);
char *plain_file_paths(const char *paths);
char *plain_import_note_image(const char *source, const char *file_name);
char *plain_import_pomodoro_sound(const char *source);
char *plain_dlna_receiver(void);
char *plain_retry_dlna_receiver(void);
char *plain_remove_dlna_rule(const char *ip, bool allowed);
char *plain_accept_dlna_cast(bool remember);
char *plain_reject_dlna_cast(bool remember);
char *plain_scan_history(void);
char *plain_add_scan_history(const char *text);
char *plain_remove_scan_history(const char *text);
char *plain_acquire_mdns_debug_session(const char *token);
char *plain_release_mdns_debug_session(const char *token);

#endif
