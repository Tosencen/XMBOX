package com.fongmi.android.tv.db;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class Migrations {

    /**
     * Download 表的完整建表语句，必须与 {@link com.fongmi.android.tv.bean.Download} 实体完全一致。
     * 之前 33→34 的建表语句漏了 `statusInt`，导致从 v33 数据库升级上来的设备在 onUpgrade 校验时
     * 抛 IllegalStateException("Migration didn't properly handle: Download") 而无法启动。
     */
    static final String DOWNLOAD_DDL = "CREATE TABLE IF NOT EXISTS `Download` (" +
            "`id` TEXT NOT NULL, " +
            "`vodPic` TEXT, " +
            "`vodName` TEXT, " +
            "`vodId` TEXT, " +
            "`url` TEXT, " +
            "`header` TEXT, " +
            "`createTime` INTEGER NOT NULL, " +
            "`progress` INTEGER NOT NULL, " +
            "`status` TEXT, " +
            "`statusInt` INTEGER NOT NULL, " +
            "`duration` INTEGER NOT NULL, " +
            "`speed` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`))";

    /** 列名 + 旧库缺列时的兜底填充值 */
    private static final String[][] DOWNLOAD_COLUMNS = {
            {"id", "''"},
            {"vodPic", "NULL"},
            {"vodName", "NULL"},
            {"vodId", "NULL"},
            {"url", "NULL"},
            {"header", "NULL"},
            {"createTime", "0"},
            {"progress", "0"},
            {"status", "NULL"},
            {"statusInt", "0"},
            {"duration", "0"},
            {"speed", "0"}
    };

    public static final Migration MIGRATION_30_31 = new Migration(30, 31) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE Track");
            database.execSQL("CREATE TABLE Track (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` INTEGER NOT NULL, `group` INTEGER NOT NULL, `track` INTEGER NOT NULL, `key` TEXT, `name` TEXT, `selected` INTEGER NOT NULL, `adaptive` INTEGER NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Track_key_type` ON `Track` (`key`, `type`)");
        }
    };

    public static final Migration MIGRATION_31_32 = new Migration(31, 32) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE History_Backup (`key` TEXT NOT NULL, `vodPic` TEXT, `vodName` TEXT, `vodFlag` TEXT, `vodRemarks` TEXT, `episodeUrl` TEXT, `revSort` INTEGER NOT NULL, `revPlay` INTEGER NOT NULL, `createTime` INTEGER NOT NULL, `opening` INTEGER NOT NULL, `ending` INTEGER NOT NULL, `position` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `speed` REAL NOT NULL, `scale` INTEGER NOT NULL, `cid` INTEGER NOT NULL, PRIMARY KEY(`key`))");
            database.execSQL("INSERT INTO History_Backup SELECT `key`, `vodPic`, `vodName`, `vodFlag`, `vodRemarks`, `episodeUrl`, `revSort`, `revPlay`, `createTime`, `opening`, `ending`, `position`, `duration`, `speed`, `scale`, `cid` FROM History");
            database.execSQL("DROP TABLE History");
            database.execSQL("ALTER TABLE History_Backup RENAME to History");
        }
    };

    public static final Migration MIGRATION_32_33 = new Migration(32, 33) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Live ADD COLUMN keep TEXT DEFAULT NULL");
        }
    };

    public static final Migration MIGRATION_33_34 = new Migration(33, 34) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            ensureDownload(database);
        }
    };

    /** 34→35：修补历史上建错（缺 statusInt）的 Download 表，同时刷新 room 身份校验值 */
    public static final Migration MIGRATION_34_35 = new Migration(34, 35) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            ensureDownload(database);
        }
    };

    /**
     * 保证 Download 表存在且包含全部字段；表已存在但缺列时按当前列重建并把数据搬过去。
     */
    private static void ensureDownload(SupportSQLiteDatabase database) {
        List<String> existing = columns(database, "Download");
        if (existing.isEmpty()) {
            database.execSQL(DOWNLOAD_DDL);
            return;
        }
        boolean complete = true;
        for (String[] column : DOWNLOAD_COLUMNS) {
            if (!existing.contains(column[0])) {
                complete = false;
                break;
            }
        }
        if (complete) return;

        database.execSQL("DROP TABLE IF EXISTS `Download_Temp`");
        database.execSQL(DOWNLOAD_DDL.replace("`Download`", "`Download_Temp`"));
        StringBuilder names = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (String[] column : DOWNLOAD_COLUMNS) {
            if (names.length() > 0) {
                names.append(", ");
                values.append(", ");
            }
            names.append('`').append(column[0]).append('`');
            values.append(existing.contains(column[0]) ? "`" + column[0] + "`" : column[1]);
        }
        database.execSQL("INSERT OR REPLACE INTO `Download_Temp` (" + names + ") SELECT " + values + " FROM `Download`");
        database.execSQL("DROP TABLE `Download`");
        database.execSQL("ALTER TABLE `Download_Temp` RENAME TO `Download`");
    }

    private static List<String> columns(SupportSQLiteDatabase database, String table) {
        List<String> names = new ArrayList<>();
        try (Cursor cursor = database.query("PRAGMA table_info(`" + table + "`)")) {
            int index = cursor.getColumnIndex("name");
            if (index < 0) return names;
            while (cursor.moveToNext()) {
                String name = cursor.getString(index);
                if (name != null) names.add(name);
            }
        }
        return names;
    }
}
