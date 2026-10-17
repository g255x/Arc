package dev.baimaodaxia.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.mod.modules.impl.misc.ExtraTab;
import java.util.Comparator;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListHud.class})
public abstract class MixinPlayerListHud {
   @Final
   @Shadow
   private static Comparator<PlayerListEntry> ENTRY_ORDERING;

   @Shadow
   protected abstract List<PlayerListEntry> collectPlayerEntries();

   @Inject(
      method = {"collectPlayerEntries"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
      if (ExtraTab.INSTANCE.isOn()) {
         cir.setReturnValue(Wrapper.mc.player.networkHandler.getListedPlayerListEntries().stream().sorted(ENTRY_ORDERING).limit((long)ExtraTab.INSTANCE.tabSize.getValueInt()).toList());
      }

   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Ljava/lang/Math;min(II)I",
   shift = Shift.BEFORE
)}
   )
   private void hookRender(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
      int totalPlayers = this.collectPlayerEntries().size();
      int newP = 1;

      int newO;
      for(newO = (totalPlayers + newP - 1) / newP; newO > ExtraTab.INSTANCE.tabHeight.getValueInt(); newO = (totalPlayers + newP - 1) / newP) {
         ++newP;
      }

      o.set(newO);
      p.set(newP);
   }

   @Inject(
      method = {"getPlayerName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void hookGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
      if (ExtraTab.INSTANCE != null && ExtraTab.INSTANCE.isOn()) {
         cir.setReturnValue(ExtraTab.INSTANCE.getPlayerName(entry));
      }

   }
}
