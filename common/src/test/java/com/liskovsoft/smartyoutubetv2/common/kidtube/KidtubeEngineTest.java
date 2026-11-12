package com.liskovsoft.smartyoutubetv2.common.kidtube;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KidtubeEngineTest {

    @Test
    public void testCatalogIntegrity() {
        assertEquals(37, KidtubeCatalog.WHITELISTED_CHANNELS.size());
        assertEquals(37, KidtubeCatalog.WHITELISTED_CHANNEL_IDS.size());
        assertEquals(52, KidtubeCatalog.READY_TO_ROLL_EPISODES.size());
        assertEquals(52, KidtubeCatalog.READY_TO_ROLL_VIDEO_IDS.size());

        for (KidtubeCatalog.ChannelEntry ch : KidtubeCatalog.WHITELISTED_CHANNELS) {
            assertTrue("Channel ID must start with UC: " + ch.channelId, ch.channelId.startsWith("UC"));
            assertEquals("Channel ID must be 24 chars: " + ch.channelId, 24, ch.channelId.length());
            assertFalse(ch.title.isEmpty());
        }

        for (KidtubeCatalog.EpisodeEntry ep : KidtubeCatalog.READY_TO_ROLL_EPISODES) {
            assertEquals("Video ID must be 11 chars: " + ep.videoId, 11, ep.videoId.length());
            assertTrue("Episode channel must be in whitelist: " + ep.channelId,
                    KidtubeCatalog.WHITELISTED_CHANNEL_IDS.contains(ep.channelId));
            assertFalse(ep.title.isEmpty());
        }
    }

    @Test
    public void testClassifierSeparatesLearningFromMinecraftAndSpoofing() {
        // 1. Mark Rober & OverSimplified WWII -> EDUCATIONAL
        assertEquals(
                KidtubeClassifier.ContentCategory.EDUCATIONAL,
                KidtubeClassifier.classify(
                        "hFZFjoX2cGg",
                        "UCY1kMZp36IQSyNx_9h4mpCg",
                        "Mark Rober",
                        "Backyard Squirrel Maze 1.0",
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        false
                )
        );

        assertEquals(
                KidtubeClassifier.ContentCategory.EDUCATIONAL,
                KidtubeClassifier.classify(
                        "_uk_6vfqwTA",
                        "UCNIuvl7V8zACPpTmmNIqP2A",
                        "OverSimplified",
                        "WW2 - OverSimplified (Part 1)",
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        false
                )
        );

        // 2. Minecraft Let's Play -> ENTERTAINMENT
        assertEquals(
                KidtubeClassifier.ContentCategory.ENTERTAINMENT,
                KidtubeClassifier.classify(
                        "abc12345678",
                        "UC_RANDOM_GAMER_CHANNEL0",
                        "PrestonPlayz",
                        "Surviving 100 Days in Hardcore Minecraft!",
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        true
                )
        );

        // 3. Clickbait Spoofing Trap ("Minecraft WW2 D-Day Battle") -> Must stay ENTERTAINMENT!
        assertEquals(
                KidtubeClassifier.ContentCategory.ENTERTAINMENT,
                KidtubeClassifier.classify(
                        "xyz12345678",
                        "UC_RANDOM_GAMER_CHANNEL1",
                        "RandomCraft",
                        "Minecraft WW2 D-Day Normandy Mod Gameplay!",
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        true
                )
        );

        // 4. Open-Curiosity Educational Discovery outside the 37 pinned channels -> EDUCATIONAL
        assertEquals(
                KidtubeClassifier.ContentCategory.EDUCATIONAL,
                KidtubeClassifier.classify(
                        "doc12345678",
                        "UC_UNKNOWN_DOC_CHANNEL00",
                        "Imperial War Museums",
                        "The Battle of Midway | World War II Documentary",
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        false
                )
        );

        // 5. Parent Blocklist overrides everything -> BLOCKED
        Set<String> blocked = new HashSet<>(Collections.singletonList("UC_BLOCKED_CHANNEL_00000"));
        assertEquals(
                KidtubeClassifier.ContentCategory.BLOCKED,
                KidtubeClassifier.classify(
                        "blk12345678",
                        "UC_BLOCKED_CHANNEL_00000",
                        "Some Channel",
                        "Quantum Physics Documentary",
                        Collections.emptySet(),
                        blocked,
                        Collections.emptySet(),
                        true
                )
        );
    }

    @Test
    public void testTenMinuteEarnedBlocksAndAntiScrubbingCheat() {
        KidtubeLedger ledger = new KidtubeLedger(null);

        // Default PIN is 1414
        assertTrue(ledger.verifyPin("1414"));
        assertFalse(ledger.verifyPin("0000"));

        // Starts with 0 Free-Choice time: Learning allowed, Minecraft locked
        assertEquals(0, ledger.getEarnedFreeSeconds());
        assertTrue(ledger.canStartPlayback(KidtubeClassifier.ContentCategory.EDUCATIONAL));
        assertFalse(ledger.canStartPlayback(KidtubeClassifier.ContentCategory.ENTERTAINMENT));

        long wallMs = 10_000L;
        long posMs = 0L;

        // Baseline anchor tick
        assertEquals(
                KidtubeLedger.TickEvent.NONE,
                ledger.onVerifiedPlaybackTick("_uk_6vfqwTA", KidtubeClassifier.ContentCategory.EDUCATIONAL, posMs, wallMs)
        );

        // Watch 299 natural seconds -> 299s progress
        for (int i = 0; i < 299; i++) {
            wallMs += 1_000L;
            posMs += 1_000L;
            assertEquals(
                    KidtubeLedger.TickEvent.NONE,
                    ledger.onVerifiedPlaybackTick("_uk_6vfqwTA", KidtubeClassifier.ContentCategory.EDUCATIONAL, posMs, wallMs)
            );
        }
        assertEquals(299, ledger.getLearningProgressSeconds());

        // 300th second (5:00) -> HALFWAY_TO_BLOCK!
        wallMs += 1_000L;
        posMs += 1_000L;
        assertEquals(
                KidtubeLedger.TickEvent.HALFWAY_TO_BLOCK,
                ledger.onVerifiedPlaybackTick("_uk_6vfqwTA", KidtubeClassifier.ContentCategory.EDUCATIONAL, posMs, wallMs)
        );

        // Anti-cheat test 1: Kid scrubs seekbar ahead by 5 minutes (+300,000ms) in 1 second of wall time -> 0 credit!
        wallMs += 1_000L;
        posMs += 300_000L;
        assertEquals(
                KidtubeLedger.TickEvent.NONE,
                ledger.onVerifiedPlaybackTick("_uk_6vfqwTA", KidtubeClassifier.ContentCategory.EDUCATIONAL, posMs, wallMs)
        );
        assertEquals(300, ledger.getLearningProgressSeconds());

        // Anti-cheat test 2: Player frozen/buffering at exact same posMs -> 0 credit!
        wallMs += 1_000L;
        assertEquals(
                KidtubeLedger.TickEvent.NONE,
                ledger.onVerifiedPlaybackTick("_uk_6vfqwTA", KidtubeClassifier.ContentCategory.EDUCATIONAL, posMs, wallMs)
        );
        assertEquals(300, ledger.getLearningProgressSeconds());

        // Watch remaining 299 natural seconds (up to 599s)
        for (int i = 0; i < 299; i++) {
            wallMs += 1_000L;
            posMs += 1_000L;
            assertEquals(
                    KidtubeLedger.TickEvent.NONE,
                    ledger.onVerifiedPlaybackTick("_uk_6vfqwTA", KidtubeClassifier.ContentCategory.EDUCATIONAL, posMs, wallMs)
            );
        }
        assertEquals(0, ledger.getEarnedFreeSeconds());

        // 600th verified second (10:00) -> BLOCK_UNLOCKED (+600s Free Choice)!
        wallMs += 1_000L;
        posMs += 1_000L;
        assertEquals(
                KidtubeLedger.TickEvent.BLOCK_UNLOCKED,
                ledger.onVerifiedPlaybackTick("_uk_6vfqwTA", KidtubeClassifier.ContentCategory.EDUCATIONAL, posMs, wallMs)
        );
        assertEquals(600, ledger.getEarnedFreeSeconds());
        assertEquals(0, ledger.getLearningProgressSeconds());
        assertTrue(ledger.canStartPlayback(KidtubeClassifier.ContentCategory.ENTERTAINMENT));

        // Now switch to a Minecraft video and spend the 600s Free-Choice block
        wallMs += 1_000L;
        long mcPosMs = 0L;
        assertEquals(
                KidtubeLedger.TickEvent.NONE,
                ledger.onVerifiedPlaybackTick("mc_video_01", KidtubeClassifier.ContentCategory.ENTERTAINMENT, mcPosMs, wallMs)
        );

        boolean sawWarn2Min = false;
        boolean sawWarn30Sec = false;
        KidtubeLedger.TickEvent finalEvent = KidtubeLedger.TickEvent.NONE;

        for (int i = 0; i < 600; i++) {
            wallMs += 1_000L;
            mcPosMs += 1_000L;
            finalEvent = ledger.onVerifiedPlaybackTick("mc_video_01", KidtubeClassifier.ContentCategory.ENTERTAINMENT, mcPosMs, wallMs);
            if (finalEvent == KidtubeLedger.TickEvent.WARN_2_MIN_LEFT) {
                sawWarn2Min = true;
            } else if (finalEvent == KidtubeLedger.TickEvent.WARN_30_SEC_LEFT) {
                sawWarn30Sec = true;
            }
        }

        assertTrue("Must warn at 2:00 left", sawWarn2Min);
        assertTrue("Must warn at 0:30 left", sawWarn30Sec);
        assertEquals("Must lock immediately when 10m block hits 0:00", KidtubeLedger.TickEvent.LOCK_REQUIRED, finalEvent);
        assertEquals(0, ledger.getEarnedFreeSeconds());
        assertFalse(ledger.canStartPlayback(KidtubeClassifier.ContentCategory.ENTERTAINMENT));
    }
}
