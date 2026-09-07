# Room 主线程查询迁移清单

> 目标：为移除 `AppDatabase.create()` 里的 `.allowMainThreadQueries()` 做准备。
> 结论：**当前不能直接删**，否则大量场景运行时直接崩溃。本文是迁移前置清单。

## 一、结论先行

`app/src/main/java/com/fongmi/android/tv/db/AppDatabase.java:109`

```java
.allowMainThreadQueries()   // ← 不能直接删
```

全项目共 **64 处 DAO 调用点**（另有 1 处 `clearAllTables()`），分布在 10 个文件。
其中大量经 bean 的 `public static` **同步**方法被 UI 层直接调用 → 主线程查库。

移除该开关后，Room 会在这些调用点抛：

```
java.lang.IllegalStateException: Cannot access database on the main thread
```

这不是"变卡"，是**直接崩**。

## 二、已核实的主线程调用（证据）

| 调用点 | 代码 | 场景 | 线程 |
|---|---|---|---|
| `mobile/tablet` `ui/fragment/SettingFragment.java:479,494` | `VodConfig.load(Config.vod(), ...)` | Fragment 生命周期 | 主线程 |
| `mobile/tablet` `ui/activity/KeepActivity.java:65` | `mAdapter.addAll(Keep.getVod())` | Activity 初始化列表 | 主线程 |
| `mobile/tablet` `ui/dialog/ConfigDialog.java:195,207,219` | `Config.vod()` / `Config.live()` / `Config.wall()` | Dialog 点击回调 | 主线程 |
| `mobile/tablet` `ui/dialog/SyncDialog.java:70,77,78` | `Config.vod()` / `Keep.getVod()` / `Config.findUrls()` | 同步弹窗 | 主线程 |
| `mobile/tablet` `ui/dialog/SyncSettingsDialog.java`, `CastDialog.java:74` | `Config.vod()` | 弹窗 | 主线程 |
| `main` `api/config/VodConfig.java:126,380` | `Config.vod()` | 应用初始化 / 取配置 | 主线程（启动路径） |
| `main` `api/config/LiveConfig.java:96,305` | `Config.live()` | 直播配置初始化 | 主线程（启动路径） |
| `main` `api/config/WallConfig.java:51,67` | `Config.wall()` | 壁纸配置初始化 | 主线程（启动路径） |
| `main` `api/config/LiveConfig.java:235` | `Keep.getLive()` | 配置加载 | 主线程 |
| `main` `player/Players.java:205` | `Track.find(getKey())` | 播放器取音轨 | **需确认**（播放线程？） |

> 启动路径（`VodConfig`/`LiveConfig`/`WallConfig`）是最危险的：App 一启动就会命中，删开关等于**开屏即崩**。

## 三、按文件统计（64 处 DAO 调用点）

| 文件 | 处数 | 说明 | 风险 |
|---|---|---|---|
| `bean/Config.java` | 16 | 全是 `public static` 同步查询/写入：`vod()` `live()` `wall()` `findUrls()` `getAll()` `find()` 等 | **高**（启动 + UI 都调） |
| `bean/Keep.java` | 10 | `find()` `getVod()` `getLive()` `delete()` `insertOrUpdate()` | **高**（UI 直调） |
| `bean/Backup.java` | 10 | 备份/恢复批量读写 + `clearAllTables()` | 中（需确认调用线程） |
| `bean/History.java` | 8 | `find()` `findAllRecent()` `delete()` `insertOrUpdate()` 等 | **高**（播放/历史页） |
| `bean/Download.java` | 6 | `getAll()` `find()` `insertOrUpdate()` `clear()` | 中（下载/列表） |
| `utils/WebDAVHistorySync.java` | 4 | `findAllRecent()` `insert()` `update()` | 低（同步在后台） |
| `bean/Track.java` | 3 | `find()` `insert()` `delete()` | 中（播放器） |
| `bean/Device.java` | 3 | `findAll()` `insertOrUpdate()` `delete()` | 低 |
| `bean/Site.java` | 2 | `find()` `insertOrUpdate()` | 中 |
| `bean/Live.java` | 2 | `find()` `insertOrUpdate()` | 中 |

已知走后台、相对安全的：`WebDAVHistorySync`、`utils/AutoSyncManager.java:126`、`server/process/Action.java:126-127`（本地 HTTP 服务线程）。

## 四、推荐迁移路径（分步，不要一步到位）

### 第 1 步：先开 StrictMode 抓真实调用栈（零改动、只读）

在 `App.onCreate()` 里临时加：

```java
StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads().detectDiskWrites()
        .penaltyLog()      // 先只打日志，别 penaltyDeath
        .build());
```

跑一遍主要页面（首页/收藏/历史/设置/播放），把所有主线程 DB 访问的**真实调用栈**捞出来。
这步不改任何业务代码，纯粹定位遗漏点 —— 比静态 grep 准。

### 第 2 步：给 bean 加异步版本，保留同步版并标注线程约束

- 保留现有同步方法，加 `@WorkerThread` 注解做静态检查。
- 新增异步版本，二选一：
  - 返回 `LiveData<T>` / `Flow`（Room 原生支持，最省事）
  - 或 `Executor` 包装 + `Callback`
- 优先覆盖 **P0 启动路径**：`Config.vod()` / `Config.live()` / `Config.wall()`。

### 第 3 步：逐个把 UI 层调用改成异步

按下面优先级推进，每改一批跑一次回归。

### 第 4 步：全部迁完后，再移除 `allowMainThreadQueries()`

此时开关才能真正起到"防止回潮"的作用。

## 五、优先级

| 优先级 | 范围 | 原因 |
|---|---|---|
| **P0** | `Config.vod()` `Config.live()` `Config.wall()`（VodConfig/LiveConfig/WallConfig 启动路径） | 开屏即崩 |
| **P1** | `SettingFragment` `KeepActivity` `ConfigDialog` `SyncDialog` 的 `Config.*` / `Keep.*` 调用 | 进页面就崩 |
| **P2** | `History.*`、`Track.find()`（播放/历史页） | 高频路径 |
| **P3** | `Download` `Device` `Site` `Live` `Backup` | 低频，可最后收尾 |
| **P4** | `WebDAVHistorySync` `AutoSyncManager` `server/process/Action` | 已在后台，基本不用改 |

## 六、注意事项

- **不要先删开关再去补调用点** —— 顺序反了，测试期会大面积崩溃。
- 迁移过程中 `allowMainThreadQueries()` 要一直保留，直到最后一步。
- `Players.java:205` 的 `Track.find()` 所在线程需实测确认（播放线程 or 主线程），别想当然。
- 本次已完成的另一项优化：两处 `newCachedThreadPool()` → `newFixedThreadPool(4)`（`ParseJob.java:49`、`DanPlayer.java:32`），与本项无关，仅作记录。
