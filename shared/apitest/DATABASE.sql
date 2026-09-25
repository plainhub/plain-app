-- plain-app Room 数据库 DDL（Room version 31，29 张表）
-- 自动生成，禁止手改。源：room-db/schemas/com.ismartcoding.plain.platform.AppDatabase/31.json
-- 再生：./gradlew :shared:testAndroidHostTest --tests "com.ismartcoding.plain.DbSchemaPrintTest"
-- 过期锁：DbSchemaTest

CREATE TABLE IF NOT EXISTS `archived_conversations` (`conversation_id` TEXT NOT NULL, `conversation_date` TEXT NOT NULL, PRIMARY KEY(`conversation_id`));

CREATE TABLE IF NOT EXISTS `audio_play_history` (`path` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `duration_ms` INTEGER NOT NULL, `play_count` INTEGER NOT NULL DEFAULT 0, `played_at` TEXT NOT NULL, PRIMARY KEY(`path`));
CREATE INDEX IF NOT EXISTS `index_audio_play_history_played_at` ON `audio_play_history` (`played_at`);

CREATE TABLE IF NOT EXISTS `audio_playlist_items` (`id` TEXT NOT NULL, `playlist_id` TEXT NOT NULL, `audio_path` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album_id` TEXT NOT NULL DEFAULT '', `duration_ms` INTEGER NOT NULL, `sort_order` INTEGER NOT NULL, `added_at` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE UNIQUE INDEX IF NOT EXISTS `index_audio_playlist_items_playlist_id_audio_path` ON `audio_playlist_items` (`playlist_id`, `audio_path`);
CREATE INDEX IF NOT EXISTS `index_audio_playlist_items_playlist_id_sort_order` ON `audio_playlist_items` (`playlist_id`, `sort_order`);

CREATE TABLE IF NOT EXISTS `audio_playlists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `audio_queue_items` (`path` TEXT NOT NULL, `sort_order` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `duration_ms` INTEGER NOT NULL, PRIMARY KEY(`path`));
CREATE INDEX IF NOT EXISTS `index_audio_queue_items_sort_order` ON `audio_queue_items` (`sort_order`);

CREATE TABLE IF NOT EXISTS `audio_queue_source` (`id` INTEGER NOT NULL, `source` TEXT NOT NULL, `playlist_id` TEXT NOT NULL, `current_path` TEXT NOT NULL, `current_index` INTEGER NOT NULL, `sort_by` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `book_chapters` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `book_id` TEXT NOT NULL, `parent_id` TEXT NOT NULL, `content` TEXT NOT NULL, `sort_order` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `bookmark_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `collapsed` INTEGER NOT NULL, `sort_order` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `bookmarks` (`id` TEXT NOT NULL, `url` TEXT NOT NULL, `title` TEXT NOT NULL, `favicon_path` TEXT NOT NULL, `group_id` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `click_count` INTEGER NOT NULL, `last_clicked_at` TEXT, `sort_order` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE INDEX IF NOT EXISTS `index_bookmarks_group_id` ON `bookmarks` (`group_id`);

CREATE TABLE IF NOT EXISTS `books` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT NOT NULL, `image` TEXT NOT NULL, `description` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `chat_channels` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `key` TEXT NOT NULL, `owner_id` TEXT NOT NULL DEFAULT '', `members` TEXT NOT NULL, `version` INTEGER NOT NULL DEFAULT 0, `status` TEXT NOT NULL DEFAULT 'JOINED', `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `from_id` TEXT NOT NULL, `to_id` TEXT NOT NULL, `channel_id` TEXT NOT NULL, `status` TEXT NOT NULL DEFAULT 'PENDING', `status_data` TEXT NOT NULL DEFAULT '', `content` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE INDEX IF NOT EXISTS `index_chats_channel_id` ON `chats` (`channel_id`);
CREATE INDEX IF NOT EXISTS `index_chats_from_id` ON `chats` (`from_id`);
CREATE INDEX IF NOT EXISTS `index_chats_to_id` ON `chats` (`to_id`);

CREATE TABLE IF NOT EXISTS `clipboards` (`id` TEXT NOT NULL, `text` TEXT NOT NULL, `hash` TEXT NOT NULL, `source` TEXT NOT NULL, `label` TEXT NOT NULL, `sensitive` INTEGER NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE INDEX IF NOT EXISTS `index_clipboards_hash` ON `clipboards` (`hash`);

CREATE TABLE IF NOT EXISTS `feed_entries` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `image` TEXT NOT NULL, `description` TEXT NOT NULL, `author` TEXT NOT NULL, `content` TEXT NOT NULL, `feed_id` TEXT NOT NULL, `raw_id` TEXT NOT NULL, `published_at` TEXT NOT NULL, `read` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE INDEX IF NOT EXISTS `index_feed_entries_feed_id` ON `feed_entries` (`feed_id`);
CREATE INDEX IF NOT EXISTS `index_feed_entries_raw_id` ON `feed_entries` (`raw_id`);

CREATE TABLE IF NOT EXISTS `feeds` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `logo` TEXT NOT NULL DEFAULT '', `fetch_content` INTEGER NOT NULL, `last_sync_at` TEXT, `last_error` TEXT NOT NULL DEFAULT '', `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE UNIQUE INDEX IF NOT EXISTS `index_feeds_url` ON `feeds` (`url`);

CREATE TABLE IF NOT EXISTS `files` (`id` TEXT NOT NULL, `size` INTEGER NOT NULL, `mime_type` TEXT NOT NULL, `real_path` TEXT NOT NULL, `ref_count` INTEGER NOT NULL, `weak_hash` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE INDEX IF NOT EXISTS `index_files_size_weak_hash` ON `files` (`size`, `weak_hash`);

CREATE TABLE IF NOT EXISTS `image_editor_projects` (`id` TEXT NOT NULL, `state_b64` TEXT NOT NULL, `thumbnail` TEXT, `canvas_width` INTEGER NOT NULL, `canvas_height` INTEGER NOT NULL, `layer_count` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `image_embeddings` (`id` TEXT NOT NULL, `path` TEXT NOT NULL, `embedding` BLOB NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `media_item` (`media_type` TEXT NOT NULL, `media_id` TEXT NOT NULL, `duration_ms` INTEGER NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`media_id`));

CREATE TABLE IF NOT EXISTS `nearby_device_cache` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `ips` TEXT NOT NULL, `port` INTEGER NOT NULL, `device_type` TEXT NOT NULL, `version` TEXT NOT NULL, `platform` TEXT NOT NULL, `last_seen` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `deleted_at` TEXT, `content` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `peers` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `ip` TEXT NOT NULL, `key` TEXT NOT NULL, `public_key` TEXT NOT NULL, `status` TEXT NOT NULL, `port` INTEGER NOT NULL, `device_type` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `pomodoro_items` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `completed_count` INTEGER NOT NULL, `total_work_seconds` INTEGER NOT NULL, `total_break_seconds` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `sessions` (`client_id` TEXT NOT NULL, `name` TEXT NOT NULL DEFAULT '', `type` TEXT NOT NULL DEFAULT 'WEB', `client_ip` TEXT NOT NULL, `os_name` TEXT NOT NULL, `os_version` TEXT NOT NULL, `browser_name` TEXT NOT NULL, `browser_version` TEXT NOT NULL, `token` TEXT NOT NULL, `last_active_at` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`client_id`));

CREATE TABLE IF NOT EXISTS `shares` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `password` TEXT NOT NULL, `url_token` TEXT NOT NULL, `expires_at` TEXT, `read_only` INTEGER NOT NULL, `data` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `tag_relations` (`tag_id` TEXT NOT NULL, `key` TEXT NOT NULL, `type` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `size` INTEGER NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`tag_id`, `key`, `type`));

CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` INTEGER NOT NULL, `count` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`));

CREATE TABLE IF NOT EXISTS `trashed_messages` (`message_id` TEXT NOT NULL, `is_mms` INTEGER NOT NULL, `trashed_at` TEXT NOT NULL, PRIMARY KEY(`message_id`));

CREATE TABLE IF NOT EXISTS `video_play_progress` (`media_id` TEXT NOT NULL, `position_ms` INTEGER NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`media_id`));
