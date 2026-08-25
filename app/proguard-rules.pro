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

# ===== App GraphQL model / enum classes =====
# kgraphql was migrated to KSP-generated reflection-free descriptors, so R8 may
# freely shrink/optimize/obfuscate ordinary app code. The ONLY remaining runtime
# reflection hook is `defaultKQLTypeName()` (TypeDSL / EnumDSL / SchemaCompilation)
# which falls back to the RUNTIME `KClass.simpleName` to derive a GraphQL type name
# when a type is not explicitly registered. Obfuscating those class names would
# corrupt the published schema, so preserve the names — but still allow R8 to
# shrink unused members (the class members themselves are reached directly via
# KSP `::class` / `::property` references or kotlinx.serialization serializers,
# and those enlistment rules for serialization are listed above).
-keepnames class com.ismartcoding.plain.httpserver.models.** { *; }
-keepnames class com.ismartcoding.plain.data.** { *; }
-keepnames class com.ismartcoding.plain.enums.** { *; }

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
