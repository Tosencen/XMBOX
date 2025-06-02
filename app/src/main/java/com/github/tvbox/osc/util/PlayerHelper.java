package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Context;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.player.EXOmPlayer;
import com.github.tvbox.osc.player.IjkMediaPlayer;
import com.github.tvbox.osc.player.render.SurfaceRenderViewFactory;
import com.github.tvbox.osc.player.thirdparty.Kodi;
import com.github.tvbox.osc.player.thirdparty.MXPlayer;
import com.github.tvbox.osc.player.thirdparty.ReexPlayer;
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox;
import com.github.tvbox.osc.player.thirdparty.VlcPlayer;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import tv.danmaku.ijk.media.player.IjkLibLoader;
import xyz.doikki.videoplayer.exo.ExoMediaPlayerFactory;
import xyz.doikki.videoplayer.player.AndroidMediaPlayerFactory;
import xyz.doikki.videoplayer.player.PlayerFactory;
import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.render.RenderViewFactory;
import xyz.doikki.videoplayer.render.TextureRenderViewFactory;

public class PlayerHelper {
    public static void updateCfg(VideoView videoView, JSONObject playerCfg) {
        int playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0);
        int renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0);
        String ijkCode = Hawk.get(HawkConfig.IJK_CODEC, "软解码");
        int scale = Hawk.get(HawkConfig.PLAY_SCALE, 0);
        try {
            playerType = playerCfg.getInt("pl");
            renderType = playerCfg.getInt("pr");
            ijkCode = playerCfg.getString("ijk");
            scale = playerCfg.getInt("sc");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        IJKCode codec = ApiConfig.get().getIJKCodec(ijkCode);
        PlayerFactory playerFactory;
        if (playerType == 1) {
            // 检查IJK播放器库是否已加载
            if (ijkLibraryLoaded) {
                LOG.i("PlayerHelper", "使用IJK播放器");
                playerFactory = new PlayerFactory<IjkMediaPlayer>() {
                    @Override
                    public IjkMediaPlayer createPlayer(Context context) {
                        return new IjkMediaPlayer(context, codec);
                    }
                };
            } else {
                // IJK播放器库未加载，使用系统播放器代替
                LOG.e("PlayerHelper", "IJK播放器库未加载，使用系统播放器代替");
                playerFactory = AndroidMediaPlayerFactory.create();
            }
        } else if (playerType == 2) {
            playerFactory = new PlayerFactory<EXOmPlayer>() {
                @Override
                public EXOmPlayer createPlayer(Context context) {
                    return new EXOmPlayer(context);
                }
            };
        } else {
            playerFactory = AndroidMediaPlayerFactory.create();
        }
        RenderViewFactory renderViewFactory = null;
        switch (renderType) {
            case 0:
            default:
                renderViewFactory = TextureRenderViewFactory.create();
                break;
            case 1:
                renderViewFactory = SurfaceRenderViewFactory.create();
                break;
        }
        videoView.setPlayerFactory(playerFactory);
        videoView.setRenderViewFactory(renderViewFactory);
        videoView.setScreenScaleType(scale);
    }

    public static void updateCfg(VideoView videoView) {
        int playType = Hawk.get(HawkConfig.PLAY_TYPE, 0);
        PlayerFactory playerFactory;
        if (playType == 1) {
            // 检查IJK播放器库是否已加载
            if (ijkLibraryLoaded) {
                LOG.i("PlayerHelper", "使用IJK播放器");
                playerFactory = new PlayerFactory<IjkMediaPlayer>() {
                    @Override
                    public IjkMediaPlayer createPlayer(Context context) {
                        return new IjkMediaPlayer(context, null);
                    }
                };
            } else {
                // IJK播放器库未加载，使用系统播放器代替
                LOG.e("PlayerHelper", "IJK播放器库未加载，使用系统播放器代替");
                playerFactory = AndroidMediaPlayerFactory.create();
            }
        } else if (playType == 2) {
            playerFactory = new PlayerFactory<EXOmPlayer>() {
                @Override
                public EXOmPlayer createPlayer(Context context) {
                    return new EXOmPlayer(context);
                }
            };
        } else {
            playerFactory = AndroidMediaPlayerFactory.create();
        }
        int renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0);
        RenderViewFactory renderViewFactory = null;
        switch (renderType) {
            case 0:
            default:
                renderViewFactory = TextureRenderViewFactory.create();
                break;
            case 1:
                renderViewFactory = SurfaceRenderViewFactory.create();
                break;
        }
        videoView.setPlayerFactory(playerFactory);
        videoView.setRenderViewFactory(renderViewFactory);
    }


    private static boolean ijkLibraryLoaded = false;

    /**
     * 尝试加载IJK播放器库
     * @return 是否成功加载
     */
    private static boolean loadIJKLibrary() {
        if (!ijkLibraryLoaded) {
            try {
                LOG.i("PlayerHelper", "尝试加载 IJK 播放器库");
                tv.danmaku.ijk.media.player.IjkMediaPlayer.loadLibrariesOnce(new IjkLibLoader() {
                    @Override
                    public void loadLibrary(String s) throws UnsatisfiedLinkError, SecurityException {
                        try {
                            LOG.i("PlayerHelper", "加载库: " + s);
                            System.loadLibrary(s);
                            LOG.i("PlayerHelper", "成功加载库: " + s);
                        } catch (Throwable th) {
                            LOG.e("PlayerHelper", "加载库失败: " + s + ", 错误: " + th.getMessage());
                            throw new UnsatisfiedLinkError("Failed to load library: " + s);
                        }
                    }
                });
                ijkLibraryLoaded = true;
                LOG.i("PlayerHelper", "IJK 播放器库加载成功");
                return true;
            } catch (Throwable th) {
                LOG.e("PlayerHelper", "IJK 播放器库加载失败: " + th.getMessage());
                return false;
            }
        }
        return ijkLibraryLoaded;
    }

    /**
     * 初始化播放器
     */
    public static void init() {
        LOG.i("PlayerHelper", "播放器初始化开始");

        try {
            // 检查设备架构
            String arch = System.getProperty("os.arch");
            boolean isArmArchitecture = arch != null && (arch.contains("arm") || arch.contains("ARM"));
            LOG.i("PlayerHelper", "设备架构: " + arch + ", 是否为ARM架构: " + isArmArchitecture);

            if (isArmArchitecture) {
                // 尝试加载IJK播放器库
                boolean ijkLoaded = loadIJKLibrary();
                if (ijkLoaded) {
                    LOG.i("PlayerHelper", "IJK播放器库加载成功，可以使用IJK播放器");
                    // 更新播放器存在信息
                    if (mPlayersExistInfo != null) {
                        mPlayersExistInfo.put(1, true);
                    }
                } else {
                    LOG.e("PlayerHelper", "IJK播放器库加载失败，将使用备用播放器");
                    // 更新播放器存在信息
                    if (mPlayersExistInfo != null) {
                        mPlayersExistInfo.put(1, false);
                    }
                    // 如果默认播放器是IJK，则切换到Exo播放器
                    if (Hawk.get(HawkConfig.PLAY_TYPE, 0) == 1) {
                        LOG.i("PlayerHelper", "默认播放器是IJK，但IJK库加载失败，切换到Exo播放器");
                        Hawk.put(HawkConfig.PLAY_TYPE, 2);
                    }
                }
            } else {
                LOG.e("PlayerHelper", "非ARM架构设备，不加载IJK播放器库");
                // 更新播放器存在信息
                if (mPlayersExistInfo != null) {
                    mPlayersExistInfo.put(1, false);
                }
                // 如果默认播放器是IJK，则切换到Exo播放器
                if (Hawk.get(HawkConfig.PLAY_TYPE, 0) == 1) {
                    LOG.i("PlayerHelper", "默认播放器是IJK，但非ARM架构，切换到Exo播放器");
                    Hawk.put(HawkConfig.PLAY_TYPE, 2);
                }
            }

            LOG.i("PlayerHelper", "播放器初始化完成");
        } catch (Throwable e) {
            LOG.e("PlayerHelper", "播放器初始化过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getPlayerName(int playType) {
        HashMap<Integer, String> playersInfo = getPlayersInfo();
        if (playersInfo.containsKey(playType)) {
            return playersInfo.get(playType);
        } else {
            return "系统播放器";
        }
    }

    private static HashMap<Integer, String> mPlayersInfo = null;
    public static HashMap<Integer, String> getPlayersInfo() {
        if (mPlayersInfo == null) {
            HashMap<Integer, String> playersInfo = new HashMap<>();
            playersInfo.put(0, "系统播放器");
            playersInfo.put(1, "IJK播放器");
            playersInfo.put(2, "Exo播放器");
            playersInfo.put(10, "MX播放器");
            playersInfo.put(11, "Reex播放器");
            playersInfo.put(12, "Kodi播放器");
            playersInfo.put(13, "附近TVBox");
            playersInfo.put(14, "VLC播放器");
            mPlayersInfo = playersInfo;
        }
        return mPlayersInfo;
    }

    private static HashMap<Integer, Boolean> mPlayersExistInfo = null;
    public static HashMap<Integer, Boolean> getPlayersExistInfo() {
        if (mPlayersExistInfo == null) {
            HashMap<Integer, Boolean> playersExist = new HashMap<>();
            playersExist.put(0, false);
            playersExist.put(1, true);
            playersExist.put(2, true);
            playersExist.put(10, MXPlayer.getPackageInfo() != null);
            playersExist.put(11, ReexPlayer.getPackageInfo() != null);
            playersExist.put(12, Kodi.getPackageInfo() != null);
            playersExist.put(13, RemoteTVBox.getAvalible() != null);
            playersExist.put(14, VlcPlayer.getPackageInfo() != null);
            mPlayersExistInfo = playersExist;
        }
        return mPlayersExistInfo;
    }

    public static Boolean getPlayerExist(int playType) {
        HashMap<Integer, Boolean> playersExistInfo = getPlayersExistInfo();
        if (playersExistInfo.containsKey(playType)) {
            return playersExistInfo.get(playType);
        } else {
            return false;
        }
    }

    public static ArrayList<Integer> getExistPlayerTypes() {
        HashMap<Integer, Boolean> playersExistInfo = getPlayersExistInfo();
        ArrayList<Integer> existPlayers = new ArrayList<>();
        for(Integer playerType : playersExistInfo.keySet()) {
            if (playersExistInfo.get(playerType)) {
                existPlayers.add(playerType);
            }
        }
        return existPlayers;
    }

    public static Boolean runExternalPlayer(int playerType, Activity activity, String url, String title, String subtitle, HashMap<String, String> headers) {
        return runExternalPlayer(playerType, activity, url, title, subtitle, headers);
    }

    public static Boolean runExternalPlayer(int playerType, Activity activity, String url, String title, String subtitle, HashMap<String, String> headers, long progress) {
        boolean callResult = false;
        switch (playerType) {
            case 10: {
                callResult = MXPlayer.run(activity, url, title, subtitle, headers);
                break;
            }
            case 11: {
                callResult = ReexPlayer.run(activity, url, title, subtitle, headers);
                break;
            }
            case 12: {
                callResult = Kodi.run(activity, url, title, subtitle, headers);
                break;
            }
            case 13: {
                callResult = RemoteTVBox.run(activity, url, title, subtitle, headers);
                break;
            }
            case 14: {
                callResult = VlcPlayer.run(activity, url, title, subtitle, progress);
                break;
            }
        }
        return callResult;
    }

    public static String getRenderName(int renderType) {
        if (renderType == 1) {
            return "SurfaceView";
        } else {
            return "TextureView";
        }
    }

    public static String getScaleName(int screenScaleType) {
        String scaleText = "默认";
        switch (screenScaleType) {
            case VideoView.SCREEN_SCALE_DEFAULT:
                scaleText = "默认";
                break;
            case VideoView.SCREEN_SCALE_16_9:
                scaleText = "16:9";
                break;
            case VideoView.SCREEN_SCALE_4_3:
                scaleText = "4:3";
                break;
            case VideoView.SCREEN_SCALE_MATCH_PARENT:
                scaleText = "填充";
                break;
            case VideoView.SCREEN_SCALE_ORIGINAL:
                scaleText = "原始";
                break;
            case VideoView.SCREEN_SCALE_CENTER_CROP:
                scaleText = "裁剪";
                break;
        }
        return scaleText;
    }

    public static String getDisplaySpeed(long speed) {
        if(speed > 1048576)
            return new DecimalFormat("#.00").format(speed / 1048576d) + "Mb/s";
        else if(speed > 1024)
            return (speed / 1024) + "Kb/s";
        else
            return speed > 0?speed + "B/s":"";
    }
}
