package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.entity.EntityManedWolf;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.tileentity.AMTileEntityRegistry;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityLeafcutterAnthill;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

public class BlockLeafcutterAnthill extends BaseEntityBlock {

    //? if >=1.20.3 {
    /*protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.github.alexthe666.alexsmobs.misc.AMPlatform.unsupportedBlockCodec();
    }
    *///?}

    public BlockLeafcutterAnthill() {
        super(BlockBehaviour.Properties.of().sound(SoundType.GRAVEL).strength(0.75F));
    }

    // 1.20.5 replaced BlockBehaviour#use with useItemOn, which occupies the same position in the
    // right-click dispatch; the upstream body lives in amUse so both eras run the exact same code.
    //? if >=1.20.5 {
    /*protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack amStack, BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        return com.github.alexthe666.alexsmobs.misc.AMCompat.itemResult(amUse(state, worldIn, pos, player, handIn, hit));
    }
    *///?}
    //? if <1.20.5 {
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        return amUse(state, worldIn, pos, player, handIn, hit);
    }
    //?}

    private InteractionResult amUse(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (worldIn.getBlockEntity(pos) instanceof TileEntityLeafcutterAnthill) {
            TileEntityLeafcutterAnthill hill = (TileEntityLeafcutterAnthill) worldIn.getBlockEntity(pos);
            ItemStack heldItem = player.getItemInHand(handIn);
            if (heldItem.getItem() == AMItemRegistry.GONGYLIDIA.get() && hill.hasQueen()) {
                hill.releaseQueens();
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }


    public RenderShape getRenderShape(BlockState p_149645_1_) {
        return RenderShape.MODEL;
    }

    // 1.20.2 made playerWillDestroy return the BlockState the break should carry on with.
    //? if >=1.20.2 {
    /*public BlockState playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player) {
        amDropAnthill(worldIn, pos, player);
        return super.playerWillDestroy(worldIn, pos, state, player);
    }
    *///?}
    //? if <1.20.2 {
    public void playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player) {
        amDropAnthill(worldIn, pos, player);
        super.playerWillDestroy(worldIn, pos, state, player);
    }
    //?}

    private void amDropAnthill(Level worldIn, BlockPos pos, Player player) {
        if (!worldIn.isClientSide() && player.isCreative() && AMCompat.gameRule(worldIn, AMCompat.Rule.BLOCK_DROPS)) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof TileEntityLeafcutterAnthill) {
                TileEntityLeafcutterAnthill anthivetileentity = (TileEntityLeafcutterAnthill) tileentity;
                ItemStack itemstack = new ItemStack(this);
                boolean flag = !anthivetileentity.hasNoAnts();
                if (!flag) {
                    return;
                }
                if (flag) {
                    CompoundTag compoundnbt = new CompoundTag();
                    compoundnbt.put("Ants", anthivetileentity.getAnts());
                    AMCompat.addTagElement(itemstack, "BlockEntityTag", compoundnbt);
                }
                CompoundTag compoundnbt1 = new CompoundTag();
                AMCompat.addTagElement(itemstack, "BlockStateTag", compoundnbt1);
                ItemEntity itementity = new ItemEntity(worldIn, pos.getX(), pos.getY(), pos.getZ(), itemstack);
                itementity.setDefaultPickUpDelay();
                worldIn.addFreshEntity(itementity);
            }
        }
    }

    // 1.21.5 widened Block#fallOn's fall distance from float to double. Upstream's override
    // carries no @Override, so on >=1.21.5 the float form is a dead overload that compiles
    // clean and never runs -- the anthill stopped angering its ants entirely. See
    // docs/notes/bug-reports.md #61.
    //? if >=1.21.5 {
    /*public void fallOn(Level worldIn, BlockState state, BlockPos pos, Entity entityIn, double fallDistance) {
        amStompAnthill(worldIn, pos, entityIn);
        super.fallOn(worldIn, state, pos, entityIn, fallDistance);
    }
    *///?}
    //? if <1.21.5 {
    public void fallOn(Level worldIn, BlockState state, BlockPos pos, Entity entityIn, float fallDistance) {
        amStompAnthill(worldIn, pos, entityIn);
        super.fallOn(worldIn, state, pos, entityIn, fallDistance);
    }
    //?}

    private void amStompAnthill(Level worldIn, BlockPos pos, Entity entityIn) {
        if (entityIn instanceof LivingEntity && !(entityIn instanceof EntityManedWolf)) {
            this.angerNearbyAnts(worldIn, (LivingEntity) entityIn, pos);
            if (!worldIn.isClientSide() && worldIn.getBlockEntity(pos) instanceof TileEntityLeafcutterAnthill) {
                TileEntityLeafcutterAnthill beehivetileentity = (TileEntityLeafcutterAnthill) worldIn.getBlockEntity(pos);
                beehivetileentity.angerAnts((LivingEntity) entityIn, worldIn.getBlockState(pos), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                if(entityIn instanceof ServerPlayer){
                    AMAdvancementTriggerRegistry.STOMP_LEAFCUTTER_ANTHILL.trigger((ServerPlayer)entityIn);
                }
            }
        }
    }

    public void playerDestroy(Level worldIn, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        super.playerDestroy(worldIn, player, pos, state, te, stack);
        if (!worldIn.isClientSide() && te instanceof TileEntityLeafcutterAnthill) {
            TileEntityLeafcutterAnthill beehivetileentity = (TileEntityLeafcutterAnthill) te;
            if (AMCompat.enchantLevel(Enchantments.SILK_TOUCH, stack, worldIn) == 0) {
                beehivetileentity.angerAnts(player, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                worldIn.updateNeighbourForOutputSignal(pos, this);
                this.angerNearbyAnts(worldIn, pos);
            }
        }
    }

    private void angerNearbyAnts(Level world, BlockPos pos) {
        List<EntityLeafcutterAnt> list = world.getEntitiesOfClass(EntityLeafcutterAnt.class, (new AABB(pos)).inflate(20D, 6.0D, 20D));
        if (!list.isEmpty()) {
            List<Player> list1 = world.getEntitiesOfClass(Player.class, (new AABB(pos)).inflate(20D, 6.0D, 20D));
            if (list1.isEmpty()) return; //Forge: Prevent Error when no players are around.
            int i = list1.size();
            for (EntityLeafcutterAnt beeentity : list) {
                if (beeentity.getTarget() == null) {
                    beeentity.setTarget(list1.get(world.getRandom().nextInt(i)));
                }
            }
        }


    }

    private void angerNearbyAnts(Level world, LivingEntity entity, BlockPos pos) {
        List<EntityLeafcutterAnt> list = world.getEntitiesOfClass(EntityLeafcutterAnt.class, (new AABB(pos)).inflate(20D, 6.0D, 20D));
        if (!list.isEmpty()) {
            for (EntityLeafcutterAnt beeentity : list) {
                if (beeentity.getTarget() == null) {
                    beeentity.setTarget(entity);
                }
            }
        }
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLeafcutterAnthill(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_152180_, BlockState p_152181_, BlockEntityType<T> p_152182_) {
        return p_152180_.isClientSide() ? null : createTickerHelper(p_152182_, AMTileEntityRegistry.LEAFCUTTER_ANTHILL.get(), TileEntityLeafcutterAnthill::serverTick);
    }
}
