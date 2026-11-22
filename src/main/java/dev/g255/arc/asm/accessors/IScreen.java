package dev.g255.arc.asm.accessors;

import java.util.List;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Screen.class})
public interface IScreen {
   @Accessor("drawables")
   List<Drawable> getDrawables();

   /**
    * {@code Screen.addDrawableChild} 是 protected，而且声明在 Screen 上、不是子类自己的成员，
    * 所以子类 mixin（比如 MixinTitleScreen）用 {@code @Shadow} 拿不到它，会在运行期炸
    * "was not located in the target class"。
    *
    * <p>这里在 Screen 自己的 mixin 里开一个 invoker，编译期是普通接口调用，
    * 运行期任何 Screen 实例（含 TitleScreen）都实现了本接口，强转即可用。</p>
    */
   @Invoker("addDrawableChild")
   <T extends Element & Drawable & Selectable> T arc$addDrawableChild(T drawableElement);
}
