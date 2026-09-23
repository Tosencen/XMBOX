package com.xunlei.downloadlib;

import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;

import java.io.File;

public class XLTaskHelper {
    private static final XLTaskHelper instance = new XLTaskHelper();
    public static XLTaskHelper get() { return instance; }
    public GetTaskId addTorrentTask(File torrent, File saveDir, int index) { return new GetTaskId(); }
    public GetTaskId addThunderTask(String url, File folder) { return new GetTaskId(); }
    public String getLocalUrl(File file) { return ""; }
    public String getLocalUrl(GetTaskId id) { return ""; }
    public void deleteTask(GetTaskId id) {}
    public void release() {}
    public Wrapper getBtSubTaskInfo(GetTaskId id, int index) { return new Wrapper(); }
    public Wrapper getTaskInfo(GetTaskId id) { return new Wrapper(); }
    public GetTaskId parse(String url, File saveDir) { return new GetTaskId(); }
    public TorrentInfo getTorrentInfo(File file) { return new TorrentInfo(); }
    public void stopTask(GetTaskId id) {}

    public static class Wrapper {
        public XLTaskInfo mTaskInfo = new XLTaskInfo();
        public int getTaskStatus() { return mTaskInfo.mTaskStatus; }
    }
}
