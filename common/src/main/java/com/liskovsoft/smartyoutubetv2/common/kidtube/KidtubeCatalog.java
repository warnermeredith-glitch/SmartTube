package com.liskovsoft.smartyoutubetv2.common.kidtube;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Curated, live-verified catalog of History, Math, Science, and Mega-Engineering
 * channels and "Ready-to-Roll" episodes for Kidtube.
 */
public final class KidtubeCatalog {
    public static final int SECTION_READY_TO_ROLL = 50;

    public static final String SPECIAL_VIDEO_ROLL = "__KIDTUBE_ROLL__";
    public static final String SPECIAL_VIDEO_STATUS = "__KIDTUBE_STATUS__";

    public static final String HUB_ENGINEERING = "Mark Rober & Mega Builds";
    public static final String HUB_HISTORY = "WWII & Epic History";
    public static final String HUB_SCIENCE = "Science & Space";
    public static final String HUB_MATH = "Math, Riddles & Logic";

    public static final class ChannelEntry {
        public final String channelId;
        public final String title;
        public final String hubName;

        public ChannelEntry(String channelId, String title, String hubName) {
            this.channelId = channelId;
            this.title = title;
            this.hubName = hubName;
        }
    }

    public static final class EpisodeEntry {
        public final String videoId;
        public final String channelId;
        public final String author;
        public final String title;
        public final String topicBadge;

        public EpisodeEntry(String videoId, String channelId, String author, String title, String topicBadge) {
            this.videoId = videoId;
            this.channelId = channelId;
            this.author = author;
            this.title = title;
            this.topicBadge = topicBadge;
        }

        public String getThumbnailUrl() {
            return "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
        }

        public String getBackgroundUrl() {
            return "https://i.ytimg.com/vi/" + videoId + "/maxresdefault.jpg";
        }
    }

    public static final List<ChannelEntry> WHITELISTED_CHANNELS;
    public static final Set<String> WHITELISTED_CHANNEL_IDS;
    public static final Set<String> WHITELISTED_CHANNEL_NAMES_LOWER;
    public static final List<EpisodeEntry> READY_TO_ROLL_EPISODES;
    public static final Set<String> READY_TO_ROLL_VIDEO_IDS;

    static {
        List<ChannelEntry> channels = new ArrayList<>(Arrays.asList(
                // Hub 1: Mark Rober & Mega Engineering Builds
                new ChannelEntry("UCY1kMZp36IQSyNx_9h4mpCg", "Mark Rober", HUB_ENGINEERING),
                new ChannelEntry("UCj1VqrHhDte54oLgPG4xpuQ", "Stuff Made Here", HUB_ENGINEERING),
                new ChannelEntry("UCbsfyGlrjrKQC0gbzK0-EiA", "Jared Owen", HUB_ENGINEERING),
                new ChannelEntry("UC1ZBQ-F-yktYD4m5AzM6pww", "Mustard", HUB_ENGINEERING),
                new ChannelEntry("UCdp4_l1vPmpN-gDbUwhaRUQ", "Branch Education", HUB_ENGINEERING),
                new ChannelEntry("UCR1IuLEqb6UEA_zQ81kwXfg", "Real Engineering", HUB_ENGINEERING),
                new ChannelEntry("UCMOqf8ab-42UUQIdVoKwjlQ", "Practical Engineering", HUB_ENGINEERING),
                new ChannelEntry("UCjgpFI5dU-D1-kh9H1muoxQ", "Hacksmith Industries", HUB_ENGINEERING),
                new ChannelEntry("UCp68_FLety0O-n9QU6phsgw", "colinfurze", HUB_ENGINEERING),

                // Hub 2: Animated WWII & Epic History
                new ChannelEntry("UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified", HUB_HISTORY),
                new ChannelEntry("UC-f2WBfSCZiu0bOBydjot3w", "Yarnhub", HUB_HISTORY),
                new ChannelEntry("UC510QYlOlKNyhy_zdQxnGYw", "Simple History", HUB_HISTORY),
                new ChannelEntry("UCeUJFQ0D9qs6aVNyUt9fkeQ", "The Armchair Historian", HUB_HISTORY),
                new ChannelEntry("UC22BdTgxefuvUivrjesETjg", "History Matters", HUB_HISTORY),
                new ChannelEntry("UCCODtTcd5M1JavPCOr_Uydg", "Extra History", HUB_HISTORY),
                new ChannelEntry("UCMmaBzfCCwZ2KqaBJjkj0fw", "Kings and Generals", HUB_HISTORY),
                new ChannelEntry("UCv_vLHiWVBh_FR9vbeuiY-A", "Historia Civilis", HUB_HISTORY),
                new ChannelEntry("UCvPXiKxH-eH9xq-80vpgmKQ", "Epic History", HUB_HISTORY),
                new ChannelEntry("UCc-N24Y5OA0gqbjBwe1ttfA", "Weird History", HUB_HISTORY),

                // Hub 3: Mind-Blowing Science, Space & Chemistry
                new ChannelEntry("UCsXVk37bltHxD1rDPwtNM8Q", "Kurzgesagt – In a Nutshell", HUB_SCIENCE),
                new ChannelEntry("UCHnyfMqiRRG1u-2MsSQLbXA", "Veritasium", HUB_SCIENCE),
                new ChannelEntry("UC6107grRI4m0o2-emgoDnAA", "SmarterEveryDay", HUB_SCIENCE),
                new ChannelEntry("UCFhXFikryT4aFcLkLw2LBLA", "NileRed", HUB_SCIENCE),
                new ChannelEntry("UCEIwxahdLz7bap-VDs9h35A", "Steve Mould", HUB_SCIENCE),
                new ChannelEntry("UC1VLQPn9cYSqx8plbk9RxxQ", "The Action Lab", HUB_SCIENCE),
                new ChannelEntry("UCzR-rom72PHN9Zg7RML9EbA", "PBS Eons", HUB_SCIENCE),
                new ChannelEntry("UCZYTClx2T1of7BRZ86-8fow", "SciShow", HUB_SCIENCE),
                new ChannelEntry("UCH4BNI0-FOK2dMXoFtViWHw", "Be Smart", HUB_SCIENCE),
                new ChannelEntry("UC415bOPUcGSamy543abLmRA", "Cleo Abram", HUB_SCIENCE),
                new ChannelEntry("UCJ0-OtVpF0wOKEqT2Z1HEtA", "ElectroBOOM", HUB_SCIENCE),

                // Hub 4: Math, Riddles, Logic & Simulations
                new ChannelEntry("UCYO_jab_esuFRV4b17AJtAw", "3Blue1Brown", HUB_MATH),
                new ChannelEntry("UCoxcjq-8xIDTYp3uz647V5A", "Numberphile", HUB_MATH),
                new ChannelEntry("UCSju5G2aFaWMqn-_0YBtq5A", "Stand-up Maths", HUB_MATH),
                new ChannelEntry("UCsooa4yRKGN_zEE8iknghZA", "TED-Ed", HUB_MATH),
                new ChannelEntry("UC6nSFpj9HTCZ5t-N3Rm3-HA", "Vsauce", HUB_MATH),
                new ChannelEntry("UCKzJFdi57J53Vr_BkTfN3uQ", "Primer", HUB_MATH),
                new ChannelEntry("UCHnj59g7jezwTy5GeL8EA_g", "MindYourDecisions", HUB_MATH)
        ));

        WHITELISTED_CHANNELS = Collections.unmodifiableList(channels);

        Set<String> idSet = new HashSet<>();
        Set<String> nameSet = new HashSet<>();
        for (ChannelEntry entry : channels) {
            idSet.add(entry.channelId);
            nameSet.add(entry.title.toLowerCase(Locale.ROOT));
        }
        // Common alternate display names
        nameSet.add("kurzgesagt");
        nameSet.add("kurzgesagt - in a nutshell");
        nameSet.add("smarter every day");
        nameSet.add("ted ed");
        WHITELISTED_CHANNEL_IDS = Collections.unmodifiableSet(idSet);
        WHITELISTED_CHANNEL_NAMES_LOWER = Collections.unmodifiableSet(nameSet);

        // Interleaved across Engineering, WWII/History, Science, and Math so every row of 4 cards on TV is exciting!
        List<EpisodeEntry> episodes = new ArrayList<>(Arrays.asList(
                new EpisodeEntry("hFZFjoX2cGg", "UCY1kMZp36IQSyNx_9h4mpCg", "Mark Rober",
                        "Backyard Squirrel Maze 1.0 - Ninja Warrior Course", "ENGINEERING"),
                new EpisodeEntry("_uk_6vfqwTA", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "WW2 - OverSimplified (Part 1)", "WWII HISTORY"),
                new EpisodeEntry("fo2Rb9h788s", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "WW2 - OverSimplified (Part 2)", "WWII HISTORY"),
                new EpisodeEntry("myO8fxhDRW0", "UCj1VqrHhDte54oLgPG4xpuQ", "Stuff Made Here",
                        "Moving Basketball Hoop Won't Let You Miss", "ENGINEERING"),

                new EpisodeEntry("xoxhDk-hwuo", "UCY1kMZp36IQSyNx_9h4mpCg", "Mark Rober",
                        "Glitter Bomb 1.0 vs Porch Pirates", "ENGINEERING"),
                new EpisodeEntry("2ZKANCH80-M", "UC-f2WBfSCZiu0bOBydjot3w", "Yarnhub",
                        "When the US Hit Tokyo After Pearl Harbor (Doolittle Raid)", "WWII HISTORY"),
                new EpisodeEntry("HLrBUwNSEo0", "UCbsfyGlrjrKQC0gbzK0-EiA", "Jared Owen",
                        "What's Inside the Titanic? (3D Cutaway)", "HOW IT WORKS"),
                new EpisodeEntry("pXypQ6d_iDg", "UC6107grRI4m0o2-emgoDnAA", "SmarterEveryDay",
                        "INSIDE a Nuclear Reactor Core", "SCIENCE"),

                new EpisodeEntry("26P1KyvrmjI", "UCY1kMZp36IQSyNx_9h4mpCg", "Mark Rober",
                        "I Solved Every Board Game (Using Math & Engineering)", "MATH & BUILDS"),
                new EpisodeEntry("4nIjmWIfn44", "UC-f2WBfSCZiu0bOBydjot3w", "Yarnhub",
                        "Night Witches (WWII Animated Documentary)", "WWII HISTORY"),
                new EpisodeEntry("ZZTC_dXcC2o", "UCdp4_l1vPmpN-gDbUwhaRUQ", "Branch Education",
                        "The 3-Nanometer Air Gap Inside Every Hard Drive", "ENGINEERING"),
                new EpisodeEntry("QtCL5pYI8Zc", "UCHnj59g7jezwTy5GeL8EA_g", "MindYourDecisions",
                        "The Game Show Math Question That Stumped Everyone", "MATH PUZZLE"),

                new EpisodeEntry("EUTo1urxwPo", "UCeUJFQ0D9qs6aVNyUt9fkeQ", "The Armchair Historian",
                        "D-Day From the German Perspective | Animated History", "WWII HISTORY"),
                new EpisodeEntry("pFEB0chiuJA", "UCY1kMZp36IQSyNx_9h4mpCg", "Mark Rober",
                        "World's Largest Horn Shatters Glass", "PHYSICS"),
                new EpisodeEntry("a0qxxlg_OCs", "UC1ZBQ-F-yktYD4m5AzM6pww", "Mustard",
                        "The Navy's Secret Giant Seaplane Plan", "AVIATION"),
                new EpisodeEntry("KfODG5REyss", "UCHnyfMqiRRG1u-2MsSQLbXA", "Veritasium",
                        "I Finally Saw Quantum Mechanics With My Own Eyes", "PHYSICS"),

                new EpisodeEntry("zqllxbPWKNI", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "The Napoleonic Wars - OverSimplified (Part 1)", "HISTORY"),
                new EpisodeEntry("mY3SEMTROas", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "The Napoleonic Wars - OverSimplified (Part 2)", "HISTORY"),
                new EpisodeEntry("vePc5V4h_kg", "UCY1kMZp36IQSyNx_9h4mpCg", "Mark Rober",
                        "Shark Attack Test - Human Blood vs. Fish Blood", "MARINE SCIENCE"),
                new EpisodeEntry("LxVW3b8jVdE", "UCoxcjq-8xIDTYp3uz647V5A", "Numberphile",
                        "A Two-Button Scientific Calculator - Numberphile", "MATH"),

                new EpisodeEntry("DG3bCFMN7GQ", "UC-f2WBfSCZiu0bOBydjot3w", "Yarnhub",
                        "When Ha-Go Tanks Ambushed M3 Stuarts (WWII)", "WWII HISTORY"),
                new EpisodeEntry("0AqxJn_iLxs", "UCbsfyGlrjrKQC0gbzK0-EiA", "Jared Owen",
                        "How NASA's Orion Spacecraft Works", "SPACE"),
                new EpisodeEntry("GmwaJnj6pfY", "UCFhXFikryT4aFcLkLw2LBLA", "NileRed",
                        "Making Liquid Nitrogen From Scratch", "CHEMISTRY"),
                new EpisodeEntry("_y9JuojAIg0", "UCsXVk37bltHxD1rDPwtNM8Q", "Kurzgesagt – In a Nutshell",
                        "We Finally Know More About Humanity's First Civilization", "HISTORY"),

                new EpisodeEntry("b_H7jLFKJuU", "UC22BdTgxefuvUivrjesETjg", "History Matters",
                        "Why Did Germany and the USSR Sign the Molotov-Ribbentrop Pact?", "WWII HISTORY"),
                new EpisodeEntry("2OfjZ3ORJfc", "UCj1VqrHhDte54oLgPG4xpuQ", "Stuff Made Here",
                        "Robot Golf vs. Holes That Keep Getting Harder", "ROBOTICS"),
                new EpisodeEntry("4JZoa12T9RY", "UCZYTClx2T1of7BRZ86-8fow", "SciShow",
                        "How 4 Perfect Space Balls Proved Einstein Right", "SPACE"),
                new EpisodeEntry("zuaVFnj4SWY", "UCSju5G2aFaWMqn-_0YBtq5A", "Stand-up Maths",
                        "The Mystery of the 14 Fractions", "MATH"),

                new EpisodeEntry("lsbcN9-jU1Y", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "The Second Punic War (Hannibal vs Rome) - Part 1", "ANCIENT HISTORY"),
                new EpisodeEntry("Q-nWA0WeF98", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "The Second Punic War (Hannibal vs Rome) - Part 2", "ANCIENT HISTORY"),
                new EpisodeEntry("1BVJzaXv3rk", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "The Second Punic War (Battle of Cannae) - Part 3", "ANCIENT HISTORY"),
                new EpisodeEntry("9kM3mnxrMMM", "UCdp4_l1vPmpN-gDbUwhaRUQ", "Branch Education",
                        "How Do microSD Cards Store 2 TB in Such a TINY Space?", "ENGINEERING"),

                new EpisodeEntry("tsxmyL7TUJg", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "The American Civil War - OverSimplified (Part 1)", "US HISTORY"),
                new EpisodeEntry("sV6uuMAnJUE", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "The American Civil War - OverSimplified (Part 2)", "US HISTORY"),
                new EpisodeEntry("y1KipwEoRBA", "UCR1IuLEqb6UEA_zQ81kwXfg", "Real Engineering",
                        "The Rise and Fall of the Supersonic Concorde", "AVIATION"),
                new EpisodeEntry("Nbwv5wHQoj0", "UCYO_jab_esuFRV4b17AJtAw", "3Blue1Brown",
                        "The Last Math Olympiad Problem AI Could Not Solve", "MATH"),

                new EpisodeEntry("5RtwFbSeV5Y", "UC1ZBQ-F-yktYD4m5AzM6pww", "Mustard",
                        "When a Soviet MiG-25 Fought an F-15", "MILITARY TECH"),
                new EpisodeEntry("u2qs-l0vgGw", "UCbsfyGlrjrKQC0gbzK0-EiA", "Jared Owen",
                        "How Did NASA's Lunar Roving Vehicle Work?", "SPACE"),
                new EpisodeEntry("qr57Gm67jQY", "UCH4BNI0-FOK2dMXoFtViWHw", "Be Smart",
                        "The Real Physics Reason Planes Stay in the Air", "PHYSICS"),
                new EpisodeEntry("Z-2pTzSZtLU", "UCEIwxahdLz7bap-VDs9h35A", "Steve Mould",
                        "This Material Sorts Itself Automatically", "SCIENCE"),

                new EpisodeEntry("QLq6GEiHqR8", "UCNIuvl7V8zACPpTmmNIqP2A", "OverSimplified",
                        "The Pig War - OverSimplified", "HISTORY"),
                new EpisodeEntry("dCt7NYI4_00", "UCvPXiKxH-eH9xq-80vpgmKQ", "Epic History",
                        "The Hundred Days: Battle of Waterloo", "HISTORY"),
                new EpisodeEntry("yGf78wc6jIU", "UCzR-rom72PHN9Zg7RML9EbA", "PBS Eons",
                        "The Rise and Fall of the Permian's Strangest Predators", "PALEONTOLOGY"),
                new EpisodeEntry("nBVuemyrMgE", "UCsooa4yRKGN_zEE8iknghZA", "TED-Ed",
                        "Is Salt the Most Important Ingredient in History?", "HISTORY"),

                new EpisodeEntry("RpslsMqPFWA", "UCj1VqrHhDte54oLgPG4xpuQ", "Stuff Made Here",
                        "I Engineered a Machine That Peels Apples in 2 Seconds", "ENGINEERING"),
                new EpisodeEntry("M0RZasms4sA", "UC510QYlOlKNyhy_zdQxnGYw", "Simple History",
                        "Engineering & History of the AK-47", "HISTORY"),
                new EpisodeEntry("eyZEqSPeMwg", "UCHnyfMqiRRG1u-2MsSQLbXA", "Veritasium",
                        "No Device Could Prove Einstein Wrong... Until Now", "PHYSICS"),
                new EpisodeEntry("GkiITbgu0V0", "UCKzJFdi57J53Vr_BkTfN3uQ", "Primer",
                        "Simulating How a Single Brain Cell Learns", "SIMULATION"),

                new EpisodeEntry("rxV9Jx7fx3c", "UC22BdTgxefuvUivrjesETjg", "History Matters",
                        "Why Didn't Napoleon Just Stop?", "HISTORY"),
                new EpisodeEntry("ucyw6GVifCE", "UCbsfyGlrjrKQC0gbzK0-EiA", "Jared Owen",
                        "How a Giant Boat-Lifting Wheel Works (Falkirk Wheel)", "ENGINEERING"),
                new EpisodeEntry("99rI-nOCajM", "UC1VLQPn9cYSqx8plbk9RxxQ", "The Action Lab",
                        "Most People Get This Physics Question Wrong", "PHYSICS"),
                new EpisodeEntry("TZnXbOhRVws", "UCHnj59g7jezwTy5GeL8EA_g", "MindYourDecisions",
                        "Putnam Math Competition Problem Solved With a Clever Trick", "MATH")
        ));

        READY_TO_ROLL_EPISODES = Collections.unmodifiableList(episodes);

        Set<String> vidSet = new HashSet<>();
        for (EpisodeEntry ep : episodes) {
            vidSet.add(ep.videoId);
        }
        READY_TO_ROLL_VIDEO_IDS = Collections.unmodifiableSet(vidSet);
    }

    private KidtubeCatalog() {
    }

    public static Map<String, List<ChannelEntry>> getChannelsByHub() {
        Map<String, List<ChannelEntry>> hubs = new LinkedHashMap<>();
        hubs.put(HUB_ENGINEERING, new ArrayList<>());
        hubs.put(HUB_HISTORY, new ArrayList<>());
        hubs.put(HUB_SCIENCE, new ArrayList<>());
        hubs.put(HUB_MATH, new ArrayList<>());

        for (ChannelEntry ch : WHITELISTED_CHANNELS) {
            List<ChannelEntry> list = hubs.get(ch.hubName);
            if (list != null) {
                list.add(ch);
            }
        }
        return hubs;
    }
}
