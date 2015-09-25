package com.jecelyin.buildprop;

import android.content.Context;
import android.content.pm.PackageManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * @author Jecelyin
 */
public class BuildPropEditor implements IXposedHookZygoteInit,
        IXposedHookLoadPackage {
    public XSharedPreferences prefs;
    public String roDebuggable;
    public Context mContext;
    private String roSecure;
    private boolean debug;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(BuildPropEditor.class.getPackage().getName());
        prefs.makeWorldReadable();
        reloadPreferences();
        debug = prefs.getBoolean(Common.PREF_ENABLE_DEBUG, true);
        boolean enableViewServer = prefs.getBoolean(Common.PREF_ENABLE_VIEW_SERVER, false);
        roDebuggable = prefs.getString(Common.PREF_RO_DEBUGGABLE, "-1");

        roSecure = prefs.getString(Common.PREF_RO_SECURE, "-1");
        if (enableViewServer)
            roSecure = "0"; //关闭ro.secure才能开启viewServer

        XposedBridge.log("debug=" + debug + " enableViewServer=" + enableViewServer + " roDebuggable=" + roDebuggable + " roSecure=" + roSecure);
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam)
            throws Throwable {
        if (debug)
            XposedBridge.log("handleLoadPackage: " + lpparam.packageName + " lpparam.processName:" + lpparam.processName);
//        if(lpparam.appInfo != null) {
//            XposedBridge.log("flags="+lpparam.appInfo.flags+" ="+(lpparam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)));
//        }
//        if(lpparam.appInfo == null ||
//                (lpparam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) !=0){
//            //系统应用
//        }

        if (Common.ANDROID_PKG.equals(lpparam.packageName) && Common.ANDROID_PKG.equals(lpparam.processName)) {

            if (!roDebuggable.equals("-1")) {
                // 4.0 and newer
                XposedBridge.hookAllMethods(android.os.Process.class, "start", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param)
                            throws Throwable {
                        int id = 5;
                        int flags = (Integer) param.args[id];
                        int oflags = flags;
                        if (roDebuggable.equals("1")) {
                            if ((flags & Common.DEBUG_ENABLE_DEBUGGER) == 0) {
                                flags |= Common.DEBUG_ENABLE_DEBUGGER;
                            }
                        } else {
                            flags = Common.DEBUG_DISABLE_DEBUGGER;
                        }
                        param.args[id] = flags;
                        if (debug)
                            XposedBridge.log("set ro.debuggable=" + flags + " old value=" + oflags);
                    }
                });
            }


            if (!roSecure.equals("-1")) {
                Class<?> wms = XposedHelpers.findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(wms, "isSystemSecure", XC_MethodReplacement.returnConstant(roSecure.equals("1")));
            }

        }


    }

    public void reloadPreferences() {
        prefs.reload();
    }


    public Context getCurrentPackageContext() {
        Context context = null;
        try {
            context = mContext.createPackageContext(Common.PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return context;
    }

}