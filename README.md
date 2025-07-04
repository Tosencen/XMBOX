# XMBOX

各模块说明:

- app - 主要的应用程序代码
- catvod - 视频点播相关功能
- forcetech - 强制技术相关功能
- hook - 钩子功能
- jianpian - 剪片相关功能
- quickjs - JavaScript 引擎
- thunder - 迅雷下载相关功能
- tvbus - TV 总线功能
- zlive - 直播相关功能

一个简单的视频播放器应用，支持以下功能：

## 主要功能
- 视频播放：支持多种格式视频播放
- 直播观看：支持直播源播放
- 收藏管理：可收藏喜欢的视频和直播源
- 设置中心：自定义应用配置

## 技术特点
- 基于 Android 原生开发
- 使用 ExoPlayer 作为播放内核
- 支持 TV 和手机双平台
- Material Design 界面设计

## 开发说明
本项目仅用于学习 Android 开发，代码改自 [FongMi/TV](https://github.com/FongMi/TV)。

## 免责声明
1. 本项目仅供学习交流使用，不得用于商业用途
2. 项目中的内容均来自网络，如有侵权请联系删除
3. 使用本项目产生的一切后果由使用者自行承担

## 许可证
GPL-3.0 license

# 影視

### 基於 CatVod 項目

https://github.com/CatVodTVOfficial/CatVodTVJarLoader

### 點播欄位

| 欄位名稱       | 預設值  | 說明   | 其他         |
|------------|------|------|------------|
| searchable | 1    | 是否搜索 | 0：關閉；1：啟用  |
| changeable | 1    | 是否換源 | 0：關閉；1：啟用  |
| quickserch | 1    | 是否快搜 | 0：關閉；1：啟用  |
| indexs     | 0    | 是否聚搜 | 0：關閉；1：啟用  |
| hide       | 0    | 是否隱藏 | 0：顯示；1：隱藏  |
| timeout    | 15   | 播放超時 | 單位：秒       |
| header     | none | 請求標頭 | 格式：json    |
| click      | none | 點擊js | javascript |

### 直播欄位

| 欄位名稱     | 預設值   | 說明    | 其他         |
|----------|-------|-------|------------|
| ua       | none  | 用戶代理  |            |
| origin   | none  | 來源    |            |
| referer  | none  | 參照地址  |            |
| epg      | none  | 節目地址  |            |
| logo     | none  | 台標地址  |            |
| pass     | false | 是否免密碼 |            |
| boot     | false | 是否自啟動 |            |
| timeout  | 15    | 播放超時  | 單位：秒       |
| header   | none  | 請求標頭  | 格式：json    |
| click    | none  | 點擊js  | javascript |
| catchup  | none  | 回看參數  |            |
| timeZone | none  | 時區    |            |

### 樣式

| 欄位名稱  | 值    | 說明  |
|-------|------|-----|
| type  | rect | 矩形  |
|       | oval | 橢圓  |
|       | list | 列表  |
| ratio | 0.75 | 3：4 |
|       | 1.33 | 4：3 |

直式

```json
{
  "style": {
    "type": "rect"
  }
}
```

橫式

```json
{
  "style": {
    "type": "rect",
    "ratio": 1.33
  }
}
```

正方

```json
{
  "style": {
    "type": "rect",
    "ratio": 1
  }
}
```

正圓

```json
{
  "style": {
    "type": "oval"
  }
}
```

橢圓

```json
{
  "style": {
    "type": "oval",
    "ratio": 1.1
  }
}
```

### API

刷新詳情

```
http://127.0.0.1:9978/action?do=refresh&type=detail
```

刷新播放

```
http://127.0.0.1:9978/action?do=refresh&type=player
```

刷新直播

```
http://127.0.0.1:9978/action?do=refresh&type=live
```

推送字幕

```
http://127.0.0.1:9978/action?do=refresh&type=subtitle&path=http://xxx
```

推送彈幕

```
http://127.0.0.1:9978/action?do=refresh&type=danmaku&path=http://xxx
```

新增緩存字串

```
http://127.0.0.1:9978/cache?do=set&key=xxx&value=xxx
```

取得緩存字串

```
http://127.0.0.1:9978/cache?do=get&key=xxx
```

刪除緩存字串

```
http://127.0.0.1:9978/cache?do=del&key=xxx
```

### Proxy

scheme 支持 http, https, socks4, socks5

```
scheme://username:password@host:port
```

配置新增 proxy 判斷域名是否走代理  
全局只需要加上一條規則 ".*."

```json
{
  "spider": "",
  "proxy": [
    "raw.githubusercontent.com",
    "googlevideo.com"
  ]
}
```

### Hosts

```json
{
  "spider": "",
  "hosts": [
    "cache.ott.*.itv.cmvideo.cn=base-v4-free-mghy.e.cdn.chinamobile.com"
  ]
}
```

### Headers

```json
{
  "spider": "",
  "headers": [
    {
      "host": "gslbserv.itv.cmvideo.cn",
      "header": {
        "User-Agent": "okhttp/3.12.13",
        "Referer": "test"
      }
    }
  ]
}
```

### 爬蟲本地代理

Java

```
proxy://
```

```
Proxy.getUrl(boolean local)
```

Python

```
proxy://do=py
```

```
getProxyUrl(boolean local)
```

JS

```
proxy://do=js
```

```
getProxy(boolean local)
```

### 配置範例

[點播-線上](other/sample/vod/online.json)  
[點播-本地](other/sample/vod/offline.json)  
[直播-線上](other/sample/live/online.json)  
[直播-本地](other/sample/live/offline.json)

