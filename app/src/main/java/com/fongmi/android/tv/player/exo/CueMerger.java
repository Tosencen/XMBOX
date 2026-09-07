package com.fongmi.android.tv.player.exo;

import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 修复双语/双轨字幕重叠 + 去除 SSA 黑色背景：
 * 1. 同一时刻出现的多条底部文字 cue 合并为一条多行文本
 * 2. 强制清除所有 cue 的 windowColor（SSA 黑色背景框的来源）
 */
public final class CueMerger implements Player.Listener {

    private static final Map<PlayerView, Holder> HOLDERS = new WeakHashMap<>();
    private static final float BOTTOM_THRESHOLD = 0.5f;

    private final SubtitleView subtitleView;

    private CueMerger(SubtitleView subtitleView) {
        this.subtitleView = subtitleView;
    }

    /** 绑定到 PlayerView 的 Player 上，拦截 onCues 做去重叠处理。重复调用安全。 */
    public static void attach(PlayerView view) {
        if (view == null) return;
        Holder holder = HOLDERS.get(view);
        if (holder != null && holder.player != null) holder.player.removeListener(holder.listener);
        Player player = view.getPlayer();
        if (player == null) return;
        CueMerger merger = new CueMerger(view.getSubtitleView());
        player.addListener(merger);
        HOLDERS.put(view, new Holder(player, merger));
    }

    @Override
    public void onCues(@NonNull CueGroup cueGroup) {
        subtitleView.setCues(merge(clearBackgrounds(cueGroup.cues)));
    }

    private static List<Cue> clearBackgrounds(List<Cue> cues) {
        List<Cue> result = new ArrayList<>(cues.size());
        for (Cue cue : cues) {
            if (cue.windowColor != Color.TRANSPARENT) {
                result.add(cue.buildUpon().setWindowColor(Color.TRANSPARENT).build());
            } else {
                result.add(cue);
            }
        }
        return result;
    }

    private static List<Cue> merge(@Nullable List<Cue> cues) {
        if (cues == null || cues.size() < 2) return cues;
        List<Cue> bottomCues = new ArrayList<>();
        List<Cue> otherCues = new ArrayList<>();
        for (Cue cue : cues) {
            if (!mergeable(cue)) {
                otherCues.add(cue);
                continue;
            }
            if (isBottomCue(cue)) {
                bottomCues.add(cue);
            } else {
                otherCues.add(cue);
            }
        }
        if (bottomCues.size() >= 2) {
            otherCues.add(combine(bottomCues));
        } else {
            otherCues.addAll(bottomCues);
        }
        return otherCues;
    }

    private static boolean isBottomCue(Cue cue) {
        float line = Math.abs(cue.line);
        return line > BOTTOM_THRESHOLD || cue.lineType == Cue.LINE_TYPE_FRACTION;
    }

    private static boolean mergeable(Cue cue) {
        return cue.text != null
                && !TextUtils.isEmpty(cue.text)
                && cue.bitmap == null
                && cue.verticalType == Cue.TYPE_UNSET;
    }

    private static Cue combine(List<Cue> group) {
        List<CharSequence> texts = new ArrayList<>();
        for (Cue cue : group) {
            if (!contains(texts, cue.text)) texts.add(cue.text);
        }
        if (texts.isEmpty()) return group.get(0);
        CharSequence text = texts.get(0);
        for (int i = 1; i < texts.size(); i++) text = TextUtils.concat(text, "\n", texts.get(i));
        Cue base = group.get(0);
        return base.buildUpon().setText(text).build();
    }

    private static boolean contains(List<CharSequence> texts, CharSequence target) {
        for (CharSequence t : texts) if (TextUtils.equals(t, target)) return true;
        return false;
    }

    private static final class Holder {
        final Player player;
        final Player.Listener listener;

        Holder(Player player, Player.Listener listener) {
            this.player = player;
            this.listener = listener;
        }
    }
}
