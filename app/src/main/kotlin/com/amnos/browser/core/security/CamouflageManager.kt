package com.amnos.browser.core.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.amnos.browser.core.session.AmnosLog

enum class CamouflageMode(val aliasName: String) {
    DEFAULT(".LauncherDefault"),
    CALCULATOR(".LauncherCalculator"),
    WEATHER(".LauncherWeather")
}

object CamouflageManager {
    
    fun applyMode(context: Context, mode: CamouflageMode) {
        val pm = context.packageManager
        val packageName = context.packageName

        AmnosLog.w("CamouflageManager", "Applying Identity Shift: ${mode.name}")

        CamouflageMode.values().forEach { m ->
            val componentName = ComponentName(packageName, "$packageName${m.aliasName}")
            val newState = if (m == mode) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            try {
                pm.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                AmnosLog.e("CamouflageManager", "Failed to shift identity component: ${m.aliasName}", e)
            }
        }
        
        AmnosLog.i("CamouflageManager", "Identity Shift Complete. Change may take a few seconds to reflect in Launcher.")
    }
}
