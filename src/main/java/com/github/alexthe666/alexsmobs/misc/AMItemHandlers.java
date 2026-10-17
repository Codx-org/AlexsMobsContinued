package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Item-handler access, which is the one capability Alex's Mobs both consumes and exposes.
 *
 * Forge asks the block entity itself ({@code getCapability} returning a {@code LazyOptional});
 * NeoForge replaced that with a static {@code BlockCapability} queried against the level, and
 * moved providers out of the block entity into a mod-bus registration event. Both shapes live
 * here so the consumers (the crow's chest deposit, the capsid's output) and the capsid itself
 * stay loader-neutral.
 */
public class AMItemHandlers {

    /**
     * The item handler a neighbouring block exposes on the given side, or null if it has none.
     */
    @Nullable
    public static IItemHandler find(@Nullable BlockEntity blockEntity, @Nullable Direction side) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return null;
        }
        //? if forge {
        return blockEntity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
        //?}
        // NeoForge 1.21.9 rebuilt item transfer on the generic ResourceHandler API: the capability
        // now yields a ResourceHandler<ItemResource>, and IItemHandler survives only as a
        // compatibility view over one (IItemHandler#of).
        //? if neoforge && >=1.21.9 {
        /*net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> handler =
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK.getCapability(
                        blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, side);
        return handler == null ? null : IItemHandler.of(handler);
        *///?}
        //? if neoforge && <1.21.9 {
        /*return net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK.getCapability(
                blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, side);
        *///?}
        // Fabric API's transfer API is the direct equivalent: a BlockApiLookup keyed on the same
        // (level, pos, state, block entity, side) tuple, yielding a Storage<ItemVariant>. Its own
        // fallback wraps any vanilla Container / WorldlyContainer, so this covers chests and the
        // capsid as well as modded inventories, exactly as the Forge capability does.
        //? if fabric {
        /*net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> storage =
                net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(
                        blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, side);
        if (storage == null || !storage.supportsInsertion()) {
            return null;
        }
        // A transaction that is closed without commit() is rolled back, which is exactly what the
        // simulate flag means here.
        return (stack, simulate) -> {
            try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction =
                         net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                int inserted = (int) storage.insert(
                        net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(stack), stack.getCount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                if (inserted <= 0) {
                    return stack;
                }
                if (inserted >= stack.getCount()) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
                net.minecraft.world.item.ItemStack remainder = stack.copy();
                remainder.setCount(stack.getCount() - inserted);
                return remainder;
            }
        };
        *///?}
    }

    // On Forge the capsid answers getCapability itself; NeoForge wants the provider registered
    // against the block entity type up front. Wired from the AlexsMobs constructor.
    //? if neoforge && >=1.21.9 {
    /*public static void onRegisterCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                com.github.alexthe666.alexsmobs.tileentity.AMTileEntityRegistry.CAPSID.get(),
                (capsid, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(capsid, side == null ? Direction.UP : side));
    }
    *///?}
    //? if neoforge && <1.21.9 {
    /*public static void onRegisterCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                com.github.alexthe666.alexsmobs.tileentity.AMTileEntityRegistry.CAPSID.get(),
                (capsid, side) -> new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(capsid, side == null ? Direction.UP : side));
    }
    *///?}
}
