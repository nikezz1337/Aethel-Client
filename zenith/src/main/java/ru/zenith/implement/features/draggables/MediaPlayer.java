package ru.zenith.implement.features.draggables;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.BufferUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.core.Main;
import ru.zenith.implement.features.modules.render.Hud;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPlayer extends AbstractDraggable {
    public static MediaPlayer getInstance() {
        return Instance.getDraggable(MediaPlayer.class);
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private MediaInfo mediaInfo = new MediaInfo("Название Трека", "Артист", new byte[0], 43, 150, false);
    private final Identifier artwork = Identifier.of("textures/xyu.png");
    private final StopWatch lastMedia = new StopWatch();
    public IMediaSession session;
    private float widthDuration;

    public MediaPlayer() {
        super("Media Player", 10, 400, 100, 40,true);
    }

    @Override
    public boolean visible() {
        return !lastMedia.finished(2000) || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        if (Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Media Player") && mc.player.age % 5 == 0) executorService.submit(() -> {
            IMediaSession currentSession = session = MediaPlayerInfo.Instance.getMediaSessions().stream().max(Comparator.comparing(s -> s.getMedia().getPlaying())).orElse(null);
            if (currentSession != null) {
                MediaInfo info = currentSession.getMedia();
                if (!info.getTitle().isEmpty() || !info.getArtist().isEmpty()) {
                    if (mediaInfo.getTitle().equals("Название Трека") || !Arrays.toString(mediaInfo.getArtworkPng()).equals(Arrays.toString(info.getArtworkPng()))) {
                        BufferUtil.registerTexture(artwork, info.getArtworkPng());
                    }
                    mediaInfo = info;
                    lastMedia.reset();
                }
            }
        });
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        ScissorManager scissor = Main.getInstance().getScissorManager();
        FontRenderer big = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer mini = Fonts.getSize(12, Fonts.Type.DEFAULT);
        int sizeArtwork = 32;
        int sizePausePlay = 4;
        int maxDurationWidth = getWidth() - (sizeArtwork + 12);
        int duration = (int) mediaInfo.getDuration();
        int position = MathHelper.clamp((int) mediaInfo.getPosition(),0, duration);
        String timeDuration = StringUtil.getDuration(duration);
        widthDuration = MathHelper.clamp(MathUtil.interpolateSmooth(1, widthDuration, Math.round((float) position / duration * maxDurationWidth)),1, maxDurationWidth);

        blur.render(ShapeProperties.create(matrix,getX(),getY(),getWidth(),getHeight()).thickness(2.25F).softness(1)
                .round(4).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        scissor.push(matrix.peek().getPositionMatrix(),getX() + sizeArtwork + 8, getY(),getWidth() - sizeArtwork - 10,getHeight());
        big.drawStringWithScroll(matrix, mediaInfo.getTitle(), getX() + sizeArtwork + 8,getY() + 7, 56, ColorUtil.getText());
        mini.drawStringWithScroll(matrix, mediaInfo.getArtist(), getX() + sizeArtwork + 8, getY() + 15.5F, 56, ColorUtil.getText(0.75F));
        scissor.pop();

        Render2DUtil.drawTexture(context,artwork,getX() + 4,getY() + 4,sizeArtwork,3,sizeArtwork,sizeArtwork,sizeArtwork,ColorUtil.getRect(1));
        mini.drawString(matrix, StringUtil.getDuration(position),getX() + 8 + sizeArtwork,getY() + 27,ColorUtil.getText());
        mini.drawString(matrix, timeDuration,getX() + getWidth() - 4 - mini.getStringWidth(timeDuration),getY() + 27,ColorUtil.getText());

        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8, maxDurationWidth, 2)
                .round(0.75F).color(ColorUtil.getRectDarker(0.75F)).build());

        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8, widthDuration, 2)
                .softness(4).round(1).color(ColorUtil.roundClientColor(0.2F)).build());

        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8, widthDuration, 2)
                .round(0.75F).color(ColorUtil.roundClientColor(1)).build());

        Render2DUtil.drawTexture(context,Identifier.of("textures/" + (mediaInfo.getPlaying() ? "pause" : "play") + ".png"), (float) (getX() + (double) (getWidth() + sizeArtwork + 4 - sizePausePlay) / 2),getY() + 26,sizePausePlay);
    }
}
