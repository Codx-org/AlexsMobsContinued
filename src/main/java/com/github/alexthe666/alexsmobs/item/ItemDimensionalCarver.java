package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityVoidPortal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemDimensionalCarver extends Item {

    public static final int MAX_TIME = 200;

    public ItemDimensionalCarver(Item.Properties props) {
        super(props);
    }

    protected static BlockHitResult rayTracePortal(Level worldIn, Player player, ClipContext.Fluid fluidMode) {
        final float f = player.getXRot();
        final float f1 = player.getYRot();
        Vec3 vector3d = player.getEyePosition(1.0F);
        final float f11 = -f1 * Mth.DEG_TO_RAD - Mth.PI;
        final float f12 = -f * Mth.DEG_TO_RAD;
        final float f2 = Mth.cos(f11);
        final float f3 = Mth.sin(f11);
        final float f4 = -Mth.cos(f12);
        final float f5 = Mth.sin(f12);
        final float f6 = f3 * f4;
        final float f7 = f2 * f4;
        final double d0 = 1.5F;
        Vec3 vector3d1 = vector3d.add((double) f6 * d0, (double) f5 * d0, (double) f7 * d0);
        return worldIn.clip(new ClipContext(vector3d, vector3d1, ClipContext.Block.OUTLINE, fluidMode, player));
    }

    public int getItemStackLimit(ItemStack stack) {
        return 1; // fix for incompatibility with other mods
    }

    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        if (itemstack.getDamageValue() >= itemstack.getMaxDamage()) {
            return AMCompat.fail(itemstack);
        } else {
            playerIn.startUsingItem(handIn);
            HitResult raytraceresult = rayTracePortal(worldIn, playerIn, ClipContext.Fluid.ANY);
            Direction dir = Direction.orderedByNearest(playerIn)[0];

            double x = raytraceresult.getLocation().x - dir.getNormal().getX() * 0.1F;
            double y = raytraceresult.getLocation().y - dir.getNormal().getY() * 0.1F;
            double z = raytraceresult.getLocation().z - dir.getNormal().getZ() * 0.1F;
            // One local, mutated, then written back — AMCompat.getOrCreateTag returns a COPY, so
            // the old shape (a put per fresh call) would have thrown all four away.
            CompoundTag carverTag = AMCompat.getOrCreateTag(itemstack);
            if (AMCompat.getBoolean(carverTag, "HASBLOCK")) {
                x = AMCompat.getDouble(carverTag, "BLOCKX");
                y = AMCompat.getDouble(carverTag, "BLOCKY");
                z = AMCompat.getDouble(carverTag, "BLOCKZ");
            } else {
                carverTag.putBoolean("HASBLOCK", true);
                carverTag.putDouble("BLOCKX", x);
                carverTag.putDouble("BLOCKY", y);
                carverTag.putDouble("BLOCKZ", z);
                AMCompat.setTag(itemstack, carverTag);
            }
            worldIn.addParticle(AMParticleRegistry.INVERT_DIG.get(), x, y, z, playerIn.getId(), 0, 0);
            return AMCompat.consume(itemstack);
        }

    }

    // 1.21 added the LivingEntity param to Item#getUseDuration. Upstream never wrote @Override
    // here, so from 1.21 up the 1-arg form below quietly stopped overriding anything and every
    // startUsingItem-driven item read vanilla's default instead. It stays as the <1.21 override
    // and as this class's own helper; this delegates to it. See docs/notes/api-eras.md.
    //? if >=1.21 {
    /*@Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity user) {
        return this.getUseDuration(stack);
    }
    *///?}
    public int getUseDuration(ItemStack stack) {
        return 200;
    }

    public float getXpRepairRatio(ItemStack stack) {
        return 100F;
    }

    public void onUseTick(Level level, LivingEntity player, ItemStack itemstack, int count) {
        player.swing(player.getUsedItemHand());
        RandomSource random = player.getRandom();
        if (count % 5 == 0) {
            player.gameEvent(GameEvent.ITEM_INTERACT_START);
            player.playSound(SoundEvents.NETHERITE_BLOCK_HIT, 1, 0.5F + random.nextFloat());
        }
        boolean flag = false;
        CompoundTag carverTag = AMCompat.getOrCreateTag(itemstack);
        if (AMCompat.getBoolean(carverTag, "HASBLOCK")) {
            double x = AMCompat.getDouble(carverTag, "BLOCKX");
            double y = AMCompat.getDouble(carverTag, "BLOCKY");
            double z = AMCompat.getDouble(carverTag, "BLOCKZ");
            if (random.nextFloat() < 0.2) {
                player.level().addParticle(AMParticleRegistry.WORM_PORTAL.get(), x + random.nextGaussian() * 0.1F, y + random.nextGaussian() * 0.1F, z + random.nextGaussian() * 0.1F, random.nextGaussian() * 0.1F, -0.1F, random.nextGaussian() * 0.1F);
            }
            if (player.distanceToSqr(x, y, z) > 9) {
                flag = true;
                if (player instanceof Player) {
                    com.github.alexthe666.alexsmobs.misc.AMCompat.addCooldown(((Player) player).getCooldowns(), this, 40);
                }
            }
            if (count == 1 && !player.level().isClientSide()) {
                player.gameEvent(GameEvent.ITEM_INTERACT_START);
                player.playSound(SoundEvents.GLASS_BREAK, 1, 0.5F);
                EntityVoidPortal portal = new EntityVoidPortal(player.level(), this);
                portal.setPos(x, y, z);
                Direction dir = Direction.orderedByNearest(player)[0].getOpposite();
                if (dir == Direction.UP) {
                    dir = Direction.DOWN;
                }
                portal.setAttachmentFacing(dir);
                player.level().addFreshEntity(portal);
                onPortalOpen(player.level(), player, portal, dir);
                AMCompat.hurtAndBreak(itemstack, 1, player, player.getUsedItemHand());
                flag = true;
                if (player instanceof Player) {
                    com.github.alexthe666.alexsmobs.misc.AMCompat.addCooldown(((Player) player).getCooldowns(), this, 200);
                }
            }
        }
        if (flag) {
            player.stopUsingItem();
            carverTag.putBoolean("HASBLOCK", false);
            carverTag.putDouble("BLOCKX", 0);
            carverTag.putDouble("BLOCKY", 0);
            carverTag.putDouble("BLOCKZ", 0);
            AMCompat.setTag(itemstack, carverTag);
        }
    }


    //? if >=1.21.2 {
    /*public boolean releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) { releaseUsingImpl(stack, worldIn, entityLiving, timeLeft); return true; }
    *///?} else {
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) { releaseUsingImpl(stack, worldIn, entityLiving, timeLeft); }
    //?}
    private void releaseUsingImpl(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        CompoundTag carverTag = AMCompat.getOrCreateTag(stack);
        carverTag.putBoolean("HASBLOCK", false);
        carverTag.putDouble("BLOCKX", 0);
        carverTag.putDouble("BLOCKY", 0);
        carverTag.putDouble("BLOCKZ", 0);
        AMCompat.setTag(stack, carverTag);

    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }

    public void onPortalOpen(Level worldIn, LivingEntity player, EntityVoidPortal portal, Direction dir){
        portal.setLifespan(1200);
        ResourceKey<Level> respawnDimension = Level.OVERWORLD;
        BlockPos respawnPosition = player.getSleepingPos().isPresent() ? player.getSleepingPos().get() : player.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.ZERO);
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            //? if >=1.21.9 {
            /*// 1.21.9 pushed RespawnConfig's dimension+pos down into a LevelData.RespawnData record.
            // Vanilla's static getDimensionOrDefault does exactly what the null branch below does,
            // but it is not visible in the Forge jar — so its one-line body is inlined instead.
            net.minecraft.server.level.ServerPlayer.RespawnConfig amCfg = serverPlayer.getRespawnConfig();
            if (amCfg != null) {
                respawnDimension = amCfg.respawnData().dimension();
                respawnPosition = amCfg.respawnData().pos();
            }
            *///?} elif >=1.21.5 {
            /*net.minecraft.server.level.ServerPlayer.RespawnConfig amCfg = serverPlayer.getRespawnConfig();
            if (amCfg != null) {
                respawnDimension = amCfg.dimension();
                respawnPosition = amCfg.pos();
            }
            *///?} else {
            respawnDimension = serverPlayer.getRespawnDimension();
            if (serverPlayer.getRespawnPosition() != null) {
                respawnPosition = serverPlayer.getRespawnPosition();
            }
            //?}
        }
        portal.exitDimension = respawnDimension;
        portal.setDestination(respawnPosition.above(2));
    }
}
