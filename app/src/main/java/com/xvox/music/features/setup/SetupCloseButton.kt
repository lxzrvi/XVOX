package com.xvox.music.features.setup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.xvox.music.core.design.theme.XvoxTheme

@Composable
fun SetupCloseButton(
    onClick: () -> Unit
) {
    val colors = XvoxTheme.colors

    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = colors.card,
        contentColor = colors.primaryText,
        border = BorderStroke(
            width = 1.dp,
            color = colors.cardBorder
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(18.dp)
            ) {
                val stroke = 2.1.dp.toPx()

                drawLine(
                    color = colors.primaryText,
                    start = Offset(
                        size.width * 0.2f,
                        size.height * 0.2f
                    ),
                    end = Offset(
                        size.width * 0.8f,
                        size.height * 0.8f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = colors.primaryText,
                    start = Offset(
                        size.width * 0.8f,
                        size.height * 0.2f
                    ),
                    end = Offset(
                        size.width * 0.2f,
                        size.height * 0.8f
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
