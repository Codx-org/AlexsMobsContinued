package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.inventory.MenuTransmutationTable;
import com.github.alexthe666.alexsmobs.message.MessageUpdateTransmutablesToDisplay;
import com.github.alexthe666.alexsmobs.tileentity.AMTileEntityRegistry;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityTransmutationTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class BlockTransmutationTable extends BaseEntityBlock implements AMSpecialRenderBlock {

    //? if >=1.20.3 {
    /*protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.github.alexthe666.alexsmobs.misc.AMPlatform.unsupportedBlockCodec();
    }
    *///?}

    private static final Component CONTAINER_TITLE = Component.translatable("alexsmobs.container.transmutation_table");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape BASE_AABB = Block.box(1, 0, 1, 15, 5, 15);
    private static final VoxelShape ARMS_NS = Block.box(1, 5, 5.5F, 15, 16, 10.5F);
    private static final VoxelShape ARMS_EW = Block.box(5.5F, 5, 1, 10.5F, 16, 15);
    private static final VoxelShape NS_AABB = Shapes.or(BASE_AABB, ARMS_NS);
    private static final VoxelShape EW_AABB = Shapes.or(BASE_AABB, ARMS_EW);

    public BlockTransmutationTable() {
        super(Properties.of().pushReaction(PushReaction.BLOCK).mapColor(DyeColor.BLACK).noOcclusion().lightLevel((block) -> 2).emissiveRendering((block, world, pos) -> true).sound(SoundType.STONE).strength(1F).requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS_AABB : EW_AABB;
    }

    // Not upstream, and not a behaviour change: 1.21.4 DELETED BaseEntityBlock#getRenderShape,
    // which had returned INVISIBLE. This block is drawn entirely by its block entity renderer and
    // its baked model is a placeholder (a plain obsidian cube), so inheriting the new default of
    // MODEL wrapped the table in obsidian on every node >=1.21.4. Stating it explicitly is what
    // upstream inherited below the boundary, so the override is correct on all nodes at once.
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityTransmutationTable(pos, state);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 1.20.5 replaced BlockBehaviour#use with useItemOn, which occupies the same position in the
    // right-click dispatch; the upstream body lives in amUse so both eras run the exact same code.
    //? if >=1.20.5 {
    /*protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack amStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        return com.github.alexthe666.alexsmobs.misc.AMCompat.itemResult(amUse(state, level, pos, player, hand, result));
    }
    *///?}
    //? if <1.20.5 {
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        return amUse(state, level, pos, player, hand, result);
    }
    //?}

    private InteractionResult amUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(state.getMenuProvider(level, pos));
            player.awardStat(Stats.INTERACT_WITH_LOOM);
            BlockEntity te = level.getBlockEntity(pos);
            if(te instanceof TileEntityTransmutationTable){
                TileEntityTransmutationTable table = (TileEntityTransmutationTable)te;

                AlexsMobs.sendMSGToAll(new MessageUpdateTransmutablesToDisplay(player.getId(), table.getPossibility(0), table.getPossibility(1), table.getPossibility(2)));

            }
            return InteractionResult.CONSUME;
        }
    }

    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity te = level.getBlockEntity(pos);
        return new SimpleMenuProvider((i, inv, player) -> {
            return new MenuTransmutationTable(i, inv, ContainerLevelAccess.create(level, pos), player, te instanceof  TileEntityTransmutationTable ? (TileEntityTransmutationTable)te : null);
        }, CONTAINER_TITLE);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_152180_, BlockState p_152181_, BlockEntityType<T> p_152182_) {
        return createTickerHelper(p_152182_, AMTileEntityRegistry.TRANSMUTATION_TABLE.get(), TileEntityTransmutationTable::commonTick);
    }

    // NeoForge 21.10 inserted the player's tool stack into IBlockExtension#onDestroyedByPlayer;
    // Forge 60 keeps the six-argument form on every node.
    //? if neoforge && >=1.21.10 {
    /*@Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.item.ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        explodeOnDestroy(level, pos);
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }
    *///?} elif !fabric {
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        explodeOnDestroy(level, pos);
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
    //?} else {
    /*// Vanilla has no onDestroyedByPlayer hook and Fabric adds none, so on Fabric the
    // explode-on-break comes from Fabric API's PlayerBlockBreakEvents.AFTER, registered in
    // FabricServerEvents against this entry point.
    //
    // AFTER, not BEFORE, even though Forge's hook runs before the block is removed: BEFORE would
    // raise the explosion while this block still occupies the position, and Level.explode with
    // ExplosionInteraction.BLOCK would then break it a second time — re-entering this hook.
    // Nothing about the effect depends on the block still being there, so AFTER is both safe and
    // closer in observable behaviour.
    public static void fabricOnDestroyedByPlayer(Level level, BlockPos pos) {
        explodeOnDestroy(level, pos);
    }
    *///?}

    // Static so the Fabric entry point above can reach it without an instance; the Forge and
    // NeoForge arms call it from an instance method, which is unaffected.
    private static void explodeOnDestroy(Level level, BlockPos pos) {
        if(AMConfig.transmutingTableExplodes){
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 3F, false, Level.ExplosionInteraction.BLOCK);
        }
    }
}

