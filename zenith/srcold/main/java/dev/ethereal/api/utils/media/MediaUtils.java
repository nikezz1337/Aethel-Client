package dev.ethereal.api.utils.media;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaUtils {
    private static boolean initialized = false;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static volatile MediaInfo mediaInfo = null;
    private static final Map<String, AbstractTexture> textureCache = new ConcurrentHashMap<>();
    private static String lastMetadataKey = "";
    private static String currentTextureHash = "";
    private static final float[] waveHeights = new float[4];
    private static final float[] waveTargets = new float[4];
    private static long lastWaveUpdate = 0L;

    public enum Status {
        PLAYING, PAUSED
    }

    public static class MediaInfo {
        public final String title;
        public final String artist;
        public final String textureHash;
        public final Status status;
        public final float[] heights;

        public MediaInfo(String title, String artist, String textureHash, Status status, float[] heights) {
            this.title = title;
            this.artist = artist;
            this.textureHash = textureHash;
            this.status = status;
            this.heights = heights.clone();
        }

        public AbstractTexture getTexture() {
            return textureCache.get(textureHash);
        }
    }

    public static MediaInfo getCurrentMedia() {
        if (!initialized) {
            initialized = true;
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // Placeholder implementation - requires mediatransport4j library
                    // For now, just update wave animation
                    updateWaveLogic(false);
                    
                    // Uncomment when mediatransport4j is available:
                    /*
                    List<MediaSession> sessions = MediaTransport.getMediaSessions();
                    if (sessions != null && !sessions.isEmpty()) {
                        MediaSession session = sessions.get(0);
                        String title = session.getTitle() != null ? session.getTitle() : "Unknown";
                        String artist = session.getArtist() != null ? session.getArtist() : "Artist";
                        String metadataKey = title + artist;

                        if (!metadataKey.equals(lastMetadataKey)) {
                            clearCache();
                            if (session.hasThumbnail()) {
                                ByteBuffer buffer = session.getThumbnail();
                                AbstractTexture texture = convertTexture(buffer);
                                if (texture != null) {
                                    currentTextureHash = String.valueOf(metadataKey.hashCode());
                                    textureCache.put(currentTextureHash, texture);
                                }
                            } else {
                                currentTextureHash = "";
                            }
                            lastMetadataKey = metadataKey;
                        }

                        boolean playing = session.isPlaying();
                        updateWaveLogic(playing);
                        mediaInfo = new MediaInfo(title, artist, currentTextureHash,
                                playing ? Status.PLAYING : Status.PAUSED, waveHeights);
                    } else {
                        if (mediaInfo != null) {
                            clearCache();
                            mediaInfo = null;
                            lastMetadataKey = "";
                        }
                    }
                    */
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
        }
        return mediaInfo;
    }

    private static void updateWaveLogic(boolean playing) {
        if (playing) {
            if (System.currentTimeMillis() - lastWaveUpdate > 90) {
                lastWaveUpdate = System.currentTimeMillis();
                waveTargets[0] = 4.0f + (float) (Math.random() * 6.0f);
                waveTargets[1] = 2.0f + (float) (Math.random() * 10.0f);
                waveTargets[2] = 5.0f + (float) (Math.random() * 4.0f);
                waveTargets[3] = 3.0f + (float) (Math.random() * 5.0f);
            }
        } else {
            for (int i = 0; i < 4; i++) waveTargets[i] = 0f;
        }

        for (int i = 0; i < 4; i++) {
            float smoothness = (i == 1) ? 0.1f : 0.25f;
            waveHeights[i] += (waveTargets[i] - waveHeights[i]) * smoothness;
        }
    }

    private static AbstractTexture convertTexture(ByteBuffer buffer) {
        try {
            ByteBuffer duplicate = buffer.asReadOnlyBuffer();
            duplicate.clear();
            byte[] bytes = new byte[duplicate.remaining()];
            duplicate.get(bytes);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return null;

            NativeImage ni = new NativeImage(img.getWidth(), img.getHeight(), false);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    ni.setColorArgb(x, y, img.getRGB(x, y));
                }
            }
            return new NativeImageBackedTexture(ni);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void clearCache() {
        textureCache.values().forEach(AbstractTexture::close);
        textureCache.clear();
        currentTextureHash = "";
    }
}
