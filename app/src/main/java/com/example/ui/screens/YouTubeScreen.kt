package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YouTubeScreen(viewModel: com.example.ui.NfcViewModel) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl("https://m.youtube.com/")
            }
        },
        update = { webView ->
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
        }
    )
}
