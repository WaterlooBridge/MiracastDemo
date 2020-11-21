package com.zhenl.miracastdemo

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedHook : IXposedHookLoadPackage {

    private var packageUid = -1

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName != "android")
            return
        XposedHelpers.findAndHookMethod("com.android.server.pm.PackageManagerService",
            lpparam.classLoader, "checkUidPermission", String::class.java, Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    if (packageUid == -1)
                        packageUid = XposedHelpers.callMethod(
                            param?.thisObject, "getPackageUid", BuildConfig.APPLICATION_ID, 0, 0
                        ) as Int
                    if (packageUid == param?.args?.get(1))
                        param.result = 0
                }
            })
    }
}