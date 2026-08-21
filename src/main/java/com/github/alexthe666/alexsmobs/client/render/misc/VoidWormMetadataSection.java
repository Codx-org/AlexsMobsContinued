package com.github.alexthe666.alexsmobs.client.render.misc;

//? if <1.21.4 {
import com.google.gson.JsonObject;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
//?}

public class VoidWormMetadataSection {
    //? if >=1.21.4 {
    /*// 1.21.4 replaced MetadataSectionSerializer with a Codec-backed MetadataSectionType record.
    // getSection(...) takes this type directly, so LayerVoidWormGlow needs no change.
    public static final net.minecraft.server.packs.metadata.MetadataSectionType<VoidWormMetadataSection> SERIALIZER =
            new net.minecraft.server.packs.metadata.MetadataSectionType<>("void_worm",
                    com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                            com.mojang.serialization.Codec.BOOL.fieldOf("end_portal_texture").forGetter(VoidWormMetadataSection::isEndPortalTexture)
                    ).apply(instance, VoidWormMetadataSection::new)));
    *///?} else {
    public static final VoidWormMetadataSection.Serializer SERIALIZER = new VoidWormMetadataSection.Serializer();
    //?}
    private final boolean hasEndPortalTexture;

    public VoidWormMetadataSection(){
        this.hasEndPortalTexture = false;
    }

    public VoidWormMetadataSection(boolean hasEndPortalTexture) {
        this.hasEndPortalTexture = hasEndPortalTexture;
    }

    public boolean isEndPortalTexture() {
        return this.hasEndPortalTexture;
    }

    //? if <1.21.4 {
    private static class Serializer  implements MetadataSectionSerializer<VoidWormMetadataSection> {
        private Serializer() {
        }

        public VoidWormMetadataSection fromJson(JsonObject json) {
            return new VoidWormMetadataSection(GsonHelper.getAsBoolean(json, "end_portal_texture"));
        }

        public String getMetadataSectionName() {
            return "void_worm";
        }
    }
    //?}

}
