package com.wobok.bibilili.player

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 播放手势层。竖屏与全屏共用同一份实现，行为完全一致。
 *
 * 画面横向三等分：
 *  - 双击**左** 1/3 → 快退；双击**右** 1/3 → 快进（步长读设置，默认 10s）
 *  - 双击**中间** 1/3 → 暂停 / 播放
 *  - 单击任意处 → 显示 / 隐藏控件
 *  - 长按任意处 → 临时倍速，松手恢复
 *  - 左右拖动 → 调进度，拖动中出小窗预览
 *
 * [enabled] 为 false 时整层不收事件：菜单开着的时候还响应长按，
 * 会变成「在倍速菜单上按住不放，速度跟着乱跳」。
 */
@Composable
fun GestureLayer(
    enabled: Boolean,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onDragStart: () -> Unit,
    /** 每像素折算多少毫秒由调用方决定：全屏和竖屏的画面宽度差得远。 */
    onDrag: (dxPixels: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val third = size.width / 3f
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        when {
                            offset.x < third -> onDoubleTapLeft()
                            offset.x > third * 2 -> onDoubleTapRight()
                            else -> onDoubleTapCenter()
                        }
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
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                ) { change, amount ->
                    change.consume()
                    onDrag(amount)
                }
            }
    )
}
