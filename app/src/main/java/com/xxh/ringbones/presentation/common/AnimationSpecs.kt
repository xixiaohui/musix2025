package com.xxh.ringbones.presentation.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** Default spring animation for button clicks, card scaling, and press feedback. */
val springDefault = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/** Bouncy spring for list entrance stagger animations. */
val springBouncy = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

/** Standard tween for page transitions and SharedTransition. */
val tweenTransition = tween<Float>(
    durationMillis = 350,
    easing = FastOutSlowInEasing
)

/** Delay per item for staggered list entrance animations (ms). */
const val ENTER_DELAY_MS = 50

/** Quick tween for small opacity animations. */
val tweenFast = tween<Float>(durationMillis = 200)