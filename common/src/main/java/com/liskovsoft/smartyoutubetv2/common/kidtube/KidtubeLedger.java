package com.liskovsoft.smartyoutubetv2.common.kidtube;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Persistent 10-minute block Time-Bank & Anti-Cheat Ledger for Kidtube.
 *
 * Rules configured for Dr. Warner:
 * - Every 10 minutes (600 verified playback seconds) of History/Math/Science/Engineering
 *   unlocks a +10-minute (600s) Free-Choice block (up to 60 minutes banked max).
 * - Fast-forwarding / scrubbing seekbar jumps are ignored by anti-cheat.
 * - Default Parent PIN: 1414.
 */
public class KidtubeLedger {
    public static final String PREFS_NAME = "kidtube_ledger_v1";
    public static final String DEFAULT_PIN = "1414";

    public static final int BLOCK_SECONDS = 10 * 60;          // 10-minute blocks (600s)
    public static final int HALF_BLOCK_SECONDS = 5 * 60;      // 5-minute halfway milestone (300s)
    public static final int MAX_BANKED_FREE_SECONDS = 60 * 60; // Cap banked free time at 60 mins

    public static final int MODE_EARN_BLOCKS = 0;    // Default: 10m Learning -> +10m Free Choice
    public static final int MODE_LEARNING_ONLY = 1;  // Strict: Only Learning videos play
    public static final int MODE_PARENT_PASS = 2;    // Temporary unlocked session (e.g. movie night)

    public enum TickEvent {
        NONE,
        HALFWAY_TO_BLOCK,
        BLOCK_UNLOCKED,
        WARN_2_MIN_LEFT,
        WARN_30_SEC_LEFT,
        LOCK_REQUIRED
    }

    public static final class CustomEpisode {
        public final String videoId;
        public final String channelId;
        public final String author;
        public final String title;

        public CustomEpisode(String videoId, String channelId, String author, String title) {
            this.videoId = videoId != null ? videoId : "";
            this.channelId = channelId != null ? channelId : "";
            this.author = author != null ? author.replace("\t", " ").replace("\n", " ") : "";
            this.title = title != null ? title.replace("\t", " ").replace("\n", " ") : "";
        }

        public String serialize() {
            return videoId + "\t" + channelId + "\t" + author + "\t" + title;
        }

        public static CustomEpisode deserialize(String line) {
            if (line == null || line.isEmpty()) {
                return null;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 4 || parts[0].isEmpty()) {
                return null;
            }
            return new CustomEpisode(parts[0], parts[1], parts[2], parts[3]);
        }
    }

    private static final String KEY_EARNED_FREE_SEC = "earned_free_sec";
    private static final String KEY_LEARNING_PROGRESS_SEC = "learning_progress_sec";
    private static final String KEY_ENFORCEMENT_MODE = "enforcement_mode";
    private static final String KEY_PARENT_PIN = "parent_pin";
    private static final String KEY_BOOTSTRAPPED = "kidtube_bootstrapped_v1";
    private static final String KEY_STATS_DATE = "stats_date";
    private static final String KEY_TODAY_LEARNED_SEC = "today_learned_sec";
    private static final String KEY_TODAY_FREE_SEC = "today_free_sec";
    private static final String KEY_WHITELIST_CHANNELS = "custom_whitelist_channels";
    private static final String KEY_WHITELIST_NAMES = "custom_whitelist_names";
    private static final String KEY_BLOCKED_CHANNELS = "custom_blocked_channels";
    private static final String KEY_CUSTOM_EPISODES = "custom_ready_episodes";

    private static KidtubeLedger sInstance;

    private final SharedPreferences mPrefs; // null in pure unit tests

    private int mEarnedFreeSeconds;
    private int mLearningProgressSeconds;
    private int mEnforcementMode = MODE_EARN_BLOCKS;
    private String mParentPin = DEFAULT_PIN;
    private boolean mBootstrapped;
    private String mStatsDate;
    private int mTodayLearnedSeconds;
    private int mTodayFreeSeconds;

    private final Set<String> mCustomWhitelistedChannelIds = new LinkedHashSet<>();
    private final Set<String> mCustomBlockedChannelIds = new LinkedHashSet<>();
    private final List<CustomEpisode> mCustomEpisodes = new ArrayList<>();

    // Anti-cheat playback tracking state
    private String mActiveVideoId;
    private long mLastWallClockMs = -1L;
    private long mLastPlayerPosMs = -1L;

    public static synchronized KidtubeLedger instance(Context context) {
        if (sInstance == null) {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            sInstance = new KidtubeLedger(prefs);
        }
        return sInstance;
    }

    /**
     * Package-private constructor for deterministic unit testing when prefs is null.
     */
    KidtubeLedger(SharedPreferences prefs) {
        mPrefs = prefs;
        mStatsDate = currentDateString();
        if (mPrefs != null) {
            loadFromPrefs();
        }
    }

    private void loadFromPrefs() {
        mEarnedFreeSeconds = Math.max(0, mPrefs.getInt(KEY_EARNED_FREE_SEC, 0));
        mLearningProgressSeconds = Math.max(0, mPrefs.getInt(KEY_LEARNING_PROGRESS_SEC, 0)) % BLOCK_SECONDS;
        mEnforcementMode = mPrefs.getInt(KEY_ENFORCEMENT_MODE, MODE_EARN_BLOCKS);
        // Reset temporary parent pass across full app restarts so it doesn't stay unlocked forever by accident
        if (mEnforcementMode == MODE_PARENT_PASS) {
            mEnforcementMode = MODE_EARN_BLOCKS;
        }
        mParentPin = mPrefs.getString(KEY_PARENT_PIN, DEFAULT_PIN);
        if (mParentPin == null || mParentPin.length() < 4) {
            mParentPin = DEFAULT_PIN;
        }
        mBootstrapped = mPrefs.getBoolean(KEY_BOOTSTRAPPED, false);

        String savedDate = mPrefs.getString(KEY_STATS_DATE, mStatsDate);
        if (mStatsDate.equals(savedDate)) {
            mTodayLearnedSeconds = Math.max(0, mPrefs.getInt(KEY_TODAY_LEARNED_SEC, 0));
            mTodayFreeSeconds = Math.max(0, mPrefs.getInt(KEY_TODAY_FREE_SEC, 0));
        } else {
            mTodayLearnedSeconds = 0;
            mTodayFreeSeconds = 0;
        }

        Set<String> wl = mPrefs.getStringSet(KEY_WHITELIST_CHANNELS, Collections.emptySet());
        if (wl != null) {
            mCustomWhitelistedChannelIds.addAll(wl);
        }
        Set<String> bl = mPrefs.getStringSet(KEY_BLOCKED_CHANNELS, Collections.emptySet());
        if (bl != null) {
            mCustomBlockedChannelIds.addAll(bl);
        }

        String rawEps = mPrefs.getString(KEY_CUSTOM_EPISODES, "");
        if (rawEps != null && !rawEps.isEmpty()) {
            String[] lines = rawEps.split("\n");
            for (String line : lines) {
                CustomEpisode ep = CustomEpisode.deserialize(line);
                if (ep != null) {
                    mCustomEpisodes.add(ep);
                }
            }
        }
    }

    private synchronized void persist() {
        if (mPrefs == null) {
            return;
        }
        StringBuilder epBuilder = new StringBuilder();
        for (int i = 0; i < mCustomEpisodes.size(); i++) {
            if (i > 0) {
                epBuilder.append('\n');
            }
            epBuilder.append(mCustomEpisodes.get(i).serialize());
        }

        mPrefs.edit()
                .putInt(KEY_EARNED_FREE_SEC, mEarnedFreeSeconds)
                .putInt(KEY_LEARNING_PROGRESS_SEC, mLearningProgressSeconds)
                .putInt(KEY_ENFORCEMENT_MODE, mEnforcementMode)
                .putString(KEY_PARENT_PIN, mParentPin)
                .putBoolean(KEY_BOOTSTRAPPED, mBootstrapped)
                .putString(KEY_STATS_DATE, mStatsDate)
                .putInt(KEY_TODAY_LEARNED_SEC, mTodayLearnedSeconds)
                .putInt(KEY_TODAY_FREE_SEC, mTodayFreeSeconds)
                .putStringSet(KEY_WHITELIST_CHANNELS, new HashSet<>(mCustomWhitelistedChannelIds))
                .putStringSet(KEY_BLOCKED_CHANNELS, new HashSet<>(mCustomBlockedChannelIds))
                .putString(KEY_CUSTOM_EPISODES, epBuilder.toString())
                .apply();
    }

    private void checkDateRollover() {
        String today = currentDateString();
        if (!today.equals(mStatsDate)) {
            mStatsDate = today;
            mTodayLearnedSeconds = 0;
            mTodayFreeSeconds = 0;
        }
    }

    private static String currentDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public synchronized boolean canStartPlayback(KidtubeClassifier.ContentCategory category) {
        if (category == KidtubeClassifier.ContentCategory.BLOCKED) {
            return false;
        }
        if (category == KidtubeClassifier.ContentCategory.EDUCATIONAL) {
            return true;
        }
        if (mEnforcementMode == MODE_PARENT_PASS) {
            return true;
        }
        if (mEnforcementMode == MODE_LEARNING_ONLY) {
            return false;
        }
        return mEarnedFreeSeconds > 0;
    }

    public synchronized void resetPlaybackTracker() {
        mActiveVideoId = null;
        mLastWallClockMs = -1L;
        mLastPlayerPosMs = -1L;
    }

    /**
     * Called once per second while ExoPlayer is actively playing.
     * Verifies natural playback progression (anti-scrubbing / anti-fast-forward cheat).
     */
    public synchronized TickEvent onVerifiedPlaybackTick(
            String videoId,
            KidtubeClassifier.ContentCategory category,
            long playerPositionMs,
            long wallClockMs) {

        checkDateRollover();

        if (category == KidtubeClassifier.ContentCategory.BLOCKED) {
            return TickEvent.LOCK_REQUIRED;
        }

        // First tick for this video or resumed after pause: establish baseline
        if (videoId == null || !videoId.equals(mActiveVideoId) || mLastWallClockMs < 0 || mLastPlayerPosMs < 0) {
            mActiveVideoId = videoId;
            mLastWallClockMs = wallClockMs;
            mLastPlayerPosMs = playerPositionMs;

            if (category == KidtubeClassifier.ContentCategory.ENTERTAINMENT && !canStartPlayback(category)) {
                return TickEvent.LOCK_REQUIRED;
            }
            return TickEvent.NONE;
        }

        long wallDeltaMs = wallClockMs - mLastWallClockMs;
        long posDeltaMs = playerPositionMs - mLastPlayerPosMs;

        // Ignore duplicate rapid callbacks (< 750ms)
        if (wallDeltaMs < 750L) {
            return TickEvent.NONE;
        }

        mLastWallClockMs = wallClockMs;
        mLastPlayerPosMs = playerPositionMs;

        // If playback was paused/asleep for > 5s, re-anchor without crediting idle gap
        if (wallDeltaMs > 5_000L) {
            if (category == KidtubeClassifier.ContentCategory.ENTERTAINMENT && !canStartPlayback(category)) {
                return TickEvent.LOCK_REQUIRED;
            }
            return TickEvent.NONE;
        }

        if (category == KidtubeClassifier.ContentCategory.EDUCATIONAL) {
            // Anti-cheat: Player position must actually move forward naturally (between 200ms and 3500ms per 1s tick).
            // Scrubbing ahead by 30s or sitting buffered at 0ms does not earn credit.
            if (posDeltaMs < 200L || posDeltaMs > 3_500L) {
                return TickEvent.NONE;
            }

            mLearningProgressSeconds++;
            mTodayLearnedSeconds++;

            TickEvent event = TickEvent.NONE;
            if (mLearningProgressSeconds >= BLOCK_SECONDS) {
                mLearningProgressSeconds -= BLOCK_SECONDS;
                mEarnedFreeSeconds = Math.min(MAX_BANKED_FREE_SECONDS, mEarnedFreeSeconds + BLOCK_SECONDS);
                event = TickEvent.BLOCK_UNLOCKED;
                persist();
            } else if (mLearningProgressSeconds == HALF_BLOCK_SECONDS) {
                event = TickEvent.HALFWAY_TO_BLOCK;
                persist();
            } else if (mLearningProgressSeconds % 15 == 0) {
                persist();
            }
            return event;
        } else {
            // ENTERTAINMENT playback
            if (mEnforcementMode == MODE_PARENT_PASS) {
                mTodayFreeSeconds++;
                return TickEvent.NONE;
            }
            if (mEnforcementMode == MODE_LEARNING_ONLY) {
                return TickEvent.LOCK_REQUIRED;
            }
            if (mEarnedFreeSeconds <= 0) {
                return TickEvent.LOCK_REQUIRED;
            }

            mEarnedFreeSeconds--;
            mTodayFreeSeconds++;

            if (mEarnedFreeSeconds <= 0) {
                mEarnedFreeSeconds = 0;
                persist();
                return TickEvent.LOCK_REQUIRED;
            }
            if (mEarnedFreeSeconds == 120) {
                persist();
                return TickEvent.WARN_2_MIN_LEFT;
            }
            if (mEarnedFreeSeconds == 30) {
                persist();
                return TickEvent.WARN_30_SEC_LEFT;
            }
            if (mEarnedFreeSeconds % 10 == 0) {
                persist();
            }
            return TickEvent.NONE;
        }
    }

    public synchronized boolean verifyPin(String candidatePin) {
        return candidatePin != null && candidatePin.trim().equals(mParentPin);
    }

    public synchronized void setParentPin(String newPin) {
        if (newPin != null && newPin.trim().length() >= 4) {
            mParentPin = newPin.trim();
            persist();
        }
    }

    public synchronized String getParentPin() {
        return mParentPin;
    }

    public synchronized void grantBonusBlock() {
        mEarnedFreeSeconds = Math.min(MAX_BANKED_FREE_SECONDS, mEarnedFreeSeconds + BLOCK_SECONDS);
        persist();
    }

    public synchronized void clearFreeTimeBank() {
        mEarnedFreeSeconds = 0;
        persist();
    }

    public synchronized int getEarnedFreeSeconds() {
        return mEarnedFreeSeconds;
    }

    public synchronized int getLearningProgressSeconds() {
        return mLearningProgressSeconds;
    }

    public synchronized int getSecondsUntilNextBlock() {
        return Math.max(0, BLOCK_SECONDS - mLearningProgressSeconds);
    }

    public synchronized int getTodayLearnedSeconds() {
        checkDateRollover();
        return mTodayLearnedSeconds;
    }

    public synchronized int getTodayFreeSeconds() {
        checkDateRollover();
        return mTodayFreeSeconds;
    }

    public synchronized int getEnforcementMode() {
        return mEnforcementMode;
    }

    public synchronized void setEnforcementMode(int mode) {
        mEnforcementMode = mode;
        persist();
    }

    public synchronized boolean isBootstrapped() {
        return mBootstrapped;
    }

    public synchronized void markBootstrapped() {
        mBootstrapped = true;
        persist();
    }

    public synchronized void whitelistChannel(String channelId) {
        if (channelId != null && !channelId.isEmpty()) {
            mCustomBlockedChannelIds.remove(channelId);
            mCustomWhitelistedChannelIds.add(channelId);
            persist();
        }
    }

    public synchronized void blockChannel(String channelId) {
        if (channelId != null && !channelId.isEmpty()) {
            mCustomWhitelistedChannelIds.remove(channelId);
            mCustomBlockedChannelIds.add(channelId);
            persist();
        }
    }

    public synchronized void unwhitelistChannel(String channelId) {
        if (channelId != null) {
            mCustomWhitelistedChannelIds.remove(channelId);
            persist();
        }
    }

    public synchronized Set<String> getCustomWhitelistedChannelIds() {
        return Collections.unmodifiableSet(new HashSet<>(mCustomWhitelistedChannelIds));
    }

    public synchronized Set<String> getCustomBlockedChannelIds() {
        return Collections.unmodifiableSet(new HashSet<>(mCustomBlockedChannelIds));
    }

    public synchronized void addCustomReadyEpisode(String videoId, String channelId, String author, String title) {
        if (videoId == null || videoId.isEmpty()) {
            return;
        }
        for (int i = mCustomEpisodes.size() - 1; i >= 0; i--) {
            if (videoId.equals(mCustomEpisodes.get(i).videoId)) {
                mCustomEpisodes.remove(i);
            }
        }
        mCustomEpisodes.add(0, new CustomEpisode(videoId, channelId, author, title));
        persist();
    }

    public synchronized List<CustomEpisode> getCustomReadyEpisodes() {
        return Collections.unmodifiableList(new ArrayList<>(mCustomEpisodes));
    }

    public synchronized Set<String> getCustomReadyVideoIds() {
        Set<String> ids = new HashSet<>();
        for (CustomEpisode ep : mCustomEpisodes) {
            ids.add(ep.videoId);
        }
        return ids;
    }

    public static String formatMinutesSeconds(int totalSeconds) {
        int safe = Math.max(0, totalSeconds);
        int mins = safe / 60;
        int secs = safe % 60;
        return String.format(Locale.US, "%d:%02d", mins, secs);
    }

    public synchronized String getCompactStatusSummary() {
        if (mEnforcementMode == MODE_LEARNING_ONLY) {
            return String.format(Locale.US, "Mode: Learning Only | Today: %dm Learned", mTodayLearnedSeconds / 60);
        }
        if (mEnforcementMode == MODE_PARENT_PASS) {
            return "Mode: Parent Pass Unlocked";
        }
        return String.format(
                Locale.US,
                "Free Choice: %s | Next 10m Block: %s / 10:00",
                formatMinutesSeconds(mEarnedFreeSeconds),
                formatMinutesSeconds(mLearningProgressSeconds)
        );
    }
}
