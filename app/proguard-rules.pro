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
# JVM-desktop-only classes pulled in transitively.
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
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.system.AppFeatureType { public static final com.ismartcoding.plain.platform.system.AppFeatureType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.system.AppChannelType { public static final com.ismartcoding.plain.platform.system.AppChannelType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.system.DataType { public static final com.ismartcoding.plain.platform.system.DataType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.audio.MediaPlayMode { public static final com.ismartcoding.plain.features.audio.MediaPlayMode *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.PackageType { public static final com.ismartcoding.plain.enums.PackageType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.web.WebSettingsFeature { public static final com.ismartcoding.plain.features.web.WebSettingsFeature *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.TextFileType { public static final com.ismartcoding.plain.enums.TextFileType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.permissions.Permission { public static final com.ismartcoding.plain.platform.permissions.Permission *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.system.Capability { public static final com.ismartcoding.plain.platform.system.Capability *; }

# Rust chat/discovery projections.
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.system.DeviceType { public static final com.ismartcoding.plain.platform.system.DeviceType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.nearby.PeerStatus { public static final com.ismartcoding.plain.features.nearby.PeerStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.chat.ChannelMemberStatus { public static final com.ismartcoding.plain.features.chat.ChannelMemberStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.ChatChannelStatus { public static final com.ismartcoding.plain.enums.ChatChannelStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.chat.ChatStatus { public static final com.ismartcoding.plain.features.chat.ChatStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.sessions.SessionType { public static final com.ismartcoding.plain.features.sessions.SessionType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.enums.NearbyMessageType { public static final com.ismartcoding.plain.enums.NearbyMessageType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.nearby.DiscoveryMethod { public static final com.ismartcoding.plain.features.nearby.DiscoveryMethod *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.db.MessageType { public static final com.ismartcoding.plain.db.MessageType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.chat.peer.transport.PeerTransportType { public static final com.ismartcoding.plain.features.chat.peer.transport.PeerTransportType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.ui.nearby.NearbyItemStatus { public static final com.ismartcoding.plain.ui.nearby.NearbyItemStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.nearby.MdnsPacketDirection { public static final com.ismartcoding.plain.features.nearby.MdnsPacketDirection *; }

# Rust media, file, feed, download and timer contracts.
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.audio.AudioServiceAction { public static final com.ismartcoding.plain.features.audio.AudioServiceAction *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.dlna.DlnaPlaybackState { public static final com.ismartcoding.plain.features.dlna.DlnaPlaybackState *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.db.AudioPlaySource { public static final com.ismartcoding.plain.db.AudioPlaySource *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.files.FileSortBy { public static final com.ismartcoding.plain.features.files.FileSortBy *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.files.FileTaskType { public static final com.ismartcoding.plain.features.files.FileTaskType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.files.FileTaskStatus { public static final com.ismartcoding.plain.features.files.FileTaskStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.media.actions.MediaAction { public static final com.ismartcoding.plain.features.media.actions.MediaAction *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.feeds.FeedWorkerStatus { public static final com.ismartcoding.plain.features.feeds.FeedWorkerStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.feeds.FeedSyncErrorCode { public static final com.ismartcoding.plain.features.feeds.FeedSyncErrorCode *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.downloads.DownloadStatus { public static final com.ismartcoding.plain.features.downloads.DownloadStatus *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.shares.ShareBatchType { public static final com.ismartcoding.plain.features.shares.ShareBatchType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.screenmirror.ScreenMirrorMode { public static final com.ismartcoding.plain.features.screenmirror.ScreenMirrorMode *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.ui.pomodoro.PomodoroState { public static final com.ismartcoding.plain.ui.pomodoro.PomodoroState *; }

# Enum names consumed by JSON serializers at platform/Rust boundaries.
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.images.search.ImageSearchStatusType { public static final com.ismartcoding.plain.features.images.search.ImageSearchStatusType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.platform.system.DevicePlatform { public static final com.ismartcoding.plain.platform.system.DevicePlatform *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.screenmirror.ScreenMirrorControlAction { public static final com.ismartcoding.plain.features.screenmirror.ScreenMirrorControlAction *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.files.DriveType { public static final com.ismartcoding.plain.features.files.DriveType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.telephony.PhoneType { public static final com.ismartcoding.plain.features.telephony.PhoneType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.telephony.EmailType { public static final com.ismartcoding.plain.features.telephony.EmailType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.telephony.PostalType { public static final com.ismartcoding.plain.features.telephony.PostalType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.telephony.WebsiteType { public static final com.ismartcoding.plain.features.telephony.WebsiteType *; }
-keepclassmembers,allowoptimization enum com.ismartcoding.plain.features.telephony.ImProtocol { public static final com.ismartcoding.plain.features.telephony.ImProtocol *; }
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
-keep class com.ismartcoding.plain.rust.native.RustNativeBindings { *; }

-keep class com.ismartcoding.plain.rust.native.RustBleBridge { *; }

-keep class com.ismartcoding.plain.rust.bridge.RustDlnaBridge { *; }

-keep class com.ismartcoding.plain.rust.bridge.RustWebLoginBridge { *; }
