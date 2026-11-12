package com.liskovsoft.smartyoutubetv2.common.kidtube;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.liskovsoft.mediaserviceinterfaces.data.ItemGroup;
import com.liskovsoft.mediaserviceinterfaces.data.ItemGroup.Item;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Playlist;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.State;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.providers.channelgroup.ChannelGroupServiceWrapper;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.service.SidebarService;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central orchestrator for Kidtube:
 * - First-boot Subject Hub seeding & Shorts/Gaming elimination
 * - Ready-to-Roll playlist & "Surprise Me!" random learning shuffler
 * - Two-stage playback gating & 10-minute earned Free-Choice block ticker
 * - Numberless-remote D-pad PIN Pad (default 1414) & Parent Controls Dashboard
 */
public final class KidtubeManager {
    public static final String KIDTUBE_PACKAGE = "org.kidtube.tv";

    private static final int DIALOG_ID_KIDTUBE_LOCK = 91414;
    private static final int DIALOG_ID_KIDTUBE_PIN = 91415;
    private static final int DIALOG_ID_KIDTUBE_PARENT = 91416;

    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static String sLastAnnouncedFreeVideoId;

    private KidtubeManager() {
    }

    public static boolean isKidtubeActive(Context context) {
        if (context == null) {
            return false;
        }
        String pkg = context.getPackageName();
        return KIDTUBE_PACKAGE.equals(pkg) || (pkg != null && pkg.startsWith(KIDTUBE_PACKAGE));
    }

    /**
     * Runs once on app startup (SplashPresenter) and whenever SidebarService initializes.
     */
    public static void bootstrapIfNeeded(Context context) {
        if (!isKidtubeActive(context)) {
            return;
        }

        Context appContext = context.getApplicationContext();

        // Keep SmartTube's built-in Child Mode OFF so legitimate WWII / Science videos are never restricted
        // and the player's Next button continues working normally.
        GeneralData.instance(appContext).setChildModeEnabled(false);

        // Enforce YouTube Shorts suppression across all API endpoints
        MediaServiceData.instance().setContentHidden(MediaServiceData.CONTENT_SHORTS_ALL, true);

        KidtubeLedger ledger = KidtubeLedger.instance(appContext);
        if (!ledger.isBootstrapped()) {
            ledger.markBootstrapped();
            syncSubjectHubsAndSidebar(appContext);
        }
    }

    /**
     * Seeds or refreshes the 4 Subject Hubs (Mark Rober & Mega Builds, WWII & Epic History,
     * Science & Space, Math & Logic) as live RSS Channel Groups and configures the Kidtube sidebar.
     */
    public static void syncSubjectHubsAndSidebar(Context context) {
        if (context == null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        ChannelGroupServiceWrapper groupService = ChannelGroupServiceWrapper.instance(appContext);

        Map<String, List<Item>> hubChannels = new LinkedHashMap<>();
        hubChannels.put(KidtubeCatalog.HUB_ENGINEERING, new ArrayList<>());
        hubChannels.put(KidtubeCatalog.HUB_HISTORY, new ArrayList<>());
        hubChannels.put(KidtubeCatalog.HUB_SCIENCE, new ArrayList<>());
        hubChannels.put(KidtubeCatalog.HUB_MATH, new ArrayList<>());

        Map<String, String> hubIcons = new LinkedHashMap<>();
        hubIcons.put(KidtubeCatalog.HUB_ENGINEERING, "https://i.ytimg.com/vi/hFZFjoX2cGg/hqdefault.jpg");
        hubIcons.put(KidtubeCatalog.HUB_HISTORY, "https://i.ytimg.com/vi/_uk_6vfqwTA/hqdefault.jpg");
        hubIcons.put(KidtubeCatalog.HUB_SCIENCE, "https://i.ytimg.com/vi/pXypQ6d_iDg/hqdefault.jpg");
        hubIcons.put(KidtubeCatalog.HUB_MATH, "https://i.ytimg.com/vi/Nbwv5wHQoj0/hqdefault.jpg");

        for (KidtubeCatalog.ChannelEntry entry : KidtubeCatalog.WHITELISTED_CHANNELS) {
            List<Item> list = hubChannels.get(entry.hubName);
            if (list != null) {
                list.add(groupService.createChannel(entry.title, null, entry.channelId));
            }
        }

        List<Video> hubVideos = new ArrayList<>();
        for (Map.Entry<String, List<Item>> hubEntry : hubChannels.entrySet()) {
            String title = hubEntry.getKey();
            List<Item> items = hubEntry.getValue();

            ItemGroup existing = groupService.findChannelGroupByTitle(title);
            ItemGroup group;
            if (existing != null) {
                for (Item channelItem : items) {
                    existing.add(channelItem);
                }
                group = existing;
            } else {
                group = groupService.createChannelGroup(title, hubIcons.get(title), items);
            }
            groupService.addChannelGroup(group);
            hubVideos.add(Video.from(group));
        }

        SidebarService.instance(appContext).setupKidtubeSidebar(hubVideos);

        BrowsePresenter browsePresenter = BrowsePresenter.instance(context);
        if (browsePresenter.getView() != null) {
            browsePresenter.updateSections();
        }
    }

    /**
     * Builds the local Video card grid for the "Ready to Roll" boot section.
     */
    public static List<Video> getReadyToRollItems(Context context) {
        if (context == null) {
            return Collections.emptyList();
        }

        KidtubeLedger ledger = KidtubeLedger.instance(context);
        VideoStateService stateService = VideoStateService.instance(context);
        List<Video> result = new ArrayList<>();

        // Card 1: 1-Click Random Learning Shuffler
        Video rollCard = new Video();
        rollCard.videoId = KidtubeCatalog.SPECIAL_VIDEO_ROLL;
        rollCard.title = "🎲 Surprise Me! Roll a Learning Video";
        rollCard.secondTitle = "Shuffles Mark Rober, WWII History, Science & Math";
        rollCard.author = "Kidtube Ready-to-Roll";
        rollCard.badge = "SHUFFLE";
        rollCard.cardImageUrl = "https://i.ytimg.com/vi/hFZFjoX2cGg/hqdefault.jpg";
        rollCard.bgImageUrl = "https://i.ytimg.com/vi/hFZFjoX2cGg/maxresdefault.jpg";
        result.add(rollCard);

        // Card 2: Live Time-Bank Status & Parent Controls Entry
        Video statusCard = new Video();
        statusCard.videoId = KidtubeCatalog.SPECIAL_VIDEO_STATUS;
        statusCard.title = "⚡ Kidtube Time Bank & Parent Controls";
        statusCard.secondTitle = ledger.getCompactStatusSummary();
        statusCard.author = "Kidtube";
        statusCard.badge = "FREE: " + KidtubeLedger.formatMinutesSeconds(ledger.getEarnedFreeSeconds());
        statusCard.cardImageUrl = "https://i.ytimg.com/vi/_uk_6vfqwTA/hqdefault.jpg";
        statusCard.bgImageUrl = "https://i.ytimg.com/vi/_uk_6vfqwTA/maxresdefault.jpg";
        result.add(statusCard);

        // Parent-pinned custom Ready-to-Roll videos (newest first)
        for (KidtubeLedger.CustomEpisode custom : ledger.getCustomReadyEpisodes()) {
            Video v = new Video();
            v.videoId = custom.videoId;
            v.channelId = custom.channelId.isEmpty() ? null : custom.channelId;
            v.author = custom.author;
            v.title = custom.title;
            v.secondTitle = custom.author + " • Parent Pick";
            v.badge = "+TIME • PINNED";
            v.cardImageUrl = "https://i.ytimg.com/vi/" + custom.videoId + "/hqdefault.jpg";
            v.bgImageUrl = "https://i.ytimg.com/vi/" + custom.videoId + "/maxresdefault.jpg";
            State saved = stateService.getByVideoId(v.videoId);
            if (saved != null) {
                v.sync(saved);
            }
            result.add(v);
        }

        // Curated 52-episode History / Math / Science / Engineering Ready-to-Roll Catalog
        for (KidtubeCatalog.EpisodeEntry ep : KidtubeCatalog.READY_TO_ROLL_EPISODES) {
            Video v = buildVideoFromEpisode(ep, stateService);
            result.add(v);
        }

        return result;
    }

    private static Video buildVideoFromEpisode(KidtubeCatalog.EpisodeEntry ep, VideoStateService stateService) {
        Video v = new Video();
        v.videoId = ep.videoId;
        v.channelId = ep.channelId;
        v.author = ep.author;
        v.title = ep.title;
        v.secondTitle = ep.author + " • " + ep.topicBadge;
        v.badge = "+TIME • " + ep.topicBadge;
        v.cardImageUrl = ep.getThumbnailUrl();
        v.bgImageUrl = ep.getBackgroundUrl();
        if (stateService != null) {
            State saved = stateService.getByVideoId(ep.videoId);
            if (saved != null) {
                v.sync(saved);
            }
        }
        return v;
    }

    /**
     * Intercepts clicks on special Ready-to-Roll action cards.
     *
     * @return true if handled by KidtubeManager; false for normal video playback.
     */
    public static boolean handleSpecialVideoClick(Context context, Video item) {
        if (!isKidtubeActive(context) || item == null || item.videoId == null) {
            return false;
        }

        if (KidtubeCatalog.SPECIAL_VIDEO_ROLL.equals(item.videoId)) {
            rollRandomLearningPlaylist(context);
            return true;
        }

        if (KidtubeCatalog.SPECIAL_VIDEO_STATUS.equals(item.videoId)) {
            showStatusAndParentEntryDialog(context, null);
            return true;
        }

        return false;
    }

    /**
     * Shuffles unwatched Ready-to-Roll episodes, populates the playback queue, and plays immediately.
     */
    public static void rollRandomLearningPlaylist(Context context) {
        if (context == null) {
            return;
        }

        VideoStateService stateService = VideoStateService.instance(context);
        KidtubeLedger ledger = KidtubeLedger.instance(context);

        List<Video> unwatched = new ArrayList<>();
        List<Video> watched = new ArrayList<>();

        for (KidtubeLedger.CustomEpisode custom : ledger.getCustomReadyEpisodes()) {
            Video v = new Video();
            v.videoId = custom.videoId;
            v.channelId = custom.channelId.isEmpty() ? null : custom.channelId;
            v.author = custom.author;
            v.title = custom.title;
            v.secondTitle = custom.author;
            v.cardImageUrl = "https://i.ytimg.com/vi/" + custom.videoId + "/hqdefault.jpg";
            v.bgImageUrl = "https://i.ytimg.com/vi/" + custom.videoId + "/maxresdefault.jpg";
            State saved = stateService.getByVideoId(v.videoId);
            if (saved != null) {
                v.sync(saved);
            }
            if (v.percentWatched >= 85f) {
                watched.add(v);
            } else {
                unwatched.add(v);
            }
        }

        for (KidtubeCatalog.EpisodeEntry ep : KidtubeCatalog.READY_TO_ROLL_EPISODES) {
            Video v = buildVideoFromEpisode(ep, stateService);
            if (v.percentWatched >= 85f) {
                watched.add(v);
            } else {
                unwatched.add(v);
            }
        }

        Collections.shuffle(unwatched);
        Collections.shuffle(watched);

        List<Video> combined = new ArrayList<>(unwatched);
        combined.addAll(watched);

        if (combined.isEmpty()) {
            return;
        }

        int queueCount = Math.min(25, combined.size());
        List<Video> queueSlice = new ArrayList<>(combined.subList(0, queueCount));

        Playlist playlist = Playlist.instance();
        playlist.clear();
        playlist.addAll(queueSlice);

        Video first = queueSlice.get(0);
        first.fromQueue = true;

        MessageHelpers.showLongMessage(context, "🎲 Rolling: " + first.title);
        PlaybackPresenter.instance(context).openVideo(first);
    }

    /**
     * Pre-flight UI gate called inside PlaybackPresenter.openVideo(Video) BEFORE launching PlaybackActivity
     * when we already have confident metadata (e.g. Shorts, blocked channel, known channelId, or explicit gaming title).
     *
     * @return true if allowed to proceed to player (where processFormatInfo performs final server-verified check).
     */
    public static boolean canOpenVideoFromUi(Context context, Video video) {
        if (!isKidtubeActive(context) || video == null) {
            return true;
        }

        if (KidtubeCatalog.SPECIAL_VIDEO_ROLL.equals(video.videoId)
                || KidtubeCatalog.SPECIAL_VIDEO_STATUS.equals(video.videoId)) {
            handleSpecialVideoClick(context, video);
            return false;
        }

        if (video.isShorts) {
            MessageHelpers.showMessage(context, "YouTube Shorts are disabled in Kidtube.");
            return false;
        }

        KidtubeLedger ledger = KidtubeLedger.instance(context);
        KidtubeClassifier.ContentCategory category = classifyVideo(video, ledger, true);

        if (category == KidtubeClassifier.ContentCategory.BLOCKED) {
            showLockDialog(context, video);
            return false;
        }

        if (category == KidtubeClassifier.ContentCategory.EDUCATIONAL) {
            return true;
        }

        // If channelId is not yet populated on a Search card AND it does not have an explicit gaming keyword,
        // let VideoLoaderController.processFormatInfo inspect the server-returned channelId before deciding.
        boolean confidentMetadata = !TextUtils.isEmpty(video.channelId)
                || KidtubeClassifier.hasExplicitGamingMarker(video.getTitle(), video.getAuthor());

        if (confidentMetadata && !ledger.canStartPlayback(category)) {
            showLockDialog(context, video);
            return false;
        }

        return true;
    }

    /**
     * Authoritative server-metadata gate inside VideoLoaderController.processFormatInfo(MediaItemFormatInfo).
     * Runs before ExoPlayer opens DASH/SABR/HLS streams.
     *
     * @return true if stream should open; false if Kidtube blocked playback and displayed the Lock Dialog.
     */
    public static boolean checkFormatInfoAndGate(
            Context context,
            Video video,
            MediaItemFormatInfo formatInfo,
            PlaybackView player) {

        if (!isKidtubeActive(context) || video == null || formatInfo == null) {
            return true;
        }

        // Enrich Video immediately with authoritative server channelId, author, and title
        if (TextUtils.isEmpty(video.channelId) && !TextUtils.isEmpty(formatInfo.getChannelId())) {
            video.channelId = formatInfo.getChannelId();
        }
        if (TextUtils.isEmpty(video.author) && !TextUtils.isEmpty(formatInfo.getAuthor())) {
            video.author = formatInfo.getAuthor();
        }
        if (TextUtils.isEmpty(video.title) && !TextUtils.isEmpty(formatInfo.getTitle())) {
            video.title = formatInfo.getTitle();
        }

        KidtubeLedger ledger = KidtubeLedger.instance(context);
        KidtubeClassifier.ContentCategory category = KidtubeClassifier.classify(
                 !TextUtils.isEmpty(formatInfo.getVideoId()) ? formatInfo.getVideoId() : video.videoId,
                 !TextUtils.isEmpty(formatInfo.getChannelId()) ? formatInfo.getChannelId() : video.channelId,
                 !TextUtils.isEmpty(formatInfo.getAuthor()) ? formatInfo.getAuthor() : video.getAuthor(),
                 !TextUtils.isEmpty(formatInfo.getTitle()) ? formatInfo.getTitle() : video.getTitle(),
                ledger.getCustomWhitelistedChannelIds(),
                ledger.getCustomBlockedChannelIds(),
                ledger.getCustomReadyVideoIds(),
                false
        );

        ledger.resetPlaybackTracker();

        if (!ledger.canStartPlayback(category)) {
            if (player != null) {
                player.showProgressBar(false);
                player.setPlayWhenReady(false);
                player.setTitle("🎓 Learning Time Needed — Watch 10m to Unlock +10m Free Choice!");
                player.showOverlay(true);
            }
            sMainHandler.post(() -> showLockDialog(context, video));
            return false;
        }

        if (category == KidtubeClassifier.ContentCategory.ENTERTAINMENT
                && ledger.getEnforcementMode() == KidtubeLedger.MODE_EARN_BLOCKS
                && !TextUtils.equals(sLastAnnouncedFreeVideoId, video.videoId)) {
            sLastAnnouncedFreeVideoId = video.videoId;
            MessageHelpers.showLongMessage(
                    context,
                    "🎮 Free-Choice Time Active: "
                            + KidtubeLedger.formatMinutesSeconds(ledger.getEarnedFreeSeconds())
                            + " remaining"
            );
        }

        return true;
    }

    /**
     * Called once every second from VideoStateController while ExoPlayer is actively playing.
     */
    public static void onPlaybackTick(Context context, Video video, PlaybackView player) {
        if (!isKidtubeActive(context) || video == null || player == null || !player.isPlaying()) {
            return;
        }

        KidtubeLedger ledger = KidtubeLedger.instance(context);
        boolean allowAuthorFallback = TextUtils.isEmpty(video.channelId);
        KidtubeClassifier.ContentCategory category = classifyVideo(video, ledger, allowAuthorFallback);

        KidtubeLedger.TickEvent event = ledger.onVerifiedPlaybackTick(
                video.videoId,
                category,
                player.getPositionMs(),
                SystemClock.elapsedRealtime()
        );

        switch (event) {
            case HALFWAY_TO_BLOCK:
                MessageHelpers.showLongMessage(
                        context,
                        "⚡ Halfway there! 5:00 more learning unlocks a +10:00 Free-Choice Block!"
                );
                break;
            case BLOCK_UNLOCKED:
                MessageHelpers.showLongMessage(
                        context,
                        "🎉 +10:00 Free-Choice Block Unlocked! (Banked: "
                                + KidtubeLedger.formatMinutesSeconds(ledger.getEarnedFreeSeconds()) + ")"
                );
                break;
            case WARN_2_MIN_LEFT:
                MessageHelpers.showLongMessage(context, "⏳ 2:00 of Free-Choice time remaining");
                break;
            case WARN_30_SEC_LEFT:
                MessageHelpers.showLongMessage(context, "⏳ 30 seconds of Free-Choice time left!");
                break;
            case LOCK_REQUIRED:
                player.setPlayWhenReady(false);
                player.showOverlay(true);
                showLockDialog(context, video);
                break;
            case NONE:
            default:
                break;
        }
    }

    public static KidtubeClassifier.ContentCategory classifyVideo(
            Video video,
            KidtubeLedger ledger,
            boolean allowAuthorFallback) {
        if (video == null) {
            return KidtubeClassifier.ContentCategory.ENTERTAINMENT;
        }
        return KidtubeClassifier.classify(
                video.videoId,
                video.channelId,
                video.getAuthor(),
                video.getTitle(),
                ledger.getCustomWhitelistedChannelIds(),
                ledger.getCustomBlockedChannelIds(),
                ledger.getCustomReadyVideoIds(),
                allowAuthorFallback
        );
    }

    /**
     * Friendly TV Lock Dialog shown when an entertainment/Minecraft video is selected at 0:00 Free-Choice time
     * or when an earned 10-minute Free-Choice block finishes counting down.
     */
    public static void showLockDialog(Context context, Video blockedVideo) {
        if (context == null) {
            return;
        }

        KidtubeLedger ledger = KidtubeLedger.instance(context);
        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        if (dialog.isDialogShown() && dialog.getId() == DIALOG_ID_KIDTUBE_LOCK) {
            return;
        }

        String header;
        if (blockedVideo != null && classifyVideo(blockedVideo, ledger, true) == KidtubeClassifier.ContentCategory.BLOCKED) {
            header = "🚫 Channel Blocked by Parent";
        } else if (ledger.getEnforcementMode() == KidtubeLedger.MODE_LEARNING_ONLY) {
            header = "🎓 Learning-Only Mode Active — Pick a History, Math, or Science Video!";
        } else {
            String needed = KidtubeLedger.formatMinutesSeconds(ledger.getSecondsUntilNextBlock());
            String progress = KidtubeLedger.formatMinutesSeconds(ledger.getLearningProgressSeconds());
            header = String.format(
                    Locale.US,
                    "⚡ 0:00 Free-Choice Time  |  Watch %s more learning (%s / 10:00) for +10m!",
                    needed,
                    progress
            );
        }

        dialog.clearBackstack();
        dialog.setId(DIALOG_ID_KIDTUBE_LOCK);

        dialog.appendSingleButton(UiOptionItem.from(
                "🎲 Surprise Me! Roll a Random Learning Video",
                option -> {
                    dialog.closeDialog();
                    rollRandomLearningPlaylist(context);
                }
        ));

        dialog.appendSingleButton(UiOptionItem.from(
                "🚀 Open Ready-to-Roll Learning Playlist",
                option -> {
                    dialog.closeDialog();
                    PlaybackPresenter.instance(context).forceFinish();
                    BrowsePresenter.instance(context).selectSection(KidtubeCatalog.SECTION_READY_TO_ROLL);
                }
        ));

        dialog.appendSingleButton(UiOptionItem.from(
                "🔧 Browse Mark Rober & Mega Builds",
                option -> {
                    dialog.closeDialog();
                    PlaybackPresenter.instance(context).forceFinish();
                    selectHubSection(context, KidtubeCatalog.HUB_ENGINEERING);
                }
        ));

        dialog.appendSingleButton(UiOptionItem.from(
                "🪖 Browse WWII & Animated History",
                option -> {
                    dialog.closeDialog();
                    PlaybackPresenter.instance(context).forceFinish();
                    selectHubSection(context, KidtubeCatalog.HUB_HISTORY);
                }
        ));

        dialog.appendSingleButton(UiOptionItem.from(
                "🔬 Browse Science, Space & Math",
                option -> {
                    dialog.closeDialog();
                    PlaybackPresenter.instance(context).forceFinish();
                    selectHubSection(context, KidtubeCatalog.HUB_SCIENCE);
                }
        ));

        dialog.appendSingleButton(UiOptionItem.from(
                "🔑 Parent Controls (PIN)",
                option -> showPinDialog(context, "Enter Parent PIN", () -> showParentDashboardDialog(context, blockedVideo, true))
        ));

        dialog.showDialog(header);
    }

    private static void selectHubSection(Context context, String hubTitle) {
        ChannelGroupServiceWrapper groupService = ChannelGroupServiceWrapper.instance(context);
        ItemGroup group = groupService.findChannelGroupByTitle(hubTitle);
        if (group != null) {
            Video pinned = Video.from(group);
            BrowsePresenter.instance(context).selectSection(pinned.getId());
        } else {
            BrowsePresenter.instance(context).selectSection(KidtubeCatalog.SECTION_READY_TO_ROLL);
        }
    }

    /**
     * Public status summary dialog (no PIN needed to check progress; PIN required to open Parent Controls).
     */
    public static void showStatusAndParentEntryDialog(Context context, Video contextVideo) {
        if (context == null) {
            return;
        }

        KidtubeLedger ledger = KidtubeLedger.instance(context);
        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        dialog.clearBackstack();

        String title = String.format(
                Locale.US,
                "⚡ Free Choice: %s  |  Next +10m Block: %s / 10:00  |  Today Learned: %dm",
                KidtubeLedger.formatMinutesSeconds(ledger.getEarnedFreeSeconds()),
                KidtubeLedger.formatMinutesSeconds(ledger.getLearningProgressSeconds()),
                ledger.getTodayLearnedSeconds() / 60
        );

        dialog.appendSingleButton(UiOptionItem.from(
                "🎲 Roll a Random Learning Video",
                option -> {
                    dialog.closeDialog();
                    rollRandomLearningPlaylist(context);
                }
        ));

        dialog.appendSingleButton(UiOptionItem.from(
                "🔑 Parent Controls & Channel Whitelist (PIN)",
                option -> showPinDialog(context, "Enter Parent PIN", () -> showParentDashboardDialog(context, contextVideo, false))
        ));

        dialog.showDialog(title);
    }

    /**
     * Numberless-Remote D-Pad PIN Pad!
     * Designed specifically for Fire TV & Samsung remotes with no number buttons.
     */
    public static void showPinDialog(Context context, String actionLabel, Runnable onVerified) {
        showPinStep(context, actionLabel, "", onVerified);
    }

    private static void showPinStep(Context context, String actionLabel, String enteredSoFar, Runnable onVerified) {
        if (context == null) {
            return;
        }

        KidtubeLedger ledger = KidtubeLedger.instance(context);
        int targetLen = ledger.getParentPin().length();
        StringBuilder mask = new StringBuilder();
        for (int i = 0; i < targetLen; i++) {
            if (i > 0) {
                mask.append("  ");
            }
            mask.append(i < enteredSoFar.length() ? "●" : "○");
        }

        String header = "🔑 " + actionLabel + "   [ " + mask + " ]";
        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        dialog.clearBackstack();
        dialog.setId(DIALOG_ID_KIDTUBE_PIN);

        List<String> digits = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0");
        for (String digit : digits) {
            dialog.appendSingleButton(UiOptionItem.from(
                    "Digit  " + digit,
                    option -> {
                        String next = enteredSoFar + digit;
                        if (next.length() >= targetLen) {
                            if (ledger.verifyPin(next)) {
                                dialog.closeDialog();
                                sMainHandler.post(onVerified);
                            } else {
                                MessageHelpers.showMessage(context, "Incorrect PIN — try again.");
                                showPinStep(context, actionLabel, "", onVerified);
                            }
                        } else {
                            showPinStep(context, actionLabel, next, onVerified);
                        }
                    }
            ));
        }

        if (!enteredSoFar.isEmpty()) {
            dialog.appendSingleButton(UiOptionItem.from(
                    "⌫ Clear Digits",
                    option -> showPinStep(context, actionLabel, "", onVerified)
            ));
        }

        dialog.appendSingleButton(UiOptionItem.from(
                "✖ Cancel",
                option -> dialog.closeDialog()
        ));

        dialog.showDialog(header);
    }

    /**
     * PIN-verified Parent Dashboard for Dr. Warner.
     */
    public static void showParentDashboardDialog(Context context, Video contextVideo, boolean openedFromLock) {
        if (context == null) {
            return;
        }

        KidtubeLedger ledger = KidtubeLedger.instance(context);
        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        dialog.clearBackstack();
        dialog.setId(DIALOG_ID_KIDTUBE_PARENT);

        String modeLabel = getModeLabel(ledger.getEnforcementMode());
        String header = String.format(
                Locale.US,
                "🎓 Kidtube Parent Controls — Free: %s | Today: %dm Learned / %dm Free | %s",
                KidtubeLedger.formatMinutesSeconds(ledger.getEarnedFreeSeconds()),
                ledger.getTodayLearnedSeconds() / 60,
                ledger.getTodayFreeSeconds() / 60,
                modeLabel
        );

        // 1. Grant +10m Bonus Block
        dialog.appendSingleButton(UiOptionItem.from(
                "➕ Grant +10 Minute Free-Choice Block Now",
                option -> {
                    ledger.grantBonusBlock();
                    MessageHelpers.showLongMessage(
                            context,
                            "Granted +10:00! Banked Free Choice: "
                                    + KidtubeLedger.formatMinutesSeconds(ledger.getEarnedFreeSeconds())
                    );
                    dialog.closeDialog();
                    refreshReadyToRollIfVisible(context);
                    if (openedFromLock && contextVideo != null) {
                        resumeOrOpenUnlockedVideo(context, contextVideo);
                    }
                }
        ));

        // 2. Context-specific video/channel actions when opened from a video card or Lock Dialog
        if (contextVideo != null && (contextVideo.hasVideo() || contextVideo.hasChannel())) {
            String displayAuthor = !TextUtils.isEmpty(contextVideo.getAuthor())
                    ? contextVideo.getAuthor()
                    : (!TextUtils.isEmpty(contextVideo.title) ? contextVideo.title : "This Channel");

            dialog.appendSingleButton(UiOptionItem.from(
                    "✅ Always Allow Channel as Learning (+TIME): " + displayAuthor,
                    option -> {
                        dialog.closeDialog();
                        whitelistChannelFromVideo(context, contextVideo, openedFromLock);
                    }
            ));

            if (contextVideo.hasVideo()
                    && !KidtubeCatalog.SPECIAL_VIDEO_ROLL.equals(contextVideo.videoId)
                    && !KidtubeCatalog.SPECIAL_VIDEO_STATUS.equals(contextVideo.videoId)) {
                dialog.appendSingleButton(UiOptionItem.from(
                        "📌 Pin This Video to Ready-to-Roll Playlist",
                        option -> {
                            dialog.closeDialog();
                            pinVideoToReadyToRoll(context, contextVideo, openedFromLock);
                        }
                ));
            }

            dialog.appendSingleButton(UiOptionItem.from(
                    "🚫 Block Channel Completely: " + displayAuthor,
                    option -> {
                        dialog.closeDialog();
                        blockChannelFromVideo(context, contextVideo);
                    }
            ));
        }

        // 3. Enforcement Mode Switcher
        dialog.appendSingleButton(UiOptionItem.from(
                "⚙️ Mode: 10-Min Earned Blocks (Default)",
                option -> {
                    ledger.setEnforcementMode(KidtubeLedger.MODE_EARN_BLOCKS);
                    MessageHelpers.showMessage(context, "Mode set: 10-Minute Earned Blocks");
                    dialog.closeDialog();
                    refreshReadyToRollIfVisible(context);
                }
        ));

        dialog.appendSingleButton(UiOptionItem.from(
                "🔒 Mode: Strict Learning-Only (No Gaming/Free-Choice)",
                option -> {
                    ledger.setEnforcementMode(KidtubeLedger.MODE_LEARNING_ONLY);
                    MessageHelpers.showMessage(context, "Mode set: Strict Learning-Only");
                    dialog.closeDialog();
                    refreshReadyToRollIfVisible(context);
                }
        ));

        dialog.appendSingleButton(UiOptionItem.from(
                "🍿 Mode: Parent Pass Unlocked (Until App Restart)",
                option -> {
                    ledger.setEnforcementMode(KidtubeLedger.MODE_PARENT_PASS);
                    MessageHelpers.showLongMessage(context, "Parent Pass active (resets automatically on next restart).");
                    dialog.closeDialog();
                    refreshReadyToRollIfVisible(context);
                    if (openedFromLock && contextVideo != null) {
                        resumeOrOpenUnlockedVideo(context, contextVideo);
                    }
                }
        ));

        // 4. Reset Free-Choice Bank to 0:00
        dialog.appendSingleButton(UiOptionItem.from(
                "🧹 Reset Banked Free-Choice Time to 0:00",
                option -> {
                    ledger.clearFreeTimeBank();
                    MessageHelpers.showMessage(context, "Free-Choice time bank reset to 0:00.");
                    dialog.closeDialog();
                    refreshReadyToRollIfVisible(context);
                }
        ));

        // 5. Restore / Re-sync Kidtube Sidebar & 4 Subject Hubs
        dialog.appendSingleButton(UiOptionItem.from(
                "🔄 Restore Kidtube Sidebar & 4 Subject Hubs",
                option -> {
                    dialog.closeDialog();
                    MediaServiceData.instance().setContentHidden(MediaServiceData.CONTENT_SHORTS_ALL, true);
                    syncSubjectHubsAndSidebar(context);
                    MessageHelpers.showLongMessage(context, "Kidtube sidebar & 4 Subject Hubs synced!");
                }
        ));

        // 6. Change 4-Digit Parent PIN
        dialog.appendSingleButton(UiOptionItem.from(
                "🔢 Change 4-Digit Parent PIN (Current: " + ledger.getParentPin() + ")",
                option -> showNewPinCreator(context, "")
        ));

        dialog.showDialog(header);
    }

    private static void showNewPinCreator(Context context, String digitsSoFar) {
        StringBuilder mask = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                mask.append("  ");
            }
            mask.append(i < digitsSoFar.length() ? digitsSoFar.charAt(i) : '_');
        }

        AppDialogPresenter dialog = AppDialogPresenter.instance(context);
        dialog.clearBackstack();

        for (String digit : Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")) {
            dialog.appendSingleButton(UiOptionItem.from(
                    "Digit  " + digit,
                    option -> {
                        String next = digitsSoFar + digit;
                        if (next.length() >= 4) {
                            KidtubeLedger.instance(context).setParentPin(next);
                            dialog.closeDialog();
                            MessageHelpers.showLongMessage(context, "New Kidtube Parent PIN saved: " + next);
                        } else {
                            showNewPinCreator(context, next);
                        }
                    }
            ));
        }

        dialog.appendSingleButton(UiOptionItem.from(
                "✖ Cancel",
                option -> dialog.closeDialog()
        ));

        dialog.showDialog("🔢 Choose New 4-Digit PIN:   [ " + mask + " ]");
    }

    public static void whitelistChannelFromVideo(Context context, Video video, boolean playImmediately) {
        if (context == null || video == null) {
            return;
        }
        Runnable applyWhitelist = () -> {
            if (!TextUtils.isEmpty(video.channelId)) {
                KidtubeLedger.instance(context).whitelistChannel(video.channelId);
                String name = !TextUtils.isEmpty(video.getAuthor()) ? video.getAuthor() : video.channelId;
                MessageHelpers.showLongMessage(context, "✅ Whitelisted as Learning Channel: " + name);
                if (playImmediately && video.hasVideo()) {
                    resumeOrOpenUnlockedVideo(context, video);
                }
            } else {
                MessageHelpers.showMessage(context, "Could not resolve Channel ID for this item.");
            }
        };

        if (!TextUtils.isEmpty(video.channelId)) {
            applyWhitelist.run();
        } else {
            MessageHelpers.showMessage(context, "Loading channel details...");
            MediaServiceManager.instance().loadMetadata(video, metadata -> {
                video.sync(metadata);
                applyWhitelist.run();
            });
        }
    }

    public static void pinVideoToReadyToRoll(Context context, Video video, boolean playImmediately) {
        if (context == null || video == null || TextUtils.isEmpty(video.videoId)) {
            return;
        }
        Runnable savePin = () -> {
            String author = !TextUtils.isEmpty(video.getAuthor()) ? video.getAuthor() : "Learning Pick";
            String title = !TextUtils.isEmpty(video.getTitle()) ? video.getTitle() : video.videoId;
            KidtubeLedger.instance(context).addCustomReadyEpisode(video.videoId, video.channelId, author, title);
            MessageHelpers.showLongMessage(context, "📌 Pinned to Ready-to-Roll: " + title);
            refreshReadyToRollIfVisible(context);
            if (playImmediately) {
                resumeOrOpenUnlockedVideo(context, video);
            }
        };

        if (!TextUtils.isEmpty(video.channelId) && !TextUtils.isEmpty(video.getAuthor())) {
            savePin.run();
        } else {
            MediaServiceManager.instance().loadMetadata(video, metadata -> {
                video.sync(metadata);
                savePin.run();
            });
        }
    }

    public static void blockChannelFromVideo(Context context, Video video) {
        if (context == null || video == null) {
            return;
        }
        Runnable applyBlock = () -> {
            if (!TextUtils.isEmpty(video.channelId)) {
                KidtubeLedger.instance(context).blockChannel(video.channelId);
                String name = !TextUtils.isEmpty(video.getAuthor()) ? video.getAuthor() : video.channelId;
                MessageHelpers.showLongMessage(context, "🚫 Blocked Channel: " + name);
                PlaybackPresenter.instance(context).forceFinish();
                if (BrowsePresenter.instance(context).getView() != null) {
                    BrowsePresenter.instance(context).refresh(false);
                }
            }
        };

        if (!TextUtils.isEmpty(video.channelId)) {
            applyBlock.run();
        } else {
            MediaServiceManager.instance().loadMetadata(video, metadata -> {
                video.sync(metadata);
                applyBlock.run();
            });
        }
    }

    private static void resumeOrOpenUnlockedVideo(Context context, Video video) {
        PlaybackPresenter playback = PlaybackPresenter.instance(context);
        PlaybackView player = playback.getPlayer();
        if (player != null && player.isEngineInitialized() && Video.equals(player.getVideo(), video) && player.containsMedia()) {
            player.showOverlay(false);
            player.setPlayWhenReady(true);
        } else {
            playback.openVideo(video);
        }
    }

    private static void refreshReadyToRollIfVisible(Context context) {
        BrowsePresenter browse = BrowsePresenter.instance(context);
        if (browse.getView() != null
                && browse.getCurrentSection() != null
                && browse.getCurrentSection().getId() == KidtubeCatalog.SECTION_READY_TO_ROLL) {
            browse.refresh(false);
        }
    }

    private static String getModeLabel(int mode) {
        switch (mode) {
            case KidtubeLedger.MODE_LEARNING_ONLY:
                return "Strict Learning-Only";
            case KidtubeLedger.MODE_PARENT_PASS:
                return "Parent Pass Unlocked";
            case KidtubeLedger.MODE_EARN_BLOCKS:
            default:
                return "10m Earned Blocks";
        }
    }
}
