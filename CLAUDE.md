# reminder — CLAUDE.md

## 專案入口
專案名稱：reminder
專案用途：Android 提醒/便條 App（Kotlin + Room 本地資料庫，支援通知排程）
主要工作目錄：D:\coding\20260519 reminder
GitHub repo：https://github.com/FengLingYu7563/reminder
預設 branch：main

## Obsidian 對應筆記
Obsidian vault：未使用
專案駕駛艙：未使用

## 同步規則
開工時：讀本檔、檢查 Git 狀態、不自動 pull/commit/push
收工時：整理已有功能/待辦/重要決策到本檔、必要時 commit + push

## 技術棧
- 語言：Kotlin
- 建置：Gradle Kotlin DSL（`build.gradle.kts`）+ Version Catalog（`gradle/libs.versions.toml`）
- AndroidX：appcompat 1.7.0、core-ktx、activity、constraintlayout、material
- 資料庫：Room 2.6.1（room-runtime / room-compiler kapt / room-ktx）
- SDK：compileSdk 35、minSdk 24、targetSdk 34
- 套件：`com.example.reminder`
- 權限：`SET_ALARM`

## 專案結構
- `app/src/main/java/com/example/reminder/`
  - `MainActivity.kt` — 主畫面（RecyclerView 列表）
  - `DetailActivity.kt` — 新增/編輯細節頁
  - `Note.kt` — Room Entity（id / title / content / time / isNotificationEnabled）
  - `NoteDao.kt` — Room DAO
  - `AppDatabase.kt` — Room 資料庫
  - `NoteViewModel.kt` — ViewModel
  - `ReminderAdapter.kt` — RecyclerView Adapter
- `app/src/main/res/` — 版面、圖示、字串、主題
- `app/src/main/AndroidManifest.xml` — `MainActivity`（LAUNCHER）+ `DetailActivity`

## 上次做到哪
最後動作：從 GitHub clone 既有專案、解壓 reminder.7z 攤平到專案根、補 .gitignore、移除 .7z、建立 CLAUDE.md
狀態：環境就緒，待開始下一步開發

## 待辦事項
- 開工後與使用者確認下一步要做什麼（功能新增 / Bug 修正 / 重構皆可）

## 重要決策
- 原 repo 程式碼以 `reminder.7z` 形式上傳，已解壓並從版控移除，未來直接以原始檔案進版控
- `.gitignore` 已加入 `*.7z` 與 Android 標準排除項，避免再把壓縮檔／build 產物推上 GitHub

## 不要做
- 不要把 API key、token、密碼、`keystore.properties`、`*.jks` 寫進 repo
- 不要自動納入無關 git 變更（避免 `git add .`，只 stage 本次相關檔案）
- 刪除任何檔案前必須先詢問使用者並取得同意
- 不要把 `local.properties`（內含本機 SDK 絕對路徑）推上 GitHub
