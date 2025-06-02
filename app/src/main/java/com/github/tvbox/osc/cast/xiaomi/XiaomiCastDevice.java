package com.github.tvbox.osc.cast.xiaomi;

/**
 * 小米投屏设备类
 */
public class XiaomiCastDevice {
    private String id;
    private String name;
    private String ip;

    public XiaomiCastDevice(String id, String name, String ip) {
        this.id = id;
        this.name = name;
        this.ip = ip;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }
}
