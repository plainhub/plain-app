# db/ — Room Database Layer

> 19 files | Package: `com.ismartcoding.plain.db`

## Database

- **AppDatabase.kt** — `@Database` config, 17 entities, auto-migrations v1→v11
- **Migrations.kt** — Manual migration specs (e.g., MIGRATION_5_6 adds pomodoro_items)
- **DbConverters.kt** — Room TypeConverters: ChannelMember JSON, Instant↔String
- **DataInitializer.kt** — Seeds default tags (music, movie, family, todo...) on first run
- **DEntityBase.kt** — Abstract base: `created_at`, `updated_at` as Instant

## Entities (each file = Entity + DAO)

| File | Entity | Key Columns |
|------|--------|------------|
| DChat.kt | `chats` | id, content (DMessageContent JSON), from_id, to_id, channel_id, status |
| DChatChannel.kt | `chat_channels` | id, name, members (ChannelMember list JSON), info |
| DNote.kt | `notes` | id, title, content, deleted_at (soft delete) |
| DFeed.kt | `feeds` | id, name, url (unique), fetch_content, count |
| DFeedEntry.kt | `feed_entries` | id, title, url, content, feed_id, author, image |
| DBook.kt | `books` | id, name, author, image, description |
| DBookChapter.kt | `book_chapters` | id, name, book_id, parent_id, content, display_order |
| DTag.kt | `tags` | id, name, type, count |
| DTagRelation.kt | `tag_relations` | tag_id+key+type (composite PK), size, title |
| DPeer.kt | `peers` | id, name, ip, public_key, status (paired/unpaired/channel), port |
| DSession.kt | `sessions` | client_id (PK), client_ip, os/browser info, token |
| DBookmark.kt | `bookmarks` | id, url, title, favicon_path, group_id, pinned, sort_order |
| DAppFile.kt | `app_files` | SHA-256 content-addressable file store (fid: URI) |
| DPomodoroItem.kt | `pomodoro_items` | date (YYYY-MM-DD), completed_count, work_seconds, break_seconds |
