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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tinker.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private fun isEmulator() = android.os.Build.FINGERPRINT.contains("generic") ||
    android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu") ||
    android.os.Build.PRODUCT.contains("sdk")

private data class Place(val name: String, val lat: Double, val lng: Double)

/** Geocode via Nominatim, biased toward [lat]/[lng] with a viewbox so nearby matches rank first. */
private suspend fun geocode(query: String, lat: Double, lng: Double): List<Place> = withContext(Dispatchers.IO) {
    runCatching {
        val d = 0.75
        val viewbox = "${lng - d},${lat - d},${lng + d},${lat + d}"
        val url = URL(
            "https://nominatim.openstreetmap.org/search?format=json&limit=5" +
                "&viewbox=" + URLEncoder.encode(viewbox, "UTF-8") +
                "&q=" + URLEncoder.encode(query, "UTF-8")
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", "Tinker/0.1 (personal geofence app)")
            connectTimeout = 8000
            readTimeout = 8000
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(body)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Place(o.getString("display_name"), o.getString("lat").toDouble(), o.getString("lon").toDouble())
        }
    }.getOrDefault(emptyList())
}

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

    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    fun runSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        searching = true
        scope.launch { results = geocode(q, center.first, center.second); searching = false }
    }

    val c = AppTheme.colors
    Box(Modifier.fillMaxSize().background(c.canvas)) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxWidth().systemBarsPadding().padding(AppTheme.space.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space.sm),
        ) {
            TopBar(
                onCancel = onCancel,
                onLocate = ::locate,
                searchOpen = searchOpen,
                onOpenSearch = { searchOpen = true },
                query = query,
                onQuery = { query = it },
                onSearch = ::runSearch,
                onCloseSearch = { searchOpen = false; query = ""; results = emptyList() },
                searching = searching,
            )
            if (searchOpen && results.isNotEmpty()) {
                ResultsList(results) { p ->
                    webView.evaluateJavascript("recenter(${p.lat},${p.lng})", null)
                    center = p.lat to p.lng
                    results = emptyList()
                    searchOpen = false
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().systemBarsPadding().padding(AppTheme.space.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space.md),
        ) {
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Overline("Radius")
                    Spacer(Modifier.weight(1f))
                    AppText("${radius.toInt()} m", AppTheme.type.heading, c.ink)
                }
                Spacer(Modifier.height(AppTheme.space.sm))
                Slider(radius, 50f..2000f, step = 10f) { radius = it }
            }
            PrimaryButton("Drop the pin here") { onConfirm(center.first, center.second, radius) }
        }
    }
}

@Composable
private fun TopBar(
    onCancel: () -> Unit,
    onLocate: () -> Unit,
    searchOpen: Boolean,
    onOpenSearch: () -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    searching: Boolean,
) {
    Box(Modifier.fillMaxWidth().height(46.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(onCancel) { CloseIcon() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(onLocate) { LocateIcon() }
                Spacer(Modifier.width(AppTheme.space.sm))
                CircleIconButton(onOpenSearch) { SearchIcon() }
            }
        }
        AnimatedVisibility(
            visible = searchOpen,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(),
        ) {
            SearchField(query, onQuery, onSearch, onCloseSearch, searching)
        }
    }
}

@Composable
private fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, c.hairline, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit,
    searching: Boolean,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.sm)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        Modifier.fillMaxWidth().height(46.dp).clip(shape).background(c.surface)
            .border(1.dp, c.hairline, shape).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchIcon(c.mute)
        Spacer(Modifier.width(AppTheme.space.sm))
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = AppTheme.type.body.merge(TextStyle(color = c.ink)),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    if (value.isEmpty()) AppText("Search a place or address", color = c.mute)
                    inner()
                },
            )
        }
        if (searching) {
            AppText("…", color = c.mute)
            Spacer(Modifier.width(AppTheme.space.sm))
        }
        Box(Modifier.clip(CircleShape).clickable { onClose() }.padding(4.dp)) { CloseIcon(c.mute) }
    }
}

@Composable
private fun ResultsList(results: List<Place>, onPick: (Place) -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.md)
    Column(Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.hairline, shape)) {
        results.forEachIndexed { i, p ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
            Box(Modifier.fillMaxWidth().clickable { onPick(p) }.padding(14.dp)) {
                BasicText(
                    p.name,
                    style = AppTheme.type.body.merge(TextStyle(color = c.ink)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CloseIcon(color: Color = AppTheme.colors.ink) {
    Canvas(Modifier.size(18.dp)) {
        val sw = size.minDimension * 0.11f
        drawLine(color, Offset(size.width * 0.22f, size.height * 0.22f), Offset(size.width * 0.78f, size.height * 0.78f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.78f, size.height * 0.22f), Offset(size.width * 0.22f, size.height * 0.78f), sw, StrokeCap.Round)
    }
}

@Composable
private fun SearchIcon(color: Color = AppTheme.colors.ink) {
    Canvas(Modifier.size(18.dp)) {
        val r = size.minDimension * 0.30f
        val cc = Offset(size.width * 0.42f, size.height * 0.42f)
        val sw = size.minDimension * 0.11f
        drawCircle(color, r, cc, style = Stroke(sw))
        drawLine(color, Offset(cc.x + r * 0.72f, cc.y + r * 0.72f), Offset(size.width * 0.86f, size.height * 0.86f), sw, StrokeCap.Round)
    }
}

@Composable
private fun LocateIcon(color: Color = AppTheme.colors.ink) {
    Canvas(Modifier.size(20.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val sw = size.minDimension * 0.10f
        val t = size.minDimension * 0.15f
        drawCircle(color, size.minDimension * 0.30f, Offset(cx, cy), style = Stroke(sw))
        drawCircle(color, size.minDimension * 0.09f, Offset(cx, cy))
        drawLine(color, Offset(cx, 0f), Offset(cx, t), sw, StrokeCap.Round)
        drawLine(color, Offset(cx, size.height - t), Offset(cx, size.height), sw, StrokeCap.Round)
        drawLine(color, Offset(0f, cy), Offset(t, cy), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width - t, cy), Offset(size.width, cy), sw, StrokeCap.Round)
    }
}
