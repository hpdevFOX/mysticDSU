package com.mysticgsi.dsu.brickfactory

import java.io.BufferedReader
import java.io.InputStreamReader

class GSITool {
    fun wipe() {
        Runtime.getRuntime().exec("su -c gsi_tool wipe").waitFor()
    }

    fun wipeData() {
        Runtime.getRuntime().exec("su -c gsi_tool wipe-data")
    }

    fun rebootIntoDSU() {
        Runtime.getRuntime().exec("su -c am start-service -a com.android.dynsystem.ACTION_REBOOT_TO_DYN_SYSTEM -n com.android.dynsystem/.DynamicSystemInstallationService")
    }

    fun abortInstall() {
        Runtime.getRuntime().exec("su -c am start-service -a com.android.dynsystem.ACTION_DISCARD_INSTALL -n com.android.dynsystem/.DynamicSystemInstallationService")
        Runtime.getRuntime().exec("su -c am start-service -a com.android.dynsystem.ACTION_CANCEL_INSTALL -n com.android.dynsystem/.DynamicSystemInstallationService")
        wipe()
    }

    fun getStatus(): Pair<Boolean, Boolean> {
        val process = Runtime.getRuntime().exec("su -c gsi_tool status")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readLines()
        val isInstalled = output.contains("installed")
        val isEnabled = !output.contains("disabled") && isInstalled
        return Pair(isInstalled, isEnabled)
    }
}