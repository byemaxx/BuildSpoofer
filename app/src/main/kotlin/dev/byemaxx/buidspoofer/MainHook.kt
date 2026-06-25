package dev.byemaxx.buidspoofer

import android.content.pm.FeatureInfo
import android.os.Build
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class MainHook : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "Loaded in ${param.processName}")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (!param.isFirstPackage) return
        if (param.packageName == "dev.byemaxx.buidspoofer") return

        val template = try {
            val prefs = getRemotePreferences(TemplateManager.REMOTE_PREFS_GROUP)
            TemplateManager.getActiveTemplateFromXposed(prefs)
        } catch (error: Throwable) {
            log(Log.ERROR, TAG, "Unable to read remote template preferences", error)
            return
        }

        if (template.id == "default") {
            log(Log.INFO, TAG, "Default template active for ${param.packageName}; skipping")
            return
        }
        log(Log.INFO, TAG, "Applying ${template.name} to ${param.packageName}")

        val props = template.properties

        fun safeSet(clazz: Class<*>, fieldName: String, key: String) {
            val value = props[key]
            if (!value.isNullOrEmpty()) {
                try {
                    val field = clazz.getDeclaredField(fieldName)
                    field.isAccessible = true
                    field.set(null, value)
                } catch (error: Throwable) {
                    log(Log.WARN, TAG, "Unable to set ${clazz.name}.$fieldName", error)
                }
            }
        }

        fun safeSetInt(clazz: Class<*>, fieldName: String, key: String) {
            val value = props[key]
            if (!value.isNullOrEmpty()) {
                value.toIntOrNull()?.let {
                    try {
                        val field = clazz.getDeclaredField(fieldName)
                        field.isAccessible = true
                        field.setInt(null, it)
                    } catch (error: Throwable) {
                        log(Log.WARN, TAG, "Unable to set ${clazz.name}.$fieldName", error)
                    }
                }
            }
        }

        fun safeSetLong(clazz: Class<*>, fieldName: String, key: String) {
            val value = props[key]
            if (!value.isNullOrEmpty()) {
                value.toLongOrNull()?.let {
                    try {
                        val field = clazz.getDeclaredField(fieldName)
                        field.isAccessible = true
                        field.setLong(null, it)
                    } catch (error: Throwable) {
                        log(Log.WARN, TAG, "Unable to set ${clazz.name}.$fieldName", error)
                    }
                }
            }
        }

        safeSet(Build::class.java, "BOOTLOADER", "BOOTLOADER")
        safeSet(Build::class.java, "HARDWARE", "DEVICE")
        safeSet(Build::class.java, "BOARD", "DEVICE")
        safeSet(Build::class.java, "BRAND", "BRAND")
        safeSet(Build::class.java, "DEVICE", "DEVICE")
        safeSet(Build::class.java, "PRODUCT", "PRODUCT")
        safeSet(Build::class.java, "MANUFACTURER", "MANUFACTURER")
        safeSet(Build::class.java, "MODEL", "MODEL")
        safeSet(Build::class.java, "SOC_MANUFACTURER", "SOC_MANUFACTURER")
        safeSet(Build::class.java, "SOC_MODEL", "SOC_MODEL")
        safeSet(Build::class.java, "ID", "ID")
        safeSetLong(Build::class.java, "TIME", "TIME")
        safeSet(Build::class.java, "TAGS", "TAGS")
        safeSet(Build::class.java, "TYPE", "TYPE")
        safeSet(Build::class.java, "USER", "USER")
        safeSet(Build::class.java, "HOST", "HOST")
        safeSet(Build::class.java, "FINGERPRINT", "FINGERPRINT")

        val displayVal = props["DISPLAY"] ?: props["ID"]
        if (!displayVal.isNullOrEmpty()) {
            try {
                val field = Build::class.java.getDeclaredField("DISPLAY")
                field.isAccessible = true
                field.set(null, displayVal)
            } catch (error: Throwable) {
                log(Log.WARN, TAG, "Unable to set Build.DISPLAY", error)
            }
        }

        try {
            props["SUPPORTED_ABIS"]?.takeIf { it.isNotEmpty() }?.let { abis ->
                val field1 = Build::class.java.getDeclaredField("SUPPORTED_ABIS")
                field1.isAccessible = true
                field1.set(null, abis.split(",").map { it.trim() }.toTypedArray())
            }
            
            props["SUPPORTED_64_BIT_ABIS"]?.takeIf { it.isNotEmpty() }?.let { abis ->
                val field2 = Build::class.java.getDeclaredField("SUPPORTED_64_BIT_ABIS")
                field2.isAccessible = true
                field2.set(null, abis.split(",").map { it.trim() }.toTypedArray())
            }

            props["SUPPORTED_32_BIT_ABIS"]?.takeIf { it.isNotEmpty() }?.let { abis ->
                val field3 = Build::class.java.getDeclaredField("SUPPORTED_32_BIT_ABIS")
                field3.isAccessible = true
                field3.set(null, abis.split(",").map { it.trim() }.toTypedArray())
            }
        } catch (error: Throwable) {
            log(Log.WARN, TAG, "Unable to override supported ABIs", error)
        }

        safeSet(Build.VERSION::class.java, "RELEASE", "RELEASE")
        safeSetInt(Build.VERSION::class.java, "SDK_INT", "SDK_INT")
        safeSet(Build.VERSION::class.java, "SECURITY_PATCH", "SECURITY_PATCH")
        safeSet(Build.VERSION::class.java, "INCREMENTAL", "INCREMENTAL")

        val standardKeys = setOf(
            "HARDWARE", "BOARD", "BRAND", "DEVICE", "PRODUCT", "MANUFACTURER", "MODEL", 
            "SOC_MANUFACTURER", "SOC_MODEL", "ID", "TIME", "TAGS", "TYPE", "USER", "HOST", 
            "FINGERPRINT", "DISPLAY", "SUPPORTED_ABIS", "SUPPORTED_64_BIT_ABIS", 
            "SUPPORTED_32_BIT_ABIS", "RELEASE", "SDK_INT", "SECURITY_PATCH", "INCREMENTAL", 
            "BASEBAND", "FIRST_API_LEVEL", "SDK_FULL", "BUILD_UUID", "CLIENT_ID", "TIME_SEC", 
            "BUILD_DATE", "BOOTLOADER", "PLATFORM"
        )
        
        for ((key, value) in props) {
            if (key in standardKeys || key.contains(".")) continue
            
            try {
                val field = Build::class.java.getDeclaredField(key)
                field.isAccessible = true
                if (field.type == Int::class.javaPrimitiveType) {
                    value.toIntOrNull()?.let { field.set(null, it) }
                } else if (field.type == Long::class.javaPrimitiveType) {
                    value.toLongOrNull()?.let { field.set(null, it) }
                } else {
                    field.set(null, value)
                }
                continue
            } catch (e: Throwable) {}
            
            try {
                val field = Build.VERSION::class.java.getDeclaredField(key)
                field.isAccessible = true
                if (field.type == Int::class.javaPrimitiveType) {
                    value.toIntOrNull()?.let { field.set(null, it) }
                } else {
                    field.set(null, value)
                }
            } catch (e: Throwable) {}
        }

        val basebandValue = props["BASEBAND"]
        if (!basebandValue.isNullOrEmpty()) {
            try {
                val getRadioVersionMethod = Build::class.java.getDeclaredMethod("getRadioVersion")
                hook(getRadioVersionMethod).intercept { _ ->
                    basebandValue
                }
            } catch (error: Throwable) {
                log(Log.WARN, TAG, "Unable to hook Build.getRadioVersion", error)
            }
        }

        val features = template.features
        if (features.isNotEmpty()) {
            try {
                val pmsClass = Class.forName("android.app.ApplicationPackageManager", true, param.defaultClassLoader)
                val hasSystemFeature1 = pmsClass.getDeclaredMethod("hasSystemFeature", String::class.java)
                hook(hasSystemFeature1).intercept { chain ->
                    if (features.contains(chain.args[0] as String)) {
                        true
                    } else {
                        chain.proceed()
                    }
                }

                val hasSystemFeature2 = pmsClass.getDeclaredMethod("hasSystemFeature", String::class.java, Int::class.javaPrimitiveType)
                hook(hasSystemFeature2).intercept { chain ->
                    if (features.contains(chain.args[0] as String)) {
                        true
                    } else {
                        chain.proceed()
                    }
                }

                val getSystemAvailableFeatures = pmsClass.getDeclaredMethod("getSystemAvailableFeatures")
                hook(getSystemAvailableFeatures).intercept { chain ->
                    val originalFeatures = chain.proceed() as? Array<FeatureInfo>
                    if (originalFeatures != null) {
                        val newFeatures = originalFeatures.toMutableList()
                        for (featureName in features) {
                            if (originalFeatures.none { it.name == featureName }) {
                                val info = FeatureInfo().apply { name = featureName }
                                newFeatures.add(info)
                            }
                        }
                        newFeatures.toTypedArray()
                    } else {
                        null
                    }
                }
            } catch (error: Throwable) {
                log(Log.ERROR, TAG, "Unable to hook package features", error)
            }
        }

        try {
            val sysPropClass = Class.forName("android.os.SystemProperties", true, param.defaultClassLoader)
            
            val handleSysProp: (String) -> String? = { key ->
                when {
                    key == "ro.soc.model" -> props["SOC_MODEL"]
                    key == "ro.soc.manufacturer" -> props["SOC_MANUFACTURER"]
                    key.endsWith(".model") || key == "ro.product.model" || key == "ro.product.model_for_attestation" -> props["MODEL"]
                    key.endsWith(".device") || key == "ro.product.device" || key == "ro.product.device_for_attestation" -> props["DEVICE"]
                    key.endsWith(".name") || key == "ro.product.name" || key == "ro.product.name_for_attestation" -> props["PRODUCT"]
                    key.endsWith(".brand") || key == "ro.product.brand" || key == "ro.product.brand_for_attestation" -> props["BRAND"]
                    key.endsWith(".manufacturer") || key == "ro.product.manufacturer" || key == "ro.product.manufacturer_for_attestation" -> props["MANUFACTURER"]
                    key.endsWith(".board") || key == "ro.product.board" -> props["DEVICE"]
                    key.endsWith(".build.display.id") || key == "ro.build.display.id" -> props["DISPLAY"] ?: props["ID"]
                    key.endsWith(".build.fingerprint") || key == "ro.build.fingerprint" -> props["FINGERPRINT"]
                    key.endsWith(".build.id") || key == "ro.build.id" -> props["ID"]
                    key.endsWith(".build.tags") || key == "ro.build.tags" -> props["TAGS"]
                    key.endsWith(".build.type") || key == "ro.build.type" -> props["TYPE"]
                    key == "ro.build.user" -> props["USER"]
                    key == "ro.build.host" -> props["HOST"]
                    key == "ro.board.platform" -> props["PLATFORM"]
                    key == "ro.bootloader" || key == "ro.build.expect.bootloader" -> props["BOOTLOADER"]
                    key == "ro.build.description" -> props["DESCRIPTION"]
                    key.startsWith("ro.com.google.clientidbase") -> props["CLIENT_ID"]
                    key.endsWith(".build.version.release") || key == "ro.build.version.release_or_codename" -> props["RELEASE"]
                    key.endsWith(".build.version.sdk") -> props["SDK_INT"]
                    key.endsWith(".build.version.security_patch") -> props["SECURITY_PATCH"]
                    key.endsWith(".build.version.incremental") -> props["INCREMENTAL"]
                    key == "ro.product.first_api_level" -> props["FIRST_API_LEVEL"]
                    key.endsWith(".build.version.sdk_full") -> props["SDK_FULL"]
                    key.endsWith(".build.uuid") -> props["BUILD_UUID"]
                    key.endsWith(".build.date") -> props["BUILD_DATE"]
                    key.endsWith(".build.date.utc") -> props["TIME_SEC"]
                    key.contains("baseband") -> props["BASEBAND"]
                    key.endsWith(".cpu.abilist") -> props["SUPPORTED_ABIS"]
                    key.endsWith(".cpu.abilist32") -> props["SUPPORTED_32_BIT_ABIS"]
                    key.endsWith(".cpu.abilist64") -> props["SUPPORTED_64_BIT_ABIS"]
                    props.containsKey(key) -> props[key]
                    else -> null
                }
            }

            val getMethod1 = sysPropClass.getDeclaredMethod("get", String::class.java)
            hook(getMethod1).intercept { chain ->
                val key = chain.args[0] as String
                val overrideValue = handleSysProp(key)
                if (!overrideValue.isNullOrEmpty()) {
                    overrideValue
                } else {
                    chain.proceed()
                }
            }

            val getMethod2 = sysPropClass.getDeclaredMethod("get", String::class.java, String::class.java)
            hook(getMethod2).intercept { chain ->
                val key = chain.args[0] as String
                val overrideValue = handleSysProp(key)
                if (!overrideValue.isNullOrEmpty()) {
                    overrideValue
                } else {
                    chain.proceed()
                }
            }
        } catch (error: Throwable) {
            log(Log.ERROR, TAG, "Unable to hook SystemProperties", error)
        }
    }

    companion object {
        private const val TAG = "BuidSpoofer"
    }
}
