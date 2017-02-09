package com.jecelyin.buildprop;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

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
    private boolean enableDebugWebview;
    private boolean debug;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(BuildPropEditor.class.getPackage().getName());
        prefs.makeWorldReadable();
        reloadPreferences();
        debug = prefs.getBoolean(Common.PREF_ENABLE_DEBUG, true);
        boolean enableViewServer = prefs.getBoolean(Common.PREF_ENABLE_VIEW_SERVER, false);
        roDebuggable = prefs.getString(Common.PREF_RO_DEBUGGABLE, "-1");
        enableDebugWebview = prefs.getBoolean(Common.PREF_ENABLE_DEBUG_WEBVIEW, true);

        roSecure = prefs.getString(Common.PREF_RO_SECURE, "-1");
        if (enableViewServer)
            roSecure = "0"; //关闭ro.secure才能开启viewServer

        XposedBridge.log("debug=" + debug + " enableViewServer=" + enableViewServer + " roDebuggable=" + roDebuggable + " roSecure=" + roSecure);
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam)
            throws Throwable {
        if (debug) {
            XposedBridge.log("handleLoadPackage: " + lpparam.packageName + " lpparam.processName:" + lpparam.processName);
        }

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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && enableDebugWebview) {
            // hook webview.loadUrl for all process to enable remote debug
            XC_MethodHook enableRemoteDebug = new XC_MethodHook() {
                @TargetApi(Build.VERSION_CODES.KITKAT)
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    WebView.setWebContentsDebuggingEnabled(true);
                }
            };

            Object[] loadData = new Object[]{
                    "java.lang.String",
                    "java.lang.String",
                    "java.lang.String",
                    enableRemoteDebug
            };

            Object[] loadDataWithBaseURL = new Object[]{
                    "java.lang.String",
                    "java.lang.String",
                    "java.lang.String",
                    "java.lang.String",
                    "java.lang.String",
                    enableRemoteDebug
            };

            Object[] loadUrl = new Object[]{
                    "java.lang.String",
                    enableRemoteDebug
            };

            Object[] loadUrlWithHeaders = new Object[]{
                    "java.lang.String",
                    "java.util.Map",
                    enableRemoteDebug
            };


            String webviewClzName = "android.webkit.WebView";
            ClassLoader classLoader = lpparam.classLoader;

            // since my device has an ancient version of xposed installed
            try {
                XposedHelpers.findAndHookMethod(webviewClzName, classLoader, "loadData", loadData);
                XposedHelpers.findAndHookMethod(webviewClzName, classLoader, "loadDataWithBaseURL", loadDataWithBaseURL);
                XposedHelpers.findAndHookMethod(webviewClzName, classLoader, "loadUrl", loadUrl);
                XposedHelpers.findAndHookMethod(webviewClzName, classLoader, "loadUrl", loadUrlWithHeaders);
            } catch (Throwable e) {
                XposedBridge.log("webviewenabled:: err -> " + Log.getStackTraceString(e));
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