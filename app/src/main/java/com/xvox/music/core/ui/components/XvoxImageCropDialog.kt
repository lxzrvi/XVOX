package com.xvox.music.core.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xvox.music.core.design.theme.XvoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun XvoxImageCropDialog(
    sourceUri: Uri,
    isCircle: Boolean = true,
    aspectRatio: Float = 1f,
    onCropped: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    val bytes = input.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

                    val maxDim = 1600
                    var sampleSize = 1
                    while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colors = XvoxTheme.colors
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crop Photo",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF181818))
                        .onSizeChanged { containerSize = it }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset = Offset(offset.x + pan.x, offset.y + pan.y)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val currentBitmap = bitmap
                    if (currentBitmap != null && containerSize.width > 0 && containerSize.height > 0) {
                        val imageBitmap = remember(currentBitmap) { currentBitmap.asImageBitmap() }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            val baseScale = max(
                                canvasWidth / currentBitmap.width.toFloat(),
                                canvasHeight / currentBitmap.height.toFloat()
                            )
                            val renderedWidth = currentBitmap.width * baseScale * scale
                            val renderedHeight = currentBitmap.height * baseScale * scale

                            val dstX = (canvasWidth - renderedWidth) / 2f + offset.x
                            val dstY = (canvasHeight - renderedHeight) / 2f + offset.y

                            drawImage(
                                image = imageBitmap,
                                dstOffset = IntOffset(dstX.toInt(), dstY.toInt()),
                                dstSize = IntSize(renderedWidth.toInt(), renderedHeight.toInt())
                            )

                            val overlayColor = Color.Black.copy(alpha = 0.65f)
                            val strokeColor = Color.White.copy(alpha = 0.85f)

                            if (isCircle) {
                                val cropRadius = min(canvasWidth, canvasHeight) * 0.45f
                                val centerOffset = Offset(canvasWidth / 2f, canvasHeight / 2f)
                                val path = Path().apply {
                                    addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                                    addOval(
                                        Rect(
                                            centerOffset.x - cropRadius,
                                            centerOffset.y - cropRadius,
                                            centerOffset.x + cropRadius,
                                            centerOffset.y + cropRadius
                                        )
                                    )
                                }
                                drawPath(path, overlayColor, blendMode = BlendMode.SrcOver)
                                drawCircle(
                                    color = strokeColor,
                                    radius = cropRadius,
                                    center = centerOffset,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            } else {
                                val cropWidth = canvasWidth * 0.94f
                                val cropHeight = canvasHeight * 0.94f
                                val cropRect = Rect(
                                    (canvasWidth - cropWidth) / 2f,
                                    (canvasHeight - cropHeight) / 2f,
                                    (canvasWidth + cropWidth) / 2f,
                                    (canvasHeight + cropHeight) / 2f
                                )
                                val path = Path().apply {
                                    addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                                    addRect(cropRect)
                                }
                                drawPath(path, overlayColor, blendMode = BlendMode.SrcOver)
                                drawRect(
                                    color = strokeColor,
                                    topLeft = Offset(cropRect.left, cropRect.top),
                                    size = Size(cropWidth, cropHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val srcBmp = bitmap
                            if (srcBmp != null && containerSize.width > 0 && containerSize.height > 0) {
                                val cW = containerSize.width.toFloat()
                                val cH = containerSize.height.toFloat()
                                val baseScale = max(
                                    cW / srcBmp.width.toFloat(),
                                    cH / srcBmp.height.toFloat()
                                )
                                val totalScale = baseScale * scale
                                val cropW = if (isCircle) min(cW, cH) * 0.90f else cW * 0.94f
                                val cropH = if (isCircle) min(cW, cH) * 0.90f else cH * 0.94f

                                val cropLeftInCanvas = (cW - cropW) / 2f
                                val cropTopInCanvas = (cH - cropH) / 2f

                                val renderedWidth = srcBmp.width * totalScale
                                val renderedHeight = srcBmp.height * totalScale
                                val imgLeftInCanvas = (cW - renderedWidth) / 2f + offset.x
                                val imgTopInCanvas = (cH - renderedHeight) / 2f + offset.y

                                val srcCropX = ((cropLeftInCanvas - imgLeftInCanvas) / totalScale).coerceIn(0f, srcBmp.width.toFloat())
                                val srcCropY = ((cropTopInCanvas - imgTopInCanvas) / totalScale).coerceIn(0f, srcBmp.height.toFloat())
                                val srcCropW = (cropW / totalScale).coerceIn(1f, srcBmp.width - srcCropX)
                                val srcCropH = (cropH / totalScale).coerceIn(1f, srcBmp.height - srcCropY)

                                val cropped = Bitmap.createBitmap(
                                    srcBmp,
                                    srcCropX.toInt(),
                                    srcCropY.toInt(),
                                    srcCropW.toInt(),
                                    srcCropH.toInt()
                                )

                                val file = File(context.filesDir, "xvox_crop_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(file).use { out ->
                                    cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                }
                                onCropped(Uri.fromFile(file))
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.background
                        )
                    ) {
                        Text("Apply", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
}
