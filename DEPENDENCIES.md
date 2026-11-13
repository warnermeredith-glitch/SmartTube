# Kidtube (`org.kidtube.tv`) — Dependencies & Build Toolchain Manifest

> **Beginner's Note on How Android Tracks Dependencies:**
> Unlike Python (which lists libraries in a single `requirements.txt`) or JavaScript (which uses `package.json`), Android and Java projects use a build automation tool called **Gradle**. When you run `./gradlew`, Gradle reads the `build.gradle` files and `SharedModules/constants.gradle` and **automatically downloads every exact library version below** from Maven Central, Google's Maven Repository, and JitPack. You do **not** have to install the Java libraries below by hand — only the **Host Toolchain** in Section 1!

---

## 1. Host Build & Deployment Toolchain (Installed on Your Computer)

| Tool / Component | Exact Version Used | Purpose |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **JDK 17 (`x64` / `windows-x64`)** | Compiles Java/Kotlin source code and runs Gradle (`7.5`). *(Note for ARM64 Windows 11 laptops: use the `x64` JDK so Android Gradle Plugin `7.4.2` resolves `windows-x64` `aapt2` cleanly via Windows Prism emulation.)* |
| **Gradle Wrapper (`gradlew`)** | **`7.5`** (`gradle/wrapper/gradle-wrapper.properties`) | Orchestrates all `1,249` build, packaging, and test tasks automatically. |
| **Android Gradle Plugin (AGP)** | **`7.4.2`** (`build.gradle`) | Compiles Android resources, merges manifests, splits 32/64-bit ABIs, and signs `.apk` files. |
| **Kotlin Gradle Plugin** | **`1.8.10`** | Compiles Kotlin helper modules alongside Java 8 bytecode (`sourceCompatibility 1.8`). |
| **Android SDK Platform** | **API 34 (`compileSdkVersion 34`)** | Android 14 SDK headers used at compile time (`minSdkVersion 21` / `targetSdkVersion 27` for Fire OS Leanback compatibility). |
| **Android SDK Build-Tools** | **`30.0.3`** (`apksigner`, `zipalign`, `d8`) | Packages DEX bytecode and verifies `v1` + `v2` cryptographic `.apk` signatures. |
| **Android Platform-Tools (`adb`)** | **Latest (`adb.exe`)** | Connects wirelessly to Fire TV Sticks over TCP port `5555` (`deploy_kidtube_to_firestick.ps1`). |
| **PowerShell** | **Windows PowerShell `5.1+` / `7+`** | Runs `deploy_kidtube_to_firestick.ps1` batched 32-socket Wi-Fi discovery and wireless installer. |

---

## 2. Git Submodules (Fetched Automatically via `git clone --recurse-submodules`)

Defined in [`.gitmodules`](.gitmodules):

| Submodule Folder | Upstream Repository | Role in Kidtube |
| :--- | :--- | :--- |
| **`SharedModules/`** | `https://github.com/yuliskov/SharedModules` | Shared utilities (`:sharedutils`), version constants (`constants.gradle`), and update checker (`:appupdatechecker2`). |
| **`MediaServiceCore/`** | `https://github.com/yuliskov/MediaServiceCore` | YouTube Innertube API client (`:youtubeapi`) and media interfaces (`:mediaserviceinterfaces`) powering search, RSS channel hubs, and `yt.be/activate` sign-in. |

---

## 3. Local Embedded Gradle Modules (`settings.gradle`)

| Module | Path | Role |
| :--- | :--- | :--- |
| **`:smarttubetv`** | `smarttubetv/` | Top-level Android TV / Fire OS application module defining the **`stkidtube`** product flavor (`org.kidtube.tv`), launcher banners, and ABI splits (`armeabi-v7a`, `arm64-v8a`, `x86`). |
| **`:common`** | `common/` | Core UI presenters, playback controllers, and the **`com.liskovsoft.smartyoutubetv2.common.kidtube`** engine (`KidtubeCatalog`, `KidtubeClassifier`, `KidtubeLedger`, `KidtubeBrowseProcessor`, `KidtubeManager`). |
| **`:leanbackassistant`** | `leanbackassistant/` | Android TV home-screen channel & search integration (namespaced for `stkidtube`). |
| **`:exoplayer-amzn-2.10.6`** | `exoplayer-amzn-2.10.6/` | Amazon Fire OS-optimized port of **ExoPlayer `2.10.6`** (`:exoplayer-library`, `:exoplayer-extension-okhttp`, `:exoplayer-extension-cronet`) for hardware-accelerated 4K/60fps/HDR playback on Fire TV Sticks. |
| **`:leanback-1.0.0`** & **`:fragment-1.1.0`** | `leanback-1.0.0/`, `fragment-1.1.0/` | Custom TV-remote D-pad focus & Leanback grid patches replacing stock AndroidX Leanback/Fragment artifacts. |
| **`:chatkit`**, **`:filepicker-lib`**, **`:doubletapplayerview`**, **`:slidableactivity`** | Local module folders | Live chat rendering, local backup picker, player overlay views, and activity transitions. |

---

## 4. External Libraries (Auto-Downloaded by Gradle from Maven Central / Google / JitPack)

All versions below are pinned centrally in `SharedModules/constants.gradle` and resolved in `build.gradle`, `common/build.gradle`, and `smarttubetv/build.gradle`:

### A. Android TV & UI Layer
| Maven Artifact | Version | Purpose |
| :--- | :--- | :--- |
| `androidx.leanback:leanback-preference` | `1.0.0` | TV-friendly settings menus and preference panels |
| `androidx.recommendation:recommendation` | `1.0.0` | Android TV / Fire OS home-row recommendations |
| `androidx.appcompat:appcompat` | `1.2.0` | Core Android activity and theme compatibility |
| `androidx.recyclerview:recyclerview` | `1.1.0` | High-speed scrolling video card rows and grids |
| `androidx.constraintlayout:constraintlayout` | `1.1.2` | Responsive 10-foot TV screen layouts |
| `androidx.media:media` | `1.2.0` | Hardware media key & session integration (`Play/Pause`, `Rewind`, `FF`) |
| `androidx.browser:browser` | `1.0.0+` | Custom tabs / external browser fallback |
| `androidx.multidex:multidex` | `2.0.1` | Multi-DEX packaging for large method counts |

### B. Video Streaming, Networking & Reactive Concurrency
| Maven Artifact | Version | Purpose |
| :--- | :--- | :--- |
| `io.reactivex.rxjava2:rxjava` | `2.2.21` | Asynchronous reactive streams for non-blocking YouTube API & RSS fetches |
| `io.reactivex.rxjava2:rxandroid` | `2.1.1` | Main-thread Android UI scheduling for RxJava streams |
| `com.squareup.okhttp3:okhttp` | `3.12.13` | HTTP/2 & TLS client for video manifests and API requests |
| `org.chromium.net:cronet-api` | `105.5195.68` | Chromium QUIC/HTTP3 network stack acceleration for ExoPlayer |
| `com.google.code.gson:gson` | `2.8.2` | JSON parsing and serialization |
| `com.jayway.jsonpath:json-path` | `2.4.0` | Deep JSON extraction for YouTube API payloads |
| `dnsjava:dnsjava` | `2.1.9` | Resilient DNS resolution |

### C. Image Loading, Background Tasks & Runtime Utilities
| Maven Artifact | Version | Purpose |
| :--- | :--- | :--- |
| `com.github.bumptech.glide:glide` | `4.11.0` | Fast video thumbnail caching and memory-safe TV image rendering |
| `com.github.zjupure:webpdecoder` | `2.0.4.11.0` | Animated WebP thumbnail decoding for Glide |
| `androidx.work:work-runtime` | `2.x` | Resilient background task scheduling |
| `com.google.guava:guava` | Pinned via `constants.gradle` | Immutable collections and concurrency primitives |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7` | `1.8.10` | Kotlin standard library runtime |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.7.3` | Kotlin coroutines support |
| `androidx.core:core-ktx` / `androidx.core:core` | `1.12.0` / `1.9.0` | AndroidX core extensions |
| `com.jakewharton:process-phoenix` | Pinned via `constants.gradle` | Clean app process restart after backup/restore or profile switches |

### D. Automated Unit Testing (`:common:testStkidtubeDebugUnitTest`)
| Maven Artifact | Version | Purpose |
| :--- | :--- | :--- |
| `junit:junit` | `4.12` | Runs `KidtubeEngineTest.java` (catalog integrity, anti-clickbait classifier, and 10-minute block anti-scrub math) in `<0.1s` |
| `org.robolectric:robolectric` | `4.6.1` | Local JVM simulation of Android SDK classes without requiring a physical TV |
| `org.mockito:mockito-all` | `1.9.5` | Test mocking framework |
