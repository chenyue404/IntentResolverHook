package com.chenyue404.intentresolverhook

import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
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

        findAndHookMethod(
            "com.android.server.IntentResolver", classLoader,
            "queryIntent",
            Intent::class.java,
            String::class.java,//resolvedType
            Boolean::class.java,//defaultOnly
            Int::class.java,//userId
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args[0] as Intent
                    val resolvedType = param.args[1]
                    val defaultOnly = param.args[2] as Boolean
                    val userId = param.args[3] as Int

                    if (intent.action != "android.intent.action.INSTALL_PACKAGE") {
                        return
                    }
                    param.args[2] = false

                    log("intent=${intent.transToStr()}")
                    log("resolvedType=$resolvedType, defaultOnly=$defaultOnly, userId=$userId")
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.result == null) {
                        log("null")
                        return
                    }
                    val intent = param.args[0] as Intent
                    if (intent.action != "android.intent.action.INSTALL_PACKAGE") {
                        return
                    }
                    val list = param.result as List<*>
                    val str = list.joinToString { it.toString() }
                    log("list.size=${list.size}, list=$str")
                }
            }
        )
    }
}