package com.github.alexthe666.alexsmobs.world;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class AMWorldData extends SavedData {

    private static final String IDENTIFIER = "alexsmobs_world_data";
    private ServerLevel level;
    private int tickCounter;
    private int beachedCachalotSpawnDelay;
    private int beachedCachalotSpawnChance;
    private UUID beachedCachalotID;
    private ChunkPos pupfishChunk;
    private int pupfishChunkTime = 0;
    private int pupfishSeedAddition = 0;
    private long startPupfishSearchTimestamp = -1;
    private boolean noPupfishChunk;
    private static final Map<Level, AMWorldData> dataMap = new HashMap<>();
    private static final Predicate<BlockState> IS_WATER = (state -> state.is(Blocks.WATER));

    public AMWorldData() {
        super();
    }

    // 1.21.5 made SavedData fully Codec-based (a SavedDataType + Codec); the save/load(CompoundTag)
    // path is gone. Nothing here is registry-backed, so a plain RecordCodecBuilder covers it.
    //? if >=1.21.5 {
    /*public static final com.mojang.serialization.Codec<AMWorldData> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            com.mojang.serialization.Codec.INT.optionalFieldOf("beachedCachalotSpawnDelay", 0).forGetter(d -> d.beachedCachalotSpawnDelay),
            com.mojang.serialization.Codec.INT.optionalFieldOf("beachedCachalotSpawnChance", 0).forGetter(d -> d.beachedCachalotSpawnChance),
            net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("beachedCachalotId").forGetter(d -> java.util.Optional.ofNullable(d.beachedCachalotID)),
            net.minecraft.world.level.ChunkPos.CODEC.optionalFieldOf("pupfishChunk").forGetter(d -> java.util.Optional.ofNullable(d.pupfishChunk)),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("noPupfishChunk", false).forGetter(d -> d.noPupfishChunk)
    ).apply(i, AMWorldData::new));

    // The explicit type argument is needed because the record carries overlapping Supplier/Factory
    // constructors; savedDataId() hides 26.1's String -> Identifier retype of the id (see below).
    public static final net.minecraft.world.level.saveddata.SavedDataType<AMWorldData> TYPE = new net.minecraft.world.level.saveddata.SavedDataType<AMWorldData>(
            savedDataId(), (java.util.function.Supplier<AMWorldData>) AMWorldData::new, CODEC, net.minecraft.util.datafix.DataFixTypes.LEVEL);

    private AMWorldData(int delay, int chance, java.util.Optional<UUID> id, java.util.Optional<ChunkPos> chunk, boolean noPupfish) {
        this.beachedCachalotSpawnDelay = delay;
        this.beachedCachalotSpawnChance = chance;
        this.beachedCachalotID = id.orElse(null);
        this.pupfishChunk = chunk.orElse(null);
        this.noPupfishChunk = noPupfish;
    }
    *///?}

    // 26.1 retyped SavedDataType's id from a String to an Identifier — LevelStorageSource resolves
    // it as data/<namespace>/<path>.dat, so saved data is namespaced per mod now. Kept as its own
    // method, at top level, because the return type differs by era and Stonecutter blocks are
    // siblings and never nest (the codec block above is already one).
    //? if >=26 {
    /*private static net.minecraft.resources.Identifier savedDataId() {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(AlexsMobs.MODID, "world_data");
    }
    *///?} else {
    private static String savedDataId() {
        return IDENTIFIER;
    }
    //?}

    public static AMWorldData get(Level world) {
        if (world instanceof ServerLevel) {
            ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
            AMWorldData fromMap = dataMap.get(overworld);
            if(fromMap == null){
                DimensionDataStorage storage = overworld.getDataStorage();
                // 1.20.2 bundled the constructor/deserializer/datafix type into SavedData.Factory;
                // 1.21.5 replaced that with a Codec-based SavedDataType.
                //? if >=1.21.5 {
                /*AMWorldData data = storage.computeIfAbsent(TYPE);
                *///?} elif >=1.20.2 {
                /*AMWorldData data = storage.computeIfAbsent(new SavedData.Factory<>(AMWorldData::new, AMWorldData::load, net.minecraft.util.datafix.DataFixTypes.LEVEL), IDENTIFIER);
                *///?} else {
                AMWorldData data = storage.computeIfAbsent(AMWorldData::load, AMWorldData::new, IDENTIFIER);
                //?}
                if (data != null) {
                    data.level =  overworld;
                    data.setDirty();
                }
                dataMap.put(world, data);
                return data;
            }
            return fromMap;
        }
        return null;
    }

    public static AMWorldData load(CompoundTag nbt) {
        AMWorldData data = new AMWorldData();
        if (AMCompat.contains(nbt, "BeachedCachalotSpawnDelay", 99)) {
            data.beachedCachalotSpawnDelay = AMCompat.getInt(nbt, "BeachedCachalotSpawnDelay");
        }
        if (AMCompat.contains(nbt, "BeachedCachalotSpawnChance", 99)) {
            data.beachedCachalotSpawnChance = AMCompat.getInt(nbt, "BeachedCachalotSpawnChance");
        }
        if (AMCompat.contains(nbt, "BeachedCachalotId", 8)) {
            data.beachedCachalotID = UUID.fromString(AMCompat.getString(nbt, "BeachedCachalotId"));
        }
        if (AMCompat.contains(nbt, "PupfishChunkX") && AMCompat.contains(nbt, "PupfishChunkZ")) {
            data.pupfishChunk = new ChunkPos(AMCompat.getInt(nbt, "PupfishChunkX"), AMCompat.getInt(nbt, "PupfishChunkZ"));
        }
        if (AMCompat.contains(nbt, "NoPupfishChunk")) {
            data.noPupfishChunk = AMCompat.getBoolean(nbt, "NoPupfishChunk");
        }
        return data;
    }

    public int getBeachedCachalotSpawnDelay() {
        return this.beachedCachalotSpawnDelay;
    }

    public void setBeachedCachalotSpawnDelay(int delay) {
        this.beachedCachalotSpawnDelay = delay;
    }

    public int getBeachedCachalotSpawnChance() {
        return this.beachedCachalotSpawnChance;
    }

    public void setBeachedCachalotSpawnChance(int chance) {
        this.beachedCachalotSpawnChance = chance;
    }

    public void setBeachedCachalotID(UUID id) {
        this.beachedCachalotID = id;
    }

    public void debug() {
    }

    public void tick() {
        ++this.tickCounter;
    }

    // 1.20.5 threaded a HolderLookup.Provider through SavedData's save and through the
    // deserializer SavedData.Factory takes. Nothing stored here is registry-backed, so both
    // eras funnel into the same saveTo body / the same one-arg load.
    // 1.21.5 dropped save(CompoundTag) entirely — persistence is via CODEC above.
    //? if >=1.21.5 {
    /*// save handled by CODEC/TYPE; no save(CompoundTag) override exists to hook.
    *///?} elif >=1.20.5 {
    /*@Override
    public CompoundTag save(CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider) {
        return this.saveTo(compound);
    }

    public static AMWorldData load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        return load(nbt);
    }
    *///?} else {
    @Override
    public CompoundTag save(CompoundTag compound) {
        return this.saveTo(compound);
    }
    //?}

    private CompoundTag saveTo(CompoundTag compound) {
        compound.putInt("beachedCachalotSpawnDelay", this.beachedCachalotSpawnDelay);
        compound.putInt("beachedCachalotSpawnChance", this.beachedCachalotSpawnChance);
        if (this.beachedCachalotID != null) {
            compound.putString("beachedCachalotId", this.beachedCachalotID.toString());
        }
        if (this.pupfishChunk != null) {
            compound.putInt("PupfishChunkX", this.pupfishChunk.x);
            compound.putInt("PupfishChunkZ", this.pupfishChunk.z);
        }
        if(this.noPupfishChunk){
            compound.putBoolean("NoPupfishChunk", noPupfishChunk);
        }
        return compound;
    }

    @Nullable
    public ChunkPos getPupfishChunk() {
        return pupfishChunk;
    }



    public boolean isInPupfishChunk(BlockPos pos) {
        if(pupfishChunk != null){
            return pos.getX() >= pupfishChunk.getMinBlockX() && pos.getX() <= pupfishChunk.getMaxBlockX() && pos.getZ() >= pupfishChunk.getMinBlockZ() && pos.getZ() <= pupfishChunk.getMaxBlockZ();
        }
        return false;
    }

    public void tickPupfish() {
        if(AMConfig.restrictPupfishSpawns && !noPupfishChunk){
            if(pupfishChunk == null && startPupfishSearchTimestamp == -1){
                startPupfishSearchTimestamp = System.currentTimeMillis();
            }
            if (pupfishChunk == null && pupfishChunkTime % 10 == 0) {
                long seconds = (System.currentTimeMillis() - startPupfishSearchTimestamp) / 1000L;
                if(seconds / 60 > 5) {
                    AlexsMobs.LOGGER.info("Giving up search for pupfish chunk after " + (seconds / 60) + " minutes. no pupfish will spawn in this world :( ");
                    noPupfishChunk = true;
                }else{
                    searchForPupfishChunk();
                }
            }
            pupfishChunkTime++;
        }
    }

    private void searchForPupfishChunk() {
        if (level != null && level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator chunkGenerator) {
            Random random = new Random(level.getSeed() + pupfishSeedAddition);
            int randomXCoord = random.nextInt(AMConfig.pupfishChunkSpawnDistance * 2) - AMConfig.pupfishChunkSpawnDistance;
            int randomZCoord = random.nextInt(AMConfig.pupfishChunkSpawnDistance * 2) - AMConfig.pupfishChunkSpawnDistance;
            ChunkPos checkPos = new ChunkPos(randomXCoord >> 4, randomZCoord >> 4);
            BlockPos center = new BlockPos(checkPos.getMiddleBlockX(), chunkGenerator.getSeaLevel(), checkPos.getMiddleBlockZ());
            int maxWater = getWaterHeight(chunkGenerator, level.getChunkSource().randomState(), center.getX(), center.getZ(), level);
            if(maxWater > 31 && maxWater < 63){
                pupfishChunk = checkPos;
                AlexsMobs.LOGGER.info("Found Pupfish chunk at " + pupfishChunk.getMaxBlockX() + " ~ " + pupfishChunk.getMinBlockZ() + " after " + pupfishSeedAddition + " tries");
            }
        }
        pupfishSeedAddition++;
    }

    public int getWaterHeight(NoiseBasedChunkGenerator generator, RandomState rand, int x, int z, LevelHeightAccessor level) {
        NoiseSettings noisesettings = generator.settings.value().noiseSettings();
        int i = Math.max(noisesettings.minY(), AMCompat.minBuildHeight(level));
        int j = Math.min(noisesettings.minY() + noisesettings.height(), AMCompat.maxBuildHeight(level));
        int k = Mth.floorDiv(i, noisesettings.getCellHeight());
        int l = Mth.floorDiv(j - i, noisesettings.getCellHeight());
        return generator.iterateNoiseColumn(level, rand, x, z, null, IS_WATER).orElse(AMCompat.minBuildHeight(level));
    }
}
