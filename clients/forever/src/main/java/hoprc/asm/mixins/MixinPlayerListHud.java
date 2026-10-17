package hoprc.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.kissoo.Forever;
import dev.kissoo.api.utils.Wrapper;
import dev.kissoo.chat.ChatClient;
import dev.kissoo.mod.modules.impl.misc.ExtraTab;
import dev.kissoo.mod.modules.impl.misc.IrcModule;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// priority > 1000 (default) so our RETURN injection runs after other mods that
// also hook getPlayerName, ensuring the IRC label is always the outermost prefix.
@Mixin(value = PlayerListHud.class, priority = 1100)
public abstract class MixinPlayerListHud {
   @Final
   @Shadow
   private static Comparator<PlayerListEntry> ENTRY_ORDERING;

   @Shadow
   protected abstract List<PlayerListEntry> collectPlayerEntries();

   @Inject(method = "collectPlayerEntries", at = @At("HEAD"), cancellable = true)
   private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
      if (ExtraTab.INSTANCE.isOn()) {
         cir.setReturnValue(
                 Wrapper.mc.player.networkHandler.getListedPlayerListEntries().stream().sorted(ENTRY_ORDERING).limit(ExtraTab.INSTANCE.size.getValueInt()).toList()
         );
      }
   }

   @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = Shift.BEFORE))
   private void hookRender(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
      int totalPlayers = this.collectPlayerEntries().size();
      int newP = 1;

      int newO;
      for (newO = (totalPlayers + newP - 1) / newP; newO > ExtraTab.INSTANCE.columns.getValueInt(); newO = (totalPlayers + newP - 1) / newP) {
         newP++;
      }

      o.set(newO);
      p.set(newP);
   }

   @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
   public void onGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
      if (entry == null || entry.getProfile() == null) return;
      String playerName = entry.getProfile().getName();
      Text result = cir.getReturnValue();
      boolean changed = false;
      boolean ircUser = ChatClient.isIrcUser(playerName);
      // 身份前缀：[Dev]/[Beta]/[User]，与 NameTags 共用同一判定
      String tag = ChatClient.getUserTagPrefix(playerName);
      Text tagText = Text.literal(tag);

      if (!ircUser && Forever.FRIEND != null && Forever.FRIEND.isFriend(playerName)) {
         result = Text.empty()
                 .append(Text.literal("§a[Friend] §r"))
                 .append(tagText)
                 .append(result != null ? result : Text.literal(playerName))
                 .formatted(Formatting.GREEN);
         changed = true;
      }

      if (ircUser) {
         // Use Text.empty() as root to isolate our label from any style that
         // server-provided display names might inherit into sibling nodes.
         result = Text.empty()
                 .append(Text.literal(IrcModule.ircStatusPrefix(playerName)))
                 .append(tagText)
                 .append(result != null ? result : Text.literal(playerName));
         changed = true;
      }

      if (!changed && !tag.isEmpty()) {
         result = Text.empty()
                 .append(tagText)
                 .append(result != null ? result : Text.literal(playerName));
         changed = true;
      }

      if (changed) cir.setReturnValue(result);
   }
}
