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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.npucourse.overlay.OverlayGesture
import com.example.npucourse.overlay.QuickOverlayConfig
import com.example.npucourse.overlay.QuickOverlayPreferences
import com.example.npucourse.overlay.QuickOverlayService
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
    var showPicker by remember { mutableStateOf(false) }
    var enableAfterPicking by remember { mutableStateOf(false) }
    var showGesturePicker by remember { mutableStateOf(false) }
    val canOverlay = remember(permissionRefresh) { Settings.canDrawOverlays(context) }
    val targetAvailable = remember(config.targetPackage, permissionRefresh) {
        config.targetPackage.isNotBlank() && context.packageManager.getLaunchIntentForPackage(config.targetPackage) != null
    }

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
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
            )
        } else QuickOverlayService.refresh(context)
    }

    fun enableOverlay() {
        if (config.targetPackage.isBlank()) {
            enableAfterPicking = true
            showPicker = true
            return
        }
        persist(config.copy(enabled = true, temporarilyHidden = false), refresh = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!Settings.canDrawOverlays(context)) {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
            )
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            Text("快捷悬浮窗", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            "双击或定向滑动后才会启动目标应用；长按再拖动可移动悬浮窗。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OverlaySettingCard {
            OverlaySwitchRow(
                title = "启用悬浮窗",
                subtitle = when {
                    !canOverlay -> "需要授予“显示在其他应用上层”权限"
                    !targetAvailable -> "请先选择一个可启动的应用"
                    config.temporarilyHidden -> "已启用，目前暂时隐藏"
                    else -> "权限就绪 · 前台服务按需运行"
                },
                checked = config.enabled,
                onCheckedChange = { enabled ->
                    if (enabled) enableOverlay() else persist(config.copy(enabled = false, temporarilyHidden = false))
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            OverlayClickRow("目标应用", if (targetAvailable) config.targetLabel else "未选择或应用已卸载") { showPicker = true }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            OverlayClickRow("启动手势", OverlayGesture.label(config.gesture)) { showGesturePicker = true }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            OverlayClickRow(
                "悬浮窗权限",
                if (canOverlay) "已允许" else "点击前往系统设置"
            ) {
                overlayPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        OverlaySettingCard {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text("大小  ${config.sizeDp} dp", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = config.sizeDp.toFloat(),
                    onValueChange = { config = config.copy(sizeDp = it.roundToInt()) },
                    onValueChangeFinished = { persist(config) },
                    valueRange = 46f..82f
                )
                Text("透明度  ${(config.opacity * 100).roundToInt()}%", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = config.opacity,
                    onValueChange = { config = config.copy(opacity = it) },
                    onValueChangeFinished = { persist(config) },
                    valueRange = 0.42f..1f
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            OverlaySwitchRow("贴边吸附", "拖动松手后用回弹动画吸附最近边缘", config.snapToEdge) {
                persist(config.copy(snapToEdge = it))
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            OverlaySwitchRow("锁定位置", "锁定后禁止拖动，快捷手势仍可用", config.lockPosition) {
                persist(config.copy(lockPosition = it))
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            OverlaySwitchRow("暂时隐藏", "保留设置但停止悬浮窗服务", config.temporarilyHidden) {
                persist(config.copy(temporarilyHidden = it))
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    if (showPicker) {
        AppPickerDialog(
            onDismiss = {
                showPicker = false
                enableAfterPicking = false
            },
            onSelected = { app ->
                showPicker = false
                val shouldEnable = enableAfterPicking || config.enabled
                enableAfterPicking = false
                persist(
                    config.copy(
                        enabled = shouldEnable,
                        temporarilyHidden = if (shouldEnable) false else config.temporarilyHidden,
                        targetPackage = app.packageName,
                        targetLabel = app.label
                    ),
                    refresh = false
                )
                if (shouldEnable) enableOverlay()
            }
        )
    }

    if (showGesturePicker) {
        AlertDialog(
            onDismissRequest = { showGesturePicker = false },
            title = { Text("选择启动手势") },
            text = {
                Column {
                    OverlayGesture.all.forEach { gesture ->
                        Text(
                            OverlayGesture.label(gesture),
                            modifier = Modifier.fillMaxWidth().clickable {
                                showGesturePicker = false
                                persist(config.copy(gesture = gesture))
                            }.padding(vertical = 13.dp),
                            fontWeight = if (gesture == config.gesture) FontWeight.Bold else FontWeight.Normal,
                            color = if (gesture == config.gesture) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGesturePicker = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun AppPickerDialog(onDismiss: () -> Unit, onSelected: (LaunchableApp) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
                .asSequence()
                .filter { it.activityInfo.packageName != context.packageName }
                .distinctBy { it.activityInfo.packageName }
                .map {
                    LaunchableApp(
                        packageName = it.activityInfo.packageName,
                        label = it.loadLabel(context.packageManager).toString(),
                        resolveInfo = it
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
    }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("选择目标应用", fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索应用") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelected(app) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(app.packageName) {
                                app.resolveInfo.loadIcon(context.packageManager).toBitmap(96, 96).asImageBitmap()
                            }
                            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(44.dp))
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

@Composable
private fun OverlaySettingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { content() }
}

@Composable
private fun OverlayClickRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun OverlaySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
