-keep class org.sqlite.** { *; }
-keep class org.sqlite.database.** { *; }

# Keep the tab strip
-keep class com.google.android.material.tabs.TabLayout { *; }
-keep class androidx.viewpager.** { *; }

# Keep the classes for reflection
-keep class co.epitre.aelf_lectures.lectures.data.** { *; }
-keep class co.epitre.aelf_lectures.settings.** { *; }

# view AndroidManifest.xml #generated:26
-keep class co.epitre.aelf_lectures.LecturesActivity { <init>(...); }

# view AndroidManifest.xml #generated:44
-keep class co.epitre.aelf_lectures.sync.StubProvider { <init>(...); }

# view AndroidManifest.xml #generated:52
-keep class co.epitre.aelf_lectures.sync.SyncService { <init>(...); }

# Fix okhttp (see https://github.com/square/okhttp/issues/6258)
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# EventBus support http://greenrobot.org/eventbus/documentation/proguard
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# Moshi support https://github.com/square/moshi/blob/master/moshi/src/main/resources/META-INF/proguard/moshi.pro
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}

-keep @com.squareup.moshi.JsonQualifier interface *

# Enum field names are used by the integrated EnumJsonAdapter.
# values() is synthesized by the Kotlin compiler and is used by EnumJsonAdapter indirectly
# Annotate enums with @JsonClass(generateAdapter = false) to use them with Moshi.
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
    **[] values();
}
