package com.tvbus.engine;

public class TVCore {
    public TVCore(String path) {}
    public TVCore listener(Listener l) { return this; }
    public TVCore auth(String s) { return this; }
    public TVCore name(String s) { return this; }
    public TVCore pass(String s) { return this; }
    public TVCore domain(String s) { return this; }
    public TVCore broker(String s) { return this; }
    public TVCore serv(int i) { return this; }
    public TVCore play(int i) { return this; }
    public TVCore mode(int i) { return this; }
    public TVCore init() { return this; }
    public void start(String url) {}
    public void stop() {}
    public void quit() {}
}
