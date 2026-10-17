package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.fabric.world.FabricStructureSpawns;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Adds this mod's four structure spawn overrides on Fabric. See {@link FabricStructureSpawns} for
 * what they are, why the merge happens here rather than at registry-load time as on Forge and
 * NeoForge, and how vanilla's own overrides are preserved.
 *
 * <p>{@code spawnOverrides()} is the only way a structure's overrides are ever read — vanilla's
 * {@code StructureManager}/{@code NaturalSpawner} path goes through it, never through the
 * {@code StructureSettings} record directly — so this is the equivalent of having rewritten the
 * settings at load time, at the cost of running on every spawn attempt. That cost is why the merged
 * map is cached per structure on the driver side.
 *
 * <p>The signature is identical on all 17 Fabric nodes, 1.20.1 through 26.2 (checked, rule 10), so
 * no arms. It is a concrete method on the abstract {@code Structure} base, not on any subclass, so
 * one mixin covers every structure type including modded ones.
 */
@Mixin(Structure.class)
public class FabricStructureMixin {

    @Inject(method = "spawnOverrides()Ljava/util/Map;", at = @At("RETURN"), cancellable = true)
    private void alexsmobs$addStructureSpawns(CallbackInfoReturnable<Map<MobCategory, StructureSpawnOverride>> cir) {
        cir.setReturnValue(FabricStructureSpawns.merge((Structure) (Object) this, cir.getReturnValue()));
    }
}
