# Linphone SDK ProGuard rules
-keep class org.linphone.core.** { *; }
-keep class org.linphone.mediastream.** { *; }
-dontwarn org.linphone.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}

# Room & Hilt
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @dagger.hilt.** <fields>;
    @dagger.hilt.** <methods>;
}

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Dropbox SDK
-dontwarn com.dropbox.core.**
-keep class com.dropbox.core.** { *; }
