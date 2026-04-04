package com.fongmi.android.tv.api.config;
import com.github.catvod.utils.Logger;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.Decoder;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Rule;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VodConfig {

    private List<Doh> doh;
    private List<Rule> rules;
    private List<Site> sites;
    private List<Parse> parses;
    private List<String> flags;
    private List<String> ads;
    private boolean loadLive;
    private Config config;
    private Parse parse;
    private String wall;
    private Site home;
    private volatile boolean isLoading = false; // 添加加载状态标记

    private VodConfig() {
        // 在构造函数中初始化列表，防止空指针异常
        this.ads = new ArrayList<>();
        this.doh = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.parses = new ArrayList<>();
    }

    private static class Loader {
        static volatile VodConfig INSTANCE = new VodConfig();
    }

    public static VodConfig get() {
        return Loader.INSTANCE;
    }

    public static int getCid() {
        return get().getConfig().getId();
    }

    public static String getUrl() {
        return get().getConfig().getUrl();
    }

    public static String getDesc() {
        return get().getConfig().getDesc();
    }

    public static int getHomeIndex() {
        return get().getSites().indexOf(get().getHome());
    }

    public static boolean hasParse() {
        return !get().getParses().isEmpty();
    }

    public static void load(Config config, Callback callback) {
        android.util.Log.d("VodConfig", "load called with config: " + (config != null ? config.toString() : "null"));
        
        // 参数检查
        if (config == null || callback == null) {
            android.util.Log.e("VodConfig", "Invalid parameters: config=" + config + ", callback=" + callback);
            if (callback != null) {
                App.post(() -> callback.error("配置参数无效"));
            }
            return;
        }
        
        android.util.Log.d("VodConfig", "Parameters valid, proceeding with load");
        
        // 添加加载状态检查，防止并发加载
        VodConfig instance = get();
        synchronized (instance) {
            if (instance.isLoading) {
                android.util.Log.d("VodConfig", "Already loading, cancelling previous load");
                // 如果正在加载，取消之前的加载
                try {
                    OkHttp.cancel("vod");
                } catch (Exception e) {
                    android.util.Log.e("VodConfig", "Error cancelling previous load", e);
                    Logger.e("Error", e);
                }
            }
            instance.isLoading = true;
        }
        
        android.util.Log.d("VodConfig", "Calling instance.clear().config(config).load(callback)");
        try {
            instance.clear().config(config).load(callback);
        } catch (Exception e) {
            instance.isLoading = false;
            android.util.Log.e("VodConfig", "Exception during load", e);
            Logger.e("Error", e);
            App.post(() -> callback.error("配置加载失败: " + e.getMessage()));
        }
    }

    public VodConfig init() {
        this.wall = null;
        this.home = null;
        this.parse = null;
        this.config = Config.vod();
        this.ads = new ArrayList<>();
        this.doh = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.loadLive = false;
        return this;
    }

    public VodConfig config(Config config) {
        this.config = config;
        return this;
    }

    public VodConfig clear() {
        this.wall = null;
        this.home = null;
        this.parse = null;
        if (this.ads != null) this.ads.clear();
        if (this.doh != null) this.doh.clear();
        if (this.rules != null) this.rules.clear();
        if (this.sites != null) this.sites.clear();
        if (this.flags != null) this.flags.clear();
        if (this.parses != null) this.parses.clear();
        this.loadLive = true;
        BaseLoader.get().clear();
        return this;
    }

    public void load(Callback callback) {
        App.execute(() -> loadConfig(callback));
    }

    private void loadConfig(Callback callback) {
        try {
            OkHttp.cancel("vod");
            checkJson(Json.parse(Decoder.getJson(UrlUtil.convert(config.getUrl()), "vod")).getAsJsonObject(), callback);
        } catch (Throwable e) {
            if (TextUtils.isEmpty(config.getUrl())) {
                isLoading = false;
                App.post(() -> callback.error(""));
            } else {
                loadCache(callback, e);
            }
            Logger.e("Error", e);
        }
    }

    private void loadCache(Callback callback, Throwable e) {
        if (!TextUtils.isEmpty(config.getJson())) {
            checkJson(Json.parse(config.getJson()).getAsJsonObject(), callback);
        } else {
            isLoading = false;
            App.post(() -> callback.error(Notify.getError(R.string.error_config_get, e)));
        }
    }

    private void checkJson(JsonObject object, Callback callback) {
        if (object.has("msg")) {
            App.post(() -> callback.error(object.get("msg").getAsString()));
        } else if (object.has("urls")) {
            parseDepot(object, callback);
        } else {
            parseConfig(object, callback);
        }
    }

    private void parseDepot(JsonObject object, Callback callback) {
        List<Depot> items = Depot.arrayFrom(object.getAsJsonArray("urls").toString());
        List<Config> configs = new ArrayList<>();
        for (Depot item : items) configs.add(Config.find(item, 0));
        Config.delete(config.getUrl());
        config = configs.get(0);
        loadConfig(callback);
    }

    private void parseConfig(JsonObject object, Callback callback) {
        try {
            initSite(object);
            initParse(object);
            initOther(object);
            if (loadLive && object.has("lives")) initLive(object);
            String notice = Json.safeString(object, "notice");
            config.logo(Json.safeString(object, "logo"));
            config.json(object.toString()).update();
            
            // 重置加载状态
            isLoading = false;
            
            // 只调用一次success回调，优先显示通知消息
            if (!TextUtils.isEmpty(notice)) {
                App.post(() -> callback.success(notice));
            } else {
                App.post(callback::success);
            }
        } catch (Throwable e) {
            Logger.e("Error", e);
            // 重置加载状态
            isLoading = false;
            App.post(() -> callback.error(Notify.getError(R.string.error_config_parse, e)));
        }
    }

    private void initSite(JsonObject object) {
        if (object.has("video")) {
            initSite(object.getAsJsonObject("video"));
            return;
        }
        String spider = Json.safeString(object, "spider");
        try {
            BaseLoader.get().parseJar(spider, true);
        } catch (Throwable e) {
            android.util.Log.e("VodConfig", "Failed to parse spider jar: " + spider, e);
            Logger.e("Error", e);
        }
        
        for (JsonElement element : Json.safeListElement(object, "sites")) {
            try {
                Site site = Site.objectFrom(element);
                if (sites.contains(site)) continue;
                site.setApi(UrlUtil.convert(site.getApi()));
                site.setExt(UrlUtil.convert(site.getExt()));
                site.setJar(parseJar(site, spider));
                sites.add(site.trans().sync());
            } catch (Throwable e) {
                android.util.Log.e("VodConfig", "Failed to add site: " + element, e);
                Logger.e("Error", e);
                // 继续处理下一个站点
            }
        }
        
        // 优先使用配置中指定的 home 站点
        boolean homeSet = false;
        String configHome = config.getHome();
        if (!TextUtils.isEmpty(configHome)) {
            for (Site site : sites) {
                if (site.getKey().equals(configHome)) {
                    setHome(site);
                    homeSet = true;
                    break;
                }
            }
        }
        
        // 如果配置的 home 站点无效，使用第一个可用站点
        if (!homeSet && !sites.isEmpty()) {
            setHome(sites.get(0));
        }
    }

    private void initLive(JsonObject object) {
        Config temp = Config.find(config, 1).save();
        boolean sync = LiveConfig.get().needSync(config.getUrl());
        if (sync) LiveConfig.get().clear().config(temp).parse(object);
    }

    private void initParse(JsonObject object) {
        for (JsonElement element : Json.safeListElement(object, "parses")) {
            Parse parse = Parse.objectFrom(element);
            if (parse.getName().equals(config.getParse()) && parse.getType() > 1) setParse(parse);
            if (!parses.contains(parse)) parses.add(parse);
        }
    }

    private void initOther(JsonObject object) {
        if (!parses.isEmpty()) parses.add(0, Parse.god());
        if (home == null) setHome(sites.isEmpty() ? new Site() : sites.get(0));
        if (parse == null) setParse(parses.isEmpty() ? new Parse() : parses.get(0));
        setRules(Rule.arrayFrom(object.getAsJsonArray("rules")));
        setDoh(Doh.arrayFrom(object.getAsJsonArray("doh")));
        setHeaders(Json.safeListElement(object, "headers"));
        setFlags(Json.safeListString(object, "flags"));
        setHosts(Json.safeListString(object, "hosts"));
        setProxy(Json.safeListString(object, "proxy"));
        setWall(Json.safeString(object, "wallpaper"));
        setAds(Json.safeListString(object, "ads"));
    }

    private String parseJar(Site site, String spider) {
        return site.getJar().isEmpty() ? spider : site.getJar();
    }

    public List<Doh> getDoh() {
        List<Doh> items = Doh.get(App.get());
        if (doh == null) return items;
        items.removeAll(doh);
        items.addAll(doh);
        return items;
    }

    public void setDoh(List<Doh> doh) {
        this.doh = doh;
    }

    public List<Rule> getRules() {
        return rules == null ? Collections.emptyList() : rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public List<Site> getSites() {
        return sites == null ? Collections.emptyList() : sites;
    }

    public List<Parse> getParses() {
        return parses == null ? Collections.emptyList() : parses;
    }

    public List<Parse> getParses(int type) {
        List<Parse> items = new ArrayList<>();
        for (Parse item : getParses()) if (item.getType() == type) items.add(item);
        return items;
    }

    public List<Parse> getParses(int type, String flag) {
        List<Parse> items = new ArrayList<>();
        for (Parse item : getParses(type)) if (item.getExt().getFlag().contains(flag)) items.add(item);
        if (items.isEmpty()) items.addAll(getParses(type));
        return items;
    }

    public void setHeaders(List<JsonElement> items) {
        OkHttp.responseInterceptor().setHeaders(items);
    }

    public List<String> getFlags() {
        return flags == null ? Collections.emptyList() : flags;
    }

    private void setFlags(List<String> flags) {
        this.flags.addAll(flags);
    }

    public void setHosts(List<String> hosts) {
        OkHttp.dns().addAll(hosts);
    }

    public void setProxy(List<String> hosts) {
        OkHttp.selector().addAll(hosts);
    }

    public List<String> getAds() {
        return ads == null ? Collections.emptyList() : ads;
    }

    private void setAds(List<String> ads) {
        this.ads = ads;
    }

    public Config getConfig() {
        return config == null ? Config.vod() : config;
    }

    public Parse getParse() {
        return parse == null ? new Parse() : parse;
    }

    public Site getHome() {
        return home == null ? new Site() : home;
    }

    public String getWall() {
        return TextUtils.isEmpty(wall) ? "" : wall;
    }

    public Parse getParse(String name) {
        int index = getParses().indexOf(Parse.get(name));
        return index == -1 ? null : getParses().get(index);
    }

    public Site getSite(String key) {
        int index = getSites().indexOf(Site.get(key));
        return index == -1 ? new Site() : getSites().get(index);
    }

    public void setParse(Parse parse) {
        this.parse = parse;
        this.parse.setActivated(true);
        config.parse(parse.getName()).save();
        for (Parse item : getParses()) item.setActivated(parse);
    }

    public void setHome(Site home) {
        if (home == null) {
            // 如果传入null，使用默认站点或创建空站点
            home = sites.isEmpty() ? new Site() : sites.get(0);
        }
        this.home = home;
        this.home.setActivated(true);
        
        // 安全地保存配置，防止空指针异常
        try {
            if (home.getKey() != null && config != null) {
                config.home(home.getKey()).save();
            }
        } catch (Exception e) {
            Logger.e("Error", e);
        }
        
        // 安全地更新所有站点的激活状态
        try {
            for (Site item : getSites()) {
                if (item != null) {
                    item.setActivated(home);
                }
            }
        } catch (Exception e) {
            Logger.e("Error", e);
        }
    }

    private void setWall(String wall) {
        this.wall = wall;
        boolean load = !TextUtils.isEmpty(wall) && WallConfig.get().needSync(wall);
        if (load) WallConfig.get().config(Config.find(wall, config.getName(), 2).update());
    }
}
