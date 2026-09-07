package com.fongmi.android.tv.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Backup;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.Download;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.db.dao.ConfigDao;
import com.fongmi.android.tv.db.dao.DeviceDao;
import com.fongmi.android.tv.db.dao.DownloadDao;
import com.fongmi.android.tv.db.dao.HistoryDao;
import com.fongmi.android.tv.db.dao.KeepDao;
import com.fongmi.android.tv.db.dao.LiveDao;
import com.fongmi.android.tv.db.dao.SiteDao;
import com.fongmi.android.tv.db.dao.TrackDao;
import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Database(entities = {Keep.class, Site.class, Live.class, Track.class, Config.class, Device.class, History.class, Download.class}, version = AppDatabase.VERSION)
public abstract class AppDatabase extends RoomDatabase {

    public static final int VERSION = 34;
    public static final String NAME = "tv";
    public static final String SYMBOL = "@@@";

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get() {
        if (instance == null) instance = create(App.get());
        return instance;
    }

    public static void backup() {
        backup(new com.fongmi.android.tv.impl.Callback());
    }

    public static void backup(com.fongmi.android.tv.impl.Callback callback) {
        App.execute(() -> {
            File file = new File(Path.tv(), "tv-" + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()) + ".bk");
            Backup backup = Backup.create();
            if (backup.getConfig().isEmpty()) {
                App.post(callback::error);
            } else {
                Path.write(file, backup.toString().getBytes());
                FileUtil.gzipCompress(file);
                App.post(callback::success);
                cleanOld();
            }
        });
    }

    public static void restore(File file, com.fongmi.android.tv.impl.Callback callback) {
        App.execute(() -> {
            File restore = Path.cache("restore");
            FileUtil.gzipDecompress(file, restore);
            Backup backup = Backup.objectFrom(Path.read(restore));
            if (backup.getConfig().isEmpty()) {
                App.post(callback::error);
            } else {
                backup.restore();
                Path.clear(restore);
                App.post(callback::success);
            }
        });
    }

    private static void cleanOld() {
        List<File> items = new ArrayList<>();
        File[] files = Path.tv().listFiles();
        if (files == null) files = new File[0];
        for (File file : files) if (file.getName().startsWith("tv") && file.getName().endsWith(".bk.gz")) items.add(file);
        if (!items.isEmpty()) Collections.sort(items, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        if (items.size() > 7) for (int i = 7; i < items.size(); i++) Path.clear(items.get(i));
    }

private static AppDatabase create(Context context) {
        // 在构建数据库前先备份，避免迁移失败时丢失数据
        backupDatabase(context);

        return Room.databaseBuilder(context, AppDatabase.class, NAME)
                .addMigrations(Migrations.MIGRATION_30_31)
                .addMigrations(Migrations.MIGRATION_31_32)
                .addMigrations(Migrations.MIGRATION_32_33)
                .addMigrations(Migrations.MIGRATION_33_34)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
    }

    /** 备份当前数据库文件，防止迁移失败或销毁重建时数据丢失 */
    private static void backupDatabase(Context context) {
        try {
            File dbFile = context.getDatabasePath(NAME);
            if (!dbFile.exists()) return;

            File backupDir = new File(Path.tv(), "db_backup");
            backupDir.mkdirs();
            File backupFile = new File(backupDir, NAME + "-" +
                    new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(new Date()) + ".db");
            copyFile(dbFile, backupFile);
            Logger.d("AppDatabase: 已备份到 " + backupFile.getAbsolutePath());

            // 清理旧备份，只保留最近 7 个
            cleanOldBackups(backupDir);
        } catch (Exception e) {
            Logger.e("AppDatabase: 备份失败 — " + e.getMessage());
        }
    }

    private static void cleanOldBackups(File dir) {
        File[] files = dir.listFiles((d, name) -> name.startsWith(NAME) && name.endsWith(".db"));
        if (files == null || files.length <= 7) return;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (int i = 7; i < files.length; i++) files[i].delete();
    }

    /** 兼容 API 24+ 的文件拷贝，替换 java.nio.file.Files.copy（需要 API 26+） */
    private static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    public abstract KeepDao getKeepDao();

    public abstract SiteDao getSiteDao();

    public abstract LiveDao getLiveDao();

    public abstract TrackDao getTrackDao();

    public abstract ConfigDao getConfigDao();

    public abstract DeviceDao getDeviceDao();

    public abstract HistoryDao getHistoryDao();

    public abstract DownloadDao getDownloadDao();
}
