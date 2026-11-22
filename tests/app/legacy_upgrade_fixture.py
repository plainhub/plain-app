import argparse
import base64
import hashlib
import json
from pathlib import Path
import sqlite3
import struct
import subprocess
import wave
import zlib


SCHEMA = "v3.3.25:room-db/schemas/com.ismartcoding.plain.platform.AppDatabase/{version}.json"
CREATED = "2025-04-03T07:08:09Z"
UPDATED = "2025-04-04T08:09:10.123Z"
CHANNEL_ID = "rva5lbpv4u0w"
PAIR_KEY = hashlib.sha256(b"PlainApp legacy upgrade test pair").digest()
CHANNEL_KEY = hashlib.sha256(b"PlainApp legacy upgrade test channel").digest()

# RFC 8032 Ed25519 test vectors 1 and 2.
IDENTITIES = (
    (
        "9ix",
        "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
        "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
    ),
    (
        "1y2p0ij32e8e7",
        "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
        "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
    ),
)


def encoded(data):
    return base64.b64encode(data).decode("ascii")


def payload(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def varint(value):
    result = bytearray()
    while value >= 128:
        result.append((value & 127) | 128)
        value >>= 7
    result.append(value)
    return bytes(result)


def bytes_field(number, data):
    return varint((number << 3) | 2) + varint(len(data)) + data


def datastore_strings(values):
    # DataStore PreferenceMap.preferences uses map entries with Value.string at field 5.
    return b"".join(
        bytes_field(1, bytes_field(1, name.encode()) + bytes_field(2, bytes_field(5, value.encode())))
        for name, value in values.items()
    )


def insert(db, table, **values):
    columns = ", ".join(f'"{column}"' for column in values)
    placeholders = ", ".join("?" for _ in values)
    db.execute(f'INSERT INTO "{table}" ({columns}) VALUES ({placeholders})', tuple(values.values()))


def create_room_schema(db, schema):
    db.execute(f'PRAGMA user_version = {schema["version"]}')
    for entity in schema["entities"]:
        statements = [entity["createSql"], *(index["createSql"] for index in entity.get("indices", []))]
        for statement in statements:
            db.execute(statement.replace("${TABLE_NAME}", entity["tableName"]))
    for statement in schema["setupQueries"]:
        db.execute(statement)


def fixture_image():
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(b"\x00\x28\xa0\xc8\xff")) + chunk(b"IEND", b""))


def add_file(db, root, filename, mime_type, content, references):
    digest = hashlib.sha256(content).hexdigest()
    relative = Path(digest[:2]) / digest[2:4] / (digest + Path(filename).suffix)
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
    weak_digest = hashlib.sha256(content if len(content) <= 8192 else content[:4096] + content[-4096:]).hexdigest()
    insert(db, "files", id=digest, size=len(content), mime_type=mime_type, real_path=relative.as_posix(),
           ref_count=references, weak_hash=weak_digest, created_at=CREATED, updated_at=UPDATED)
    return digest, f"fid:{relative.name}"


def populate_content(db, file_root, stored_file_root, client_id, peer_id, is_owner, version):
    timestamps = {"created_at": CREATED, "updated_at": UPDATED}
    note_id = "s6xz9bxr6skg"
    tag_id = "rx7b96vgy1a8"
    feed_id = "s0kx8nbzca9s"
    entry_id = "s9jk362nyh5s"
    insert(db, "notes", id=note_id, title="Fixture note", deleted_at=None,
           content="# Heading\n\n合成笔记 🌱\n\n![Fixture image](app://note-images/fixture.png)", **timestamps)
    insert(db, "notes", id="rwc3k2t342bk", title="Trashed fixture note", content="Trashed content", deleted_at=UPDATED, **timestamps)
    insert(db, "pomodoro_items", id="s7ujs7x37gjo", date="2025-04-03", completed_count=2,
           total_work_seconds=3000, total_break_seconds=600, **timestamps)
    sync = {"last_sync_at": UPDATED, "last_error": payload({"code": "", "detail": ""})} if version >= 24 else {}
    insert(db, "feeds", id=feed_id, name="Fixture feed", url="https://example.com/feed.xml", fetch_content=1,
           **sync, **timestamps)
    for current_id, is_read in ((entry_id, 1), ("rvya3298aub4", 0)):
        insert(db, "feed_entries", id=current_id, title=f"Fixture entry {is_read}",
               url=f"https://example.com/entry/{is_read}", image="", description="Fixture summary", author="Fixture author",
               content="<p>Fixture content</p>", feed_id=feed_id, raw_id=f"fixture-entry-{is_read}", published_at=CREATED,
               read=is_read, **timestamps)
    insert(db, "tags", id=tag_id, name="Fixture notes", type=6, count=1, **timestamps)
    insert(db, "tag_relations", tag_id=tag_id, key=note_id, type=6, size=0, title="", created_at=CREATED)
    insert(db, "tags", id="s7gjz7z1ta4g", name="Fixture feed entries", type=7, count=1, **timestamps)
    insert(db, "tag_relations", tag_id="s7gjz7z1ta4g", key=entry_id, type=7, size=0, title="", created_at=CREATED)

    png = fixture_image()
    note_image = file_root / "note-images/fixture.png"
    note_image.parent.mkdir(parents=True)
    note_image.write_bytes(png)
    _, image_uri = add_file(db, file_root, "fixture.png", "image/png", png, 2)
    file = b"Synthetic chat attachment\n"
    _, file_uri = add_file(db, file_root, "fixture.txt", "text/plain", file, 1)
    add_file(db, file_root, "standalone.txt", "text/plain", b"Synthetic app file without a chat reference\n", 1)
    if version == 8:
        relative = Path("Documents/legacy-text.txt")
        (file_root / relative).parent.mkdir()
        (file_root / relative).write_bytes(file)
        file_uri = str(stored_file_root / relative)

    preview = {"url": "https://example.com/preview", "title": "Fixture preview", "imageUrl": "https://example.com/preview.png",
               "imageLocalPath": image_uri, "imageWidth": 1, "imageHeight": 1, "createdAt": CREATED}
    messages = (
        ("s7i4mdmpjlds", "me", peer_id, "", "SENT", "", "TEXT", {"text": "Fixture link", "linkPreviews": [preview]}),
        ("sa5o8zx7s3sw", peer_id, "me", "", "SENT", "", "IMAGES", {"items": [
            {"id": "s5umcabbs1jk", "uri": image_uri, "size": len(png), "width": 1, "height": 1}]}),
        ("sahrc0inqfpc", "me" if is_owner else peer_id, "", CHANNEL_ID, "PARTIAL", payload({"results": [
            {"peerId": peer_id if is_owner else client_id, "peerName": "Fixture recipient"},
            {"peerId": "0", "peerName": "Fixture third member", "error": "Fixture delivery error"}
        ]}), "FILES", {"items": [
            {"id": "rw8aqqchbjxc", "uri": file_uri, "size": len(file), "duration": 1500, "fileName": "fixture.txt"}]}),
        ("s7njua0awoj4", "me", "local", "", "SENT", "", "TEXT", {"text": "Fixture local message"}),
    )
    for message_id, sender, recipient, channel_id, status, deliveries, kind, value in messages:
        channel = {"group_id" if version < 11 else "channel_id": channel_id}
        delivery = {"status_data": deliveries} if version >= 12 else {}
        insert(db, "chats", id=message_id, from_id=sender, to_id=recipient, **channel,
               status=status if version >= 20 else status.lower(),
               content=payload({"type": kind if version >= 21 else kind.lower(), "value": value}),
               **delivery, **timestamps)


def populate_other_data(db, file_root, stored_file_root, peer_id, version):
    timestamps = {"created_at": CREATED, "updated_at": UPDATED}
    group = "s08ajwngtuy8"
    insert(db, "bookmark_groups", id=group, name="Fixture group", collapsed=1, sort_order=0, **timestamps)
    insert(db, "bookmarks", id="rwhjogdk8co0", url="https://example.com/bookmark", title="Fixture bookmark",
           favicon_path="", group_id=group, pinned=1, click_count=3, last_clicked_at=UPDATED, sort_order=0, **timestamps)
    if version >= 25:
        clipboard = "合成剪贴板"
        insert(db, "clipboard", id="rx9qnweslg1s", text=clipboard, hash=hashlib.sha256(clipboard.encode()).hexdigest(),
               source=peer_id, label="Fixture label", sensitive=1, created_at=CREATED)
    shared = file_root / "FixtureShared"
    shared.mkdir()
    (shared / "shared.txt").write_text("Synthetic shared file\n", encoding="utf-8")
    if version >= 23:
        insert(db, "shares", id="s9kuxq7lb1fk", name="Fixture share", password="", url_token=encoded(CHANNEL_KEY),
               expires_at=None, read_only=1,
               data=payload([{"virtualPath": "/FixtureShared", "realPath": str(stored_file_root / shared.name), "isDir": True}]),
               **timestamps)
    if version >= 14:
        insert(db, "archived_conversations", conversation_id="42", conversation_date=1743754150123)
    if version >= 17:
        insert(db, "image_editor_projects", id="s406cifw4nwg", state_b64=encoded(b"fixture"), thumbnail=None,
               canvas_width=16, canvas_height=16, layer_count=1, **timestamps)
    if version >= 15:
        insert(db, "video_play_progress", media_id="17", duration=2500, updated_at=UPDATED)

    sound = file_root / "audio/pomodoro_sound.mp3"
    sound.parent.mkdir()
    with wave.open(str(sound), "wb") as wav:
        wav.setparams((1, 2, 8000, 0, "NONE", "not compressed"))
        wav.writeframes(b"\x00\x00" * 800)


def write_device(root, schema, identity, peer, is_owner, stored_file_root):
    client_id, private_key, public_key = identity
    peer_id, _, peer_public_key = peer
    database = root / "data/databases/plain.db"
    database.parent.mkdir(parents=True)
    file_root = root / "external-files"
    stored_file_root = stored_file_root or file_root.resolve()
    version = schema["version"]
    with sqlite3.connect(database) as db:
        create_room_schema(db, schema)
        insert(db, "peers", id=peer_id, name="Fixture paired device", ip="192.0.2.1", key=encoded(PAIR_KEY),
               public_key=encoded(bytes.fromhex(peer_public_key)), port=8443,
               status="PAIRED" if version >= 19 else "paired", device_type="PHONE" if version >= 19 else "phone",
               created_at=CREATED, updated_at=UPDATED)
        insert(db, "peers", id="0", name="Fixture third member", ip="", key="", public_key="",
               port=0, status="CHANNEL" if version >= 19 else "channel", device_type="PHONE" if version >= 19 else "phone",
               created_at=CREATED, updated_at=UPDATED)
        member_ids = (client_id, peer_id, "0")
        members = payload(member_ids if version == 8 else [{"id": member} for member in member_ids])
        metadata = {"owner": "me" if is_owner else peer_id, "version": 2,
                    "status": "JOINED" if version >= 20 else "joined"} if version >= 11 else {}
        channel_table = "chat_groups" if version < 9 else "chat_channels"
        insert(db, channel_table, id=CHANNEL_ID, name="Fixture channel", key=encoded(CHANNEL_KEY),
               members=members, created_at=CREATED, updated_at=UPDATED, **metadata)
        populate_content(db, file_root, stored_file_root, client_id, peer_id, is_owner, version)
        populate_other_data(db, file_root, stored_file_root, peer_id, version)
    db.close()

    settings = root / "data/files/datastore/settings.preferences_pb"
    settings.parent.mkdir(parents=True)
    settings.write_bytes(datastore_strings({
        "client_id": client_id,
        "signature_key_pair": payload({"privateKey": encoded(bytes.fromhex(private_key)),
                                       "publicKey": encoded(bytes.fromhex(public_key))}),
        "device_name": "Fixture owner" if is_owner else "Fixture recipient",
        "scan_history": payload(["https://example.com/scanned", "离线合成扫码结果"]),
        "pomodoro_settings": payload({"workDuration": 30, "shortBreakDuration": 7, "longBreakDuration": 20,
                                      "pomodorosBeforeLongBreak": 3, "showNotification": False,
                                      "soundPath": "app://audio/pomodoro_sound.mp3", "originalSoundName": "Fixture tone.wav"}),
        "favorite_folders": payload([{"rootPath": str(stored_file_root),
                                      "fullPath": str(stored_file_root / "FixtureShared"), "alias": "Fixture favorite"}]),
        "quick_note_draft": "Fixture note draft",
        "chat_input_text": "Fixture global chat draft",
    }))


def main():
    parser = argparse.ArgumentParser(description="Generate synthetic PlainApp upgrade data for two paired devices.")
    parser.add_argument("directory", type=Path, help="New output directory, typically inside the system temporary directory.")
    parser.add_argument("--stored-files-root", type=Path, help="Absolute app files directory on the test device.")
    parser.add_argument("--room-version", type=int, choices=(8, 16, 25), default=25)
    args = parser.parse_args()
    if args.stored_files_root is not None and not args.stored_files_root.is_absolute():
        parser.error("--stored-files-root must be absolute")
    repo = Path(__file__).resolve().parents[2]
    schema = json.loads(subprocess.check_output(
        ["git", "show", SCHEMA.format(version=args.room_version)], cwd=repo))["database"]
    args.directory.mkdir()
    write_device(args.directory / "device-a", schema, IDENTITIES[0], IDENTITIES[1], True, args.stored_files_root)
    write_device(args.directory / "device-b", schema, IDENTITIES[1], IDENTITIES[0], False, args.stored_files_root)


if __name__ == "__main__":
    main()
