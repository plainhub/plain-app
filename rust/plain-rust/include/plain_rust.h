#ifndef PLAIN_RUST_H
#define PLAIN_RUST_H

char *plain_prefs_open(const char *system_path, const char *user_path);
char *plain_prefs_system_snapshot(void);
char *plain_prefs_user_snapshot(void);
char *plain_prefs_set_system(const char *key, const char *value_json);
char *plain_prefs_set_user(const char *key, const char *value_json);
char *plain_prefs_remove_system(const char *key);
char *plain_prefs_remove_user(const char *key);
void plain_prefs_string_free(char *pointer);

char *plain_core_start(const char *database_path, const char *token);
void plain_core_stop(void);

#endif
