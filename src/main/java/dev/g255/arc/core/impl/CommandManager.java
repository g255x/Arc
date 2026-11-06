package dev.g255.arc.core.impl;

import dev.g255.arc.Arc;
import dev.g255.arc.api.interfaces.IChatHudHook;
import dev.g255.arc.api.utils.Wrapper;
import dev.g255.arc.mod.commands.Command;
import dev.g255.arc.mod.commands.impl.AimCommand;
import dev.g255.arc.mod.commands.impl.BindCommand;
import dev.g255.arc.mod.commands.impl.BindsCommand;
import dev.g255.arc.mod.commands.impl.CleanerCommand;
import dev.g255.arc.mod.commands.impl.ClipCommand;
import dev.g255.arc.mod.commands.impl.EsuCommand;
import dev.g255.arc.mod.commands.impl.FakePlayerCommand;
import dev.g255.arc.mod.commands.impl.FriendCommand;
import dev.g255.arc.mod.commands.impl.GamemodeCommand;
import dev.g255.arc.mod.commands.impl.GcCommand;
import dev.g255.arc.mod.commands.impl.KitCommand;
import dev.g255.arc.mod.commands.impl.LoadCommand;
import dev.g255.arc.mod.commands.impl.PeekCommand;
import dev.g255.arc.mod.commands.impl.PingCommand;
import dev.g255.arc.mod.commands.impl.PrefixCommand;
import dev.g255.arc.mod.commands.impl.RejoinCommand;
import dev.g255.arc.mod.commands.impl.ReloadCommand;
import dev.g255.arc.mod.commands.impl.SaveCommand;
import dev.g255.arc.mod.commands.impl.TCommand;
import dev.g255.arc.mod.commands.impl.TeleportCommand;
import dev.g255.arc.mod.commands.impl.ToggleCommand;
import dev.g255.arc.mod.commands.impl.TradeCommand;
import dev.g255.arc.mod.commands.impl.WatermarkCommand;
import dev.g255.arc.mod.commands.impl.WebCommand;
import dev.g255.arc.mod.commands.impl.XrayCommand;
import dev.g255.arc.mod.modules.Module;
import dev.g255.arc.mod.modules.impl.client.ClientSetting;
import java.util.HashMap;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.ChatHud;

public class CommandManager implements Wrapper {
   private final HashMap<String, Command> commands = new HashMap();

   public CommandManager() {
      this.init();
   }

   public void init() {
      this.registerCommand(new AimCommand());
      this.registerCommand(new BindCommand());
      this.registerCommand(new BindsCommand());
      this.registerCommand(new CleanerCommand());
      this.registerCommand(new ClipCommand());
      this.registerCommand(new EsuCommand());
      this.registerCommand(new FakePlayerCommand());
      this.registerCommand(new FriendCommand());
      this.registerCommand(new XrayCommand());
      this.registerCommand(new GamemodeCommand());
      this.registerCommand(new KitCommand());
      this.registerCommand(new LoadCommand());
      this.registerCommand(new PingCommand());
      this.registerCommand(new PrefixCommand());
      this.registerCommand(new RejoinCommand());
      this.registerCommand(new ReloadCommand());
      this.registerCommand(new SaveCommand());
      this.registerCommand(new TeleportCommand());
      this.registerCommand(new TCommand());
      this.registerCommand(new ToggleCommand());
      this.registerCommand(new TradeCommand());
      this.registerCommand(new WatermarkCommand());
      this.registerCommand(new PeekCommand());
      this.registerCommand(new GcCommand());
      this.registerCommand(new WebCommand());
   }

   public static void sendMessage(String message) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
               mc.inGameHud.getChatHud().addMessage(Text.of(message));
            } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
               ChatHud var1 = mc.inGameHud.getChatHud();
               String var2 = ClientSetting.INSTANCE.hackName.getValue();
               var1.addMessage(Text.of("§f[§b" + var2 + "§f] " + message));
            } else {
               IChatHudHook var10000 = (IChatHudHook)mc.inGameHud.getChatHud();
               String var10001 = ClientSetting.INSTANCE.hackName.getValue();
               var10000.arc$addMessage(Text.of(var10001 + "§f " + message));
            }
         }
      });
   }

   public static void sendMessageId(String message, int id) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Earth) {
               ((IChatHudHook)mc.inGameHud.getChatHud()).arc$addMessage(Text.of(message), id);
            } else if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
               IChatHudHook var2 = (IChatHudHook)mc.inGameHud.getChatHud();
               String var3 = ClientSetting.INSTANCE.hackName.getValue();
               var2.arc$addMessage(Text.of("§f[§b" + var3 + "§f] " + message), id);
            } else {
               IChatHudHook var10000 = (IChatHudHook)mc.inGameHud.getChatHud();
               String var10001 = ClientSetting.INSTANCE.hackName.getValue();
               var10000.arc$addMessage(Text.of(var10001 + "§f " + message), id);
            }
         }
      });
   }

   public static void sendChatMessageWidthIdNoSync(String message, int id) {
      mc.execute(() -> {
         if (!Module.nullCheck()) {
            ((IChatHudHook)mc.inGameHud.getChatHud()).arc$addMessageOutSync(Text.of("§f" + message), id);
         }
      });
   }

   private void registerCommand(Command command) {
      this.commands.put(command.getName(), command);
   }

   public Command getCommandBySyntax(String string) {
      return (Command)this.commands.get(string);
   }

   public HashMap<String, Command> getCommands() {
      return this.commands;
   }

   public void command(String[] commandIn) {
      Command command = (Command)this.commands.get(commandIn[0].substring(Arc.getPrefix().length()).toLowerCase());
      if (command == null) {
         sendMessage("§4Invalid Command!");
      } else {
         String[] parameterList = new String[commandIn.length - 1];
         System.arraycopy(commandIn, 1, parameterList, 0, commandIn.length - 1);
         if (parameterList.length == 1 && parameterList[0].equals("help")) {
            command.sendUsage();
            return;
         }

         command.runCommand(parameterList);
      }

   }
}
