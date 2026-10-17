package com.github.alexthe666.alexsmobs.tileentity;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.message.MessageUpdateTransmutablesToDisplay;
import com.github.alexthe666.alexsmobs.misc.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.TransmutationData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.*;

public class TileEntityTransmutationTable  extends BlockEntity {

    private static final ResourceLocation COMMON_ITEMS = AMCompat.rl("alexsmobs", "gameplay/transmutation_table_common");
    private static final ResourceLocation UNCOMMON_ITEMS = AMCompat.rl("alexsmobs", "gameplay/transmutation_table_uncommon");
    private static final ResourceLocation RARE_ITEMS = AMCompat.rl("alexsmobs", "gameplay/transmutation_table_rare");
    public int ticksExisted;
    private int totalTransmuteCount = 0;
    private final Map<UUID, TransmutationData> playerToData = new HashMap<>();
    private final ItemStack[] possiblities = new ItemStack[3];
    private static final Random RANDOM = new Random();

    private UUID rerollPlayerUUID = null;

    public TileEntityTransmutationTable(BlockPos pos, BlockState state) {
        super(AMTileEntityRegistry.TRANSMUTATION_TABLE.get(), pos, state);
    }

    public static void commonTick(Level level, BlockPos pos, BlockState state, TileEntityTransmutationTable entity) {
        entity.tick();
    }

    private static ItemStack createFromLootTable(Player player, ResourceLocation loc) {
        if(player.level().isClientSide()){
            return ItemStack.EMPTY;
        }else{
            LootTable loottable = com.github.alexthe666.alexsmobs.misc.AMCompat.lootTable(player.level().getServer(), loc);
            // Upstream passed THIS_ENTITY while declaring the EMPTY param set, which allows nothing.
            // Forge patched that validation out (up to 1.21.1) and NeoForge still does, so it never
            // threw on the loaders upstream shipped for -- but vanilla always checks, and the check
            // moved into ContextMap.Builder at 1.21.2 where Forge stopped carrying the patch. So the
            // roll hard-crashed the server tick on every Fabric node and on Forge >=1.21.3.
            // PIGLIN_BARTER is the vanilla set whose only member is a required THIS_ENTITY -- the same
            // one this mod's five other "random item, entity as context" call sites already use.
            List<ItemStack> loots = loottable.getRandomItems((new LootParams.Builder((ServerLevel) player.level())).withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.PIGLIN_BARTER));
            return loots.isEmpty() ? ItemStack.EMPTY : loots.get(0);
        }
    }
    //? if >=1.20.5 {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
    *///?} else {
    public void load(CompoundTag tag) {
    //?}
        //? if <1.20.5
        net.minecraft.core.HolderLookup.Provider provider = null;
        //? if >=1.20.5
        //super.loadAdditional(tag, provider);
        //? if <1.20.5
        super.load(tag);
        // Upstream had this method's body and saveAdditional's swapped: the load hook wrote the
        // player data into the tag it was handed, and the save hook read from the (empty) tag it was
        // meant to fill. Both compiled while a CompoundTag was the parameter of each, so the table
        // silently persisted nothing at all. It also tested for "Possibility" while writing
        // "Possiblity", so the rolled possibilities would not have survived either.
        totalTransmuteCount = AMCompat.getInt(tag, "TotalCount");
        ListTag list = AMCompat.getList(tag, "PlayerTransmutationData", 10);
        if(!list.isEmpty()){
            for(int i = 0; i < list.size(); ++i) {
                CompoundTag compoundtag = AMCompat.getCompound(list, i);
                UUID uuid = AMCompat.getUUID(compoundtag, "UUID");
                if(uuid != null){
                    playerToData.put(uuid, TransmutationData.fromNBT(provider, AMCompat.getCompound(compoundtag, "TransmutationData")));
                }
            }
        }
        for(int i = 0; i < 3; i++){
            if(AMCompat.contains(tag, "Possiblity" + i)){
                possiblities[i] = com.github.alexthe666.alexsmobs.misc.AMCompat.loadItem(provider, AMCompat.getCompound(tag, "Possiblity" + i));
            }
        }

    }
    //? if >=1.20.5 {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
    *///?} else {
    protected void saveAdditional(CompoundTag tag) {
    //?}
        //? if <1.20.5
        net.minecraft.core.HolderLookup.Provider provider = null;
        //? if >=1.20.5
        //super.saveAdditional(tag, provider);
        //? if <1.20.5
        super.saveAdditional(tag);
        tag.putInt("TotalCount", totalTransmuteCount);
        ListTag list = new ListTag();
        for(Map.Entry<UUID, TransmutationData> entry : playerToData.entrySet()){
            CompoundTag innerTag = new CompoundTag();
            AMCompat.putUUID(innerTag, "UUID", entry.getKey());
            innerTag.put("TransmutationData", entry.getValue().saveAsNBT(provider));
            list.add(innerTag);
        }
        AMCompat.put(tag, "PlayerTransmutationData", list);
        for(int i = 0; i < 3; i++){
            if(possiblities[i] != null && !possiblities[i].isEmpty()){
                AMCompat.put(tag, "Possiblity" + i, com.github.alexthe666.alexsmobs.misc.AMCompat.saveItem(provider, possiblities[i]));
            }
        }
    }


    private void randomizeResults(Player player){
        rollPossiblity(player, 0);
        rollPossiblity(player, 1);
        rollPossiblity(player, 2);
        int dataIndex = RANDOM.nextInt(2);
        if(playerToData.containsKey(player.getUUID()) && !AMConfig.limitTransmutingToLootTables){
            TransmutationData data = playerToData.get(player.getUUID());
            if(RANDOM.nextFloat() < Math.min(0.01875F * data.getTotalWeight(), 0.2F)){
                ItemStack stack = data.getRandomItem(RANDOM);
                if(stack != null && !stack.isEmpty()){
                    possiblities[dataIndex] = stack;
                }
            }
        }
        AlexsMobs.sendMSGToAll(new MessageUpdateTransmutablesToDisplay(player.getId(), possiblities[0], possiblities[1], possiblities[2]));
    }

    public void rollPossiblity(Player player, int i){
        if(player == null || player.level().isClientSide() || !(player.level() instanceof ServerLevel)){
            return;
        }
        ResourceLocation loot;
        int safeIndex = Mth.clamp(i, 0, 2);
        switch (safeIndex){
            default:
            case 0:
                loot = COMMON_ITEMS;
                break;
            case 1:
                loot = UNCOMMON_ITEMS;
                break;
            case 2:
                loot = RARE_ITEMS;
                break;
        }
        possiblities[safeIndex] = createFromLootTable(player, loot);
    }

    public boolean hasPossibilities(){
        for(int i = 0; i < 3; i++){
            if(possiblities[i] == null || possiblities[i].isEmpty()){
                return false;
            }
        }
        return true;
    }

    public ItemStack getPossibility(int i){
        int safeIndex = Mth.clamp(i, 0, 2);
        ItemStack possible = possiblities[safeIndex];
        return possible == null ? ItemStack.EMPTY : possible;
    }

    public void postTransmute(Player player, ItemStack from, ItemStack to){
        TransmutationData data;
        if(playerToData.containsKey(player.getUUID())){
            data = playerToData.get(player.getUUID());
        }else{
            data = new TransmutationData();
        }
        data.onTransmuteItem(from, to);
        playerToData.put(player.getUUID(), data);
        totalTransmuteCount += from.getCount();
        if(player instanceof ServerPlayer && totalTransmuteCount >= 1000){
            AMAdvancementTriggerRegistry.TRANSMUTE_1000_ITEMS.trigger((ServerPlayer)player);
        }
        setRerollPlayerUUID(player.getUUID());
    }

    public void tick() {
        ticksExisted++;
        if(rerollPlayerUUID != null){
            Player player = level.getPlayerByUUID(rerollPlayerUUID);
            if(player != null){
                this.level.playSound(null, this.getBlockPos(), AMSoundRegistry.TRANSMUTE_ITEM.get(), SoundSource.BLOCKS, 1F, 0.9F + player.getRandom().nextFloat() * 0.2F);
                this.randomizeResults(player);
            }
            rerollPlayerUUID = null;
        }
    }

    public void setRerollPlayerUUID(UUID uuid){
        this.rerollPlayerUUID = uuid;
    }
}
