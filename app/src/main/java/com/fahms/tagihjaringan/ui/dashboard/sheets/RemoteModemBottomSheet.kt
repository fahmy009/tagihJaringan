package com.fahms.tagihjaringan.ui.dashboard.sheets

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fahms.tagihjaringan.data.model.CustomerItem
import com.fahms.tagihjaringan.ui.theme.PinkPrimary
import com.fahms.tagihjaringan.ui.theme.SkySecondary

fun launchChromeCustomTab(context: Context, url: String) {
    try {
        val uri = Uri.parse(url)
        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(android.graphics.Color.parseColor("#EC4899"))
            .setSecondaryToolbarColor(android.graphics.Color.parseColor("#0EA5E9"))
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorParams)
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()

        val intent = customTabsIntent.intent
        intent.data = uri
        intent.addFlags(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
            android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
        )

        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun RemoteModemBottomSheet(
    customer: CustomerItem,
    remoteUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var progressProgress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }

    fun purgeAndDismiss() {
        try {
            webView?.stopLoading()
            webView?.clearCache(true)
            webView?.clearHistory()
            webView?.clearFormData()
            webView?.loadUrl("about:blank")
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView?.stopLoading()
                webView?.clearCache(true)
                webView?.clearHistory()
                webView?.clearFormData()
                webView?.loadUrl("about:blank")
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = ::purgeAndDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Navigation Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (webView?.canGoBack() == true) {
                                        webView?.goBack()
                                    }
                                },
                                enabled = canGoBack
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = if (canGoBack) PinkPrimary else Color.Gray
                                )
                            }

                            IconButton(
                                onClick = { webView?.reload() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Muat Ulang",
                                    tint = SkySecondary
                                )
                            }

                            IconButton(
                                onClick = { launchChromeCustomTab(context, remoteUrl) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Buka Chrome Custom Tab",
                                    tint = PinkPrimary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🌐 Modem: ${customer.name}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = remoteUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(onClick = ::purgeAndDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progressProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = PinkPrimary,
                        trackColor = SkySecondary.copy(alpha = 0.2f)
                    )
                }

                // Native In-App WebView (100% Full-Screen Window Viewport matching Chrome)
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.setSupportZoom(true)
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            setInitialScale(75)

                            try {
                                clearCache(true)
                                clearHistory()
                                clearFormData()
                                android.webkit.WebStorage.getInstance().deleteAllData()
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString() ?: ""
                                    if (reqUrl.contains("192.168.0.1") || reqUrl.contains("127.0.0.1") || reqUrl.contains("192.168.1.1")) {
                                        val path = request?.url?.path ?: ""
                                        val query = request?.url?.query ?: ""
                                        val basePublicUrl = if (remoteUrl.contains("?")) remoteUrl.split("?")[0] else remoteUrl
                                        val publicOrigin = if (basePublicUrl.contains("/")) basePublicUrl.substringBeforeLast("/") else basePublicUrl
                                        val rewrittenUrl = "$publicOrigin$path${if (query.isNotBlank()) "?$query" else ""}"
                                        view?.loadUrl(rewrittenUrl)
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    canGoBack = view?.canGoBack() == true

                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            try {
                                                var origAssign = window.location.assign;
                                                window.location.assign = function(url) {
                                                    if (url && (url.indexOf("192.168.0.1") !== -1 || url.indexOf("127.0.0.1") !== -1)) {
                                                        var cleanPath = url.replace(/^https?:\/\/[^\/]+/, "");
                                                        url = window.location.origin + cleanPath;
                                                    }
                                                    return origAssign.call(window.location, url);
                                                };
                                            } catch(e) {}
                                        })();
                                        """.trimIndent(), null
                                    )
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    progressProgress = newProgress
                                }
                            }

                            loadUrl(remoteUrl)
                            webView = this
                        }
                    },
                    update = { view ->
                        webView = view
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}
