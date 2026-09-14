package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    videoId: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()

                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            html, body {
                                margin: 0;
                                padding: 0;
                                background: #000;
                                width: 100%;
                                height: 100%;
                                overflow: hidden;
                            }
                            iframe {
                                width: 100%;
                                height: 100%;
                                border: 0;
                            }
                        </style>
                    </head>
                    <body>
                        <iframe
                            src="https://www.youtube.com/embed/$videoId?playsinline=1"
                            allow="autoplay; encrypted-media; picture-in-picture"
                            allowfullscreen>
                        </iframe>
                    </body>
                    </html>
                    """.trimIndent(),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://www.youtube.com",
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        html, body {
                            margin: 0;
                            padding: 0;
                            background: #000;
                            width: 100%;
                            height: 100%;
                            overflow: hidden;
                        }
                        iframe {
                            width: 100%;
                            height: 100%;
                            border: 0;
                        }
                    </style>
                </head>
                <body>
                    <iframe
                        src="https://www.youtube.com/embed/$videoId?playsinline=1"
                        allow="autoplay; encrypted-media; picture-in-picture"
                        allowfullscreen>
                    </iframe>
                </body>
                </html>
                """.trimIndent(),
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}
