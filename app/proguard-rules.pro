# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Restore some Source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless

# ===== R8 missing rules (classes not available on Android) =====
# JVM-desktop-only classes pulled in transitively. Netty's own rules went
# away with the Ktor server engine.
-dontwarn java.lang.management.**
-dontwarn javax.naming.ldap.**
-dontwarn jdk.jfr.**

# ===== kotlinx.serialization =====
# Keep @Serializable companions and serializer accessors.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}

# ===== Enum constants used as external or persisted keys =====
# Rust owns GraphQL schema names. Only constant fields need name/retention rules.
# Class names, methods and private backing fields remain eligible for shrinking.

# Persisted preferences, platform permissions and public API keys.
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.AppFeatureType { public static final com.ismartcoding.plain.enums.AppFeatureType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.AppChannelType { public static final com.ismartcoding.plain.enums.AppChannelType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.DataType { public static final com.ismartcoding.plain.enums.DataType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.MediaDataType { public static final com.ismartcoding.plain.enums.MediaDataType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.MediaPlayMode { public static final com.ismartcoding.plain.enums.MediaPlayMode *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.PackageType { public static final com.ismartcoding.plain.enums.PackageType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.WebSettingsFeature { public static final com.ismartcoding.plain.enums.WebSettingsFeature *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.TextFileType { public static final com.ismartcoding.plain.enums.TextFileType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.Permission { public static final com.ismartcoding.plain.platform.Permission *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.Capability { public static final com.ismartcoding.plain.platform.Capability *; }

# Room values and Rust chat/discovery projections.
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.DeviceType { public static final com.ismartcoding.plain.enums.DeviceType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.PeerStatus { public static final com.ismartcoding.plain.enums.PeerStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.ChannelMemberStatus { public static final com.ismartcoding.plain.enums.ChannelMemberStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.ChatChannelStatus { public static final com.ismartcoding.plain.enums.ChatChannelStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.ChatStatus { public static final com.ismartcoding.plain.enums.ChatStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.SessionType { public static final com.ismartcoding.plain.enums.SessionType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.NearbyMessageType { public static final com.ismartcoding.plain.enums.NearbyMessageType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.DiscoveryMethod { public static final com.ismartcoding.plain.enums.DiscoveryMethod *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.db.MessageType { public static final com.ismartcoding.plain.db.MessageType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.chat.peer.transport.PeerTransportType { public static final com.ismartcoding.plain.chat.peer.transport.PeerTransportType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.ui.models.NearbyItemStatus { public static final com.ismartcoding.plain.ui.models.NearbyItemStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.lib.mdns.MdnsPacketDirection { public static final com.ismartcoding.plain.lib.mdns.MdnsPacketDirection *; }

# Rust media, file, feed, download and timer contracts.
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.AudioServiceAction { public static final com.ismartcoding.plain.enums.AudioServiceAction *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.dlna.DlnaPlaybackState { public static final com.ismartcoding.plain.features.dlna.DlnaPlaybackState *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.db.AudioPlaySource { public static final com.ismartcoding.plain.db.AudioPlaySource *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.file.FileSortBy { public static final com.ismartcoding.plain.features.file.FileSortBy *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.file.FileTaskType { public static final com.ismartcoding.plain.features.file.FileTaskType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.file.FileTaskStatus { public static final com.ismartcoding.plain.features.file.FileTaskStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.mediaactions.MediaAction { public static final com.ismartcoding.plain.features.mediaactions.MediaAction *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.feed.FeedWorkerStatus { public static final com.ismartcoding.plain.features.feed.FeedWorkerStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.feed.FeedSyncErrorCode { public static final com.ismartcoding.plain.features.feed.FeedSyncErrorCode *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.download.DownloadStatus { public static final com.ismartcoding.plain.features.download.DownloadStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.share.ShareBatchType { public static final com.ismartcoding.plain.features.share.ShareBatchType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.ScreenMirrorMode { public static final com.ismartcoding.plain.enums.ScreenMirrorMode *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.ui.page.pomodoro.PomodoroState { public static final com.ismartcoding.plain.ui.page.pomodoro.PomodoroState *; }

# Enum names consumed by JSON serializers at platform/Rust boundaries.
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.ai.ImageSearchStatusType { public static final com.ismartcoding.plain.ai.ImageSearchStatusType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.data.DevicePlatform { public static final com.ismartcoding.plain.data.DevicePlatform *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.ScreenMirrorControlAction { public static final com.ismartcoding.plain.enums.ScreenMirrorControlAction *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.DriveType { public static final com.ismartcoding.plain.enums.DriveType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.PhoneType { public static final com.ismartcoding.plain.enums.PhoneType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.EmailType { public static final com.ismartcoding.plain.enums.EmailType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.PostalType { public static final com.ismartcoding.plain.enums.PostalType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.EventType { public static final com.ismartcoding.plain.enums.EventType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.WebsiteType { public static final com.ismartcoding.plain.enums.WebsiteType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.ImProtocol { public static final com.ismartcoding.plain.enums.ImProtocol *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.lib.dlna.DlnaMediaType { public static final com.ismartcoding.plain.lib.dlna.DlnaMediaType *; }

# ===== ASN.1 / X.509 self-signed certificate generation =====
# Asn1DerEncoder/Asn1BerParser drive (de)serialization purely via the
# @Asn1Class/@Asn1Field RUNTIME annotations, which are invisible to R8.
# Without this rule R8 strips the annotations and HTTPS keystore generation
# crashes on fresh installs with "<class> not annotated with <annotation>".
-keep class com.ismartcoding.plain.lib.apk.cert.** { *; }

# ===== Google Tink =====
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ===== OkHttp / Okio =====
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-keep class com.ismartcoding.plain.preferences.RustPrefsBridge { *; }
