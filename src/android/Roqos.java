package com.roqos.cordova.plugin;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Roqos extends Application {
    private static final String SHORTCUT_ID_ACTIVATE = "shortcut_activate";

    public static final List<Rule> RULES = new ArrayList<Rule>() {{
        //Build-in Hosts rule providers
        add(new Rule("googlehosts/hosts", "googlehosts.hosts", Rule.TYPE_HOSTS,
                "https://raw.githubusercontent.com/googlehosts/hosts/master/hosts-files/hosts", false));
        add(new Rule("vokins/yhosts", "vokins.hosts", Rule.TYPE_HOSTS,
                "https://raw.githubusercontent.com/vokins/yhosts/master/hosts", false));
        add(new Rule("adaway", "adaway.hosts", Rule.TYPE_HOSTS,
                "https://adaway.org/hosts.txt", false));
        //Build-in DNSMasq rule providers
        add(new Rule("vokins/yhosts/union", "union.dnsmasq", Rule.TYPE_DNAMASQ,
                "https://raw.githubusercontent.com/vokins/yhosts/master/dnsmasq/union.conf", false));
    }};

    public static final String[] DEFAULT_TEST_DOMAINS = new String[]{
            "google.com",
            "twitter.com",
            "youtube.com",
            "facebook.com",
            "wikipedia.org"
    };

    public static Configurations configurations;

    public static String rulePath = null;
    public static String logPath = null;
    private static String configPath = null;

    public static String MacAddress = "02:00:00:00:00:00";
    public static String dnsServer = "18.217.143.81";
    public static int port = 53;
    public static String VPNSession = "Roqos";
    public static String AccountId = "";

    public static ArrayList<String> optionsCode = new ArrayList<String>();
    public static ArrayList<String> ednsMessage = new ArrayList<String>();

    public static List<DNSServer> DNS_SERVERS = new ArrayList<DNSServer>() {{
        add(new DNSServer(getDNSServer(), 0, getPort()));
    }};

    private static Roqos instance = null;
    private SharedPreferences prefs;
    private Thread mResolver;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

//        Logger.init();

        mResolver = new Thread(new RuleResolver());
        mResolver.start();

//        initData();
    }

    public static void addEDNSOption(String optionCode, String message){
        optionsCode.add(optionCode);
        ednsMessage.add(message);
    }

    public static void config(String dServer, String portNum, String VPNSessionTitle){
        dnsServer = dServer;
        port = Integer.parseInt(portNum);
        VPNSession = VPNSessionTitle;
    }

    private void initDirectory(String dir) {
        File directory = new File(dir);
        if (!directory.isDirectory()) {
//            Logger.warning(dir + " is not a directory. Delete result: " + String.valueOf(directory.delete()));
        }
        if (!directory.exists()) {
//            Logger.debug(dir + " does not exist. Create result: " + String.valueOf(directory.mkdirs()));
        }
    }

//    private void initData() {
//        PreferenceManager.setDefaultValues(this, R.xml.perf_settings, false);
//        prefs = PreferenceManager.getDefaultSharedPreferences(this);
//
//        if (getExternalFilesDir(null) != null) {
//            rulePath = getExternalFilesDir(null).getPath() + "/rules/";
//            logPath = getExternalFilesDir(null).getPath() + "/logs/";
//            configPath = getExternalFilesDir(null).getPath() + "/config.json";
//
//            initDirectory(rulePath);
//            initDirectory(logPath);
//        }
//
//        if (configPath != null) {
//            configurations = Configurations.load(new File(configPath));
//        } else {
//            configurations = new Configurations();
//        }
//    }

    public static <T> T parseJson(Class<T> beanClass, JsonReader reader) throws JsonParseException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(reader, beanClass);
    }

    public static void initRuleResolver() {
        if (Roqos.getPrefs().getBoolean("settings_local_rules_resolution", false)) {
            ArrayList<String> pendingLoad = new ArrayList<String>();
            ArrayList<Rule> usingRules = configurations.getUsingRules();
            if (usingRules != null && usingRules.size() > 0) {
                for (Rule rule : usingRules) {
                    if (rule.isUsing()) {
                        pendingLoad.add(rulePath + rule.getFileName());
                    }
                }
                if (pendingLoad.size() > 0) {
                    String[] arr = new String[pendingLoad.size()];
                    pendingLoad.toArray(arr);
                    switch (usingRules.get(0).getType()) {
                        case Rule.TYPE_HOSTS:
                            RuleResolver.startLoadHosts(arr);
                            break;
                        case Rule.TYPE_DNAMASQ:
                            RuleResolver.startLoadDnsmasq(arr);
                            break;
                    }
                } else {
                    RuleResolver.clear();
                }
            } else {
                RuleResolver.clear();
            }
        }
    }

    public static int getPort(){
        return port;
    }

    public static String getDNSServer(){
        return dnsServer;
    }

    public static String getMacAddress(){
        return MacAddress;
    }

    public static void setMacAddress(String str){
        MacAddress = str;
    }

    public static String getAccountId(){
        return AccountId;
    }

    public static void setAccountId(String str){
        AccountId = str;
    }

    public static void setRulesChanged() {
        if (RoqosVPNService.isActivated() &&
                getPrefs().getBoolean("settings_allow_dynamic_rule_reload", false)) {
            initRuleResolver();
        }
    }

    public static SharedPreferences getPrefs() {
        return getInstance().prefs;
    }

//    public static boolean isDarkTheme() {
//        return getInstance().prefs.getBoolean("settings_dark_theme", false);
//    }

    @Override
    public void onTerminate() {
        Log.d("Roqos", "onTerminate");
        super.onTerminate();

        instance = null;
        prefs = null;
        RuleResolver.shutdown();
        mResolver.interrupt();
        RuleResolver.clear();
        mResolver = null;
//        Logger.shutdown();
    }

    public static Intent getServiceIntent(Context context) {
        return new Intent(context, RoqosVPNService.class);
    }

    public static boolean switchService() {
        if (RoqosVPNService.isActivated()) {
            deactivateService(instance);
            return false;
        } else {
            activateService(instance);
            return true;
        }
    }

    public static boolean activateService(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent != null) {
            return false;
        } else {
//            RoqosVPNService.primaryServer = DNSServerHelper.getAddressById(DNSServerHelper.getPrimary());
//            RoqosVPNService.secondaryServer = DNSServerHelper.getAddressById(DNSServerHelper.getSecondary());
            context.startService(Roqos.getServiceIntent(context).setAction(RoqosVPNService.ACTION_ACTIVATE));
            return true;
        }
    }

    public static void deactivateService(Context context) {
        context.startService(getServiceIntent(context).setAction(RoqosVPNService.ACTION_DEACTIVATE));
        context.stopService(getServiceIntent(context));
    }

//    public static void updateShortcut(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
//            Log.d("Roqos", "Updating shortcut");
//            boolean activate = RoqosVPNService.isActivated();
//            String notice = activate ? context.getString(R.string.button_text_deactivate) : context.getString(R.string.button_text_activate);
//            ShortcutInfo info = new ShortcutInfo.Builder(context, Roqos.SHORTCUT_ID_ACTIVATE)
//                    .setLongLabel(notice)
//                    .setShortLabel(notice)
//                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
//                    .setIntent(new Intent(context, MainActivity.class).setAction(Intent.ACTION_VIEW)
//                            .putExtra(MainActivity.LAUNCH_ACTION, activate ? MainActivity.LAUNCH_ACTION_DEACTIVATE : MainActivity.LAUNCH_ACTION_ACTIVATE))
//                    .build();
//            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(SHORTCUT_SERVICE);
//            shortcutManager.addDynamicShortcuts(Collections.singletonList(info));
//        }
//    }


    public static Roqos getInstance() {
        return instance;
    }
}
