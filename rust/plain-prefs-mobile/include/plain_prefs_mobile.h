#ifndef PLAIN_PREFS_MOBILE_H
#define PLAIN_PREFS_MOBILE_H

char *plain_prefs_open(const char *path);
char *plain_prefs_snapshot(void);
char *plain_prefs_set(const char *key, const char *value_json);
char *plain_prefs_remove(const char *key);
void plain_prefs_string_free(char *pointer);

#endif
