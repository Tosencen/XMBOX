# Run doc — XMBOX app web assets preview

This project is an Android (Gradle) TV app. The only web content is a static
WebView page bundled in `app/src/main/assets/` (used by the app's built-in
local HTTP server for 搜索/推送/設定/本地 file management). There is no
package.json, no build step, and no framework — serving the directory
statically is enough to preview the UI in a browser.

## Reproduce the artifacts

Nothing to reproduce. The assets are checked into the repo already:

- `app/src/main/assets/index.html` (page)
- `app/src/main/assets/css/style.css`, `css/ui.css`
- `app/src/main/assets/js/jquery.min.js`, `js/script.js`

No env files, no dependencies, no build required.

## Run the server

Serve the assets directory with Python's built-in HTTP server (detached so it
survives the conversation):

```bash
bash .freebuff/start_preview_server.sh 8000 .freebuff/preview-544db612-07ed-4d60-904a-30147455dc3f.log
```

- `start_preview_server.sh` double-forks via Python (`os.setsid` + second fork)
  so the server is fully detached; `setsid` itself is not available on macOS.
- Server listens on `127.0.0.1:8000` with the assets dir as document root.
- Check it is up: `curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8000/index.html` (expect 200).
- Logs: `.freebuff/preview-544db612-07ed-4d60-904a-30147455dc3f.log`

Then register the preview with URL `http://127.0.0.1:8000/index.html`.

Note: the 本地 (Local) tab calls app-only endpoints (`/file`, `/upload`,
`/newFolder`, ...) that only exist in the Android WebView backend, so those
actions 404 in a browser preview. All other UI (tabs, forms, dialogs) works.
