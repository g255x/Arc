package dev.baimaodaxia.mod.modules.impl.misc;

import dev.baimaodaxia.api.events.eventbus.EventListener;
import dev.baimaodaxia.api.events.impl.UpdateEvent;
import dev.baimaodaxia.api.utils.combat.CombatUtil;
import dev.baimaodaxia.api.utils.math.MathUtil;
import dev.baimaodaxia.api.utils.math.Timer;
import dev.baimaodaxia.mod.modules.Module;
import dev.baimaodaxia.mod.modules.settings.impl.EnumSetting;
import dev.baimaodaxia.mod.modules.settings.impl.SliderSetting;
import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public class AutoSex extends Module {
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 5, 1, 10));
   private final EnumSetting<SexMode> mode;
   private final SliderSetting msgDelay;
   private static final String[] PASSIVE_MESSAGES = new String[]{"想要！", "别插了啊啊！好疼呜呜呜。。。", "不要不要啊啊啊，哥哥你好坏！", "Be careful daddy <3", "Oh, I feel it inside me!"};
   private static final String[] ACTIVE_MESSAGES = new String[]{"Oh, I'm cumming!", "Oh, ur pussy is so nice!", "Yeah, yeah", "I feel u!", "Oh, im inside u"};
   private PlayerEntity target;
   private final Timer messageTimer;
   private final Timer sneakTimer;

   public AutoSex() {
      super("AutoSex", Module.Category.Misc);
      this.mode = this.add(new EnumSetting("Mode", AutoSex.SexMode.Active));
      this.msgDelay = this.add(new SliderSetting("MessageDelay", 1, 0, 50));
      this.messageTimer = new Timer();
      this.sneakTimer = new Timer();
      this.setChinese("自动性行为");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         PlayerEntity player = CombatUtil.getClosestEnemy(this.targetRange.getValue());
         this.target = player;
         if (player != null) {
            switch (((SexMode)this.mode.getValue()).ordinal()) {
               case 0:
                  if (this.sneakTimer.passedMs((double)((long)MathUtil.random(200.0F, 1200.0F)))) {
                     mc.options.sneakKey.setPressed(!mc.options.sneakKey.isPressed());
                     this.sneakTimer.reset();
                  }
                  break;
               case 1:
                  if (!mc.options.sneakKey.isPressed()) {
                     mc.options.sneakKey.setPressed(true);
                  }
            }

            if (this.messageTimer.passedMs(this.msgDelay.getValue() * (double)1000.0F) && mc.getNetworkHandler() != null) {
               List<String> messages = Arrays.stream(this.mode.getValue() == AutoSex.SexMode.Active ? ACTIVE_MESSAGES : PASSIVE_MESSAGES).toList();
               ClientPlayNetworkHandler var10000 = mc.getNetworkHandler();
               String var10001 = this.target.getName().getString();
               var10000.sendChatCommand("msg " + var10001 + " " + (String)messages.get((int)(Math.random() * (double)messages.size())));
               this.messageTimer.reset();
            }

         }
      }
   }

   private static enum SexMode {
      Active,
      Passive;

      // $FF: synthetic method
      private static SexMode[] $values() {
         return new SexMode[]{Active, Passive};
      }
   }
}
