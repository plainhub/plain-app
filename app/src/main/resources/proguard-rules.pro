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

# Restore some Source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless

# WebRTC / jni_zero: JniInit is loaded by native code at runtime via System.loadLibrary().
# R8 cannot see this reference through static analysis, so it must be kept explicitly.
-keep class org.jni_zero.JniInit { *; }
-keep class org.jni_zero.** { *; }
-keep class org.webrtc.** { *; }
