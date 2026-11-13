# 🚀 Kidtube for Fire TV & Android TV (`org.kidtube.tv`)

<p align="center">
  <img src="smarttubetv/src/stkidtube/res/mipmap-nodpi/app_banner.png" alt="Kidtube Fire TV Banner" width="560" />
</p>

**Kidtube** is a custom, kid-focused educational television app built for **Amazon Fire TV Sticks** and **Android TV** devices (forked from [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube) under the `stkidtube` product flavor).

Instead of locking kids into babyish apps or handing them an unfiltered algorithm of YouTube Shorts and endless gaming streams, **Kidtube** rewards real curiosity across **Engineering, History, Science, and Math** — turning verified learning time into earned Free-Choice blocks automatically right on the TV.

---

## ✨ Core Features

- **⚡ 10-Minute Earned Time-Bank (`KidtubeLedger`):**
  - Every **10 minutes (`600s`)** of verified educational playback automatically unlocks a **+10-minute (`600s`) Free-Choice block** (up to a 60-minute bank cap) that can be spent on gaming/entertainment videos like Minecraft.
  - Persistent across app closes and TV power cycles via `SharedPreferences` (`kidtube_ledger_v1`).
- **🛡️ Two-Clock Anti-Scrubbing Verification:**
  - Compares monotonic **wall-clock time** against **ExoPlayer seekbar progression** every second (`200ms <= posDeltaMs <= 3500ms`). Fast-forwarding, scrubbing the seekbar ahead, or leaving a video paused/buffering earns zero credit, while genuine `1x–2x` watching is credited accurately.
- **🧠 Anti-Clickbait Two-Layer Classifier (`KidtubeClassifier`):**
  - Keeps **open-curiosity search** alive (*"sky is the limit, as long as he learns something"*) with **`+TIME`** badges on learning cards, while evaluating explicit gaming/brain-rot traps *before* subject keywords so clickbait like `"Minecraft WW2 D-Day Gameplay!"` can never spoof the history filter.
- **🚫 Zero YouTube Shorts & Zero Brain-Rot Tabs:**
  - Strips `isShorts` at both the YouTube Innertube API layer and the UI card processor (`KidtubeBrowseProcessor`), and hides *Shorts*, *Gaming*, *Trending*, *Sports*, *Live*, and *News* sidebar tabs.
- **🚀 Boots Straight to "Ready to Roll" + 4 Live RSS Subject Hubs:**
  - **52 Curated Ready-to-Roll Episodes** + a 1-click **`🎲 Surprise Me! Roll a Learning Video`** shuffler.
  - **4 Live YouTube RSS Subject Hubs (37 Verified `UC...` Channels — Works Even Without Sign-In):**
    1. **Mark Rober & Mega Builds** (*Mark Rober, Stuff Made Here, Jared Owen, Mustard, Branch Education, Real Engineering, Practical Engineering, Hacksmith, colinfurze*)
    2. **WWII & Epic History** (*OverSimplified, Yarnhub, Simple History, The Armchair Historian, History Matters, Extra History, Kings and Generals, Historia Civilis, Epic History, Weird History*)
    3. **Science & Space** (*Kurzgesagt, Veritasium, SmarterEveryDay, NileRed, Steve Mould, The Action Lab, PBS Eons, SciShow, Be Smart, Cleo Abram, ElectroBOOM*)
    4. **Math, Riddles & Logic** (*3Blue1Brown, Numberphile, Stand-up Maths, TED-Ed, Vsauce, Primer, MindYourDecisions*)
- **🔑 Numberless-Remote D-Pad PIN Pad & Parent Dashboard:**
  - Designed specifically for Amazon Fire TV remotes (which have no `0–9` number buttons) using a masked `[ ●  ●  ○  ○ ]` vertical D-pad digit picker.
  - Lets parents grant `+10m` blocks on the spot, one-click whitelist new educational channels, pin favorite episodes, block unwanted channels, or toggle between **10-Min Earned Blocks**, **Strict Learning-Only**, and **Temporary Parent Pass** (which automatically resets on reboot).
- **📦 Separate App Identity (`org.kidtube.tv`):**
  - Coexists cleanly alongside other TV apps with its own icon, launcher banner, and isolated storage, with upstream SmartTube auto-update prompts disabled.

---

## 🗂️ Repository Architecture

All Kidtube logic is cleanly modularized inside [`common/src/main/java/com/liskovsoft/smartyoutubetv2/common/kidtube/`](common/src/main/java/com/liskovsoft/smartyoutubetv2/common/kidtube/) with minimal surgical hooks into SmartTube's presenters and playback controllers:

| File | Responsibility |
| :--- | :--- |
| [`KidtubeCatalog.java`](common/src/main/java/com/liskovsoft/smartyoutubetv2/common/kidtube/KidtubeCatalog.java) | 37 verified `UC...` educational channel IDs across 4 Subject Hubs + 52 Ready-to-Roll video IDs |
| [`KidtubeClassifier.java`](common/src/main/java/com/liskovsoft/smartyoutubetv2/common/kidtube/KidtubeClassifier.java) | 8-step deterministic classifier (`BLOCKED`, `EDUCATIONAL`, `ENTERTAINMENT`) + anti-spoofing guards |
| [`KidtubeLedger.java`](common/src/main/java/com/liskovsoft/smartyoutubetv2/common/kidtube/KidtubeLedger.java) | Persistent `600s` time-bank ledger, anti-scrub playback verification, modes, and Parent PIN |
| [`KidtubeBrowseProcessor.java`](common/src/main/java/com/liskovsoft/smartyoutubetv2/common/kidtube/KidtubeBrowseProcessor.java) | Strips YouTube Shorts and blocked channels from all rows/search and stamps `+TIME` badges |
| [`KidtubeManager.java`](common/src/main/java/com/liskovsoft/smartyoutubetv2/common/kidtube/KidtubeManager.java) | Seeds Subject Hubs, enforces two-stage playback gating, runs the 1s ticker, and renders TV dialogs |
| [`KidtubeEngineTest.java`](common/src/test/java/com/liskovsoft/smartyoutubetv2/common/kidtube/KidtubeEngineTest.java) | Automated JUnit 4 suite verifying catalog integrity, clickbait defense, and anti-cheat time math |
| [`deploy_kidtube_to_firestick.ps1`](deploy_kidtube_to_firestick.ps1) | Wireless Wi-Fi scanner (batched 32-socket TCP `5555` discovery) & 1-command Fire Stick ADB installer |
| [`DEPENDENCIES.md`](DEPENDENCIES.md) | Complete manifest of host build tools, Git submodules, embedded modules, and Maven libraries |

---

## 🛠️ Building, Testing & Deploying to Fire TV

See **[`DEPENDENCIES.md`](DEPENDENCIES.md)** for the complete list of required build tools and library versions.

### 1. Clone with Submodules
```bash
git clone --recurse-submodules -b kidtube-v1 https://github.com/warnermeredith-glitch/SmartTube.git
cd SmartTube
```

### 2. Run the Automated Unit Test Suite
```powershell
.\gradlew.bat :common:testStkidtubeDebugUnitTest
```

### 3. Build the Universal Fire TV `.apk` (`32-bit` + `64-bit` ARM)
*(Optional: create a local, git-ignored `keystore.properties` pointing to your `.jks` signing key for release signing.)*
```powershell
.\gradlew.bat :smarttubetv:assembleStkidtubeRelease
```
Built `.apk` outputs are written to `smarttubetv/build/outputs/apk/stkidtube/release/`.

### 4. Wireless Push to Any Fire TV Stick on Your Home Wi-Fi
1. On your Fire TV Stick, go to **Settings → My Fire TV → About**, click **Fire TV Stick** 7 times to unlock **Developer Options**, then turn **ADB Debugging ON**.
2. Run the wireless installer from PowerShell:
```powershell
powershell -ExecutionPolicy Bypass -File .\deploy_kidtube_to_firestick.ps1
```
*(Or target a specific Fire Stick IP directly with `-FireStickIp 192.168.1.144`.)*

---

## 📜 Credits & License

- Built upon the open-source **[SmartTube](https://github.com/yuliskov/SmartTube)** Android TV client by [@yuliskov](https://github.com/yuliskov) and contributors.
- Licensed under the **[GNU General Public License v3.0 (GPLv3)](LICENSE)**.
