package com.wobok.bibilili.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * 全屏播放锁横屏，退出时放回。
 *
 * 用 `SENSOR_LANDSCAPE` 而不是 `LANDSCAPE`，这样横过来的两个方向都认，
 * 不会出现「手机转了 180 度画面倒过来」。
 */
@Composable
fun LockOrientation(landscape: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(landscape) {
        activity.requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
