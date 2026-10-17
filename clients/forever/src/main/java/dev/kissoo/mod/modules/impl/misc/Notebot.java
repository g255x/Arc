package dev.kissoo.mod.modules.impl.misc;

import dev.kissoo.Forever;
import dev.kissoo.api.events.impl.UpdateEvent;
import dev.kissoo.api.utils.notebot.NotebotUtils;
import dev.kissoo.api.utils.notebot.decoder.SongDecoders;
import dev.kissoo.api.utils.notebot.song.Note;
import dev.kissoo.api.utils.notebot.song.Song;
import dev.kissoo.api.utils.render.Render3DUtil;
import dev.kissoo.api.utils.world.BlockUtil;
import dev.kissoo.core.Manager;
import dev.kissoo.mod.modules.Module;
import dev.kissoo.mod.modules.settings.impl.BooleanSetting;
import dev.kissoo.mod.modules.settings.impl.EnumSetting;
import dev.kissoo.mod.modules.settings.impl.SliderSetting;
import dev.kissoo.mod.modules.settings.impl.StringSetting;
import hoprc.api.events.eventbus.EventListener;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Notebot extends Module {
    private final StringSetting fileName = this.add(new StringSetting("FileName", "song.nbs"));
    private final SliderSetting range = this.add(new SliderSetting("Range", 15, 5, 50));
    private final SliderSetting tickDelay = this.add(new SliderSetting("TickDelay", 1, 1, 10));
    private final SliderSetting concurrentTuneBlocks = this.add(new SliderSetting("ConcurrentTuneBlocks", 20, 1, 200));
    private final EnumSetting<NotebotUtils.NotebotMode> mode = this.add(new EnumSetting<>("Mode", NotebotUtils.NotebotMode.ExactInstruments));
    private final BooleanSetting autoRotate = this.add(new BooleanSetting("AutoRotate", false));
    private final BooleanSetting renderBoxes = this.add(new BooleanSetting("RenderBoxes", true));
    private final BooleanSetting renderText = this.add(new BooleanSetting("RenderText", true));

    private enum Stage {
        None, LoadingSong, SetUp, Tune, WaitingToCheckNoteblocks, Playing
    }

    private Stage stage = Stage.None;
    private Song song;

    private final Map<BlockPos, BlockState> noteblocks = new HashMap<>();
    private final Map<Note, List<BlockPos>> noteblocksMap = new HashMap<>();
    private final Map<BlockPos, Integer> tuneHits = new HashMap<>();
    private final List<BlockPos> tuneQueue = new ArrayList<>();

    private int tick;
    private int delayTicks;
    private String error;

    public Notebot() {
        super("Notebot", "Automatically plays a song using note blocks around you.", Module.Category.Misc);
        this.setChinese("自动音乐播放器");
    }

    @Override
    public void onEnable() {
        this.stage = Stage.None;
        this.error = null;
        this.loadSong();
    }

    @Override
    public void onDisable() {
        this.stage = Stage.None;
        this.song = null;
        this.noteblocks.clear();
        this.noteblocksMap.clear();
        this.tuneHits.clear();
        this.tuneQueue.clear();
    }

    private void loadSong() {
        this.stage = Stage.LoadingSong;
        File file = new File(Manager.getFolder(), this.fileName.getValue());

        if (!file.exists() || !SongDecoders.hasDecoder(file)) {
            this.error = "Song file not found or unsupported: " + file.getName();
            this.sendMessage("§4Notebot: " + this.error);
            this.disable();
            return;
        }

        try {
            this.song = SongDecoders.parse(file);
            this.tick = 0;
            this.stage = Stage.SetUp;
            this.sendMessage("§2Notebot: §floaded \"" + this.song.getTitle() + "\"");
        } catch (Exception e) {
            this.error = "Failed to parse song: " + e.getMessage();
            this.sendMessage("§4Notebot: " + this.error);
            this.disable();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck() || this.song == null) return;

        switch (this.stage) {
            case SetUp -> {
                this.scanForNoteblocks();
                this.setupNoteblocksMap();
                this.setupTuneQueue();
                this.stage = this.tuneQueue.isEmpty() ? Stage.Playing : Stage.Tune;
            }
            case Tune -> this.tune();
            case WaitingToCheckNoteblocks -> {
                if (--this.delayTicks <= 0) {
                    this.stage = Stage.SetUp;
                }
            }
            case Playing -> this.playTick();
            default -> {
            }
        }
    }

    private void scanForNoteblocks() {
        this.noteblocks.clear();

        for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePos())) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() instanceof NoteBlock) {
                this.noteblocks.put(pos.toImmutable(), state);
            }
        }
    }

    private void setupNoteblocksMap() {
        this.noteblocksMap.clear();

        for (Map.Entry<BlockPos, BlockState> entry : this.noteblocks.entrySet()) {
            Note note = NotebotUtils.getNoteFromNoteBlock(entry.getValue(), this.mode.getValue());
            this.noteblocksMap.computeIfAbsent(note, k -> new ArrayList<>()).add(entry.getKey());
        }
    }

    private void setupTuneQueue() {
        this.tuneQueue.clear();
        this.tuneHits.clear();

        for (Note required : this.song.getRequirements()) {
            List<BlockPos> candidates = this.noteblocksMap.get(required);
            if (candidates == null || candidates.isEmpty()) continue;

            BlockPos pos = candidates.remove(0);
            BlockState state = this.noteblocks.get(pos);
            int currentLevel = state.get(NoteBlock.NOTE);
            int hits = NotebotUtils.calcNumberOfHits(currentLevel, required.getNoteLevel());

            if (hits > 0) {
                this.tuneHits.put(pos, hits);
                this.tuneQueue.add(pos);
            }
        }
    }

    private void tune() {
        int tuned = 0;

        while (tuned < this.concurrentTuneBlocks.getValueInt() && !this.tuneQueue.isEmpty()) {
            BlockPos pos = this.tuneQueue.get(0);
            Direction side = BlockUtil.getClickSideStrict(pos);

            if (side == null) {
                this.tuneQueue.remove(0);
                this.tuneHits.remove(pos);
                continue;
            }

            BlockUtil.clickBlock(pos, side, this.autoRotate.getValue(), Hand.MAIN_HAND, true);

            int remaining = this.tuneHits.get(pos) - 1;
            if (remaining <= 0) {
                this.tuneQueue.remove(0);
                this.tuneHits.remove(pos);
            } else {
                this.tuneHits.put(pos, remaining);
            }

            tuned++;
        }

        if (this.tuneQueue.isEmpty()) {
            this.delayTicks = this.tickDelay.getValueInt();
            this.stage = Stage.WaitingToCheckNoteblocks;
        }
    }

    private void playTick() {
        List<Note> notes = this.song.get(this.tick);

        for (Note note : notes) {
            List<BlockPos> candidates = this.noteblocksMap.get(note);
            if (candidates == null || candidates.isEmpty()) continue;

            BlockPos pos = candidates.get(0);
            Direction side = BlockUtil.getClickSideStrict(pos);
            if (side == null) continue;

            if (this.autoRotate.getValue()) {
                Forever.ROTATION.snapAt(pos.toCenterPos());
            }

            sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, side, id));

            if (this.autoRotate.getValue()) {
                Forever.ROTATION.snapBack();
            }
        }

        this.tick++;
        if (this.tick > this.song.getLastTick()) {
            this.sendMessage("§2Notebot: §ffinished playing \"" + this.song.getTitle() + "\"");
            this.disable();
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!this.renderBoxes.getValue()) return;

        for (BlockPos pos : this.noteblocks.keySet()) {
            Color color = this.stage == Stage.Tune && this.tuneHits.containsKey(pos) ? new Color(255, 165, 0, 100) : new Color(0, 255, 0, 60);
            Render3DUtil.drawBox(matrixStack, new Box(pos), color);
        }
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (!this.renderText.getValue() || this.song == null) return;
    }

    @Override
    public String getInfo() {
        if (this.error != null) return "error";
        return switch (this.stage) {
            case Tune -> "tuning";
            case Playing -> "playing";
            case WaitingToCheckNoteblocks -> "waiting";
            case SetUp -> "setup";
            default -> null;
        };
    }
}
