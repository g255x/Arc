package dev.kissoo.auth;

/**
 * 授权闸门 —— 已注释认证，所有检查直接通过。
 */
public final class AuthGate {
    private AuthGate() {}

    public static boolean isTokenValid()       { return true; }
    public static boolean isHwidValid()        { return true; }
    public static boolean allowBootstrap()     { return true; }
    public static boolean allowModuleActivation() { return true; }
    public static String  moduleDenialReason() { return ""; }
    public static boolean allowConfig()        { return true; }
    public static boolean allowEvents()        { return true; }
    public static boolean allowModules()       { return true; }
    public static boolean allowCommands()      { return true; }
    public static boolean allowHud()           { return true; }
    public static boolean allowClickGui()      { return true; }
    public static boolean allowMixinHooks()    { return true; }
}