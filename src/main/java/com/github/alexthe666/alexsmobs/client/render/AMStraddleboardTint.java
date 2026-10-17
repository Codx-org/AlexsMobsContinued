package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import net.minecraft.world.item.ItemStack;

/**
 * The straddleboard item icon's two tints, and the one place their colours are decided.
 *
 * <p>The icon is an {@code item/generated} pair: {@code layer0} is the wooden base and {@code layer1}
 * the grey panel, and the panel has always been the dyeable one (undyed it falls back to a fixed
 * {@code 0xADC3D7}). A player whose resource pack paints the inventory background grey reported the
 * panel vanishing into it, so both layers are now configurable — {@code straddleboardBaseColor} and
 * {@code straddleboardPanelColor}. The difference between the two <em>is</em> the contrast; either
 * default is a no-op, so a pack that is happy as it is sees no change.
 *
 * <p>Only <b>rendering</b> reads them. {@code EntityStraddleboard#getColor} and
 * {@code ItemStraddleboard#getColor} keep the hardcoded default, because those are synched/logical
 * values — a client that has re-coloured its boards must not disagree with the server about what
 * colour a board <i>is</i>, only about how to draw an undyed one.
 *
 * <p>Two eras reach this class from opposite directions. Below 1.21.4 the loaders' item colour
 * handlers call {@link #tintOf} directly (see ClientProxy#onItemColors) and this compiles to a dead
 * plain class — the implements clause and every override below are gated, exactly as
 * {@link AMIconSpecialRenderer} is. From 1.21.4 there are no item colour handlers at all: tints are
 * declared in the item model definition JSON, which is why the board's dye had been a documented
 * cosmetic loss on those 30 nodes since {@code 2.0.0}. {@code DataPackMigration} now writes
 * {@code "tints":[{"type":"alexsmobs:straddleboard_base"},{"type":"alexsmobs:straddleboard_panel"}]}
 * into {@code assets/alexsmobs/items/straddleboard.json}, so the dye comes back with the setting.
 *
 * <p>⚠️ A {@code minecraft:constant} tint would freeze the value into the parsed model, so these
 * have to be sources of our own; and a tint must be an <b>ARGB</b> int on <i>both</i> sides of the
 * split, which is the trap here. {@code ItemRenderer.renderQuadList} began reading the handler's
 * alpha at <b>1.20.6</b>, not at 1.21.4 — {@code VertexConsumer.putBulkData} gained its fourth float
 * there and the tint is unpacked through {@code ARGB32.alpha/red/green/blue} (swept over the mapped
 * 1.20.1 → 1.21.3 jars; 1.20.1 and 1.20.4 pass three floats and never look at the top byte). So
 * {@link #tintOf} always ORs {@code 0xFF000000} in, on every node.
 *
 * <p>Upstream's fallback of {@code 0xADC3D7} does not, which is bug #108:
 * {@code DyedItemColor.getOrDefault} wraps a <i>stored</i> dye in {@code ARGB.opaque} but returns the
 * fallback verbatim (bytecode-checked on 1.20.6 / 1.21.1 / 1.21.3), so an <i>undyed</i> board's panel
 * has been drawing at alpha 0 — invisible — on the 14 nodes from 1.20.6 to 1.21.3. Vanilla's own
 * leather default is the alpha-carrying {@code -6265536} for exactly this reason.
 *
 * <p>Live per frame: {@code BlockModelWrapper#update} ({@code CuboidItemModelWrapper} from 26.x)
 * calls {@code calculate} on every update, so an edit through {@code /amc config set} shows up
 * without a resource reload. The returned {@code int} is boxed into the render state's model
 * identity from 1.21.6 — harmless, unlike #107's {@code ItemStack}, because {@link Integer} has
 * value equality, so the GUI atlas key is stable for as long as the colour is.
 */
public final class AMStraddleboardTint
        //? if >=1.21.4
        /*implements net.minecraft.client.color.item.ItemTintSource*/
{

    /** {@code layer0} — the wooden base. Never dyed; the setting is the whole of it. */
    public static final AMStraddleboardTint BASE = new AMStraddleboardTint(false);
    /** {@code layer1} — the grey panel. The dye wins when there is one; the setting is the fallback. */
    public static final AMStraddleboardTint PANEL = new AMStraddleboardTint(true);

    /** Unit codecs: each instance is a singleton with no serialised state. */
    public static final com.mojang.serialization.MapCodec<AMStraddleboardTint> BASE_CODEC =
            com.mojang.serialization.MapCodec.unit(BASE);
    public static final com.mojang.serialization.MapCodec<AMStraddleboardTint> PANEL_CODEC =
            com.mojang.serialization.MapCodec.unit(PANEL);

    private final boolean panel;

    private AMStraddleboardTint(boolean panel) {
        this.panel = panel;
    }

    /**
     * The opaque ARGB colour of one layer — the shape both eras want, see the alpha note on the
     * class. {@code panel} picks the layer, i.e. it is {@code tintIndex >= 1}.
     *
     * <p>⚠️ Deliberately not named after the colour it returns: the {@code !mc121-vtx-color}
     * replacement rule rewrites a dot followed by that word and an open paren into the 1.21 vertex
     * setter, matching as a plain substring with no boundary check on either edge — so every call
     * to a method of that name would be renamed out from under it. (The rule reaches into comments
     * too, which is why this sentence spells none of it out.)
     */
    public static int tintOf(ItemStack stack, boolean panel) {
        int rgb = panel
                ? com.github.alexthe666.alexsmobs.misc.AMCompat.getDyedColor(stack, AMConfig.straddleboardPanelColor)
                : AMConfig.straddleboardBaseColor;
        return 0xFF000000 | rgb;
    }

    //? if >=1.21.4 {
    /*@Override
    public int calculate(ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level,
                         net.minecraft.world.entity.LivingEntity entity) {
        return tintOf(stack, this.panel);
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends net.minecraft.client.color.item.ItemTintSource> type() {
        return this.panel ? PANEL_CODEC : BASE_CODEC;
    }

    *///?}

    /**
     * Forge + Fabric registration: put both ids into {@code ItemTintSources.ID_MAPPER}, located by
     * field type. Vanilla keeps that field private on 1.21.4 through 1.21.11 (it is public only from
     * 26.1), it is the class's one {@code LateBoundIdMapper}, and a by-type lookup is immune to
     * classic-Forge SRG member names — the same reasoning, and the same shape, as
     * {@link AMIconSpecialRenderer#register}. NeoForge has a dedicated mod-bus event instead
     * ({@code RegisterColorHandlersEvent.ItemTintSources}, present on every node from 1.21.4).
     * Fails loudly: a miss would otherwise surface only as an untinted board.
     */
    //? if >=1.21.4 {
    /*@SuppressWarnings({"rawtypes", "unchecked"})
    public static void register() {
        try {
            net.minecraft.util.ExtraCodecs.LateBoundIdMapper mapper = null;
            for (java.lang.reflect.Field field : net.minecraft.client.color.item.ItemTintSources.class.getDeclaredFields()) {
                if (field.getType() == net.minecraft.util.ExtraCodecs.LateBoundIdMapper.class) {
                    field.setAccessible(true);
                    mapper = (net.minecraft.util.ExtraCodecs.LateBoundIdMapper) field.get(null);
                    break;
                }
            }
            if (mapper == null) {
                throw new IllegalStateException("No LateBoundIdMapper field in ItemTintSources");
            }
            mapper.put(com.github.alexthe666.alexsmobs.misc.AMCompat.rl("alexsmobs:straddleboard_base"), BASE_CODEC);
            mapper.put(com.github.alexthe666.alexsmobs.misc.AMCompat.rl("alexsmobs:straddleboard_panel"), PANEL_CODEC);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register the alexsmobs straddleboard item tint sources", e);
        }
    }
    *///?}
}
