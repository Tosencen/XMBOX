package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.Vod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 搜索结果优化器
 * 功能：
 * 1. 结果去重 - 根据 vodId 或 vodName + vodYear 去重
 * 2. 相关性排序 - 根据标题与关键词的匹配程度排序
 * 3. 精确匹配优先 - 标题以关键词开头的优先显示
 * 4. 过滤无效结果 - 过滤标题为空或无效的结果
 */
public class SearchResultOptimizer {

    /**
     * 优化搜索结果
     * @param list 原始搜索结果列表
     * @param keyword 搜索关键词
     * @return 优化后的结果列表
     */
    public static List<Vod> optimize(List<Vod> list, String keyword) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 过滤无效结果
        List<Vod> filtered = filterInvalid(list);

        // 2. 去重
        List<Vod> deduplicated = deduplicate(filtered);

        // 3. 相关性排序
        return sortByRelevance(deduplicated, keyword);
    }

    /**
     * 过滤无效结果
     */
    private static List<Vod> filterInvalid(List<Vod> list) {
        List<Vod> result = new ArrayList<>();
        for (Vod vod : list) {
            String name = vod.getVodName();
            // 过滤标题为空、null 或纯空白的结果
            if (TextUtils.isEmpty(name) || name.trim().isEmpty()) {
                continue;
            }
            // 过滤标题太短的结果（小于2个字符）
            if (name.trim().length() < 2) {
                continue;
            }
            // 过滤无 id 的结果（点开后无法拉详情）
            if (TextUtils.isEmpty(vod.getVodId())) {
                continue;
            }
            result.add(vod);
        }
        return result;
    }

    /**
     * 去除重复结果
     * 优先使用 vodId 去重，其次使用 vodName + vodYear 组合
     */
    private static List<Vod> deduplicate(List<Vod> list) {
        List<Vod> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Map<String, Integer> seenNames = new HashMap<>();

        for (Vod vod : list) {
            String id = vod.getVodId();
            String name = vod.getVodName();
            String year = vod.getVodYear();

            // 优先根据 vodId 去重
            if (!TextUtils.isEmpty(id)) {
                if (seenIds.contains(id)) {
                    continue;
                }
                seenIds.add(id);
            } else {
                // 如果没有 vodId，则根据 name + year 组合去重
                String key = (name + "_" + (year != null ? year : "")).toLowerCase().trim();
                if (seenNames.containsKey(key)) {
                    // 保留已有结果，丢弃重复的
                    continue;
                }
                seenNames.put(key, 1);
            }
            result.add(vod);
        }
        return result;
    }

    /**
     * 根据相关性排序
     * 排序优先级：
     * 1. 标题完全匹配关键词（权重最高）
     * 2. 标题以关键词开头
     * 3. 标题包含关键词
     * 4. 按年份降序（越新的越靠前）
     */
    private static List<Vod> sortByRelevance(List<Vod> list, String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return list;
        }

        final String searchKey = keyword.toLowerCase().trim();

        // 计算每个结果的匹配分数
        for (Vod vod : list) {
            vod.setSearchScore(calculateScore(vod, searchKey));
        }

        // 按分数降序排序
        Collections.sort(list, new Comparator<Vod>() {
            @Override
            public int compare(Vod v1, Vod v2) {
                // 首先按相关性分数排序
                int scoreDiff = v2.getSearchScore() - v1.getSearchScore();
                if (scoreDiff != 0) {
                    return scoreDiff;
                }
                // 分数相同则按年份降序
                String year1 = v1.getVodYear();
                String year2 = v2.getVodYear();
                if (year1 != null && year2 != null) {
                    try {
                        int y1 = Integer.parseInt(year1);
                        int y2 = Integer.parseInt(year2);
                        return y2 - y1; // 降序
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }
                return 0;
            }
        });

        return list;
    }

    /**
     * 计算搜索结果的相关性分数
     * @param vod 视频对象
     * @param keyword 搜索关键词
     * @return 相关性分数 (0-100)
     */
    private static int calculateScore(Vod vod, String keyword) {
        int score = 0;
        String name = vod.getVodName();
        if (name == null) return 0;
        
        name = name.toLowerCase();

        // 精确匹配标题（50分）
        if (name.equals(keyword)) {
            score += 50;
        }
        // 标题以关键词开头（40分）
        else if (name.startsWith(keyword)) {
            score += 40;
        }
        // 标题以关键词开头（前缀匹配，考虑空格）（35分）
        else if (name.trim().startsWith(keyword)) {
            score += 35;
        }
        // 标题包含完整关键词（30分）
        else if (name.contains(keyword)) {
            score += 30;
        }
        // 标题包含关键词的所有字符（但不是连续包含）（20分）
        else if (containsAllChars(name, keyword)) {
            score += 20;
        }

        // 标题长度惩罚：太长的标题适当降低分数
        if (name.length() > 30) {
            score -= 5;
        }

        // 附加信息匹配加分
        // 导演匹配（+10分）
        String director = vod.getVodDirector();
        if (!TextUtils.isEmpty(director) && director.toLowerCase().contains(keyword)) {
            score += 10;
        }
        // 演员匹配（+10分）
        String actor = vod.getVodActor();
        if (!TextUtils.isEmpty(actor) && actor.toLowerCase().contains(keyword)) {
            score += 10;
        }

        return Math.max(score, 0);
    }

    /**
     * 检查字符串是否包含所有字符（用于模糊匹配）
     */
    private static boolean containsAllChars(String source, String chars) {
        for (char c : chars.toCharArray()) {
            if (source.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }
}
