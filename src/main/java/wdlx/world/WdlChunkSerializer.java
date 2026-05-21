package wdlx.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Adapted from vanilla ChunkSerializer
 */
public class WdlChunkSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WdlChunkSerializer.class);

    // todo: if we could construct our own ServerLevel, we could reuse vanilla logic entirely
    public static CompoundTag write(Level level, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        CompoundTag compoundTag = NbtUtils.addCurrentDataVersion(new CompoundTag());
        compoundTag.putInt("xPos", chunkPos.x);
        compoundTag.putInt("yPos", chunk.getMinSection());
        compoundTag.putInt("zPos", chunkPos.z);
        compoundTag.putLong("LastUpdate", level.getGameTime());
        compoundTag.putLong("InhabitedTime", chunk.getInhabitedTime());
        compoundTag.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(chunk.getPersistedStatus()).toString());
        BlendingData blendingData = chunk.getBlendingData();
        if (blendingData != null) {
            BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingData).resultOrPartial(LOGGER::error).ifPresent(data -> compoundTag.put("blending_data", data));
        }

        BelowZeroRetrogen belowZeroRetrogen = chunk.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null) {
            BelowZeroRetrogen.CODEC
                .encodeStart(NbtOps.INSTANCE, belowZeroRetrogen)
                .resultOrPartial(LOGGER::error)
                .ifPresent(belowZeroRetrogenx -> compoundTag.put("below_zero_retrogen", belowZeroRetrogenx));
        }

        UpgradeData upgradeData = chunk.getUpgradeData();
        if (!upgradeData.isEmpty()) {
            compoundTag.put("UpgradeData", upgradeData.write());
        }

        LevelChunkSection[] levelChunkSections = chunk.getSections();
        ListTag listTag = new ListTag();
        LevelLightEngine levelLightEngine = level.getChunkSource().getLightEngine();
        Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> codec = ChunkSerializer.makeBiomeCodec(registry);
        boolean bl = chunk.isLightCorrect();

        for (int i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); i++) {
            int j = chunk.getSectionIndexFromSectionY(i);
            boolean bl2 = j >= 0 && j < levelChunkSections.length;
            DataLayer dataLayer = levelLightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkPos, i));
            DataLayer dataLayer2 = levelLightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkPos, i));
            if (bl2 || dataLayer != null || dataLayer2 != null) {
                CompoundTag compoundTag2 = new CompoundTag();
                if (bl2) {
                    LevelChunkSection levelChunkSection = levelChunkSections[j];
                    compoundTag2.put("block_states", ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, levelChunkSection.getStates()).getOrThrow());
                    compoundTag2.put("biomes", codec.encodeStart(NbtOps.INSTANCE, levelChunkSection.getBiomes()).getOrThrow());
                }

                if (dataLayer != null && !dataLayer.isEmpty()) {
                    compoundTag2.putByteArray("BlockLight", dataLayer.getData());
                }

                if (dataLayer2 != null && !dataLayer2.isEmpty()) {
                    compoundTag2.putByteArray("SkyLight", dataLayer2.getData());
                }

                if (!compoundTag2.isEmpty()) {
                    compoundTag2.putByte("Y", (byte)i);
                    listTag.add(compoundTag2);
                }
            }
        }

        compoundTag.put("sections", listTag);
        if (bl) {
            compoundTag.putBoolean("isLightOn", true);
        }

        ListTag listTag2 = new ListTag();

        for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            CompoundTag compoundTag3 = chunk.getBlockEntityNbtForSaving(blockPos, level.registryAccess());
            if (compoundTag3 != null) {
                listTag2.add(compoundTag3);
            }
        }

        compoundTag.put("block_entities", listTag2);
        if (chunk.getPersistedStatus().getChunkType() == ChunkType.PROTOCHUNK) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            ListTag listTag3 = new ListTag();
            listTag3.addAll(protoChunk.getEntities());
            compoundTag.put("entities", listTag3);
            CompoundTag compoundTag3 = new CompoundTag();

            for (GenerationStep.Carving carving : GenerationStep.Carving.values()) {
                CarvingMask carvingMask = protoChunk.getCarvingMask(carving);
                if (carvingMask != null) {
                    compoundTag3.putLongArray(carving.toString(), carvingMask.toArray());
                }
            }

            compoundTag.put("CarvingMasks", compoundTag3);
        }

        var ticksToSave = chunk.getTicksForSerialization();
        long l = level.getLevelData().getGameTime();
        compoundTag.put("block_ticks", ticksToSave.blocks().save(l, block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        compoundTag.put("fluid_ticks", ticksToSave.fluids().save(l, fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString()));
        compoundTag.put("PostProcessing", ChunkSerializer.packOffsets(chunk.getPostProcessing()));
        CompoundTag compoundTag4 = new CompoundTag();

        for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
                compoundTag4.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
            }
        }

        compoundTag.put("Heightmaps", compoundTag4);
        return compoundTag;
    }

}
