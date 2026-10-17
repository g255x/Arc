package dev.kissoo.mod.modules.impl.client;

import dev.kissoo.Forever;
import hoprc.api.events.eventbus.EventListener;
import dev.kissoo.api.events.impl.Render2DEvent;
import dev.kissoo.api.utils.render.Render2DUtil;
import dev.kissoo.api.utils.render.TextUtil;
import dev.kissoo.mod.gui.music.MusicPlayerScreen;
import dev.kissoo.mod.music.MusicPlayerConfig;
import dev.kissoo.mod.music.MusicPlayerService;
import dev.kissoo.mod.music.NeteaseSong;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;

import java.awt.Color;

public final class MusicPlayer extends Module {
    public enum Source {
        netease,
        kuwo,
        joox
    }

    public static final MusicPlayerService SERVICE = new MusicPlayerService();
    public static final MusicPlayerConfig  CFG     = MusicPlayerConfig.load();

    public final EnumSetting<Source> source = add(new EnumSetting<>("Source", sourceFromConfig()).injectTask(this::applyGdSettings));
    public final SliderSetting bitrate      = add(new SliderSetting("Bitrate", CFG.gdBitrate, 128, 999, 1).injectTask(this::applyGdSettings));
    public final SliderSetting lyricOffsetY = add(new SliderSetting("LyricOffsetY", 0, -200, 200));
    public final SliderSetting hudX         = add(new SliderSetting("HudX", 10, 0, 800));
    public final SliderSetting hudY         = add(new SliderSetting("HudY", 10, 0, 600));
    public final SliderSetting hudScale     = add(new SliderSetting("HudScale", 1.0, 0.5, 2.0, 0.1));
    private final MusicHudRenderer hudRenderer = new MusicHudRenderer();
    private boolean hudRendererRegistered = false;

    public MusicPlayer() {
        super("MusicPlayer", Category.Client);
        setChinese("音乐播放器");
        SERVICE.setVolume01(CFG.volume01);
        applyGdSettings();
    }

    private static Source sourceFromConfig() {
        if (CFG == null || CFG.gdSource == null) return Source.netease;
        try {
            return Source.valueOf(CFG.gdSource.trim().toLowerCase());
        } catch (Throwable ignored) {
            return Source.netease;
        }
    }

    private void applyGdSettings() {
        int requested = bitrate == null ? CFG.gdBitrate : bitrate.getValueInt();
        int br = requested >= 870 ? 999 : requested >= 530 ? 740 : requested >= 256 ? 320 : requested >= 160 ? 192 : 128;
        SERVICE.api().setGdSource(source == null ? CFG.gdSource : source.getValue().name());
        SERVICE.api().setGdBitrate(br);
        CFG.gdSource = SERVICE.api().getGdSource();
        CFG.gdBitrate = SERVICE.api().getGdBitrate();
        CFG.save();
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            this.state = false;
            Forever.EVENT_BUS.unsubscribe(this);
            return;
        }
        if (!hudRendererRegistered) {
            Forever.EVENT_BUS.subscribe(hudRenderer);
            hudRendererRegistered = true;
        }
        applyGdSettings();
        mc.setScreen(new MusicPlayerScreen(SERVICE, CFG));
        this.state = false;
        Forever.EVENT_BUS.unsubscribe(this);
    }

    @Override
    public void onDisable() {
    }
    public class MusicHudRenderer {

        @EventListener
        public void onRender2D(Render2DEvent e) {
            if (!CFG.desktopLyrics) return;

            NeteaseSong cur = SERVICE.getCurrentSong();
            if (cur == null) return;

            if (mc == null || mc.getWindow() == null) return;

            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            long   pos  = SERVICE.getSongPositionMs();
            String line = SERVICE.getLyricAt(pos);
            String next = SERVICE.getNextLyricAt(pos);
            int    y    = sh - 54 + lyricOffsetY.getValueInt();

            if (line == null || line.isBlank()) {
                String songInfo = cur.name() + " - " + cur.artists();
                if (songInfo.length() > 40) songInfo = songInfo.substring(0, 37) + "...";
                TextUtil.drawCenteredString(e.drawContext, "♪ " + songInfo + " ♪",
                        sw / 2.0, y, new Color(180, 180, 180).getRGB(), true);
                TextUtil.drawCenteredString(e.drawContext, "暂无歌词",
                        sw / 2.0, y + 14, new Color(120, 120, 120).getRGB(), true);
            } else {
                TextUtil.drawCenteredString(e.drawContext, line,
                        sw / 2.0, y, Color.WHITE.getRGB(), true);
                if (next != null && !next.isBlank()) {
                    TextUtil.drawCenteredString(e.drawContext, next,
                            sw / 2.0, y + 14, new Color(255, 255, 255, 150).getRGB(), true);
                }
            }
            float sc    = (float) hudScale.getValue();
            int   barW  = Math.round(160 * sc);
            int   barH  = Math.round(40  * sc);
            int   bx    = Math.max(0, Math.min(sw - barW, hudX.getValueInt()));
            int   by    = Math.max(0, Math.min(sh - barH, hudY.getValueInt()));

            Render2DUtil.drawRect(e.drawContext.getMatrices(), bx, by, barW, barH,
                    new Color(0, 0, 0, 170));
            Render2DUtil.drawRectWithOutline(e.drawContext.getMatrices(), bx, by, barW, barH,
                    new Color(0, 0, 0, 0), new Color(255, 255, 255, 70));

            int art  = Math.round(30 * sc);
            int padX = Math.round(6  * sc);
            int artY = by + (barH - art) / 2;

            var tex = SERVICE.getCurrentAlbumTex();
            if (tex != null) {
                e.drawContext.drawTexture(tex, bx + padX, artY, 0, 0, art, art, art, art);
            } else {
                Render2DUtil.drawRect(e.drawContext.getMatrices(), bx + padX, artY, art, art,
                        new Color(60, 60, 60, 160));
            }

            int    textX      = bx + Math.round(46 * sc);
            String drawName   = cur.name();
            String drawArtist = cur.artists();
            if (drawName   != null && drawName.length()   > 13) drawName   = drawName.substring(0, 12)   + "...";
            if (drawArtist != null && drawArtist.length() > 16) drawArtist = drawArtist.substring(0, 15) + "...";

            TextUtil.drawString(e.drawContext, drawName,   textX, by + Math.round(8  * sc), 0xFFFFFFFF, false);
            TextUtil.drawString(e.drawContext, drawArtist, textX, by + Math.round(22 * sc), new Color(210, 210, 210).getRGB(), false);
        }
    }
}
