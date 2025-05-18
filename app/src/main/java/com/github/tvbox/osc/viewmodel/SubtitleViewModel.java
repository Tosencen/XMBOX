package com.github.tvbox.osc.viewmodel;

import android.text.TextUtils;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.tvbox.osc.bean.Subtitle;
import com.github.tvbox.osc.bean.SubtitleData;
import com.github.tvbox.osc.network.repository.SubtitleRepository;
import com.github.tvbox.osc.ui.dialog.SearchSubtitleDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.launch;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SubtitleViewModel extends ViewModel {

    public MutableLiveData<SubtitleData> searchResult;
    private SubtitleRepository subtitleRepository;
    private Job searchJob;
    private CoroutineScope viewModelScope = new CoroutineScope(Dispatchers.getMain()) {
        @Override
        public Job getCoroutineContext() {
            return searchJob;
        }
    };

    public SubtitleViewModel() {
        searchResult = new MutableLiveData<>();
        subtitleRepository = new SubtitleRepository();
        searchJob = new Job();
    }

    public void searchResult(String title, int page) {
        searchResultFromAssrt(title, page);
    }

    public void getSearchResultSubtitleUrls(Subtitle subtitle) {
        getSearchResultSubtitleUrlsFromAssrt(subtitle);
    }

    public void getSubtitleUrl(Subtitle subtitle, SearchSubtitleDialog.SubtitleLoader subtitleLoader) {
        getSubtitleUrlFromAssrt(subtitle, subtitleLoader);
    }

    private void setSearchListData(List<Subtitle> data, boolean isNew, boolean isZip) {
        try {
            SubtitleData subtitleData = new SubtitleData();
            subtitleData.setSubtitleList(data);
            subtitleData.setIsNew(isNew);
            subtitleData.setIsZip(isZip);
            searchResult.postValue(subtitleData);
        } catch (Throwable e) {
            e.printStackTrace();
            searchResult.postValue(null);
        }
    }

    private int pagesTotal = -1;

    private void searchResultFromAssrt(String title, int page) {
        try {
            viewModelScope.launch(Dispatchers.getIO(), () -> {
                try {
                    // 调用仓库方法搜索字幕
                    subtitleRepository.searchSubtitles(title, page)
                        .fold(
                            result -> {
                                List<Subtitle> data = result.getFirst();
                                boolean isZip = result.getSecond();
                                setSearchListData(data, page <= 1, isZip);
                                return null;
                            },
                            error -> {
                                error.printStackTrace();
                                setSearchListData(null, page <= 1, true);
                                return null;
                            }
                        );
                } catch (Exception e) {
                    e.printStackTrace();
                    setSearchListData(null, page <= 1, true);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
            setSearchListData(null, page <= 1, true);
        }
    }

    Pattern regexShooterFileOnclick = Pattern.compile("onthefly\\(\"(\\d+)\",\"(\\d+)\",\"([\\s\\S]*)\"\\)");

    private void getSearchResultSubtitleUrlsFromAssrt(Subtitle subtitle) {
        try {
            viewModelScope.launch(Dispatchers.getIO(), () -> {
                try {
                    // 调用仓库方法获取字幕详情
                    subtitleRepository.getSubtitleDetail(subtitle)
                        .fold(
                            data -> {
                                setSearchListData(data, true, false);
                                return null;
                            },
                            error -> {
                                error.printStackTrace();
                                setSearchListData(null, true, true);
                                return null;
                            }
                        );
                } catch (Exception e) {
                    e.printStackTrace();
                    setSearchListData(null, true, true);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
            setSearchListData(null, true, true);
        }
    }

    private void getSubtitleUrlFromAssrt(Subtitle subtitle, SearchSubtitleDialog.SubtitleLoader subtitleLoader) {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.54 Safari/537.36";
        Request request = new Request.Builder()
                .url(subtitle.getUrl())
                .get()
                .addHeader("Referer", "https://secure.assrt.net")
                .addHeader("User-Agent", ua)
                .build();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(true);
        OkHttpClient client = builder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                subtitle.setUrl(response.header("location"));
                subtitleLoader.loadSubtitle(subtitle);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (searchJob != null && !searchJob.isCancelled()) {
            searchJob.cancel();
        }
    }
}
