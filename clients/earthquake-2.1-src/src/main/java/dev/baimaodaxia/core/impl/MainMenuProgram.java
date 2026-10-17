package dev.baimaodaxia.core.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baimaodaxia.api.utils.Wrapper;
import dev.baimaodaxia.api.utils.math.AnimateUtil;
import java.util.Objects;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.ladysnake.satin.api.managed.ManagedCoreShader;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import org.ladysnake.satin.api.managed.uniform.Uniform1f;
import org.ladysnake.satin.api.managed.uniform.Uniform2f;
import org.ladysnake.satin.api.managed.uniform.Uniform4f;

public class MainMenuProgram {
   private Uniform1f Time;
   private Uniform2f uSize;
   private Uniform4f color;
   public static float time_ = 10000.0F;
   public static final ManagedCoreShader MAIN_MENU;

   public MainMenuProgram() {
      this.setup();
   }

   public void setParameters(float x, float y, float width, float height) {
      float i = (float)Wrapper.mc.getWindow().getScaleFactor();
      this.uSize.set(width * i, height * i);
      time_ += (float)(0.55 * (double)AnimateUtil.deltaTime());
      this.Time.set(time_);
   }

   public void use() {
      ManagedCoreShader var10000 = MAIN_MENU;
      Objects.requireNonNull(var10000);
      RenderSystem.setShader(var10000::getProgram);
   }

   protected void setup() {
      this.uSize = MAIN_MENU.findUniform2f("uSize");
      this.Time = MAIN_MENU.findUniform1f("Time");
      this.color = MAIN_MENU.findUniform4f("color");
   }

   static {
      MAIN_MENU = ShaderEffectManager.getInstance().manageCoreShader(Identifier.of("alien", "mainmenu"), VertexFormats.POSITION);
   }
}
