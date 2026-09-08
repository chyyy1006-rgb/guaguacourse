package com.example.npucourse.ui.screens

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.npucourse.data.CampusServiceStore
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

private const val ELECTRICITY_SSO_URL =
    "https://yktapp.nwpu.edu.cn/berserker-auth/cas/login/supwisdom?targetUrl=https%3A%2F%2Fyktapp.nwpu.edu.cn%2Fplat"
private const val NETWORK_SELF_SERVICE_URL = "https://zizhu.nwpu.edu.cn/"
private const val CAMPUS_PAYMENT_URL = "https://wszf.nwpu.edu.cn/CasLogin_zf.aspx"
private const val CAMPUS_CARPOOL_URL = "https://nwpu.codepix.top/"

private enum class CampusServiceKind { ELECTRICITY, NETWORK, CARPOOL }

private data class CampusWebDestination(
    val title: String,
    val url: String,
    val hint: String,
    val kind: CampusServiceKind
)

@Composable
fun CampusServicesPage(
    onBack: (() -> Unit)? = null,
    onWebPageVisibilityChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var destination by remember { mutableStateOf<CampusWebDestination?>(null) }
    var selectedService by rememberSaveable { mutableStateOf<String?>(null) }
    var showServiceHelp by rememberSaveable { mutableStateOf(false) }
    var electricityBalance by rememberSaveable {
        mutableStateOf(CampusServiceStore.electricityBalance(context))
    }

    val currentDestination = destination
    DisposableEffect(currentDestination) {
        onWebPageVisibilityChanged(currentDestination != null)
        onDispose {
            if (currentDestination != null) onWebPageVisibilityChanged(false)
        }
    }

    if (currentDestination != null) {
        CampusOfficialWebPage(
            destination = currentDestination,
            electricityBalance = electricityBalance,
            onElectricityBalance = {
                electricityBalance = it
                CampusServiceStore.saveElectricityBalance(context, it)
            },
            onBack = { destination = null }
        )
        return
    }

    if (selectedService == "electricity") {
        ElectricityFeePage(
            electricityBalance = electricityBalance,
            onOpenQuery = {
                if (isElectricitySettlementWindow()) {
                    Toast.makeText(
                        context,
                        "电费系统正在结算，请在 1:00 后查询",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    destination = CampusWebDestination(
                        title = "宿舍电费",
                        url = ELECTRICITY_SSO_URL,
                        hint = "正在尝试复用课表的统一认证会话；会话有效时将自动进入一卡通并读取剩余电量。",
                        kind = CampusServiceKind.ELECTRICITY
                    )
                }
            },
            onOpenBrowser = { openExternalUrl(context, ELECTRICITY_SSO_URL) },
            onBack = { selectedService = null }
        )
        return
    }

    if (selectedService == "network") {
        NetworkFeePage(
            onOpenQuery = {
                destination = CampusWebDestination(
                    title = "校园网自助",
                    url = NETWORK_SELF_SERVICE_URL,
                    hint = "请使用校园网上网账号登录。验证码和账号密码仅提交给学校网络自助系统。",
                    kind = CampusServiceKind.NETWORK
                )
            },
            onOpenPayment = { openExternalUrl(context, CAMPUS_PAYMENT_URL) },
            onBack = { selectedService = null }
        )
        return
    }

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        CampusPageHeader(
            title = "服务",
            onBack = onBack,
            onHelpClick = { showServiceHelp = true }
        )

        if (showServiceHelp) {
            ServiceHelpDialog(onDismiss = { showServiceHelp = false })
        }

        ServiceCard(
            title = "宿舍电费",
            subtitle = electricityBalance?.let {
                "当前剩余电量 ${formatCampusNumber(it)} 度"
            } ?: "查询宿舍剩余电量与充值",
            detail = "独立电费页面会通过一卡通系统自动读取当前剩余电量。",
            primaryText = "进入电费页面",
            onPrimaryClick = { selectedService = "electricity" }
        )

        Spacer(modifier = Modifier.height(14.dp))

        ServiceCard(
            title = "校园网费",
            subtitle = "查看余额、当前套餐、资费与缴费记录",
            detail = "余额、套餐和充值入口集中到独立网费页面。",
            primaryText = "进入网费页面",
            onPrimaryClick = { selectedService = "network" }
        )

        Spacer(modifier = Modifier.height(14.dp))

        ServiceCard(
            title = "校园拼车",
            subtitle = "瓜拼白车 · 西工大学生拼车平台",
            detail = "第三方校园服务（nwpu.codepix.top），并非西北工业大学官方系统，请自行核验行程、费用与同行人员。",
            primaryText = "打开拼车平台",
            onPrimaryClick = {
                destination = CampusWebDestination(
                    title = "校园拼车",
                    url = CAMPUS_CARPOOL_URL,
                    hint = "这是第三方校园拼车平台，不是学校官方服务。请勿向陌生人泄露密码、验证码或敏感个人信息。",
                    kind = CampusServiceKind.CARPOOL
                )
            },
            secondaryText = "浏览器打开",
            onSecondaryClick = { openExternalUrl(context, CAMPUS_CARPOOL_URL) }
        )

        Spacer(modifier = Modifier.height(122.dp))
    }
}

@Composable
private fun ElectricityFeePage(
    electricityBalance: Double?,
    onOpenQuery: () -> Unit,
    onOpenBrowser: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        CampusPageHeader(title = "宿舍电费", onBack = onBack)
        InfoCard(
            title = electricityBalance?.let {
                "当前剩余电量 ${formatCampusNumber(it)} 度"
            } ?: "尚未读取剩余电量",
            detail = "会先尝试复用课表登录建立的统一认证会话；会话有效时无需再次输入密码。认证已过期或学校要求二次验证时仍需重新登录。0:00—1:00 为系统结算时段。"
        )
        Spacer(modifier = Modifier.height(16.dp))
        ServiceCard(
            title = "电费查询与充值",
            subtitle = "学校一卡通电费项目",
            detail = "完成认证后会自动进入电费项目并读取电量；支付页面产生的微信或支付宝跳转会交给对应应用处理。",
            primaryText = if (electricityBalance == null) "登录并查询" else "刷新电量",
            onPrimaryClick = onOpenQuery,
            secondaryText = "浏览器备用",
            onSecondaryClick = onOpenBrowser
        )
        Spacer(modifier = Modifier.height(14.dp))
        InfoCard(
            title = "充值结果",
            detail = "支付完成后请回到本页重新查询，以学校一卡通显示的剩余电量为准。"
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun NetworkFeePage(
    onOpenQuery: () -> Unit,
    onOpenPayment: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        CampusPageHeader(title = "校园网费", onBack = onBack)
        InfoCard(
            title = "独立网费页面",
            detail = "网络余额与套餐由学校网络自助系统提供。该页面使用独立的上网账号、密码和验证码，并未接入课表使用的统一身份认证。"
        )
        Spacer(modifier = Modifier.height(16.dp))
        ServiceCard(
            title = "余额与套餐",
            subtitle = "查询余额、套餐、资费和缴费记录",
            detail = "请使用校园网上网账号登录，验证码和账号密码仅提交给学校网络自助系统。",
            primaryText = "打开网络自助",
            onPrimaryClick = onOpenQuery,
            secondaryText = "网费充值",
            onSecondaryClick = onOpenPayment
        )
        Spacer(modifier = Modifier.height(14.dp))
        InfoCard(
            title = "关于充值",
            detail = "充值会在系统浏览器中打开西北工业大学统一支付平台。完成后请回到网络自助页面刷新余额。"
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun CampusPageHeader(
    title: String,
    onBack: (() -> Unit)?,
    onHelpClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }
        Text(
            text = title,
            modifier = Modifier
                .padding(start = 6.dp)
                .weight(1f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onHelpClick != null) {
            Surface(
                onClick = onHelpClick,
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务说明", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("校园服务", fontWeight = FontWeight.Bold)
                    Text(
                        text = "电费与网费连接学校官方系统；校园拼车是第三方平台。瓜瓜课程表不保存统一身份认证密码、网络密码或支付信息。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("后台自动刷新", fontWeight = FontWeight.Bold)
                    Text(
                        text = "课程表约每 24 小时刷新一次，检测到课程变化时通知；电费约每 30 分钟刷新并通知当前余额，余额金额不高于 5 元时会特别提醒充值。若学校只返回剩余电量，则按电量数值不高于 5 提醒。系统省电策略可能延后刷新。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
private fun ServiceCard(
    title: String,
    subtitle: String,
    detail: String,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = if (secondaryText == null) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.weight(1f)
                    },
                    onClick = onPrimaryClick
                ) {
                    Text(primaryText)
                }
                if (secondaryText != null && onSecondaryClick != null) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onSecondaryClick
                    ) {
                        Text(secondaryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun CampusOfficialWebPage(
    destination: CampusWebDestination,
    electricityBalance: Double?,
    onElectricityBalance: (Double) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var collectionStatus by remember(destination) {
        mutableStateOf(
            if (destination.kind == CampusServiceKind.ELECTRICITY) {
                electricityBalance?.let {
                    "上次读取 ${formatCampusNumber(it)} 度，正在刷新……"
                } ?: "正在尝试复用统一认证会话"
            } else {
                null
            }
        )
    }
    val collectorHandler = remember { Handler(Looper.getMainLooper()) }

    fun stopElectricityCollector() {
        collectorHandler.removeCallbacksAndMessages(null)
    }

    fun startElectricityCollector(view: WebView) {
        if (destination.kind != CampusServiceKind.ELECTRICITY) return
        stopElectricityCollector()
        val attempts = intArrayOf(0)
        val collector = object : Runnable {
            override fun run() {
                if (webView !== view || ++attempts[0] > 180) return
                view.evaluateJavascript(ELECTRICITY_COLLECTION_SCRIPT) { rawResult ->
                    val result = parseElectricityCollectionResult(rawResult)
                    when (result.phase) {
                        "navigating" -> collectionStatus = "正在进入电费项目……"
                        "balance" -> {
                            val balance = result.balance
                            if (balance != null) {
                                collectionStatus = "已读取 ${formatCampusNumber(balance)} 度"
                                onElectricityBalance(balance)
                                return@evaluateJavascript
                            }
                        }
                        "electricity_page" -> collectionStatus = "正在读取剩余电量……"
                        "auth" -> collectionStatus = "请完成学校统一身份认证"
                        "error" -> collectionStatus = "读取页面数据失败，正在重试……"
                    }
                    if (webView === view) collectorHandler.postDelayed(this, 1000L)
                }
            }
        }
        collectorHandler.postDelayed(collector, 500L)
    }

    BackHandler {
        val current = webView
        if (current?.canGoBack() == true) current.goBack() else onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            stopElectricityCollector()
            CookieManager.getInstance().flush()
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
            webView = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    val current = webView
                    if (current?.canGoBack() == true) current.goBack() else onBack()
                }
            ) {
                Text("返回")
            }
            Text(
                text = destination.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { openExternalUrl(context, destination.url) }) {
                Text("浏览器")
            }
        }

        Text(
            text = when {
                destination.kind == CampusServiceKind.ELECTRICITY && collectionStatus != null ->
                    "${destination.hint}\n状态：$collectionStatus"
                else -> destination.hint
            },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webView = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.setSupportMultipleWindows(false)
                        settings.javaScriptCanOpenWindowsAutomatically =
                            destination.kind == CampusServiceKind.ELECTRICITY
                        settings.userAgentString = settings.userAgentString
                            .replace("; wv", "")
                            .replace(" Version/4.0", "")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = true
                        }

                        val currentWebView = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(currentWebView, true)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                stopElectricityCollector()
                                loading = true
                                loadError = null
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                loading = false
                                CookieManager.getInstance().flush()
                                if (destination.kind == CampusServiceKind.ELECTRICITY) {
                                    view.evaluateJavascript(OPEN_IN_CURRENT_WEBVIEW_SCRIPT, null)
                                }
                                if (destination.kind == CampusServiceKind.CARPOOL) {
                                    view.evaluateJavascript(CARPOOL_LAYOUT_FIX_SCRIPT, null)
                                }
                                startElectricityCollector(view)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean = handleCampusNavigation(
                                context,
                                request.url.toString(),
                                destination.kind
                            )

                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                                handleCampusNavigation(context, url, destination.kind)

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                if (request.isForMainFrame) {
                                    loading = false
                                    loadError = error.description.toString()
                                }
                            }
                        }

                        CookieManager.getInstance().flush()
                        loadUrl(destination.url)
                    }
                }
            )

            if (loading && loadError == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            loadError?.let { message ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("服务页面加载失败", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            loadError = null
                            loading = true
                            webView?.reload()
                        }
                    ) {
                        Text("重新加载")
                    }
                    TextButton(onClick = { openExternalUrl(context, destination.url) }) {
                        Text("在浏览器中打开")
                    }
                }
            }
        }
    }
}

private fun handleCampusNavigation(
    context: Context,
    rawUrl: String,
    kind: CampusServiceKind
): Boolean {
    val uri = runCatching { rawUrl.toUri() }.getOrNull() ?: return true
    val scheme = uri.scheme?.lowercase()
    val host = uri.host?.lowercase()
    val isOfficialHost = host == "nwpu.edu.cn" || host?.endsWith(".nwpu.edu.cn") == true
    val isAllowedHttps = scheme == "https" && when (kind) {
        CampusServiceKind.ELECTRICITY,
        CampusServiceKind.NETWORK -> isOfficialHost
        CampusServiceKind.CARPOOL -> host == "nwpu.codepix.top" || isOfficialHost
    }

    if (kind != CampusServiceKind.CARPOOL && scheme == "https" && host == "wszf.nwpu.edu.cn") {
        openExternalUrl(context, rawUrl)
        Toast.makeText(context, "充值已转到系统浏览器", Toast.LENGTH_SHORT).show()
        return true
    }

    if (kind == CampusServiceKind.ELECTRICITY &&
        (scheme == "weixin" || scheme == "alipays" || scheme == "alipay")
    ) {
        openExternalUrl(context, rawUrl)
        return true
    }

    if (isAllowedHttps) return false

    if (scheme == "https") {
        openExternalUrl(context, rawUrl)
        Toast.makeText(context, "已在浏览器中打开外部页面", Toast.LENGTH_SHORT).show()
    } else if (kind == CampusServiceKind.CARPOOL &&
        (scheme == "tel" || scheme == "mailto" || scheme == "geo")
    ) {
        openExternalUrl(context, rawUrl)
    } else {
        Toast.makeText(context, "已阻止非 HTTPS 页面", Toast.LENGTH_SHORT).show()
    }
    return true
}

internal data class ElectricityCollectionResult(
    val phase: String,
    val balance: Double? = null,
    val moneyBalance: Double? = null
)

internal fun parseElectricityCollectionResult(rawResult: String?): ElectricityCollectionResult {
    if (rawResult.isNullOrBlank() || rawResult == "null") {
        return ElectricityCollectionResult("waiting")
    }
    return runCatching {
        val decoded = JSONArray("[$rawResult]").getString(0)
        val json = JSONObject(decoded)
        val balance = json.optDouble("balance", Double.NaN).takeIf {
            it.isFinite() && it >= 0.0 && it < 100000.0
        }
        val moneyBalance = json.optDouble("moneyBalance", Double.NaN).takeIf {
            it.isFinite() && it >= 0.0 && it < 100000.0
        }
        ElectricityCollectionResult(
            phase = json.optString("phase", "waiting"),
            balance = balance,
            moneyBalance = moneyBalance
        )
    }.getOrElse {
        ElectricityCollectionResult("error")
    }
}

private fun formatCampusNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString().trimEnd('0').trimEnd('.')
    }
}

private fun isElectricitySettlementWindow(): Boolean =
    Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 0

internal val ELECTRICITY_COLLECTION_SCRIPT =
    """
    (function() {
        try {
            var host = (location.hostname || "").toLowerCase();
            var path = location.pathname || "";
            if (host !== "yktapp.nwpu.edu.cn") {
                return JSON.stringify({ phase: "auth" });
            }

            if (path.indexOf("/plat") === 0 && path !== "/plat/login") {
                var token = new URL(location.href).searchParams.get("synjones-auth") ||
                    sessionStorage.getItem("access_token") ||
                    localStorage.getItem("access_token") || "";
                if (token) {
                    var target = location.origin + "/jfdt/charge/feeitem/toAppitem" +
                        "?feeitemid=182&synjones-auth=" + encodeURIComponent(token) +
                        "&appId=36&loginFrom=h5&type=app";
                    location.replace(target);
                    return JSON.stringify({ phase: "navigating" });
                }
                return JSON.stringify({ phase: "auth" });
            }

            if (path.indexOf("/jfdt/") !== 0) {
                return JSON.stringify({ phase: "auth" });
            }

            function numberFrom(value) {
                var match = String(value == null ? "" : value).match(/-?\d+(?:\.\d+)?/);
                var parsed = match ? Number.parseFloat(match[0]) : Number.NaN;
                return Number.isFinite(parsed) && parsed >= 0 && parsed < 100000 ? parsed : null;
            }

            var labels = [
                "当前剩余电量", "剩余电量", "电量余额",
                "剩余电费", "电费余额", "剩余金额"
            ];

            function balanceFrom(info) {
                if (!info || typeof info !== "object") return null;
                var firstValue = null;
                var moneyValue = null;
                for (var i = 0; i < labels.length; i++) {
                    var label = labels[i];
                    if (!Object.prototype.hasOwnProperty.call(info, label)) continue;
                    var parsed = numberFrom(info[label]);
                    if (parsed === null) continue;
                    if (firstValue === null) firstValue = parsed;
                    if (/(?:电费|金额)/.test(label) && moneyValue === null) {
                        moneyValue = parsed;
                    }
                }
                var entries = Object.entries(info);
                for (var j = 0; j < entries.length; j++) {
                    var key = String(entries[j][0] || "");
                    if (!/(?:剩余.*(?:电费|金额|电量)|(?:电费|电量).*余额)/.test(key)) continue;
                    var fallback = numberFrom(entries[j][1]);
                    if (fallback === null) continue;
                    if (firstValue === null) firstValue = fallback;
                    if (/(?:电费|金额)/.test(key) && moneyValue === null) {
                        moneyValue = fallback;
                    }
                }
                return firstValue === null
                    ? null
                    : { value: firstValue, moneyValue: moneyValue };
            }

            var app = document.querySelector("#app");
            var root = app && app.__vue__;
            var queue = root ? [root] : [];
            var visited = [];
            while (queue.length && visited.length < 120) {
                var component = queue.shift();
                if (!component || visited.indexOf(component) >= 0) continue;
                visited.push(component);
                var data = component["\u0024data"] || {};
                var candidates = [
                    component.aboutEleric && component.aboutEleric.electricInfo,
                    data.aboutEleric && data.aboutEleric.electricInfo,
                    component.electricInfo,
                    data.electricInfo
                ];
                for (var c = 0; c < candidates.length; c++) {
                    var reading = balanceFrom(candidates[c]);
                    if (reading !== null) {
                        return JSON.stringify({
                            phase: "balance",
                            balance: reading.value,
                            moneyBalance: reading.moneyValue
                        });
                    }
                }
                var children = component["\u0024children"] || [];
                for (var k = 0; k < children.length; k++) queue.push(children[k]);
            }

            var body = String(document.body && document.body.innerText || "")
                .replace(/\s+/g, " ");
            var match = body.match(/(?:当前剩余电量|剩余电量|电量余额)\s*[：:]?\s*(-?\d+(?:\.\d+)?)/) ||
                body.match(/(?:剩余电费|电费余额|剩余金额)\s*[：:]?\s*(?:¥|￥)?\s*(-?\d+(?:\.\d+)?)/);
            if (match) {
                var domBalance = numberFrom(match[1]);
                if (domBalance !== null) {
                    var moneyMatch = body.match(
                        /(?:剩余电费|电费余额|剩余金额)\s*[：:]?\s*(?:¥|￥)?\s*(-?\d+(?:\.\d+)?)/
                    );
                    var domMoney = moneyMatch ? numberFrom(moneyMatch[1]) : null;
                    return JSON.stringify({
                        phase: "balance",
                        balance: domBalance,
                        moneyBalance: domMoney
                    });
                }
            }
            return JSON.stringify({ phase: "electricity_page" });
        } catch (error) {
            return JSON.stringify({ phase: "error" });
        }
    })();
    """.trimIndent()

private val OPEN_IN_CURRENT_WEBVIEW_SCRIPT =
    """
    (function() {
        window.open = function(url) {
            if (url) window.location.href = url;
            return window;
        };
    })();
    """.trimIndent()

private val CARPOOL_LAYOUT_FIX_SCRIPT =
    """
    (function() {
        try {
            function fixDialog(dialog) {
                if (!dialog || window.innerWidth > 767) return;
                dialog.style.setProperty("top", "auto", "important");
                dialog.style.setProperty("bottom", "0", "important");
                dialog.style.setProperty("height", "70dvh", "important");
                dialog.style.setProperty("min-height", "420px", "important");
                dialog.style.setProperty("max-height", "85dvh", "important");
                dialog.style.setProperty("overflow-y", "auto", "important");
                dialog.style.setProperty("box-sizing", "border-box", "important");
                dialog.style.setProperty("transform", "none", "important");
                dialog.style.setProperty("animation", "none", "important");
                dialog.style.setProperty("transition", "none", "important");
                dialog.style.setProperty(
                    "padding-bottom",
                    "calc(1.5rem + env(safe-area-inset-bottom))",
                    "important"
                );
            }

            function scanDialogs() {
                var dialogs = document.querySelectorAll(
                    "[role='dialog'][data-state='open'], [data-radix-portal] [role='dialog']"
                );
                for (var i = 0; i < dialogs.length; i++) fixDialog(dialogs[i]);
            }

            var styleId = "guagua-carpool-webview-fix";
            if (!document.getElementById(styleId)) {
                var style = document.createElement("style");
                style.id = styleId;
                style.textContent = [
                    "@media (max-width: 767px) {",
                    "  [role='dialog'][data-state='open'],",
                    "  [data-radix-portal] [role='dialog'] {",
                    "    top: auto !important; bottom: 0 !important;",
                    "    height: 70dvh !important; min-height: 420px !important;",
                    "    max-height: 85dvh !important; overflow-y: auto !important;",
                    "    box-sizing: border-box !important; transform: none !important;",
                    "    animation: none !important; transition: none !important;",
                    "  }",
                    "}"
                ].join("\n");
                document.head.appendChild(style);
            }

            scanDialogs();
            if (!window.__guaguaCarpoolDialogObserver) {
                var observer = new MutationObserver(scanDialogs);
                observer.observe(document.documentElement, {
                    childList: true,
                    subtree: true,
                    attributes: true,
                    attributeFilter: ["data-state", "role", "class"]
                });
                window.__guaguaCarpoolDialogObserver = observer;
                window.setInterval(scanDialogs, 500);
            }
        } catch (_) {}
    })();
    """.trimIndent()

private fun openExternalUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "没有可打开该页面的浏览器", Toast.LENGTH_SHORT).show()
    }
}
