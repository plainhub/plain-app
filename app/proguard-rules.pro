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

# ===== Enum names (cross-process data contracts) =====
# Enum class names and constant names ARE data contracts and cannot be fixed at
# compile time:
#  - GraphQL enum type names default to KClass.simpleName (EnumDSL)
#  - GraphQL enum VALUE names are matched via runtime `it.name` (nameToValue),
#    including introspection TypeKind/DirectiveLocation ("OBJECT", "SCALAR", ...)
#  - `Enum.valueOf(str)` lookups on protocol/persisted keys:
#    DeviceType (mDNS TXT), AppFeatureType (Preferences), iOS pick callbacks
-keepnames enum com.ismartcoding.plain.** { *; }
-keepclassmembers enum com.ismartcoding.plain.** { <fields>; }

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
