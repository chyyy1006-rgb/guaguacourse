// Top-level build file

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Room 使用 KSP 生成数据库代码
    id("com.google.devtools.ksp") version "2.3.11" apply false
}