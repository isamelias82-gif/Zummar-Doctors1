package com.example.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.TealPrimaryDark

class SupportWebActivity : ComponentActivity() {
    companion object {
        const val CRISP_URL = "https://go.crisp.chat/chat/embed/?website_id=50ac8743-e9cf-4f46-a2f1-888d6724bd72"
        const val EXTRA_URL = "extra_url"

        fun start(context: Context, url: String? = null) {
            val targetUrl = if (url.isNullOrBlank()) {
                CRISP_URL
            } else {
                url.trim()
            }
            val intent = Intent(context, SupportWebActivity::class.java).apply {
                putExtra(EXTRA_URL, targetUrl)
            }
            context.startActivity(intent)
        }
    }

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL) ?: CRISP_URL

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                } else {
                    finish()
                }
            }
        })

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        Surface(
                            color = TealPrimaryDark,
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "دعم أطباء زمار - المحادثة الفورية",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // WebView with JS, DOM Storage & Database enabled
                        Box(modifier = Modifier.weight(1f)) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        this@SupportWebActivity.webView = this
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            databaseEnabled = true
                                            allowFileAccess = true
                                            allowContentAccess = true
                                            setSupportMultipleWindows(true)
                                            javaScriptCanOpenWindowsAutomatically = true
                                            loadsImagesAutomatically = true
                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            cacheMode = WebSettings.LOAD_DEFAULT
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                        }
                                        webViewClient = object : WebViewClient() {
                                            @Deprecated("Deprecated in Java")
                                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                return handleUrl(view, url)
                                            }

                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                return handleUrl(view, request?.url?.toString())
                                            }

                                            private fun handleUrl(view: WebView?, targetUrl: String?): Boolean {
                                                if (targetUrl == null) return false
                                                if (targetUrl.startsWith("tel:") || targetUrl.startsWith("whatsapp:") || targetUrl.startsWith("mailto:")) {
                                                    try {
                                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                                    } catch (e: Exception) {}
                                                    return true
                                                }
                                                // Keep all link navigations inside the WebView
                                                view?.loadUrl(targetUrl)
                                                return true
                                            }
                                        }
                                        webChromeClient = WebChromeClient()
                                        loadUrl(url)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
