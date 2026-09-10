package com.xvox.music.features.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xvox.music.R
import com.xvox.music.core.design.theme.XvoxTheme
import com.xvox.music.core.ui.effects.xvoxPressScale

private val XvoxSettingsEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * An accordion label card for Settings with a smooth organic reveal animation.
 * Tapping expands its controls; tapping another collapses this one.
 */
@Composable
fun SettingsAccordionItem(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    content: @Composable () -> Unit
) {
    val colors = XvoxTheme.colors
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "accordion_chevron"
    )
    val surface by animateColorAsState(
        targetValue = if (expanded) colors.cardElevated else colors.card,
        animationSpec = tween(240, easing = XvoxSettingsEasing),
        label = "accordion_surface"
    )
    val accent by animateColorAsState(
        targetValue = if (expanded) colors.primaryAccent else colors.primaryText,
        animationSpec = tween(240, easing = XvoxSettingsEasing),
        label = "accordion_accent"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(surface)
            .border(0.7.dp, if (expanded) colors.primaryAccent.copy(alpha = 0.35f) else colors.cardBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .xvoxPressScale(pressedScale = 0.985f, onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (expanded) colors.primaryAccent.copy(alpha = 0.15f) else colors.cardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
            }

            Text(
                text = title,
                color = accent,
                fontSize = 13.sp,
                fontWeight = if (expanded) FontWeight.Bold else FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(R.drawable.ic_xvox_caret_right),
                contentDescription = null,
                tint = if (expanded) colors.primaryAccent else colors.mutedText,
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                expandFrom = Alignment.Top
            ) + fadeIn(
                animationSpec = tween(280, delayMillis = 40, easing = XvoxSettingsEasing)
            ),
            exit = shrinkVertically(
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 450f),
                shrinkTowards = Alignment.Top
            ) + fadeOut(
                animationSpec = tween(160, easing = XvoxSettingsEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(colors.cardBorder.copy(alpha = 0.5f))
                )
                Spacer(Modifier.height(10.dp))
                content()
            }
        }
    }
}
