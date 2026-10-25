package pro.coreservermod.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
    @Invoker("setText")
    void coreservermod$setText(Component text);

    @Invoker("getText")
    Component coreservermod$getText();

    @Invoker("setBackgroundColor")
    void coreservermod$setBackgroundColor(int color);

    @Invoker("setFlags")
    void coreservermod$setFlags(byte flags);
}
