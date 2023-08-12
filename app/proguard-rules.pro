# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.AsyncTask
-dontwarn io.netty.internal.tcnative.Buffer
-dontwarn io.netty.internal.tcnative.CertificateCallback
-dontwarn io.netty.internal.tcnative.CertificateCompressionAlgo
-dontwarn io.netty.internal.tcnative.CertificateVerifier
-dontwarn io.netty.internal.tcnative.Library
-dontwarn io.netty.internal.tcnative.SSL
-dontwarn io.netty.internal.tcnative.SSLContext
-dontwarn io.netty.internal.tcnative.SSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.SSLSession
-dontwarn io.netty.internal.tcnative.SSLSessionCache
-dontwarn io.netty.internal.tcnative.SessionTicketKey
-dontwarn io.netty.internal.tcnative.SniHostNameMatcher
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn java.lang.System$Logger$Level
-dontwarn java.lang.System$Logger
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.apache.logging.log4j.Level
-dontwarn org.apache.logging.log4j.LogManager
-dontwarn org.apache.logging.log4j.Logger
-dontwarn org.apache.logging.log4j.message.MessageFactory
-dontwarn org.apache.logging.log4j.spi.ExtendedLogger
-dontwarn org.apache.logging.log4j.spi.ExtendedLoggerWrapper
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ClientProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$Provider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ServerProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego
-dontwarn org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
-dontwarn org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
-dontwarn org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
-dontwarn pl.droidsonroids.gif.GifDrawable
-dontwarn reactor.blockhound.integration.BlockHoundIntegration

-keep class com.apurebase.kgraphql.** { *; }
-keep class com.ismartcoding.plain.databinding.** { *; }
-keep class org.bouncycastle.** { *; }
-keep interface org.bouncycastle.** { *; }

-keep class io.ktor.server.netty.EngineMain { *; }
-keep class io.ktor.server.config.HoconConfigLoader { *; }
-keep class com.ismartcoding.plain.web.HttpModuleKt { *; }
-keep class com.ismartcoding.plain.MainApp { *; }
-keep class com.ismartcoding.plain.services.HttpServerService { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.text.RegexOption { *; }
