package com.example.npucourse.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.npucourse.importer.EduCourseRecord
import com.example.npucourse.importer.NwpuSemesterDetector
import com.example.npucourse.importer.NwpuSemesterInfo
import com.example.npucourse.importer.NwpuTimetableExtractor


private const val NPU_PORTAL_URL =
    "https://ecampus.nwpu.edu.cn/"

private const val NPU_ACADEMIC_HOME_URL =
    "https://jwxt.nwpu.edu.cn/student/home"

private const val NPU_COURSE_TABLE_URL =
    "https://jwxt.nwpu.edu.cn/student/for-std/course-table"


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EduLoginPage(
    onBack: () -> Unit,
    onLoginCompleted: () -> Unit,
    onCoursesExtracted:
        (
        List<EduCourseRecord>,
        NwpuSemesterInfo?
    ) -> Unit
) {

    val webViewHolder =
        remember {
            arrayOfNulls<WebView>(1)
        }


    var currentUrl by remember {
        mutableStateOf(
            NPU_PORTAL_URL
        )
    }


    var currentTitle by remember {
        mutableStateOf("")
    }


    var loading by remember {
        mutableStateOf(false)
    }


    var portalLoggedIn by remember {
        mutableStateOf(false)
    }


    var academicOpened by remember {
        mutableStateOf(false)
    }


    var courseTableOpened by remember {
        mutableStateOf(false)
    }


    var extracting by remember {
        mutableStateOf(false)
    }


    var statusText by remember {
        mutableStateOf<String?>(
            null
        )
    }


    var pageError by remember {
        mutableStateOf<String?>(
            null
        )
    }


    BackHandler {

        val webView =
            webViewHolder[0]


        if (
            webView != null &&
            webView.canGoBack()
        ) {

            webView.goBack()

        } else {

            onBack()
        }
    }


    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    )
        ) {


            Text(
                text =
                    when {

                        courseTableOpened ->
                            "我的课表"

                        academicOpened ->
                            "翱翔教务"

                        portalLoggedIn ->
                            "翱翔门户已登录"

                        else ->
                            "翱翔门户登录"
                    },

                fontSize =
                    21.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme.onBackground
            )


            Spacer(
                modifier =
                    Modifier.height(
                        3.dp
                    )
            )


            Text(
                text =
                    when {

                        courseTableOpened ->
                            "课表页面已打开，可以智能识别学期并读取课表"

                        academicOpened ->
                            "教务系统已登录，可以打开本人课表"

                        portalLoggedIn ->
                            "门户白屏不影响认证"

                        else ->
                            "请在学校官方页面中完成统一身份认证"
                    },

                fontSize =
                    12.sp,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )


            if (
                currentTitle.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )


                Text(
                    text =
                        "页面：" +
                                currentTitle,

                    fontSize =
                        11.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    sanitizeUrl(
                        currentUrl
                    ),

                fontSize =
                    10.sp,

                maxLines =
                    2,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )


            if (
                loading ||
                extracting
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                LinearProgressIndicator(
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }


            if (
                pageError != null
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                Text(
                    text =
                        "页面异常：" +
                                pageError,

                    fontSize =
                        11.sp,

                    color =
                        MaterialTheme.colorScheme.error
                )
            }


            if (
                statusText != null
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                Text(
                    text =
                        statusText
                            ?: "",

                    fontSize =
                        11.sp,

                    color =
                        MaterialTheme.colorScheme.primary
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {


                OutlinedButton(
                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    onClick = {

                        val webView =
                            webViewHolder[0]


                        if (
                            webView != null &&
                            webView.canGoBack()
                        ) {

                            webView.goBack()

                        } else {

                            onBack()
                        }
                    }
                ) {

                    Text(
                        "← 返回"
                    )
                }


                OutlinedButton(
                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    onClick = {

                        pageError =
                            null

                        statusText =
                            null


                        webViewHolder[0]
                            ?.reload()
                    }
                ) {

                    Text(
                        "刷新"
                    )
                }
            }


            /*
             * =================================================
             * 门户 → 教务
             * =================================================
             */

            if (
                portalLoggedIn &&
                !academicOpened
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                Button(
                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        statusText =
                            "正在进入翱翔教务……"


                        openAcademicSystem(
                            webView =
                                webViewHolder[0],

                            onResult = {
                                    result: String ->


                                if (
                                    result == "CLICKED"
                                ) {

                                    statusText =
                                        "已触发翱翔教务入口"

                                } else {

                                    statusText =
                                        "正在直接打开翱翔教务"


                                    webViewHolder[0]
                                        ?.loadUrl(
                                            NPU_ACADEMIC_HOME_URL
                                        )
                                }
                            }
                        )
                    }
                ) {

                    Text(
                        "打开翱翔教务"
                    )
                }
            }


            /*
             * =================================================
             * 教务 → 我的课表
             * =================================================
             */

            if (
                academicOpened &&
                !courseTableOpened
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                Button(
                    modifier =
                        Modifier.fillMaxWidth(),

                    onClick = {

                        CookieManager
                            .getInstance()
                            .flush()


                        statusText =
                            "正在打开我的课表……"


                        webViewHolder[0]
                            ?.loadUrl(
                                NPU_COURSE_TABLE_URL
                            )
                    }
                ) {

                    Text(
                        "打开我的课表"
                    )
                }
            }


            /*
             * =================================================
             * 读取课表 + 学期
             * =================================================
             */

            if (
                courseTableOpened
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                Button(
                    modifier =
                        Modifier.fillMaxWidth(),

                    enabled =
                        !extracting,

                    onClick = {

                        val webView =
                            webViewHolder[0]


                        if (
                            webView == null
                        ) {

                            statusText =
                                "WebView 尚未准备好"

                            return@Button
                        }


                        extracting =
                            true


                        statusText =
                            "正在读取课表并识别学期……"


                        webView.postDelayed(
                            {

                                NwpuTimetableExtractor
                                    .extract(
                                        webView
                                    ) {
                                            result ->


                                        result
                                            .onSuccess {
                                                    records ->


                                                /*
                                                 * 课程成功后，
                                                 * 再检测当前网页选择的学期。
                                                 */
                                                NwpuSemesterDetector
                                                    .detect(
                                                        webView
                                                    ) {
                                                            semesterInfo ->


                                                        extracting =
                                                            false


                                                        statusText =
                                                            if (
                                                                semesterInfo
                                                                    ?.startMillis != null
                                                            ) {

                                                                "成功读取 " +
                                                                        records.size +
                                                                        " 条课程，并识别到 " +
                                                                        semesterInfo.label

                                                            } else if (
                                                                semesterInfo != null
                                                            ) {

                                                                "成功读取 " +
                                                                        records.size +
                                                                        " 条课程；已识别学期，但起始日期需要手动确认"

                                                            } else {

                                                                "成功读取 " +
                                                                        records.size +
                                                                        " 条课程；未识别学期"
                                                            }


                                                        onCoursesExtracted(
                                                            records,
                                                            semesterInfo
                                                        )
                                                    }
                                            }
                                            .onFailure {
                                                    throwable ->


                                                extracting =
                                                    false


                                                statusText =
                                                    "读取失败：" +
                                                            (
                                                                    throwable.message
                                                                        ?: "未知错误"
                                                                    )
                                            }
                                    }

                            },
                            700L
                        )
                    }
                ) {

                    Text(
                        if (
                            extracting
                        ) {

                            "正在读取……"

                        } else {

                            "读取当前课表并智能识别学期"
                        }
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )


            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),

                onClick = {

                    CookieManager
                        .getInstance()
                        .flush()


                    onLoginCompleted()
                }
            ) {

                Text(
                    "仅保存登录状态并返回"
                )
            }
        }


        /*
         * =================================================
         * WebView
         * =================================================
         */

        AndroidView(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(
                        1f
                    ),

            factory = {
                    context ->


                WebView(
                    context
                ).apply {


                    webViewHolder[0] =
                        this


                    settings.javaScriptEnabled =
                        true

                    settings.domStorageEnabled =
                        true

                    settings.cacheMode =
                        WebSettings.LOAD_DEFAULT

                    settings.loadsImagesAutomatically =
                        true

                    settings.useWideViewPort =
                        true

                    settings.loadWithOverviewMode =
                        true

                    settings.allowFileAccess =
                        false

                    settings.allowContentAccess =
                        false

                    settings.mixedContentMode =
                        WebSettings
                            .MIXED_CONTENT_NEVER_ALLOW

                    settings.setSupportMultipleWindows(
                        false
                    )

                    settings.javaScriptCanOpenWindowsAutomatically =
                        true


                    settings.userAgentString =
                        settings
                            .userAgentString
                            .replace(
                                "; wv",
                                ""
                            )
                            .replace(
                                " Version/4.0",
                                ""
                            )


                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.O
                    ) {

                        settings.safeBrowsingEnabled =
                            true
                    }


                    val cookieManager =
                        CookieManager
                            .getInstance()


                    cookieManager
                        .setAcceptCookie(
                            true
                        )


                    cookieManager
                        .setAcceptThirdPartyCookies(
                            this,
                            true
                        )


                    webViewClient =
                        object :
                            WebViewClient() {


                            override fun onPageStarted(
                                view: WebView,
                                url: String,
                                favicon: Bitmap?
                            ) {

                                super.onPageStarted(
                                    view,
                                    url,
                                    favicon
                                )


                                currentUrl =
                                    url

                                loading =
                                    true

                                pageError =
                                    null
                            }


                            override fun onPageFinished(
                                view: WebView,
                                url: String
                            ) {

                                super.onPageFinished(
                                    view,
                                    url
                                )


                                currentUrl =
                                    url

                                currentTitle =
                                    view.title
                                        ?: ""

                                loading =
                                    false


                                CookieManager
                                    .getInstance()
                                    .flush()


                                when {


                                    isCourseTablePage(
                                        url
                                    ) -> {

                                        portalLoggedIn =
                                            true

                                        academicOpened =
                                            true

                                        courseTableOpened =
                                            true

                                        statusText =
                                            "课表页面加载完成"
                                    }


                                    isAcademicSystemPage(
                                        url
                                    ) -> {

                                        portalLoggedIn =
                                            true

                                        academicOpened =
                                            true

                                        courseTableOpened =
                                            false
                                    }


                                    isPortalHome(
                                        url
                                    ) -> {

                                        portalLoggedIn =
                                            true

                                        academicOpened =
                                            false

                                        courseTableOpened =
                                            false
                                    }
                                }
                            }


                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request:
                                WebResourceRequest
                            ): Boolean {

                                return shouldBlockUrl(
                                    request
                                        .url
                                        .toString()
                                )
                            }


                            @Suppress(
                                "DEPRECATION"
                            )
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                url: String
                            ): Boolean {

                                return shouldBlockUrl(
                                    url
                                )
                            }


                            override fun onReceivedError(
                                view: WebView,
                                request:
                                WebResourceRequest,
                                error:
                                WebResourceError
                            ) {

                                super.onReceivedError(
                                    view,
                                    request,
                                    error
                                )


                                if (
                                    request.isForMainFrame
                                ) {

                                    loading =
                                        false


                                    pageError =
                                        error
                                            .description
                                            .toString()
                                }
                            }


                            override fun onReceivedHttpError(
                                view: WebView,
                                request:
                                WebResourceRequest,
                                errorResponse:
                                WebResourceResponse
                            ) {

                                super.onReceivedHttpError(
                                    view,
                                    request,
                                    errorResponse
                                )


                                if (
                                    request.isForMainFrame
                                ) {

                                    loading =
                                        false


                                    pageError =
                                        "HTTP " +
                                                errorResponse
                                                    .statusCode
                                }
                            }
                        }


                    loadUrl(
                        NPU_PORTAL_URL
                    )
                }
            }
        )
    }


    DisposableEffect(
        Unit
    ) {

        onDispose {


            CookieManager
                .getInstance()
                .flush()


            val webView =
                webViewHolder[0]


            if (
                webView != null
            ) {

                webView.stopLoading()

                webView.removeAllViews()

                webView.destroy()
            }


            webViewHolder[0] =
                null
        }
    }
}


/* =========================================================
   门户打开教务
   ========================================================= */

private fun openAcademicSystem(
    webView: WebView?,
    onResult: (String) -> Unit
) {

    if (
        webView == null
    ) {

        onResult(
            "NO_WEBVIEW"
        )

        return
    }


    val script =
        """
        (function() {

            try {

                var keyword = "翱翔教务";

                window.open =
                    function(url) {

                        if (url) {
                            window.location.href = url;
                        }

                        return window;
                    };


                var nodes =
                    Array.prototype.slice.call(
                        document.querySelectorAll(
                            "a,button,[role='button'],li,div,span"
                        )
                    );


                var matches =
                    nodes.filter(
                        function(node) {

                            var text =
                                (node.innerText || "")
                                    .replace(/\s+/g, "")
                                    .trim();

                            return text === keyword;
                        }
                    );


                if (
                    matches.length === 0
                ) {

                    return "NOT_FOUND";
                }


                var element =
                    matches[0];


                var anchor =
                    element.closest
                        ? element.closest("a")
                        : null;


                if (anchor) {

                    anchor.target =
                        "_self";

                    anchor.click();

                } else {

                    element.click();
                }


                return "CLICKED";


            } catch (error) {

                return "ERROR";
            }

        })();
        """.trimIndent()


    webView.evaluateJavascript(
        script
    ) {
            result ->


        val cleaned =
            result
                ?.trim()
                ?.removeSurrounding(
                    "\""
                )
                ?: "ERROR"


        onResult(
            cleaned
        )
    }
}


/* =========================================================
   页面判断
   ========================================================= */

private fun isPortalHome(
    url: String
): Boolean {

    val uri =
        safeUri(
            url
        )
            ?: return false


    return uri.host
        ?.lowercase() ==
            "ecampus.nwpu.edu.cn" &&
            (
                    uri.path
                        ?.contains(
                            "main.html"
                        ) ==
                            true
                    )
}


private fun isAcademicSystemPage(
    url: String
): Boolean {

    val uri =
        safeUri(
            url
        )
            ?: return false


    return uri.host
        ?.lowercase() ==
            "jwxt.nwpu.edu.cn"
}


private fun isCourseTablePage(
    url: String
): Boolean {

    val uri =
        safeUri(
            url
        )
            ?: return false


    return uri.host
        ?.lowercase() ==
            "jwxt.nwpu.edu.cn" &&
            (
                    uri.path
                        ?.contains(
                            "/student/for-std/course-table"
                        ) ==
                            true
                    )
}


/* =========================================================
   URL
   ========================================================= */

private fun shouldBlockUrl(
    url: String
): Boolean {

    val uri =
        safeUri(
            url
        )
            ?: return true


    return uri.scheme
        ?.lowercase() !=
            "https"
}


private fun sanitizeUrl(
    url: String
): String {

    val uri =
        safeUri(
            url
        )
            ?: return url


    return buildString {

        append(
            uri.scheme
                ?: "https"
        )

        append(
            "://"
        )

        append(
            uri.host
                ?: ""
        )

        append(
            uri.path
                ?: ""
        )

        if (
            !uri.fragment
                .isNullOrBlank()
        ) {

            append(
                "#"
            )

            append(
                uri.fragment
            )
        }
    }
}


private fun safeUri(
    url: String
): Uri? {

    return runCatching {

        Uri.parse(
            url
        )

    }.getOrNull()
}
