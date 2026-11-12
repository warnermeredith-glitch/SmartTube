package com.liskovsoft.smartyoutubetv2.common.kidtube;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.misc.BrowseProcessor;

import java.util.Iterator;
import java.util.List;

/**
 * Strips YouTube Shorts and Parent-blocked channels from all grids, rows, search results,
 * and player suggestion trays, and adds clear "[+TIME]" badges on verified learning videos.
 */
public class KidtubeBrowseProcessor implements BrowseProcessor {
    private final Context mContext;
    private final OnItemReady mOnItemReady;

    public KidtubeBrowseProcessor(Context context, OnItemReady onItemReady) {
        mContext = context.getApplicationContext();
        mOnItemReady = onItemReady;
    }

    @Override
    public void process(VideoGroup videoGroup) {
        if (videoGroup == null || videoGroup.isEmpty() || !KidtubeManager.isKidtubeActive(mContext)) {
            return;
        }

        List<Video> videos = videoGroup.getVideos();
        if (videos == null || videos.isEmpty()) {
            return;
        }

        KidtubeLedger ledger = KidtubeLedger.instance(mContext);

        Iterator<Video> it = videos.iterator();
        while (it.hasNext()) {
            Video video = it.next();
            if (video == null) {
                continue;
            }

            // 1. Eliminate YouTube Shorts ("brain-rot vertical scroll")
            if (video.isShorts) {
                it.remove();
                continue;
            }

            KidtubeClassifier.ContentCategory category = KidtubeClassifier.classify(
                    video.videoId,
                    video.channelId,
                    video.getAuthor(),
                    video.getTitle(),
                    ledger.getCustomWhitelistedChannelIds(),
                    ledger.getCustomBlockedChannelIds(),
                    ledger.getCustomReadyVideoIds(),
                    true
            );

            // 2. Remove Parent-blocked channels
            if (category == KidtubeClassifier.ContentCategory.BLOCKED) {
                it.remove();
                continue;
            }

            // 3. Add clear visual badge so child immediately sees which videos earn 10-min blocks
            if (video.hasVideo()
                    && !KidtubeCatalog.SPECIAL_VIDEO_ROLL.equals(video.videoId)
                    && !KidtubeCatalog.SPECIAL_VIDEO_STATUS.equals(video.videoId)) {
                if (category == KidtubeClassifier.ContentCategory.EDUCATIONAL) {
                    if (video.badge == null || !video.badge.contains("+TIME")) {
                        video.badge = (video.badge == null || video.badge.isEmpty())
                                ? "+TIME"
                                : "+TIME • " + video.badge;
                        if (mOnItemReady != null) {
                            mOnItemReady.onItemReady(video);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        // Synchronous in-memory filtering; no async disposables required
    }
}
