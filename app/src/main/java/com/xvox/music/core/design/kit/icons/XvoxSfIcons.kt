package com.xvox.music.core.design.kit.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.neoruaa.glasmio.icons.GlasmioIconResolver

/**
 * Apple SF Symbols geometry, converted from the android-sf-symbols-svg catalogue
 * (https://github.com/kingconan/android-sf-symbols-svg) with its own svg2compose pipeline.
 *
 * Glasmio's own navigation bar asks for symbols by system name, so [XvoxSfResolver] hands it these
 * real vectors instead of the generic Material fallback. No Apple font is bundled or required.
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

    /** SF Symbol: microphone.fill */
    val MicrophoneFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "MicrophoneFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.3158f, 16.0658f)
                curveTo(6.3158f, 21.2237f, 9.7237f, 24.8421f, 14.7368f, 25.2632f)
                lineTo(14.7368f, 28.3289f)
                lineTo(9.8553f, 28.3289f)
                curveTo(9.3026f, 28.3289f, 8.8553f, 28.7763f, 8.8553f, 29.3289f)
                curveTo(8.8553f, 29.8816f, 9.3026f, 30.3158f, 9.8553f, 30.3158f)
                lineTo(21.6052f, 30.3158f)
                curveTo(22.1579f, 30.3158f, 22.6052f, 29.8816f, 22.6052f, 29.3289f)
                curveTo(22.6052f, 28.7763f, 22.1579f, 28.3289f, 21.6052f, 28.3289f)
                lineTo(16.7237f, 28.3289f)
                lineTo(16.7237f, 25.2632f)
                curveTo(21.75f, 24.8421f, 25.1447f, 21.2237f, 25.1447f, 16.0658f)
                lineTo(25.1447f, 13.3947f)
                curveTo(25.1447f, 12.8421f, 24.7105f, 12.4079f, 24.1579f, 12.4079f)
                curveTo(23.6052f, 12.4079f, 23.1579f, 12.8421f, 23.1579f, 13.3947f)
                lineTo(23.1579f, 15.9869f)
                curveTo(23.1579f, 20.4737f, 20.2368f, 23.4474f, 15.7368f, 23.4474f)
                curveTo(11.2237f, 23.4474f, 8.3026f, 20.4737f, 8.3026f, 15.9869f)
                lineTo(8.3026f, 13.3947f)
                curveTo(8.3026f, 12.8421f, 7.8684f, 12.4079f, 7.3026f, 12.4079f)
                curveTo(6.75f, 12.4079f, 6.3158f, 12.8421f, 6.3158f, 13.3947f)
                moveTo(15.7368f, 20.6184f)
                curveTo(18.3552f, 20.6184f, 20.1184f, 18.6579f, 20.1184f, 15.8553f)
                lineTo(20.1184f, 7.0526f)
                curveTo(20.1184f, 4.2369f, 18.3552f, 2.2895f, 15.7368f, 2.2895f)
                curveTo(13.1052f, 2.2895f, 11.3421f, 4.2369f, 11.3421f, 7.0526f)
                lineTo(11.3421f, 15.8553f)
                curveTo(11.3421f, 18.6579f, 13.1052f, 20.6184f, 15.7368f, 20.6184f)
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


    /** SF Symbol: xmark */
    val Xmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "Xmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.2794f, 27.7207f)
                curveTo(4.7941f, 28.2207f, 5.6471f, 28.2207f, 6.1471f, 27.7207f)
                lineTo(15.5588f, 18.3089f)
                lineTo(24.9706f, 27.7207f)
                curveTo(25.4706f, 28.2207f, 26.3382f, 28.2354f, 26.8382f, 27.7207f)
                curveTo(27.3382f, 27.206f, 27.3382f, 26.3677f, 26.8382f, 25.8677f)
                lineTo(17.4265f, 16.4413f)
                lineTo(26.8382f, 7.0295f)
                curveTo(27.3382f, 6.5295f, 27.3529f, 5.6766f, 26.8382f, 5.1766f)
                curveTo(26.3235f, 4.6619f, 25.4706f, 4.6619f, 24.9706f, 5.1766f)
                lineTo(15.5588f, 14.5883f)
                lineTo(6.1471f, 5.1766f)
                curveTo(5.6471f, 4.6619f, 4.7794f, 4.6472f, 4.2794f, 5.1766f)
                curveTo(3.7794f, 5.6913f, 3.7794f, 6.5295f, 4.2794f, 7.0295f)
                lineTo(13.6912f, 16.4413f)
                lineTo(4.2794f, 25.8677f)
                curveTo(3.7794f, 26.3677f, 3.7647f, 27.2207f, 4.2794f, 27.7207f)
            }
        }.build()
    }

    /** SF Symbol: gearshape.fill */
    val GearshapeFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "GearshapeFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.6795f, 29.5384f)
                lineTo(17.0385f, 29.5384f)
                curveTo(17.7051f, 29.5384f, 18.1795f, 29.141f, 18.3333f, 28.4872f)
                lineTo(19f, 25.6667f)
                curveTo(19.5f, 25.5f, 19.9872f, 25.3077f, 20.4102f, 25.1026f)
                lineTo(22.8846f, 26.6154f)
                curveTo(23.4359f, 26.9615f, 24.0641f, 26.9102f, 24.5256f, 26.4487f)
                lineTo(26.1795f, 24.8077f)
                curveTo(26.641f, 24.3461f, 26.7051f, 23.6795f, 26.3333f, 23.1282f)
                lineTo(24.8205f, 20.6795f)
                curveTo(25.0384f, 20.2308f, 25.2308f, 19.7692f, 25.3718f, 19.3077f)
                lineTo(28.2179f, 18.6282f)
                curveTo(28.8718f, 18.4744f, 29.2564f, 18f, 29.2564f, 17.3333f)
                lineTo(29.2564f, 15.0128f)
                curveTo(29.2564f, 14.359f, 28.8718f, 13.8846f, 28.2179f, 13.7308f)
                lineTo(25.3974f, 13.0513f)
                curveTo(25.2308f, 12.5256f, 25.0256f, 12.0513f, 24.8461f, 11.6538f)
                lineTo(26.359f, 9.1667f)
                curveTo(26.7051f, 8.6026f, 26.6795f, 8f, 26.2051f, 7.5256f)
                lineTo(24.5256f, 5.8718f)
                curveTo(24.0513f, 5.4359f, 23.4743f, 5.3333f, 22.9231f, 5.6795f)
                lineTo(20.4102f, 7.2308f)
                curveTo(20f, 7.0128f, 19.5256f, 6.8205f, 19f, 6.6538f)
                lineTo(18.3333f, 3.7949f)
                curveTo(18.1795f, 3.141f, 17.7051f, 2.7436f, 17.0385f, 2.7436f)
                lineTo(14.6795f, 2.7436f)
                curveTo(14.0128f, 2.7436f, 13.5384f, 3.141f, 13.3718f, 3.7949f)
                lineTo(12.7051f, 6.6282f)
                curveTo(12.2051f, 6.7949f, 11.7179f, 6.9872f, 11.282f, 7.2179f)
                lineTo(8.7949f, 5.6795f)
                curveTo(8.2436f, 5.3333f, 7.641f, 5.4103f, 7.1795f, 5.8718f)
                lineTo(5.5128f, 7.5256f)
                curveTo(5.0385f, 8f, 5f, 8.6026f, 5.359f, 9.1667f)
                lineTo(6.859f, 11.6538f)
                curveTo(6.6923f, 12.0513f, 6.4872f, 12.5256f, 6.3205f, 13.0513f)
                lineTo(3.5f, 13.7308f)
                curveTo(2.8462f, 13.8846f, 2.4615f, 14.359f, 2.4615f, 15.0128f)
                lineTo(2.4615f, 17.3333f)
                curveTo(2.4615f, 18f, 2.8462f, 18.4744f, 3.5f, 18.6282f)
                lineTo(6.3461f, 19.3077f)
                curveTo(6.4872f, 19.7692f, 6.6795f, 20.2308f, 6.8846f, 20.6795f)
                lineTo(5.3846f, 23.1282f)
                curveTo(5f, 23.6795f, 5.0769f, 24.3461f, 5.5385f, 24.8077f)
                lineTo(7.1795f, 26.4487f)
                curveTo(7.641f, 26.9102f, 8.282f, 26.9615f, 8.8333f, 26.6154f)
                lineTo(11.2949f, 25.1026f)
                curveTo(11.7308f, 25.3077f, 12.2051f, 25.5f, 12.7051f, 25.6667f)
                lineTo(13.3718f, 28.4872f)
                curveTo(13.5384f, 29.141f, 14.0128f, 29.5384f, 14.6795f, 29.5384f)
                moveTo(15.859f, 20.6667f)
                curveTo(13.359f, 20.6667f, 11.3333f, 18.641f, 11.3333f, 16.141f)
                curveTo(11.3333f, 13.641f, 13.359f, 11.6154f, 15.859f, 11.6154f)
                curveTo(18.359f, 11.6154f, 20.3846f, 13.641f, 20.3846f, 16.141f)
                curveTo(20.3846f, 18.641f, 18.359f, 20.6667f, 15.859f, 20.6667f)
            }
        }.build()
    }

    private fun chevron(name: String, vararg pts: Float): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 3.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(pts[0], pts[1])
                lineTo(pts[2], pts[3])
                lineTo(pts[4], pts[5])
            }
        }.build()

    /** SF Symbol: chevron.left / chevron.backward - the Settings back button. */
    val ChevronLeft: ImageVector by lazy { chevron("ChevronLeft", 15f, 4.8f, 7.8f, 12f, 15f, 19.2f) }
    val ChevronRight: ImageVector by lazy { chevron("ChevronRight", 9f, 4.8f, 16.2f, 12f, 9f, 19.2f) }
    val ChevronDown: ImageVector by lazy { chevron("ChevronDown", 4.8f, 9f, 12f, 16.2f, 19.2f, 9f) }
    val ChevronUp: ImageVector by lazy { chevron("ChevronUp", 4.8f, 15f, 12f, 7.8f, 19.2f, 15f) }

    private fun PathBuilder.roundRect(x1: Float, y1: Float, x2: Float, y2: Float, r: Float) {
        moveTo(x1 + r, y1)
        lineTo(x2 - r, y1)
        quadTo(x2, y1, x2, y1 + r)
        lineTo(x2, y2 - r)
        quadTo(x2, y2, x2 - r, y2)
        lineTo(x1 + r, y2)
        quadTo(x1, y2, x1, y2 - r)
        lineTo(x1, y1 + r)
        quadTo(x1, y1, x1 + r, y1)
        close()
    }

    /** music.note.square.stack.fill - Apple Music's Library glyph: two strips over a note tile. */
    val LibraryFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "LibraryFill", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                roundRect(6f, 2.2f, 18f, 4.4f, 1.1f)
                roundRect(4.2f, 5.4f, 19.8f, 7.8f, 1.2f)
            }
            // The tile and the note share one path; even-odd turns the note into a cut-out.
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                roundRect(2.6f, 9f, 21.4f, 22f, 3.4f)
                // note head
                moveTo(8.4f, 18.2f)
                arcTo(1.9f, 1.9f, 0f, true, true, 12.2f, 18.2f)
                arcTo(1.9f, 1.9f, 0f, true, true, 8.4f, 18.2f)
                close()
                // stem and flag
                moveTo(11.5f, 12.4f)
                lineTo(15.8f, 13.3f)
                lineTo(15.8f, 14.9f)
                lineTo(12.9f, 14.3f)
                lineTo(12.9f, 18.2f)
                lineTo(11.5f, 18.2f)
                close()
            }
        }.build()
    }

    /** sun.max.fill - disc with eight round rays. */
    val SunFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "SunFill", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 7f)
                arcTo(5f, 5f, 0f, true, true, 12f, 17f)
                arcTo(5f, 5f, 0f, true, true, 12f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = Math.cos(a).toFloat()
                    val s = Math.sin(a).toFloat()
                    moveTo(12f + c * 8.4f, 12f + s * 8.4f)
                    lineTo(12f + c * 10.6f, 12f + s * 10.6f)
                }
            }
        }.build()
    }

    /** moon.fill - a crescent: the big disc minus an offset disc. */
    val MoonFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoonFill", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.86f, 13.61f)
                arcTo(9f, 9f, 0f, true, true, 10.4f, 3.15f)
                arcTo(7.5f, 7.5f, 0f, false, false, 20.86f, 13.61f)
                close()
            }
        }.build()
    }

    /** circle.lefthalf.filled - the "follow the system" theme glyph. */
    val CircleHalfFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "CircleHalfFill", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, false, false, 12f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
            ) {
                moveTo(12f, 3.2f)
                arcTo(8.8f, 8.8f, 0f, true, true, 12f, 20.8f)
                arcTo(8.8f, 8.8f, 0f, true, true, 12f, 3.2f)
                close()
            }
        }.build()
    }

    /** Maps the SF symbol names Glasmio asks for onto the real vector geometry above. */
    val Resolver: GlasmioIconResolver = GlasmioIconResolver { systemName ->
        when {
            systemName.startsWith("chevron.left") || systemName.startsWith("chevron.backward") -> ChevronLeft
            systemName.startsWith("chevron.right") || systemName.startsWith("chevron.forward") -> ChevronRight
            systemName.startsWith("chevron.down") -> ChevronDown
            systemName.startsWith("chevron.up") -> ChevronUp
            systemName.startsWith("sun") -> SunFill
            systemName.startsWith("moon") -> MoonFill
            systemName.startsWith("circle.lefthalf") -> CircleHalfFill
            systemName.startsWith("house") -> HouseFill
            systemName.startsWith("magnifyingglass") -> Magnifyingglass
            systemName == "mic" || systemName.startsWith("microphone") -> MicrophoneFill
            // Albums: Glasmio's stacked-square symbol, drawn as the disc SF geometry.
            systemName.startsWith("music.note.square.stack") -> LibraryFill
            systemName.contains("stack") -> OpticaldiscFill
            systemName.startsWith("music.note.list") -> MusicNoteList
            systemName.startsWith("music.note") -> MusicNoteList
            systemName.startsWith("xmark") || systemName == "close" -> Xmark
            systemName.startsWith("gear") -> GearshapeFill
            else -> null
        }
    }
}
