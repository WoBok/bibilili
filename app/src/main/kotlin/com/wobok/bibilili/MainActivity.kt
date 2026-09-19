package com.wobok.bibilili

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.wobok.bibilili.ui.BibiliRoot
import com.wobok.bibilili.ui.theme.BibiliTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 起强制 edge-to-edge，播放页和全屏页自己处理 insets。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as BibiliApp).container

        setContent {
            val credentials by container.credentialStore.current.collectAsState()
            BibiliTheme {
                BibiliRoot(
                    container = container,
                    loggedIn = credentials?.isUsable == true,
                )
            }
        }
    }
}
