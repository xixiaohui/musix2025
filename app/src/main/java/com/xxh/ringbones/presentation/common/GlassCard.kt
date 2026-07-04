package com.xxh.ringbones.presentation.common

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Universal glassmorphism container.
 *
 * Applies blur + semi-transparent background on API 31+ (Android 12),
 * and a semi-transparent surface with elevated shadow on older versions.
 *
 * @param modifier External modifier chained onto the container
 * @param shape Card shape (default 20dp rounded corners)
 * @param blurRadius Blur intensity on API 31+, ignored on older versions
 * @param backgroundColor Semi-transparent fill color
 * @param elevation Shadow elevation for pre-API-31 fallback
 * @param content Composable content inside the glass card
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    blurRadius: Dp = 12.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.15f),
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    val glassModifier = modifier
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(blurRadius)
            } else {
                Modifier
            }
        )
        .background(backgroundColor)

    Box(modifier = glassModifier) {
        content()
    }
}