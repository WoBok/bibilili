package com.wobok.bibilili.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 播放手势层。竖屏与全屏共用同一份实现，行为完全一致。
 *
 *  - 双击左半屏 → 快退；双击右半屏 → 快进（步长读设置，默认 10s）
 *  - 长按任意处 → 临时倍速（默认 2.0X），松手恢复
 *  - 单击 → 显示/隐藏控件
 *
 * 用 `detectTapGestures` 而不是自己数 down/up：双击与长按的判定阈值
 * 交给框架，比手写计时器稳。
 */
@Composable
fun GestureLayer(
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val halfWidth = size.width / 2f
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        if (offset.x < halfWidth) onDoubleTapLeft() else onDoubleTapRight()
                    },
                    onPress = {
                        // onPress 的挂起点结束即代表手指抬起，用它来收尾长按倍速，
                        // 比在 onLongPress 里另起协程等抬手要可靠。
                        try {
                            awaitRelease()
                        } finally {
                            onLongPressEnd()
                        }
                    },
                    onLongPress = { onLongPressStart() },
                )
            }
    )
}
