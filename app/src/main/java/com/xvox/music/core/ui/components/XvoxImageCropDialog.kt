package com.xvox.music.core.ui.components

import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Aspect-constrained image cropper used by artwork/profile/playlist flows. GIF inputs stay GIFs:
 * the dialog previews their first frame while export samples up to 90 frames and writes an
 * animated, quantized crop. Static images retain the established JPEG output path.
 */
@Composable
fun XvoxImageCropDialog(
    sourceUri: Uri,
    aspectRatio: Float = 1f,
    isCircle: Boolean = true,
    title: String = "Crop image",
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    val colors = XvoxTheme.colors
    val scope = rememberCoroutineScope()
    val aspect = if (isCircle) 1f else aspectRatio.coerceIn(.25f, 4f)
    var source by remember(sourceUri) { mutableStateOf<XvoxGifCrop.CropSource?>(null) }
    var loadFailed by remember(sourceUri) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var zoom by remember(source) { mutableStateOf(1f) }
    var pan by remember(source) { mutableStateOf(Offset.Zero) }
    // Compose draw state is intentionally not used for this ref: it is refreshed by Canvas every
    // frame and only read on Save, so pinching/panning never causes an extra composition loop.
    val cropRectRef = remember(source) { FloatArray(4) }

    LaunchedEffect(sourceUri) {
        source = null
        loadFailed = false
        val loaded = XvoxGifCrop.loadCropSource(context, sourceUri)
        source = loaded
        loadFailed = loaded == null
    }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !saving,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.92f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.card)
                .border(1.dp, colors.cardBorder.copy(alpha = .86f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            when {
                source == null && !loadFailed -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Preparing image…", color = colors.secondaryText, fontSize = 14.sp)
                    }
                }
                source == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Could not read this image", color = colors.primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Choose a smaller valid image or GIF and try again.",
                            color = colors.secondaryText,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        XvoxCropButton("Close", prominent = true, onClick = onDismiss)
                    }
                }
                else -> {
                    val activeSource = source ?: return@Box
                    val isGif = activeSource is XvoxGifCrop.CropSource.Gif
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(title, color = colors.primaryText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = if (isGif) "Animated GIF · crop keeps motion and samples up to 90 frames" else "Pinch to zoom and drag to position the crop",
                                color = colors.secondaryText,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        CropViewport(
                            source = activeSource,
                            aspect = aspect,
                            isCircle = isCircle,
                            zoom = zoom,
                            pan = pan,
                            onZoomChange = { zoom = it },
                            onPanChange = { pan = it },
                            cropRectRef = cropRectRef,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            XvoxCropButton("Reset", Modifier.weight(1f), enabled = !saving) {
                                zoom = 1f
                                pan = Offset.Zero
                            }
                            XvoxCropButton("−", Modifier.width(42.dp), enabled = !saving) {
                                zoom = (zoom / 1.18f).coerceIn(1f, 6f)
                                if (zoom == 1f) pan = Offset.Zero
                            }
                            Text(
                                text = "${(zoom * 100).roundToInt()}%",
                                color = colors.secondaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(38.dp)
                            )
                            XvoxCropButton("+", Modifier.width(42.dp), enabled = !saving) {
                                zoom = (zoom * 1.18f).coerceIn(1f, 6f)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            XvoxCropButton("Cancel", Modifier.weight(1f), enabled = !saving, onClick = onDismiss)
                            XvoxCropButton(
                                label = if (saving) "Saving…" else if (isGif) "Save GIF" else "Save crop",
                                modifier = Modifier.weight(1f),
                                prominent = true,
                                enabled = !saving,
                                onClick = {
                                    val crop = RectF(cropRectRef[0], cropRectRef[1], cropRectRef[2], cropRectRef[3])
                                    if (crop.width() > 0f && crop.height() > 0f) {
                                        saving = true
                                        scope.launch {
                                            val output = XvoxGifCrop.exportCrop(context, activeSource, crop, aspect)
                                            saving = false
                                            if (output != null) {
                                                onCropped(output)
                                                onDismiss()
                                            } else {
                                                loadFailed = true
                                                source = null
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CropViewport(
    source: XvoxGifCrop.CropSource,
    aspect: Float,
    isCircle: Boolean,
    zoom: Float,
    pan: Offset,
    onZoomChange: (Float) -> Unit,
    onPanChange: (Offset) -> Unit,
    cropRectRef: FloatArray,
    modifier: Modifier = Modifier
) {
    val colors = XvoxTheme.colors
    val density = LocalDensity.current
    val preview = when (source) {
        is XvoxGifCrop.CropSource.Static -> source.bitmap
        is XvoxGifCrop.CropSource.Gif -> source.firstFrame
    }.asImageBitmap()

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = .86f))
            .border(.8.dp, colors.cardBorder.copy(alpha = .72f), RoundedCornerShape(18.dp))
    ) {
        val viewportWidth = with(density) { maxWidth.toPx() }
        val viewportHeight = with(density) { maxHeight.toPx() }
        val wantedWidth = min(viewportWidth - with(density) { 18.dp.toPx() },
            (viewportHeight - with(density) { 18.dp.toPx() }) * aspect)
        val cropWidth = wantedWidth.coerceAtLeast(1f)
        val cropHeight = (cropWidth / aspect).coerceAtLeast(1f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(source, cropWidth, cropHeight, aspect) {
                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                        val nextZoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                        val baseScale = max(cropWidth / source.width, cropHeight / source.height)
                        val displayWidth = source.width * baseScale * nextZoom
                        val displayHeight = source.height * baseScale * nextZoom
                        val maxPanX = ((displayWidth - cropWidth) * .5f).coerceAtLeast(0f)
                        val maxPanY = ((displayHeight - cropHeight) * .5f).coerceAtLeast(0f)
                        onZoomChange(nextZoom)
                        onPanChange(
                            Offset(
                                (pan.x + gesturePan.x).coerceIn(-maxPanX, maxPanX),
                                (pan.y + gesturePan.y).coerceIn(-maxPanY, maxPanY)
                            )
                        )
                    }
                }
        ) {
            val cropLeft = (size.width - cropWidth) * .5f
            val cropTop = (size.height - cropHeight) * .5f
            val cropBounds = Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight)
            val baseScale = max(cropWidth / source.width, cropHeight / source.height)
            val imageScale = baseScale * zoom
            val displayWidth = source.width * imageScale
            val displayHeight = source.height * imageScale
            val imageLeft = cropBounds.center.x - displayWidth * .5f + pan.x
            val imageTop = cropBounds.center.y - displayHeight * .5f + pan.y

            drawImage(
                image = preview,
                dstOffset = IntOffset(imageLeft.roundToInt(), imageTop.roundToInt()),
                dstSize = IntSize(displayWidth.roundToInt().coerceAtLeast(1), displayHeight.roundToInt().coerceAtLeast(1))
            )

            val shadePath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                if (isCircle) addOval(cropBounds) else addRoundRect(RoundRect(cropBounds, CornerRadius(14.dp.toPx())))
            }
            drawPath(shadePath, Color.Black.copy(alpha = .62f))
            if (isCircle) {
                drawCircle(
                    color = colors.primaryAccent,
                    radius = cropWidth * .5f,
                    center = cropBounds.center,
                    style = Stroke(width = 2.dp.toPx())
                )
            } else {
                drawRoundRect(
                    color = colors.primaryAccent,
                    topLeft = cropBounds.topLeft,
                    size = cropBounds.size,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            // Small corner dots make the crop bounds explicit without offering a freeform resize;
            // aspect remains locked while users only pan and pinch the image underneath.
            if (!isCircle) {
                listOf(cropBounds.topLeft, cropBounds.topRight, cropBounds.bottomLeft, cropBounds.bottomRight)
                    .forEach { point -> drawCircle(colors.primaryAccent, 4.dp.toPx(), point) }
            }

            val rawLeft = (cropLeft - imageLeft) / imageScale
            val rawTop = (cropTop - imageTop) / imageScale
            val rawRight = (cropLeft + cropWidth - imageLeft) / imageScale
            val rawBottom = (cropTop + cropHeight - imageTop) / imageScale
            val selectedWidth = (rawRight - rawLeft).coerceIn(1f, source.width.toFloat())
            val selectedHeight = (rawBottom - rawTop).coerceIn(1f, source.height.toFloat())
            val selectedLeft = rawLeft.coerceIn(0f, source.width - selectedWidth)
            val selectedTop = rawTop.coerceIn(0f, source.height - selectedHeight)
            cropRectRef[0] = selectedLeft
            cropRectRef[1] = selectedTop
            cropRectRef[2] = selectedLeft + selectedWidth
            cropRectRef[3] = selectedTop + selectedHeight
        }
    }
}

@Composable
private fun XvoxCropButton(
    label: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(
                when {
                    !enabled -> colors.cardElevated.copy(alpha = .50f)
                    prominent -> colors.primaryAccent
                    else -> colors.cardElevated
                }
            )
            .border(.8.dp, if (prominent) Color.Transparent else colors.cardBorder.copy(alpha = .74f), shape)
            .then(if (enabled) Modifier.xvoxPressScale(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (prominent && enabled) colors.background else colors.primaryText.copy(alpha = if (enabled) 1f else .46f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
