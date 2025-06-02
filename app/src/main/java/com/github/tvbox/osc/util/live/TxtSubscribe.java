package com.github.tvbox.osc.util.live;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxtSubscribe {
    private static final String TAG = "TxtSubscribe";
    private static final Pattern EXTINF_PATTERN = Pattern.compile("#EXTINF:(.+),(.+)");
    private static final Pattern GROUP_TITLE_PATTERN = Pattern.compile("group-title=\"([^\"]+)\"");

    public static void parse(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        if (TextUtils.isEmpty(str)) return;

        // 检测直播源格式
        boolean isM3U = str.trim().startsWith("#EXTM3U");

        if (isM3U) {
            parseM3U(linkedHashMap, str);
        } else {
            parseTxt(linkedHashMap, str);
        }
    }

    /**
     * 解析传统TXT格式直播源
     */
    private static void parseTxt(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        ArrayList<String> arrayList;
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            String readLine = bufferedReader.readLine();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = new LinkedHashMap<>();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap3 = linkedHashMap2;
            while (readLine != null) {
                if (readLine.trim().isEmpty()) {
                    readLine = bufferedReader.readLine();
                } else {
                    String[] split = readLine.split(",");
                    if (split.length < 2) {
                        readLine = bufferedReader.readLine();
                    } else {
                        if (readLine.contains("#genre#")) {
                            String trim = split[0].trim();
                            if (!linkedHashMap.containsKey(trim)) {
                                linkedHashMap3 = new LinkedHashMap<>();
                                linkedHashMap.put(trim, linkedHashMap3);
                            } else {
                                linkedHashMap3 = linkedHashMap.get(trim);
                            }
                        } else {
                            String trim2 = split[0].trim();
                            for (String str2 : split[1].trim().split("#")) {
                                String trim3 = str2.trim();
                                if (!trim3.isEmpty() && (trim3.startsWith("http") || trim3.startsWith("rtp") || trim3.startsWith("rtsp") || trim3.startsWith("rtmp"))) {
                                    if (!linkedHashMap3.containsKey(trim2)) {
                                        arrayList = new ArrayList<>();
                                        linkedHashMap3.put(trim2, arrayList);
                                    } else {
                                        arrayList = linkedHashMap3.get(trim2);
                                    }
                                    if (!arrayList.contains(trim3)) {
                                        arrayList.add(trim3);
                                    }
                                }
                            }
                        }
                        readLine = bufferedReader.readLine();
                    }
                }
            }
            bufferedReader.close();
            if (linkedHashMap2.isEmpty()) {
                return;
            }
            linkedHashMap.put("未分组", linkedHashMap2);
        } catch (Throwable unused) {
            Log.e(TAG, "解析TXT格式直播源出错", unused);
        }
    }

    /**
     * 解析M3U格式直播源
     */
    private static void parseM3U(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(str));
            String line;
            String currentGroupTitle = "默认分组";
            String currentChannelName = "";
            LinkedHashMap<String, ArrayList<String>> currentGroup = null;

            // 确保默认分组存在
            if (!linkedHashMap.containsKey(currentGroupTitle)) {
                currentGroup = new LinkedHashMap<>();
                linkedHashMap.put(currentGroupTitle, currentGroup);
            } else {
                currentGroup = linkedHashMap.get(currentGroupTitle);
            }

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                if (line.startsWith("#EXTINF:")) {
                    // 解析频道信息
                    Matcher extinfMatcher = EXTINF_PATTERN.matcher(line);
                    if (extinfMatcher.find()) {
                        currentChannelName = extinfMatcher.group(2).trim();
                    }

                    // 解析分组信息
                    Matcher groupMatcher = GROUP_TITLE_PATTERN.matcher(line);
                    if (groupMatcher.find()) {
                        String groupTitle = groupMatcher.group(1).trim();
                        if (!TextUtils.isEmpty(groupTitle)) {
                            currentGroupTitle = groupTitle;
                            if (!linkedHashMap.containsKey(currentGroupTitle)) {
                                currentGroup = new LinkedHashMap<>();
                                linkedHashMap.put(currentGroupTitle, currentGroup);
                            } else {
                                currentGroup = linkedHashMap.get(currentGroupTitle);
                            }
                        }
                    }
                } else if (!line.startsWith("#") && !TextUtils.isEmpty(line)) {
                    // 这是一个URL行
                    if (TextUtils.isEmpty(currentChannelName)) {
                        currentChannelName = "未命名频道";
                    }

                    if (line.startsWith("http") || line.startsWith("rtp") || line.startsWith("rtsp") || line.startsWith("rtmp")) {
                        ArrayList<String> urls = currentGroup.get(currentChannelName);
                        if (urls == null) {
                            urls = new ArrayList<>();
                            currentGroup.put(currentChannelName, urls);
                        }
                        if (!urls.contains(line)) {
                            urls.add(line);
                        }

                        // 重置频道名，为下一个频道做准备
                        currentChannelName = "";
                    }
                }
            }
            reader.close();

            // 移除空分组
            ArrayList<String> keysToRemove = new ArrayList<>();
            for (String key : linkedHashMap.keySet()) {
                if (linkedHashMap.get(key).isEmpty()) {
                    keysToRemove.add(key);
                }
            }
            for (String key : keysToRemove) {
                linkedHashMap.remove(key);
            }

            // 如果没有任何分组，添加一个空的默认分组
            if (linkedHashMap.isEmpty()) {
                linkedHashMap.put("默认分组", new LinkedHashMap<>());
            }
        } catch (Throwable e) {
            Log.e(TAG, "解析M3U格式直播源出错", e);
        }
    }

    public static JsonArray live2JsonArray(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap) {
        JsonArray jsonarr = new JsonArray();
        for (String str : linkedHashMap.keySet()) {
            JsonArray jsonarr2 = new JsonArray();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = linkedHashMap.get(str);
            if (!linkedHashMap2.isEmpty()) {
                for (String str2 : linkedHashMap2.keySet()) {
                    ArrayList<String> arrayList = linkedHashMap2.get(str2);
                    if (!arrayList.isEmpty()) {
                        JsonArray jsonarr3 = new JsonArray();
                        for (int i = 0; i < arrayList.size(); i++) {
                            jsonarr3.add(arrayList.get(i));
                        }
                        JsonObject jsonobj = new JsonObject();
                        try {
                            jsonobj.addProperty("name", str2);
                            jsonobj.add("urls", jsonarr3);
                        } catch (Throwable e) {
                        }
                        jsonarr2.add(jsonobj);
                    }
                }
                JsonObject jsonobj2 = new JsonObject();
                try {
                    jsonobj2.addProperty("group", str);
                    jsonobj2.add("channels", jsonarr2);
                } catch (Throwable e) {
                }
                jsonarr.add(jsonobj2);
            }
        }
        return jsonarr;
    }
}
