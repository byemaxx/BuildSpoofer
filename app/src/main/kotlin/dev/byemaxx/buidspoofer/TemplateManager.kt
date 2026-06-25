package dev.byemaxx.buidspoofer

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TemplateManager {
    const val PREFS_NAME = "buidspoofer_prefs"
    const val REMOTE_PREFS_GROUP = "templates"
    const val KEY_ACTIVE_TEMPLATE_ID = "active_template_id"
    const val KEY_CUSTOM_TEMPLATES = "custom_templates"
    const val KEY_DEFAULT_TEMPLATE = "default_template"

    val PIXEL_10_PRO_XL = Template(
        id = "pixel_10_pro_xl",
        name = "Pixel 10 Pro XL (mustang)",
        properties = mapOf(
            "MODEL" to "Pixel 10 Pro XL",
            "DEVICE" to "mustang",
            "PRODUCT" to "mustang",
            "BRAND" to "google",
            "MANUFACTURER" to "Google",
            "FINGERPRINT" to "google/mustang/mustang:17/CP2A.260605.012/15430684:user/release-keys",
            "DESCRIPTION" to "mustang-user 17 CP2A.260605.012 15430684 release-keys",
            "ID" to "CP2A.260605.012",
            "BOOTLOADER" to "deepspace-17.2-15372054",
            "SOC_MANUFACTURER" to "Google",
            "SOC_MODEL" to "Tensor G5",
            "PLATFORM" to "laguna",
            "TIME" to "1778884728000",
            "TIME_SEC" to "1778884728",
            "TAGS" to "release-keys",
            "TYPE" to "user",
            "USER" to "android-build",
            "HOST" to "bcb8c9bcce95",
            "RELEASE" to "",
            "SDK_INT" to "",
            "SDK_FULL" to "",
            "SECURITY_PATCH" to "2026-06-05",
            "INCREMENTAL" to "15430684",
            "FIRST_API_LEVEL" to "",
            "BUILD_DATE" to "Fri May 15 15:38:48 PDT 2026",
            "BUILD_UUID" to "Pp8difaf-KKDmY-2rNwgGVP0L-Oy5ujwsPvXWqUJuoo",
            "BASEBAND" to "g5400i-260317-260429-B-15308590",
            "CLIENT_ID" to "android-google"
        ),
        features = listOf(
            "com.google.android.feature.PIXEL_EXPERIENCE",
            "com.google.android.feature.TURBO_PRELOAD",
            "com.google.android.feature.WELLBEING",
            "com.google.android.feature.D2D_CABLE_MIGRATION_FEATURE",
            "com.google.android.feature.PIXEL_2017_EXPERIENCE",
            "com.google.android.feature.PIXEL_2018_EXPERIENCE",
            "com.google.android.feature.PIXEL_2019_EXPERIENCE",
            "com.google.android.feature.PIXEL_2019_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2020_EXPERIENCE",
            "com.google.android.feature.PIXEL_2020_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2021_EXPERIENCE",
            "com.google.android.feature.PIXEL_2021_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2022_EXPERIENCE",
            "com.google.android.feature.PIXEL_2022_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2023_EXPERIENCE",
            "com.google.android.feature.PIXEL_2023_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2024_EXPERIENCE",
            "com.google.android.feature.PIXEL_2024_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2025_EXPERIENCE",
            "com.google.android.feature.PIXEL_2025_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.GOOGLE_BUILD",
            "com.google.android.feature.GOOGLE_EXPERIENCE",
            "com.google.android.feature.GOOGLE_CAMERA_EXPERIENCE",
            "com.google.android.feature.QUICK_TAP",
            "com.google.android.feature.NOW_PLAYING_APP_26Q1",
            "com.google.android.feature.NEXT_GENERATION_ASSISTANT",
            "com.google.android.feature.GEMINI_EXPERIENCE",
            "com.google.android.feature.AMBIENT_DATA",
            "com.google.android.feature.CONTEXTUAL_SEARCH",
            "com.google.android.feature.CONTEXTUAL_SEARCH_LIVE_TRANSLATE",
            "com.android.systemui.SUPPORTS_DRAG_ASSISTANT_TO_SPLIT"
        ),
        isEditable = false
    )

    private val gson = Gson()

    fun getSharedPreferences(context: Context): SharedPreferences {
        val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun generateAndSaveDefaultTemplate(prefs: SharedPreferences): Template {
        val sysProps = mutableMapOf<String, String>()
        
        if (hasRoot()) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop"))
                val reader = process.inputStream.bufferedReader()
                reader.forEachLine { line ->
                    val regex = """\[(.*?)\]:\s*\[(.*?)\]""".toRegex()
                    val match = regex.find(line)
                    if (match != null) {
                        sysProps[match.groupValues[1]] = match.groupValues[2]
                    }
                }
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val mappedProps = mutableMapOf<String, String>()
        mappedProps["MODEL"] = sysProps["ro.product.model"] ?: Build.MODEL
        mappedProps["DEVICE"] = sysProps["ro.product.device"] ?: Build.DEVICE
        mappedProps["PRODUCT"] = sysProps["ro.product.name"] ?: Build.PRODUCT
        mappedProps["BRAND"] = sysProps["ro.product.brand"] ?: Build.BRAND
        mappedProps["MANUFACTURER"] = sysProps["ro.product.manufacturer"] ?: Build.MANUFACTURER
        mappedProps["FINGERPRINT"] = sysProps["ro.build.fingerprint"] ?: Build.FINGERPRINT
        mappedProps["ID"] = sysProps["ro.build.id"] ?: Build.ID
        mappedProps["DISPLAY"] = sysProps["ro.build.display.id"] ?: Build.DISPLAY
        mappedProps["BOOTLOADER"] = sysProps["ro.bootloader"] ?: Build.BOOTLOADER
        mappedProps["SOC_MANUFACTURER"] = sysProps["ro.soc.manufacturer"] ?: ""
        mappedProps["SOC_MODEL"] = sysProps["ro.soc.model"] ?: ""
        mappedProps["PLATFORM"] = sysProps["ro.board.platform"] ?: ""
        
        val timeUtc = sysProps["ro.build.date.utc"]
        mappedProps["TIME"] = timeUtc?.let { (it.toLong() * 1000).toString() } ?: Build.TIME.toString()
        mappedProps["TIME_SEC"] = timeUtc ?: (Build.TIME / 1000).toString()
        
        mappedProps["TAGS"] = sysProps["ro.build.tags"] ?: Build.TAGS
        mappedProps["TYPE"] = sysProps["ro.build.type"] ?: Build.TYPE
        mappedProps["USER"] = sysProps["ro.build.user"] ?: Build.USER
        mappedProps["HOST"] = sysProps["ro.build.host"] ?: Build.HOST
        mappedProps["RELEASE"] = sysProps["ro.build.version.release"] ?: Build.VERSION.RELEASE
        mappedProps["SDK_INT"] = sysProps["ro.build.version.sdk"] ?: Build.VERSION.SDK_INT.toString()
        mappedProps["SDK_FULL"] = sysProps["ro.build.version.sdk_full"] ?: ""
        mappedProps["SECURITY_PATCH"] = sysProps["ro.build.version.security_patch"] ?: Build.VERSION.SECURITY_PATCH
        mappedProps["INCREMENTAL"] = sysProps["ro.build.version.incremental"] ?: Build.VERSION.INCREMENTAL
        mappedProps["FIRST_API_LEVEL"] = sysProps["ro.product.first_api_level"] ?: ""
        mappedProps["BUILD_DATE"] = sysProps["ro.build.date"] ?: ""
        mappedProps["BUILD_UUID"] = sysProps["ro.build.uuid"] ?: ""
        
        @Suppress("DEPRECATION")
        mappedProps["BASEBAND"] = sysProps["gsm.version.baseband"] ?: Build.getRadioVersion() ?: ""
        mappedProps["CLIENT_ID"] = sysProps["ro.com.google.clientidbase"] ?: ""
        mappedProps["DESCRIPTION"] = sysProps["ro.build.description"] ?: ""

        val defaultTemplate = Template(
            id = "default",
            name = "Default (System)",
            properties = mappedProps,
            features = emptyList(),
            isEditable = false
        )

        prefs.edit().putString(KEY_DEFAULT_TEMPLATE, gson.toJson(defaultTemplate)).apply()
        return defaultTemplate
    }

    fun getDefaultTemplate(prefs: SharedPreferences): Template {
        val json = prefs.getString(KEY_DEFAULT_TEMPLATE, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Template::class.java)
            } catch (e: Exception) {
                generateAndSaveDefaultTemplate(prefs)
            }
        } else {
            generateAndSaveDefaultTemplate(prefs)
        }
    }

    fun getCustomTemplates(prefs: SharedPreferences): List<Template> {
        val json = prefs.getString(KEY_CUSTOM_TEMPLATES, "[]")
        val list = try {
            val type = object : TypeToken<List<Template>>() {}.type
            gson.fromJson<List<Template>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return list.map { it.copy(isEditable = true) }
    }

    fun saveCustomTemplates(prefs: SharedPreferences, templates: List<Template>, context: Context) {
        prefs.edit().putString(KEY_CUSTOM_TEMPLATES, gson.toJson(templates)).commit()
        App.syncPreferences(context)
    }

    fun getActiveTemplate(prefs: SharedPreferences): Template {
        val defaultTemplate = getDefaultTemplate(prefs)
        val activeId = prefs.getString(KEY_ACTIVE_TEMPLATE_ID, "default")
        val allTemplates = listOf(defaultTemplate, PIXEL_10_PRO_XL) + getCustomTemplates(prefs)
        return allTemplates.find { it.id == activeId } ?: defaultTemplate
    }

    fun setActiveTemplateId(prefs: SharedPreferences, id: String, context: Context) {
        prefs.edit().putString(KEY_ACTIVE_TEMPLATE_ID, id).commit()
        App.syncPreferences(context)
    }

    // For Xposed Hooks
    fun getActiveTemplateFromXposed(prefs: SharedPreferences): Template {
        val activeId = prefs.getString(KEY_ACTIVE_TEMPLATE_ID, "default") ?: "default"
        val customTemplatesJson = prefs.getString(KEY_CUSTOM_TEMPLATES, "[]") ?: "[]"

        val defaultTemplate = Template(
            id = "default",
            name = "Default (System)",
            properties = emptyMap(),
            features = emptyList(),
            isEditable = false
        )
        
        val customTemplates = try {
            val type = object : TypeToken<List<Template>>() {}.type
            val parsed = gson.fromJson<List<Template>>(customTemplatesJson, type) ?: emptyList()
            parsed.map { it.copy(isEditable = true) }
        } catch (e: Exception) {
            emptyList()
        }

        val allTemplates = listOf(defaultTemplate, PIXEL_10_PRO_XL) + customTemplates
        return allTemplates.find { it.id == activeId } ?: defaultTemplate
    }
}
