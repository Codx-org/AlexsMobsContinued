package com.github.alexthe666.alexsmobs.fabric.client;

/**
 * Fabric stand-in for {@code net.minecraftforge.client.extensions.common.IClientItemExtensions},
 * reached by the Fabric-only {@code !fab-clientitemext} replacement rule — the same relocated
 * compat-namespace pattern as {@code fabric/registries/DeferredRegister},
 * {@code fabric/registries/DeferredRegister} and {@code fabric/entity/PartEntity}.
 *
 * <p><b>Deliberately empty, and that is faithful.</b> In this mod the interface is only ever used
 * as an <i>opaque type token</i>: every one of the ~13 items that implements
 * {@code IClientExtensionItem} has the identical one-line body
 * {@code consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties())}, and the
 * consumer is supplied by {@code ClientProxy.onRegisterClientExtensions} — a NeoForge mod-bus
 * handler with no Fabric counterpart. So on this loader nothing ever calls the consumer and no
 * method on this type is ever invoked; declaring the Forge methods would be dead weight that has
 * to be re-gated on every MC version (their signatures moved at 1.21.2 and again at 1.21.4).
 *
 * <p>What the two implementations carry is therefore reached by a Fabric-native registration instead,
 * and both are now wired — the consumer above stays uncalled, but nothing depends on it:
 * <ul>
 *   <li>{@code AMItemRenderProperties#getCustomRenderer} — the BEWLR path. <b>Inert on
 *       {@code >=1.21.4}</b> on every loader (vanilla deleted it; see the note in that class), so
 *       there it is genuinely nothing to lose. <b>Below</b> 1.21.4 it was very much something to
 *       lose — eleven items whose model is {@code builtin/entity} rendered as empty slots — and it
 *       goes to Fabric API's {@code BuiltinItemRendererRegistry} in {@link FabricItemRenderers}.</li>
 *   <li>{@code CustomArmorRenderProperties#getHumanoidArmorModel} — the 13 custom armour models,
 *       handed to Fabric API's {@code ArmorRenderer} in {@link FabricArmorRenderers}.</li>
 * </ul>
 */
public interface IClientItemExtensions {
}
