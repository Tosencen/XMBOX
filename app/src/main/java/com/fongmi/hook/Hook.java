package com.fongmi.hook;

public class Hook {
    private final String sign;
    private final String pkg;

    public Hook(String sign, String pkg) {
        this.sign = sign;
        this.pkg = pkg;
    }

    public String getPackageName() { return pkg; }
    public String getSign() { return sign; }
}
