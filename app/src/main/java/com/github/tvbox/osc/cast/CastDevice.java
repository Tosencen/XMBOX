package com.github.tvbox.osc.cast;

/**
 * 投屏设备类 - 统一不同投屏方式的设备信息
 */
public class CastDevice {
    public static final int TYPE_DLNA = 1;
    public static final int TYPE_XIAOMI = 2;

    private String id;
    private String name;
    private String ip;
    private int type;
    private Object originalDevice; // 原始设备对象，用于特定投屏方式的操作

    public CastDevice(String id, String name, String ip, int type, Object originalDevice) {
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.type = type;
        this.originalDevice = originalDevice;
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

    public int getType() {
        return type;
    }

    public Object getOriginalDevice() {
        return originalDevice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CastDevice that = (CastDevice) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * 获取设备类型名称
     */
    public String getTypeName() {
        switch (type) {
            case TYPE_DLNA:
                return "DLNA";
            case TYPE_XIAOMI:
                return "小米投屏";
            default:
                return "未知";
        }
    }
}
