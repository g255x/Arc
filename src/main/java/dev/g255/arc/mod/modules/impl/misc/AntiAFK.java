package dev.g255.arc.mod.modules.impl.misc;

import dev.g255.arc.api.events.eventbus.EventListener;
import dev.g255.arc.api.events.impl.PacketEvent;
import dev.g255.arc.api.events.impl.UpdateEvent;
import dev.g255.arc.api.utils.math.Timer;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.settings.impl.BooleanSetting;
import dev.g255.arc.mod.modules.settings.impl.SliderSetting;
import dev.g255.arc.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Hand;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class AntiAFK extends Module {
   public final SliderSetting delay = this.add((new SliderSetting("Delay", (double)15.0F, (double)1.0F, (double)300.0F, (double)1.0F)).setSuffix("s"));
   public final BooleanSetting jump = this.add(new BooleanSetting("Jump", true));
   public final BooleanSetting sneak = this.add(new BooleanSetting("Sneak", false));
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   public final BooleanSetting swing = this.add(new BooleanSetting("Swing", true));
   public final BooleanSetting autoReply = this.add(new BooleanSetting("AutoReply", false));
   public final StringSetting reply = this.add(new StringSetting("Reply", "r I am currently AFK :)"));
   private final Timer timer = new Timer();
   private final Timer replyTimer = new Timer();
   private final Random random = new Random();

   public AntiAFK() {
      super("AntiAFK", Module.Category.Misc);
      this.setChinese("防挂机");
   }

   public void onEnable() {
      this.timer.reset();
      this.replyTimer.reset();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && !(this.delay.getValue() <= (double)0.0F)) {
         if (mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F || mc.player.input.jumping || mc.player.input.sneaking) {
            this.timer.reset();
         }

         if (this.timer.passedS(this.delay.getValue())) {
            List<Runnable> actions = new ArrayList();
            if (this.jump.getValue()) {
               actions.add((Runnable)() -> mc.player.input.jumping = true);
            }

            if (this.sneak.getValue()) {
               actions.add((Runnable)() -> mc.player.input.sneaking = true);
            }

            if (this.rotate.getValue()) {
               actions.add((Runnable)() -> {
                  mc.player.setYaw(this.random.nextFloat(360.0F));
                  mc.player.setPitch(this.random.nextFloat(75.0F) - 37.5F);
               });
            }

            if (this.swing.getValue()) {
               actions.add((Runnable)() -> mc.player.swingHand(Hand.MAIN_HAND));
            }

            if (!actions.isEmpty()) {
               Collections.shuffle(actions);
               ((Runnable)actions.get(0)).run();
            }

            this.timer.reset();
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck() && this.autoReply.getValue()) {
         Object p = event.getPacket();
         String text = null;
         if (p instanceof GameMessageS2CPacket && ((GameMessageS2CPacket)p).content() != null) {
            text = ((GameMessageS2CPacket)p).content().getString();
         } else if (p instanceof ChatMessageS2CPacket) {
            ChatMessageS2CPacket cp = (ChatMessageS2CPacket)p;
            text = cp.unsignedContent() != null ? cp.unsignedContent().getString() : cp.body().content();
         }

         if (text != null && this.replyTimer.passedS(Math.max((double)5.0F, this.delay.getValue()))) {
            mc.getNetworkHandler().sendChatMessage(this.reply.getValue());
            this.replyTimer.reset();
         }

      }
   }
}
