package com.tvbus.engine;

public interface Listener {
    void onPrepared(String result);
    void onStop(String result);
    void onInited(String result);
    void onStart(String result);
    void onInfo(String result);
    void onQuit(String result);
}
