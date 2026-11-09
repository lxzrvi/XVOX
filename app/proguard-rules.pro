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

# Icon packs are looked up by reflection (core/ui/icons/XvoxIconPacks.kt); keep only the glyphs we map.
-keep class androidx.compose.material.icons.Icons$* { *; }
-keep class androidx.compose.material.icons.**.AddKt { *; }
-keep class androidx.compose.material.icons.**.AlbumKt { *; }
-keep class androidx.compose.material.icons.**.ArrowBackKt { *; }
-keep class androidx.compose.material.icons.**.AutoAwesomeKt { *; }
-keep class androidx.compose.material.icons.**.BluetoothKt { *; }
-keep class androidx.compose.material.icons.**.CallSplitKt { *; }
-keep class androidx.compose.material.icons.**.CheckKt { *; }
-keep class androidx.compose.material.icons.**.ChevronRightKt { *; }
-keep class androidx.compose.material.icons.**.CloseKt { *; }
-keep class androidx.compose.material.icons.**.ContentCopyKt { *; }
-keep class androidx.compose.material.icons.**.DeleteKt { *; }
-keep class androidx.compose.material.icons.**.DescriptionKt { *; }
-keep class androidx.compose.material.icons.**.DoneAllKt { *; }
-keep class androidx.compose.material.icons.**.DownloadKt { *; }
-keep class androidx.compose.material.icons.**.DragIndicatorKt { *; }
-keep class androidx.compose.material.icons.**.EditKt { *; }
-keep class androidx.compose.material.icons.**.EqualizerKt { *; }
-keep class androidx.compose.material.icons.**.FavoriteKt { *; }
-keep class androidx.compose.material.icons.**.FavoriteBorderKt { *; }
-keep class androidx.compose.material.icons.**.FolderKt { *; }
-keep class androidx.compose.material.icons.**.FullscreenKt { *; }
-keep class androidx.compose.material.icons.**.FullscreenExitKt { *; }
-keep class androidx.compose.material.icons.**.GraphicEqKt { *; }
-keep class androidx.compose.material.icons.**.HeadphonesKt { *; }
-keep class androidx.compose.material.icons.**.HistoryKt { *; }
-keep class androidx.compose.material.icons.**.HomeKt { *; }
-keep class androidx.compose.material.icons.**.InfoKt { *; }
-keep class androidx.compose.material.icons.**.KeyboardArrowDownKt { *; }
-keep class androidx.compose.material.icons.**.LyricsKt { *; }
-keep class androidx.compose.material.icons.**.MicKt { *; }
-keep class androidx.compose.material.icons.**.MicNoneKt { *; }
-keep class androidx.compose.material.icons.**.MoreVertKt { *; }
-keep class androidx.compose.material.icons.**.MusicNoteKt { *; }
-keep class androidx.compose.material.icons.**.OpenInFullKt { *; }
-keep class androidx.compose.material.icons.**.PauseKt { *; }
-keep class androidx.compose.material.icons.**.PersonKt { *; }
-keep class androidx.compose.material.icons.**.PhoneAndroidKt { *; }
-keep class androidx.compose.material.icons.**.PlayArrowKt { *; }
-keep class androidx.compose.material.icons.**.PlaylistAddKt { *; }
-keep class androidx.compose.material.icons.**.PlaylistPlayKt { *; }
-keep class androidx.compose.material.icons.**.QueueMusicKt { *; }
-keep class androidx.compose.material.icons.**.RefreshKt { *; }
-keep class androidx.compose.material.icons.**.RepeatKt { *; }
-keep class androidx.compose.material.icons.**.RepeatOneKt { *; }
-keep class androidx.compose.material.icons.**.SearchKt { *; }
-keep class androidx.compose.material.icons.**.SettingsKt { *; }
-keep class androidx.compose.material.icons.**.ShareKt { *; }
-keep class androidx.compose.material.icons.**.ShuffleKt { *; }
-keep class androidx.compose.material.icons.**.SkipNextKt { *; }
-keep class androidx.compose.material.icons.**.SkipPreviousKt { *; }
-keep class androidx.compose.material.icons.**.StarKt { *; }
-keep class androidx.compose.material.icons.**.SwapHorizKt { *; }
-keep class androidx.compose.material.icons.**.TimerKt { *; }
-keep class androidx.compose.material.icons.**.UndoKt { *; }
-keep class androidx.compose.material.icons.**.UnfoldLessKt { *; }
-keep class androidx.compose.material.icons.**.VolumeDownKt { *; }
-keep class androidx.compose.material.icons.**.VolumeOffKt { *; }
-keep class androidx.compose.material.icons.**.VolumeUpKt { *; }
