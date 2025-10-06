package com.example.remotecontrolapp.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID

object DeviceUtils {
    private const val PREFS_NAME = "rc_app_prefs"
    private const val KEY_APP_INSTANCE_ID = "app_instance_id"

    /** Human readable device name: "Manufacturer Model" */
    fun getDeviceName(): String {
        val man = Build.MANUFACTURER ?: "Unknown"
        val model = Build.MODEL ?: "Unknown"
        return if (model.startsWith(man, ignoreCase = true)) {
            model.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            "${man.replaceFirstChar { it.titlecase() }} $model"
        }
    }

    /** Manufacturer */
    fun getManufacturer(): String = Build.MANUFACTURER ?: "Unknown"

    /** Model */
    fun getModel(): String = Build.MODEL ?: "Unknown"

    /** Android ID (Settings.Secure.ANDROID_ID). Good for fingerprint, but not perfect global ID. */
    @Suppress("HardwareIds")
    fun getAndroidId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_android_id"

    /** App-scoped stable UUID persisted in SharedPreferences. Use this as your primary device id. */
    fun getOrCreateAppInstanceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_APP_INSTANCE_ID, null)
        if (id.isNullOrEmpty()) {
            id = UUID.randomUUID().toString()
            prefs.edit {
                putString(KEY_APP_INSTANCE_ID, id)
            }
        }
        return id
    }

    /**
     * Deterministic fingerprint: SHA-256(AndroidId | appInstanceId | deviceName)
     */
    fun getDeviceFingerprint(context: Context): String {
        val raw = "${getAndroidId(context)}|${getOrCreateAppInstanceId(context)}|${getDeviceName()}"
        return sha256Hex(raw)
    }

    /** Alias dễ hiểu hơn — unique ID chung cho app */
    fun getUniqueDeviceId(context: Context): String = getOrCreateAppInstanceId(context)

    // --- helpers ---
    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return String.format("%064x", BigInteger(1, digest))
    }
}