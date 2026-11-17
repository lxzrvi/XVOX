package com.xvox.music.core.design.kit.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Apple SF Symbols geometry, converted from the android-sf-symbols-svg catalogue
 * (https://github.com/kingconan/android-sf-symbols-svg) with its own svg2compose pipeline.
 *
 * The vectors are tinted at the call site, exactly like `SfIcon` on iOS.
 */
object XvoxSfIcons {

    /** SF Symbol: house.fill */
    val HouseFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "HouseFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.8261f, 25.7282f)
                lineTo(12.8261f, 19.3369f)
                curveTo(12.8261f, 18.8369f, 13.163f, 18.5108f, 13.663f, 18.5108f)
                lineTo(17.8696f, 18.5108f)
                curveTo(18.3804f, 18.5108f, 18.6956f, 18.8369f, 18.6956f, 19.3369f)
                lineTo(18.6956f, 25.7282f)
                moveTo(6.1739f, 25.0869f)
                curveTo(6.1739f, 26.5978f, 7.087f, 27.4782f, 8.6196f, 27.4782f)
                lineTo(22.913f, 27.4782f)
                curveTo(24.4456f, 27.4782f, 25.3478f, 26.5978f, 25.3478f, 25.0869f)
                lineTo(25.3478f, 16.4565f)
                lineTo(16.413f, 8.9674f)
                curveTo(16f, 8.6195f, 15.5109f, 8.6304f, 15.1087f, 8.9674f)
                lineTo(6.1739f, 16.4565f)
                moveTo(3.7174f, 16.2282f)
                curveTo(4.0217f, 16.2282f, 4.2717f, 16.0652f, 4.5f, 15.8804f)
                lineTo(15.3804f, 6.7391f)
                curveTo(15.5f, 6.6413f, 15.6413f, 6.5869f, 15.7609f, 6.5869f)
                curveTo(15.8913f, 6.5869f, 16.0217f, 6.6413f, 16.1413f, 6.7391f)
                lineTo(27.0326f, 15.8804f)
                curveTo(27.25f, 16.0652f, 27.5f, 16.2282f, 27.8043f, 16.2282f)
                curveTo(28.3913f, 16.2282f, 28.7391f, 15.8043f, 28.7391f, 15.3587f)
                curveTo(28.7391f, 15.1087f, 28.6413f, 14.8478f, 28.3913f, 14.6521f)
                lineTo(17.0652f, 5.1413f)
                curveTo(16.6522f, 4.7935f, 16.2065f, 4.6196f, 15.7609f, 4.6196f)
                curveTo(15.3152f, 4.6196f, 14.8696f, 4.7935f, 14.4565f, 5.1413f)
                lineTo(3.1304f, 14.6521f)
                curveTo(2.8913f, 14.8478f, 2.7826f, 15.1087f, 2.7826f, 15.3587f)
                curveTo(2.7826f, 15.8043f, 3.1304f, 16.2282f, 3.7174f, 16.2282f)
                moveTo(22.8369f, 10.4565f)
                lineTo(25.5652f, 12.7608f)
                lineTo(25.5652f, 7.8587f)
                curveTo(25.5652f, 7.3804f, 25.2609f, 7.0761f, 24.7826f, 7.0761f)
                lineTo(23.6196f, 7.0761f)
                curveTo(23.1522f, 7.0761f, 22.8369f, 7.3804f, 22.8369f, 7.8587f)
            }
        }.build()
    }

    /** SF Symbol: magnifyingglass */
    val Magnifyingglass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Magnifyingglass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2.9474f, 13.5922f)
                curveTo(2.9474f, 19.3816f, 7.6579f, 24.0922f, 13.4474f, 24.0922f)
                curveTo(15.7368f, 24.0922f, 17.8289f, 23.3553f, 19.5526f, 22.1185f)
                lineTo(26.0263f, 28.6053f)
                curveTo(26.3289f, 28.908f, 26.7237f, 29.0527f, 27.1447f, 29.0527f)
                curveTo(28.0394f, 29.0527f, 28.6579f, 28.3816f, 28.6579f, 27.5001f)
                curveTo(28.6579f, 27.079f, 28.5f, 26.6974f, 28.2237f, 26.4211f)
                lineTo(21.7894f, 19.9474f)
                curveTo(23.1447f, 18.1843f, 23.9474f, 15.9869f, 23.9474f, 13.5922f)
                curveTo(23.9474f, 7.8027f, 19.2368f, 3.0922f, 13.4474f, 3.0922f)
                curveTo(7.6579f, 3.0922f, 2.9474f, 7.8027f, 2.9474f, 13.5922f)
                moveTo(5.1974f, 13.5922f)
                curveTo(5.1974f, 9.0395f, 8.8947f, 5.3422f, 13.4474f, 5.3422f)
                curveTo(18f, 5.3422f, 21.6974f, 9.0395f, 21.6974f, 13.5922f)
                curveTo(21.6974f, 18.1448f, 18f, 21.8422f, 13.4474f, 21.8422f)
                curveTo(8.8947f, 21.8422f, 5.1974f, 18.1448f, 5.1974f, 13.5922f)
            }
        }.build()
    }

    /** SF Symbol: music.microphone */
    val MusicMicrophone: ImageVector by lazy {
        ImageVector.Builder(
            name = "MusicMicrophone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.9872f, 29.5385f)
                curveTo(16.641f, 29.5385f, 17.1538f, 29.0256f, 17.1538f, 28.3846f)
                lineTo(17.1538f, 18.8718f)
                lineTo(20.9872f, 15.3333f)
                curveTo(22.4487f, 15.5256f, 23.9872f, 14.9231f, 25.3333f, 13.5769f)
                lineTo(18.282f, 6.5128f)
                curveTo(16.9231f, 7.859f, 16.359f, 9.3718f, 16.5641f, 10.8462f)
                lineTo(4.641f, 23.6795f)
                curveTo(4.1538f, 24.2051f, 4.0769f, 24.9359f, 4.6795f, 25.5385f)
                lineTo(3.0385f, 27.6538f)
                curveTo(2.8846f, 27.859f, 2.8718f, 28.1538f, 3.1026f, 28.3846f)
                lineTo(3.4744f, 28.7692f)
                curveTo(3.6923f, 28.9872f, 3.9872f, 29f, 4.218f, 28.8205f)
                lineTo(6.3333f, 27.1667f)
                curveTo(6.9103f, 27.7692f, 7.6538f, 27.6923f, 8.1667f, 27.2051f)
                lineTo(14.8205f, 21.0513f)
                lineTo(14.8205f, 28.3846f)
                curveTo(14.8205f, 29.0256f, 15.3333f, 29.5385f, 15.9872f, 29.5385f)
                moveTo(5.9615f, 24.7308f)
                lineTo(17.2692f, 12.7308f)
                curveTo(17.4872f, 13.1026f, 17.7436f, 13.4487f, 18.0641f, 13.7949f)
                curveTo(18.3718f, 14.1154f, 18.7308f, 14.4103f, 19.0641f, 14.6154f)
                lineTo(7.141f, 25.9103f)
                moveTo(19.5641f, 5.218f)
                lineTo(26.6282f, 12.2821f)
                curveTo(28.8846f, 10.0513f, 28.9743f, 7.1923f, 26.8077f, 5.0385f)
                curveTo(24.6538f, 2.9103f, 21.8205f, 2.9616f, 19.5641f, 5.218f)
            }
        }.build()
    }

    /** SF Symbol: opticaldisc.fill */
    val OpticaldiscFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "OpticaldiscFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.4079f, 28.9605f)
                curveTo(13.5395f, 29.2895f, 14.7368f, 29.4737f, 15.9474f, 29.4737f)
                curveTo(23.3552f, 29.4737f, 29.3684f, 23.4605f, 29.3684f, 16.0526f)
                curveTo(29.3684f, 14.8421f, 29.1973f, 13.6842f, 28.8816f, 12.579f)
                lineTo(21.8816f, 14.8421f)
                curveTo(21.9737f, 15.2369f, 22.0131f, 15.6316f, 22.0131f, 16.0526f)
                curveTo(22.0131f, 19.4079f, 19.3026f, 22.1184f, 15.9605f, 22.1316f)
                curveTo(15.5131f, 22.1316f, 15.0921f, 22.0921f, 14.671f, 21.9869f)
                moveTo(28.5263f, 11.4869f)
                curveTo(28.171f, 10.4737f, 27.6973f, 9.5395f, 27.1184f, 8.6316f)
                lineTo(21.1842f, 13.0263f)
                curveTo(21.3289f, 13.2632f, 21.4474f, 13.5132f, 21.5263f, 13.7763f)
                moveTo(26.4342f, 7.7237f)
                curveTo(24.9342f, 5.8026f, 22.9605f, 4.3553f, 20.6447f, 3.5263f)
                lineTo(18.3553f, 10.5526f)
                curveTo(19.1842f, 10.8816f, 19.9342f, 11.4211f, 20.5f, 12.1184f)
                moveTo(19.5658f, 3.1711f)
                curveTo(18.4079f, 2.829f, 17.1974f, 2.6316f, 15.9474f, 2.6316f)
                curveTo(8.5395f, 2.6316f, 2.5263f, 8.6448f, 2.5263f, 16.0526f)
                curveTo(2.5263f, 17.3158f, 2.7237f, 18.5658f, 3.0789f, 19.7237f)
                lineTo(10.0658f, 17.4474f)
                curveTo(9.9474f, 17f, 9.8947f, 16.5395f, 9.8947f, 16.0526f)
                curveTo(9.8947f, 12.75f, 12.6447f, 10.0263f, 15.9737f, 10.0263f)
                curveTo(16.421f, 10.0263f, 16.8553f, 10.0921f, 17.2763f, 10.1842f)
                moveTo(3.4474f, 20.8026f)
                curveTo(3.8158f, 21.7763f, 4.3026f, 22.7105f, 4.8684f, 23.5921f)
                lineTo(10.7895f, 19.2237f)
                curveTo(10.6579f, 19f, 10.5526f, 18.7632f, 10.4474f, 18.5132f)
                moveTo(5.5658f, 24.5132f)
                curveTo(7.0526f, 26.3684f, 9.0658f, 27.8421f, 11.3289f, 28.6053f)
                lineTo(13.5921f, 21.6316f)
                curveTo(12.7895f, 21.3158f, 12.0526f, 20.7895f, 11.5f, 20.1316f)
                moveTo(15.9474f, 20.7632f)
                curveTo(18.5526f, 20.7632f, 20.6579f, 18.6448f, 20.6579f, 16.0526f)
                curveTo(20.6579f, 13.4474f, 18.5526f, 11.3421f, 15.9474f, 11.3421f)
                curveTo(13.3552f, 11.3421f, 11.2368f, 13.4474f, 11.2368f, 16.0526f)
                curveTo(11.2368f, 18.6448f, 13.3552f, 20.7632f, 15.9474f, 20.7632f)
                moveTo(15.9474f, 19.25f)
                curveTo(14.171f, 19.25f, 12.75f, 17.829f, 12.75f, 16.0526f)
                curveTo(12.75f, 14.2763f, 14.171f, 12.8553f, 15.9474f, 12.8553f)
                curveTo(17.7237f, 12.8553f, 19.1447f, 14.2763f, 19.1447f, 16.0526f)
                curveTo(19.1447f, 17.829f, 17.7237f, 19.25f, 15.9474f, 19.25f)
                moveTo(15.9474f, 17.7895f)
                curveTo(16.9079f, 17.7895f, 17.6842f, 17f, 17.6842f, 16.0526f)
                curveTo(17.6842f, 15.0921f, 16.9079f, 14.3158f, 15.9474f, 14.3158f)
                curveTo(14.9868f, 14.3158f, 14.2105f, 15.0921f, 14.2105f, 16.0526f)
                curveTo(14.2105f, 17f, 14.9868f, 17.7895f, 15.9474f, 17.7895f)
            }
        }.build()
    }

    /** SF Symbol: music.note.list */
    val MusicNoteList: ImageVector by lazy {
        ImageVector.Builder(
            name = "MusicNoteList",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.05f, 18.8625f)
                lineTo(15.225f, 18.8625f)
                curveTo(15.7f, 18.8625f, 16.075f, 18.4875f, 16.075f, 18.0125f)
                curveTo(16.075f, 17.55f, 15.6875f, 17.175f, 15.225f, 17.175f)
                lineTo(4.05f, 17.175f)
                curveTo(3.575f, 17.175f, 3.2f, 17.55f, 3.2f, 18.0125f)
                curveTo(3.2f, 18.4875f, 3.5625f, 18.8625f, 4.05f, 18.8625f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.05f, 14.4125f)
                lineTo(15.225f, 14.4125f)
                curveTo(15.7f, 14.4125f, 16.075f, 14.025f, 16.075f, 13.55f)
                curveTo(16.075f, 13.0875f, 15.6875f, 12.725f, 15.225f, 12.725f)
                lineTo(4.05f, 12.725f)
                curveTo(3.575f, 12.725f, 3.2f, 13.0875f, 3.2f, 13.55f)
                curveTo(3.2f, 14.025f, 3.5625f, 14.4125f, 4.05f, 14.4125f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.05f, 9.9625f)
                lineTo(15.225f, 9.9625f)
                curveTo(15.6875f, 9.9625f, 16.075f, 9.575f, 16.075f, 9.1125f)
                curveTo(16.075f, 8.65f, 15.6875f, 8.275f, 15.225f, 8.275f)
                lineTo(4.05f, 8.275f)
                curveTo(3.575f, 8.275f, 3.2f, 8.65f, 3.2f, 9.1125f)
                curveTo(3.2f, 9.575f, 3.575f, 9.9625f, 4.05f, 9.9625f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(28.6875f, 9.375f)
                lineTo(28.6875f, 4.675f)
                curveTo(28.6875f, 4.0125f, 28.15f, 3.5875f, 27.5125f, 3.7125f)
                lineTo(21.0875f, 5.1125f)
                curveTo(20.2875f, 5.2875f, 19.85f, 5.725f, 19.85f, 6.425f)
                lineTo(19.875f, 20.325f)
                curveTo(19.9375f, 20.9375f, 19.65f, 21.3375f, 19.1f, 21.45f)
                lineTo(17.1125f, 21.8625f)
                curveTo(14.6125f, 22.3875f, 13.4375f, 23.6625f, 13.4375f, 25.55f)
                curveTo(13.4375f, 27.4625f, 14.9125f, 28.8f, 16.9875f, 28.8f)
                curveTo(18.825f, 28.8f, 21.575f, 27.45f, 21.575f, 23.8125f)
                lineTo(21.575f, 12.375f)
                curveTo(21.575f, 11.7125f, 21.7f, 11.575f, 22.2875f, 11.45f)
                lineTo(28f, 10.2f)
                curveTo(28.425f, 10.1125f, 28.6875f, 9.7875f, 28.6875f, 9.375f)
            }
        }.build()
    }
}
