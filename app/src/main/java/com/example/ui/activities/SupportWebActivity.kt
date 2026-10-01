package com.example.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.TealPrimaryDark
import com.example.util.SupportChatManager

class SupportWebActivity : ComponentActivity() {
    companion object {
        const val CRISP_URL = "https://go.crisp.chat/chat/embed/?website_id=50ac8743-e9cf-4f46-a2f1-888d6724bd72"
        const val EXTRA_URL = "extra_url"

        fun start(context: Context, url: String? = null) {
            val targetUrl = SupportChatManager.resolveDynamicChatUrl(url).ifBlank { CRISP_URL }
            val intent = Intent(context, SupportWebActivity::class.java).apply {
                putExtra(EXTRA_URL, targetUrl)
                if (context !is ComponentActivity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.white)

        val rawUrl = intent.getStringExtra(EXTRA_URL)
        val targetUrl = SupportChatManager.resolveDynamicChatUrl(rawUrl).ifBlank { CRISP_URL }

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
                var isLoading by remember { mutableStateOf(true) }
                var progress by remember { mutableFloatStateOf(0f) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { webView?.reload() }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "تحديث",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (isLoading) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                trackColor = TealPrimaryDark.copy(alpha = 0.4f)
                            )
                        }

                        // Native Android WebView with third-party cookies & session persistence
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.White)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        this@SupportWebActivity.webView = this

                                        // Set white background to prevent black/blank screens
                                        setBackgroundColor(android.graphics.Color.WHITE)

                                        // Ensure hardware acceleration on physical devices; fallback to software on emulator if needed
                                        val isEmulator = Build.FINGERPRINT.contains("generic") ||
                                                Build.MODEL.contains("google_sdk") ||
                                                Build.HARDWARE.contains("goldfish") ||
                                                Build.HARDWARE.contains("ranchu")
                                        if (isEmulator) {
                                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                        }

                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )

                                        // 1. Enable Third-Party Cookies (CRITICAL for Crisp session restore)
                                        val cookieManager = CookieManager.getInstance()
                                        cookieManager.setAcceptCookie(true)
                                        cookieManager.setAcceptThirdPartyCookies(this, true)

                                        // 2. Comprehensive WebSettings
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            @Suppress("DEPRECATION")
                                            databaseEnabled = true
                                            javaScriptCanOpenWindowsAutomatically = true
                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            cacheMode = WebSettings.LOAD_DEFAULT

                                            // Remove "; wv" from userAgentString so Crisp doesn't block the webview client
                                            userAgentString = userAgentString.replace("; wv", "")

                                            allowFileAccess = true
                                            allowContentAccess = true
                                            setSupportMultipleWindows(true)
                                            loadsImagesAutomatically = true
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                        }

                                        // 2. Configure Clients & Rendering
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                progress = newProgress / 100f
                                                if (newProgress >= 100) {
                                                    isLoading = false
                                                }
                                            }
                                        }

                                        // 3. Proper Link Navigation via WebViewClient
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                val url = request?.url?.toString() ?: return false
                                                if (url.contains("crisp.chat") || url.contains("crisp.help") || url.contains("go.crisp.chat")) {
                                                    return false // Keep inside WebView
                                                }
                                                return try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    view?.context?.startActivity(intent)
                                                    true
                                                } catch (e: Exception) {
                                                    true
                                                }
                                            }

                                            @Deprecated("Deprecated in Java")
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                url: String?
                                            ): Boolean {
                                                if (url == null) return false
                                                if (url.contains("crisp.chat") || url.contains("crisp.help") || url.contains("go.crisp.chat")) {
                                                    return false // Keep inside WebView
                                                }
                                                return try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    view?.context?.startActivity(intent)
                                                    true
                                                } catch (e: Exception) {
                                                    true
                                                }
                                            }

                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                super.onPageStarted(view, url, favicon)
                                                isLoading = true
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                isLoading = false
                                                try {
                                                    CookieManager.getInstance().flush()
                                                } catch (_: Exception) {}
                                            }

                                            override fun onReceivedError(
                                                view: WebView?,
                                                request: WebResourceRequest?,
                                                error: WebResourceError?
                                            ) {
                                                super.onReceivedError(view, request, error)
                                                if (request?.isForMainFrame == true) {
                                                    isLoading = false
                                                }
                                            }

                                            override fun onRenderProcessGone(
                                                view: WebView?,
                                                detail: RenderProcessGoneDetail?
                                            ): Boolean {
                                                val failedUrl = view?.url ?: targetUrl
                                                try {
                                                    (view?.parent as? ViewGroup)?.removeView(view)
                                                    view?.destroy()
                                                } catch (_: Exception) {}
                                                this@SupportWebActivity.webView = null

                                                try {
                                                    val builder = CustomTabsIntent.Builder()
                                                    builder.build().launchUrl(this@SupportWebActivity, Uri.parse(failedUrl))
                                                } catch (_: Exception) {
                                                    try {
                                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(failedUrl)))
                                                    } catch (_: Exception) {}
                                                }
                                                finish()
                                                return true
                                            }
                                        }

                                        try {
                                            loadUrl(targetUrl)
                                        } catch (_: Exception) {}
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

    // Lifecycle Handling
    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        try {
            webView?.stopLoading()
            (webView?.parent as? ViewGroup)?.removeView(webView)
            webView?.destroy()
        } catch (_: Exception) {}
        webView = null
        super.onDestroy()
    }
}
