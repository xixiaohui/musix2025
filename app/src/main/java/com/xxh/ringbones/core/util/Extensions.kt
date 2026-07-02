package com.xxh.ringbones.core.util

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity

/**
 * Walk up the Context chain to find the hosting Activity.
 * Returns null if called from a non-Activity context (e.g. Service).
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
