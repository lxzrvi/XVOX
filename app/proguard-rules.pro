# XVOX ships minified APKs. Android Gradle Plugin retains manifest components automatically;
# these explicit component rules additionally protect lifecycle and platform callback methods from
# OEM/framework reflection while allowing the rest of the app and dependency graph to shrink.
-keep class com.xvox.music.XvoxApplication { *; }
-keep class com.xvox.music.MainActivity { *; }
-keep class com.xvox.music.player.session.XvoxPlaybackService { *; }
-keep class com.xvox.music.artwork.XvoxFallbackArtworkProvider { *; }
-keep class com.xvox.music.widget.XvoxAppWidgetProvider { *; }
-keep class com.xvox.music.notifications.XvoxReminderReceiver { *; }

# JavaAudioTagger discovers several format readers dynamically. Keep this small metadata library
# intact so shrinking does not alter supported local-audio tags or artwork extraction. Its desktop-
# only image helpers reference AWT/Swing/ImageIO, none of which exists on Android and none is used
# by XVOX's MediaStore artwork path. Suppressing only those absent desktop namespaces lets R8 keep
# the Android-relevant tag readers without treating optional JVM helpers as a build error.
-keep class org.jaudiotagger.** { *; }
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
