package com.github.alexthe666.alexsmobs.client.render.item;

//? if <1.21.4 {
import com.github.alexthe666.alexsmobs.client.render.AMItemstackRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
//?}
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class AMItemRenderProperties implements IClientItemExtensions {

    // 1.21.4 removed IClientItemExtensions#getCustomRenderer (BEWLR) — custom item rendering moved to
    // SpecialModelRenderer. The items still route their initializeClient here (harmlessly) so this class
    // stays; on >=1.21.4 it simply supplies no custom renderer and the items use their default models.
    //? if <1.21.4 {
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return new AMItemstackRenderer();
    }
    //?}
}
