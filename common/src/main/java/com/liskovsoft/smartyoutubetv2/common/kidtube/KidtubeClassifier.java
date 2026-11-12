package com.liskovsoft.smartyoutubetv2.common.kidtube;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fast, deterministic two-layer classifier separating History/Math/Science/Engineering
 * learning videos from gaming/entertainment videos (such as Minecraft Let's Plays).
 */
public final class KidtubeClassifier {
    public enum ContentCategory {
        EDUCATIONAL,
        ENTERTAINMENT,
        BLOCKED
    }

    // Gaming, let's play, and mind-numbing content markers that always require earned Free-Choice blocks
    // (unless the video comes directly from a parent-whitelisted or catalog-whitelisted learning channel).
    private static final List<String> GAMING_AND_ENTERTAINMENT_MARKERS = Collections.unmodifiableList(Arrays.asList(
            "minecraft", "roblox", "fortnite", "bedwars", "skyblock", "hypixel", "blox fruits",
            "brookhaven", "adopt me", "brawl stars", "stumble guys", "among us", "valorant",
            "call of duty", "warzone", "apex legends", "gta ", "grand theft auto", "fnaf",
            "five nights at freddy", "poppy playtime", "rainbow friends", "garten of banban",
            "skibidi", "let's play", "lets play", "playthrough", "gameplay", "speedrun",
            "hardcore survival", "100 days in", "smp", "mrbeast gaming", "lankybox", "unspeakable",
            "prestonplayz", "preston", "brianna", "aphmau", "ssundee", "eystreem", "maizen",
            "camman18", "technoblade", "tommyinnit", "tubbo", "dan tdm", "dantdm", "flamingo",
            "kreekcraft", "unboxing", "toy review", "try not to laugh", "prank", "pranks",
            "rage compilation", "funny moments", "funny fails", "tiktok", "brainrot", "asmr"
    ));

    // Broad open-curiosity educational creators & subject markers ("sky is the limit, as long as he learns something")
    private static final List<String> EDUCATIONAL_DISCOVERY_MARKERS = Collections.unmodifiableList(Arrays.asList(
            // Trusted documentary & science/history/math creators beyond the 37 pinned channels
            "national geographic", "nat geo", "nova pbs", "smithsonian", "nasa", "khan academy",
            "crashcourse", "crash course", "minutephysics", "minuteearth", "sixty symbols",
            "computerphile", "periodic videos", "numberphile2", "mathologer", "zach star",
            "standupmaths", "tierzoo", "deep look", "sci show", "scishow kids", "pbs space time",
            "pbs terra", "real life lore", "wendover productions", "half as interesting",
            "geography now", "the operations room", "operations room", "imperial", "invicta",
            "sandrhoman history", "drachinifel", "military aviation history", "rex's hangar",
            "megaprojects", "sideprojects", "biographics", "toldinstone", "timeline - world history",
            "how it's made", "how its made", "mit opencourseware", "royal institution",
            // Explicit academic/historical/scientific subject phrases
            "world war ii", "world war 2", "wwii", "ww2 documentary", "animated history",
            "history documentary", "science documentary", "engineering documentary",
            "roman empire", "ancient rome", "ancient egypt", "ancient greece", "middle ages",
            "american revolution", "revolutionary war", "american civil war", "napoleonic",
            "cold war", "battle of midway", "pearl harbor", "d-day", "normandy",
            "quantum mechanics", "quantum physics", "astrophysics", "black hole", "solar system",
            "general relativity", "special relativity", "thermodynamics", "aerodynamics",
            "nuclear reactor", "nuclear physics", "electromagnetism", "periodic table",
            "organic chemistry", "chemical reaction", "microchip", "semiconductor",
            "calculus", "geometry", "trigonometry", "linear algebra", "number theory",
            "prime numbers", "fibonacci", "pythagorean", "math puzzle", "math riddle",
            "game theory", "probability"
    ));

    private KidtubeClassifier() {
    }

    /**
     * Authoritative classification when server-verified channelId is available (e.g. playback gate),
     * or browse-time classification when allowAuthorNameFallback is true.
     */
    public static ContentCategory classify(
            String videoId,
            String channelId,
            String author,
            String title,
            Set<String> customWhitelistedChannelIds,
            Set<String> customBlockedChannelIds,
            Set<String> customReadyVideoIds,
            boolean allowAuthorNameFallback) {

        Set<String> blockedIds = customBlockedChannelIds != null ? customBlockedChannelIds : Collections.emptySet();
        Set<String> extraChannelIds = customWhitelistedChannelIds != null ? customWhitelistedChannelIds : Collections.emptySet();
        Set<String> extraVideoIds = customReadyVideoIds != null ? customReadyVideoIds : Collections.emptySet();

        // 1. Explicit Parent Blocklist always wins
        if (channelId != null && blockedIds.contains(channelId)) {
            return ContentCategory.BLOCKED;
        }

        // 2. Special Kidtube UI action cards
        if (KidtubeCatalog.SPECIAL_VIDEO_ROLL.equals(videoId) || KidtubeCatalog.SPECIAL_VIDEO_STATUS.equals(videoId)) {
            return ContentCategory.EDUCATIONAL;
        }

        // 3. Curated Ready-to-Roll video IDs or Parent-pinned video IDs
        if (videoId != null && (KidtubeCatalog.READY_TO_ROLL_VIDEO_IDS.contains(videoId) || extraVideoIds.contains(videoId))) {
            return ContentCategory.EDUCATIONAL;
        }

        // 4. Verified Whitelisted Channel IDs (Catalog + Parent Whitelist)
        if (channelId != null && (KidtubeCatalog.WHITELISTED_CHANNEL_IDS.contains(channelId) || extraChannelIds.contains(channelId))) {
            return ContentCategory.EDUCATIONAL;
        }

        String normAuthor = normalize(author);
        String normTitle = normalize(title);
        String combined = (normTitle + " " + normAuthor).trim();

        // 5. Optional UI-card fallback when YouTube search cards omit channelId prior to clicking
        if (allowAuthorNameFallback && !normAuthor.isEmpty() && KidtubeCatalog.WHITELISTED_CHANNEL_NAMES_LOWER.contains(normAuthor)) {
            return ContentCategory.EDUCATIONAL;
        }

        // 6. Gaming / Let's Play / Brain-Rot trap check BEFORE open-curiosity keyword discovery
        //    (Prevents clickbait like "Minecraft WW2 Battle!!" from spoofing history keywords)
        for (String gamingTerm : GAMING_AND_ENTERTAINMENT_MARKERS) {
            if (combined.contains(gamingTerm)) {
                return ContentCategory.ENTERTAINMENT;
            }
        }

        // 7. Open-Curiosity Educational Subject Discovery
        for (String eduTerm : EDUCATIONAL_DISCOVERY_MARKERS) {
            if (combined.contains(eduTerm)) {
                return ContentCategory.EDUCATIONAL;
            }
        }

        // 8. Safe Default: Unrecognized videos require earned Free-Choice time and never grant free blocks
        return ContentCategory.ENTERTAINMENT;
    }

    public static boolean hasExplicitGamingMarker(String title, String author) {
        String combined = (normalize(title) + " " + normalize(author)).trim();
        for (String gamingTerm : GAMING_AND_ENTERTAINMENT_MARKERS) {
            if (combined.contains(gamingTerm)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .trim();
    }
}
