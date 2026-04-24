# Sleeper — minimal proguard rules for Compose app
-keepattributes *Annotation*

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
