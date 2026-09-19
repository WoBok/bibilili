package com.wobok.bibilili.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.wobok.bibilili.data.auth.QrLoginRepository
import com.wobok.bibilili.data.auth.QrLoginState
import com.wobok.bibilili.ui.theme.PaperTheme
import android.graphics.Bitmap

/**
 * 扫码登录。
 *
 * 二维码在本地用 ZXing 渲染，所以配色完全由我们控制——纸底墨点，
 * 和整个 App 是一套气质。这也是选扫码的附带好处之一。
 */
@Composable
fun LoginScreen(
    repository: QrLoginRepository,
    onSuccess: () -> Unit,
) {
    var state by remember { mutableStateOf<QrLoginState>(QrLoginState.Idle) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        repository.login().collect { next ->
            state = next
            if (next is QrLoginState.Success) onSuccess()
        }
    }

    val colors = PaperTheme.colors

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = "bibilili",
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                color = colors.onBg,
            )
            Box(
                Modifier
                    .padding(top = 18.dp)
                    .width(26.dp)
                    .height(2.dp)
                    .background(colors.accent)
            )

            Column(
                modifier = Modifier
                    .padding(top = 52.dp)
                    .width(246.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                QrArea(state, colors.onBg.toArgb(), colors.surface.toArgb())
                Text(
                    text = state.statusText(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onBg,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    text = state.hintText(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onBg3,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }

            Text(
                text = "使用哔哩哔哩客户端验证",
                style = MaterialTheme.typography.titleSmall,
                color = colors.bg,
                modifier = Modifier
                    .padding(top = 26.dp)
                    .width(246.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary)
                    .padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Text(
                text = "刷新二维码",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBg2,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clickable { attempt++ },
            )
        }
    }
}

@Composable
private fun QrArea(state: QrLoginState, dark: Int, light: Int) {
    val content = (state as? QrLoginState.WaitingScan)?.content
        ?: (state as? QrLoginState.WaitingConfirm)?.let { "" }

    val bitmap: ImageBitmap? = remember(content) {
        content?.takeIf { it.isNotBlank() }?.let { renderQr(it, dark, light) }
    }

    Box(
        Modifier
            .size(202.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PaperTheme.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "登录二维码",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private fun renderQr(content: String, dark: Int, light: Int): ImageBitmap? = runCatching {
    val size = 512
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) dark else light
        }
    }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}.getOrNull()

private fun QrLoginState.statusText(): String = when (this) {
    QrLoginState.Idle -> "正在取二维码"
    is QrLoginState.WaitingScan -> "用哔哩哔哩 App 扫码登录"
    is QrLoginState.WaitingConfirm -> "已扫描，请在手机上确认"
    QrLoginState.Expired -> "二维码已失效"
    is QrLoginState.Success -> "登录成功"
    is QrLoginState.Failed -> message
}

private fun QrLoginState.hintText(): String = when (this) {
    is QrLoginState.WaitingScan -> "二维码 180 秒内有效"
    QrLoginState.Expired -> "点下方刷新"
    else -> ""
}
