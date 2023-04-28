package com.chenyue404.intentresolverhook

import android.content.Intent
import android.os.Build
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Created by chenyue on 2022/9/27 0027.
 */
class MainHook : IXposedHookLoadPackage {

    private val PACKAGE_NAME = "android"
    private val TAG = "intentresolver-hook-"

    private fun log(str: String) {
        XposedBridge.log("$TAG$str")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        val packageName = lpparam.packageName
        val classLoader = lpparam.classLoader

        if (packageName != PACKAGE_NAME) {
            return
        }

        log("")

        val androidVersion = Build.VERSION.SDK_INT
        var paramIndex = 0

        val hook = object : XC_MethodHook() {
            var before = ""
            override fun beforeHookedMethod(param: MethodHookParam) {
                val intent = param.args[paramIndex] as Intent
                val resolvedType = param.args[paramIndex + 1]
                val defaultOnly = param.args[paramIndex + 2] as Boolean
                val userId = param.args[paramIndex + 3] as Int

//                if (intent.action != "android.intent.action.INSTALL_PACKAGE") {
//                    return
//                }
//                param.args[paramIndex + 2] = false

//                log("resolvedType=$resolvedType, defaultOnly=$defaultOnly, userId=$userId\nintent=${intent.transToStr()}")
                before =
                    "resolvedType=$resolvedType, defaultOnly=$defaultOnly, userId=$userId\nintent=${intent.transToStr()}"
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.result == null) {
                    log("null")
                    return
                }
                val intent = param.args[paramIndex] as Intent
//                if (intent.action != "android.intent.action.INSTALL_PACKAGE") {
//                    return
//                }
                val list = param.result as List<*>
                val str = list.joinToString { it.toString() }
//                log("list.size=${list.size}, list=$str")
                if (list.isNotEmpty()) {
                    log("$before\nlist.size=${list.size}")
                }
            }
        }

        when (androidVersion) {
            in 0..Build.VERSION_CODES.S_V2 -> {
                findAndHookMethod(
                    "com.android.server.IntentResolver", classLoader,
                    "queryIntent",
                    Intent::class.java,
                    String::class.java,//resolvedType
                    Boolean::class.java,//defaultOnly
                    Int::class.java,//userId
                    hook
                )
            }

            else -> {
                paramIndex = 1
                findAndHookMethod(
                    "com.android.server.IntentResolver", classLoader,
                    "queryIntent",
                    XposedHelpers.findClass(
                        "com.android.server.pm.snapshot.PackageDataSnapshot",
                        classLoader
                    ),
                    Intent::class.java,
                    String::class.java,//resolvedType
                    Boolean::class.java,//defaultOnly
                    Int::class.java,//userId
                    Long::class.java,//customFlags
                    hook
                )
            }
        }
    }
}