package com.tinker.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.tinker.app.ui.theme.AppTheme

private fun isEmulator() = android.os.Build.FINGERPRINT.contains("generic") ||
    android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu") ||
    android.os.Build.PRODUCT.contains("sdk")

@SuppressLint("SetJavaScriptEnabled", "MissingPermission")
@Composable
fun MapPickerScreen(
    initialLat: Double,
    initialLng: Double,
    initialRadius: Float,
    hasInitial: Boolean,
    onConfirm: (Double, Double, Float) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val startLat = if (hasInitial) initialLat else 20.0
    val startLng = if (hasInitial) initialLng else 0.0
    val startZoom = if (hasInitial) 16 else 2

    var center by remember { mutableStateOf(startLat to startLng) }
    var radius by remember { mutableStateOf(initialRadius) }
    var mapReady by remember { mutableStateOf(false) }

    val bridge = remember {
        object {
            @JavascriptInterface
            fun onCenter(lat: Double, lng: Double) = mainHandler.post { center = lat to lng }.let {}
        }
    }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Emulators render Leaflet's GPU-composited panes blank; software layer fixes it.
            // Real devices keep hardware acceleration.
            if (isEmulator()) setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    evaluateJavascript("init($startLat,$startLng,$radius,$startZoom)", null)
                    mapReady = true
                    if (!hasInitial && granted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        fetchLocation(context) { la, lo -> evaluateJavascript("recenter($la,$lo)", null) }
                    }
                }
            }
            addJavascriptInterface(bridge, "Android")
            loadUrl("file:///android_asset/map.html")
        }
    }
    DisposableEffect(Unit) { onDispose { webView.destroy() } }
    LaunchedEffect(radius, mapReady) { if (mapReady) webView.evaluateJavascript("setRadius($radius)", null) }

    val locateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (granted(context, Manifest.permission.ACCESS_FINE_LOCATION))
            fetchLocation(context) { la, lo -> webView.evaluateJavascript("recenter($la,$lo)", null) }
    }
    fun locate() {
        if (granted(context, Manifest.permission.ACCESS_FINE_LOCATION))
            fetchLocation(context) { la, lo -> webView.evaluateJavascript("recenter($la,$lo)", null) }
        else locateLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    val c = AppTheme.colors
    Box(Modifier.fillMaxSize().background(c.canvas)) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())

        Row(
            Modifier.fillMaxWidth().systemBarsPadding().padding(AppTheme.space.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Pill("Cancel", onCancel)
            Pill("Locate me") { locate() }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().systemBarsPadding().padding(AppTheme.space.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space.md),
        ) {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline("Radius")
                        Spacer(Modifier.height(AppTheme.space.xs))
                        AppText("${radius.toInt()} m", AppTheme.type.heading, c.ink)
                    }
                    SmallButton("–") { radius = (radius - 50f).coerceAtLeast(50f) }
                    Spacer(Modifier.width(AppTheme.space.sm))
                    SmallButton("+") { radius = (radius + 50f).coerceAtMost(2000f) }
                }
            }
            PrimaryButton("Drop the pin here") { onConfirm(center.first, center.second, radius) }
        }
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.hairline, shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) { AppText(text, AppTheme.type.label, c.ink) }
}
