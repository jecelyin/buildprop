package com.jecelyin.buildprop;

/**
 * @author Jecelyin
 */
public class Common {
    public static final String PREF_RO_DEBUGGABLE = "ro.debuggable";
    public static final String PREF_RO_SECURE = "ro.secure";
    public static final String PREF_ENABLE_VIEW_SERVER = "enable_view_server";
    public static final String PREF_ENABLE_DEBUG = "enable_debug";
    public static final String PREF_ENABLE_DEBUG_WEBVIEW = "enable_debug_webview";

    public static final String PACKAGE_NAME = Common.class.getPackage().getName();
    public static final String PACKAGE_PREFERENCES = PACKAGE_NAME;

    public static final String ANDROID_PKG = "android";

    public static final int DEBUG_ENABLE_DEBUGGER = 0x1;
    public static final int DEBUG_DISABLE_DEBUGGER = 0x0;

}
