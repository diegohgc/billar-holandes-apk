# Add project specific ProGuard rules here.

# Room/WorkManager (pulled in transitively by AdMob) generate database implementation
# classes via reflection at runtime - R8 was stripping/renaming them without this rule,
# crashing the app on startup with "Failed to create an instance of WorkDatabase".
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class androidx.work.impl.** { *; }
-keep class androidx.room.** { *; }
