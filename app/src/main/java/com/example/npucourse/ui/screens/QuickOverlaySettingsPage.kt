package com.example.npucourse.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.npucourse.overlay.GestureAction
import com.example.npucourse.overlay.OverlayGesture
import com.example.npucourse.overlay.QuickOverlayConfig
import com.example.npucourse.overlay.QuickOverlayPreferences
import com.example.npucourse.overlay.QuickOverlayService
import com.example.npucourse.ui.components.LiquidGlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class LaunchableApp(val packageName: String, val label: String, val resolveInfo: ResolveInfo)

@Composable
fun QuickOverlaySettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var config by remember { mutableStateOf(QuickOverlayPreferences.load(context)) }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    var pickerGesture by remember { mutableStateOf<String?>(null) }
    var enableAfterPicking by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    val canOverlay = remember(permissionRefresh) { Settings.canDrawOverlays(context) }

    fun persist(value: QuickOverlayConfig, refresh: Boolean = true) {
        config = value
        QuickOverlayPreferences.save(context, value)
        if (refresh) QuickOverlayService.refresh(context)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionRefresh++
        if (Settings.canDrawOverlays(context)) QuickOverlayService.refresh(context)
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!Settings.canDrawOverlays(context)) {
            overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
        } else QuickOverlayService.refresh(context)
    }

    fun finishEnable(base: QuickOverlayConfig = config) {
        if (!base.hasConfiguredAction) {
            enableAfterPicking = true
            pickerGesture = OverlayGesture.DOUBLE_TAP
            return
        }
        persist(base.copy(enabled = true, temporarilyHidden = false), refresh = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else if (!Settings.canDrawOverlays(context)) {
            overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
        } else QuickOverlayService.refresh(context)
    }

    DisposableEffect(lifecycleOwner, config.enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRefresh++
                if (config.enabled && Settings.canDrawOverlays(context)) QuickOverlayService.refresh(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            Text("快捷悬浮窗", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text("五种手势可以分别启动不同应用；长按后才进入拖动，松手时才确认快捷手势。",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))

        LiquidGlassCard {
            OverlaySwitchRow("启用悬浮窗", when {
                !canOverlay -> "需要授予“显示在其他应用上层”权限"
                !config.hasConfiguredAction -> "请至少绑定一个手势"
                config.temporarilyHidden -> "已启用，目前暂时隐藏"
                else -> "${config.actions.values.count { it.isConfigured }} 个手势已绑定"
            }, config.enabled) { enabled ->
                if (!enabled) persist(config.copy(enabled = false, temporarilyHidden = false))
                else if (!config.tutorialShown) showTutorial = true else finishEnable()
            }
            GlassDivider()
            OverlayClickRow("悬浮窗权限", if (canOverlay) "已允许" else "点击前往系统设置") {
                overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("手势动作", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        LiquidGlassCard {
            OverlayGesture.all.forEachIndexed { index, gesture ->
                val action = config.actions.getValue(gesture)
                OverlayClickRow(OverlayGesture.label(gesture), when {
                    !action.enabled -> "已禁用"
                    action.packageName.isBlank() -> "未设置"
                    else -> action.label
                }) { pickerGesture = gesture }
                if (index < OverlayGesture.all.lastIndex) GlassDivider()
            }
        }

        Spacer(Modifier.height(18.dp))
        LiquidGlassCard {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text("大小  ${config.sizeDp} dp", fontWeight = FontWeight.SemiBold)
                Slider(config.sizeDp.toFloat(), { config = config.copy(sizeDp = it.roundToInt()) }, valueRange = 46f..82f,
                    onValueChangeFinished = { persist(config) })
                Text("透明度  ${(config.opacity * 100).roundToInt()}%", fontWeight = FontWeight.SemiBold)
                Slider(config.opacity, { config = config.copy(opacity = it) }, valueRange = 0.42f..1f,
                    onValueChangeFinished = { persist(config) })
            }
            GlassDivider()
            OverlaySwitchRow("贴边吸附", "拖动松手后吸附最近边缘", config.snapToEdge) { persist(config.copy(snapToEdge = it)) }
            GlassDivider()
            OverlaySwitchRow("锁定位置", "锁定后禁止拖动，快捷手势仍可用", config.lockPosition) { persist(config.copy(lockPosition = it)) }
            GlassDivider()
            OverlaySwitchRow("触感反馈", "识别成功时提供轻微振动", config.hapticFeedback) { persist(config.copy(hapticFeedback = it)) }
            GlassDivider()
            OverlaySwitchRow("暂时隐藏", "保留设置但停止悬浮窗服务", config.temporarilyHidden) { persist(config.copy(temporarilyHidden = it)) }
        }
        Spacer(Modifier.height(112.dp))
    }

    pickerGesture?.let { gesture ->
        val current = config.actions.getValue(gesture)
        AppPickerDialog(
            gesture = gesture,
            current = current,
            onDismiss = { pickerGesture = null; enableAfterPicking = false },
            onSelected = { app ->
                pickerGesture = null
                val updated = current.copy(packageName = app.packageName, activityName = app.resolveInfo.activityInfo.name, label = app.label, enabled = true)
                val updatedConfig = config.copy(actions = config.actions + (gesture to updated))
                persist(updatedConfig, refresh = false)
                if (enableAfterPicking) { enableAfterPicking = false; finishEnable(updatedConfig) } else QuickOverlayService.refresh(context)
            },
            onClear = {
                pickerGesture = null
                persist(config.copy(actions = config.actions + (gesture to GestureAction(gesture))))
            },
            onEnabledChange = { enabled ->
                pickerGesture = null
                persist(config.copy(actions = config.actions + (gesture to current.copy(enabled = enabled))))
            }
        )
    }

    if (showTutorial) AlertDialog(
        onDismissRequest = { showTutorial = false },
        title = { Text("快捷手势") },
        text = { Text("双击：快速打开默认动作\n四向滑动：沿明确方向滑动后松手\n长按拖动：等待触感反馈后再移动\n\n拖动状态不会启动应用，方向不明确的滑动会自动取消。") },
        confirmButton = { TextButton(onClick = {
            showTutorial = false
            val updatedConfig = config.copy(tutorialShown = true)
            persist(updatedConfig, refresh = false)
            finishEnable(updatedConfig)
        }) { Text("知道了") } },
        dismissButton = { TextButton(onClick = { showTutorial = false }) { Text("以后再说") } }
    )
}

@Composable
private fun AppPickerDialog(
    gesture: String,
    current: GestureAction,
    onDismiss: () -> Unit,
    onSelected: (LaunchableApp) -> Unit,
    onClear: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .asSequence().filter { it.activityInfo.packageName != context.packageName }
                .distinctBy { it.activityInfo.packageName }.map {
                    LaunchableApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString(), it)
                }.sortedBy { it.label.lowercase() }.toList()
        }
    }
    val filtered = remember(apps, query) { if (query.isBlank()) apps else apps.filter {
        it.label.contains(query, true) || it.packageName.contains(query, true)
    } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxSize().padding(12.dp), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${OverlayGesture.label(gesture)}动作", fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onEnabledChange(!current.enabled) }) { Text(if (current.enabled) "禁用手势" else "启用手势") }
                    if (current.packageName.isNotBlank()) TextButton(onClick = onClear) { Text("清除绑定") }
                }
                OutlinedTextField(query, { query = it }, label = { Text("搜索应用") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(Modifier.fillMaxWidth().clickable { onSelected(app) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            val bitmap = remember(app.packageName) { app.resolveInfo.loadIcon(context.packageManager).toBitmap(96, 96).asImageBitmap() }
                            Image(bitmap, null, Modifier.size(44.dp))
                            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun GlassDivider() = HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = .16f))

@Composable
private fun OverlayClickRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun OverlaySwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange)
    }
}
