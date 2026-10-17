package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.item.CustomTabBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class AMCreativeTabRegistry {


    public static final DeferredRegister<CreativeModeTab> DEF_REG = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AlexsMobs.MODID);

    // Vanilla's only builder factory is CreativeModeTab.builder(Row, int column) — the no-arg
    // overload and withTabsBefore are both loader patches. Fabric API's
    // FabricCreativeModeTab.builder() is the equivalent seam: it allocates a free position itself,
    // so tab ORDER is the loader's choice there rather than ours. (That factory used to live in
    // fabric-item-group-api-v1; the module was renamed fabric-creative-tab-api-v1, and the old
    // package no longer exists in the 26.x Fabric API bundle at all.)
    public static final Supplier<CreativeModeTab> TAB = DEF_REG.register(AlexsMobs.MODID, () ->
            //? if !fabric {
            CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + AlexsMobs.MODID))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            //?} else {
            /*net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + AlexsMobs.MODID))
            *///?}
            .icon(() -> new ItemStack(AMItemRegistry.TAB_ICON.get()))
            .displayItems((enabledFeatures, output) -> {
                for(Supplier<? extends Item> item : AMItemRegistry.DEF_REG.getEntries()){
                    if(item.get() instanceof CustomTabBehavior customTabBehavior){
                        customTabBehavior.fillItemCategory(output);
                    }else{
                        output.accept(item.get());
                    }
                }
            })
            .build());
}
