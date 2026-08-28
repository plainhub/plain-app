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
# Netty (Ktor server engine) references many optional JVM-desktop classes
# that don't exist on Android: tcnative SSL, JFR, log4j, JMX, BlockHound, etc.
-dontwarn io.netty.**
-dontwarn reactor.blockhound.**
-dontwarn java.lang.management.**
-dontwarn javax.naming.ldap.**
-dontwarn jdk.jfr.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**

# Netty 4.2 uses java.lang.invoke.VarHandle for byte[]/ByteBuffer short/int/long
# access. R8 horizontal class merging corrupts the invoke-polymorphic call sites,
# producing java.lang.VerifyError ("expected Reference: java.lang.Object[]") on
# launch. Keeping the package intact disables that optimization for these classes.
-keep class io.netty.** { *; }

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

# ===== Ktor server (Netty engine, routing via reflection) =====
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**

# ===== kgraphql engine + app GraphQL models =====
# Fully reflection-free since the KSP accessor-lambda migration: field access,
# type names, and unions are all compile-time fixed; the last kotlin-reflect
# dependency was removed. R8 may shrink/optimize/obfuscate everything here.

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

# ===== MediaPipe / LiteRT =====
-keep class com.google.mediapipe.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.ai.edge.litert.**

# ===== OkHttp / Okio =====
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
