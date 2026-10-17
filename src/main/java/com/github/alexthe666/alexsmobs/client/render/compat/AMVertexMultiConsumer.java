package com.github.alexthe666.alexsmobs.client.render.compat;

// MC 26.2 deleted com.mojang.blaze3d.vertex.VertexMultiConsumer along with the public
// ItemFeatureRenderer#getFoilBuffer that was its last caller — the enchantment glint is drawn from
// the submit pipeline's own item path now, which this mod's ~130 hand-written renderers do not go
// through.
//
// The glint is a visible feature here (RenderToucan always asks for it on a golden toucan, and the
// tusklin gear / armour layers ask for it per stack), so rather than drop it this reproduces
// vanilla's deleted VertexMultiConsumer.Double: every vertex call is forwarded to both delegates,
// so the same geometry is emitted once into the glint render type and once into the base one.
//
// Only the eight abstract methods need forwarding — VertexConsumer's defaults (the 11-arg
// addVertex, setLight, setOverlay, the float setColor) are all written in terms of them, so they
// reach both delegates too.
//
// Below 26.2 this compilation unit is just a package declaration; every other node still has
// vanilla's own multi-consumer behind ItemRenderer/ItemFeatureRenderer#getFoilBuffer.
// (client/render/compat/** is excluded from the compile below 1.21.2.)
//? if >=26.2 {
/*import com.mojang.blaze3d.vertex.VertexConsumer;

public class AMVertexMultiConsumer implements VertexConsumer {

    private final VertexConsumer first;
    private final VertexConsumer second;

    public AMVertexMultiConsumer(VertexConsumer first, VertexConsumer second) {
        if (first == second) {
            throw new IllegalArgumentException("Duplicate delegates");
        }
        this.first = first;
        this.second = second;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.first.addVertex(x, y, z);
        this.second.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.first.setColor(red, green, blue, alpha);
        this.second.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setColor(int argb) {
        this.first.setColor(argb);
        this.second.setColor(argb);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.first.setUv(u, v);
        this.second.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.first.setUv1(u, v);
        this.second.setUv1(u, v);
        return this;
    }

    // 26.3 added setUv3 to VertexConsumer as a new abstract method, and this consumer's whole job is
    // to fan every call out to both delegates, so the new one has to fan out too. It CANNOT be a //?
    // gate: this sits inside the branch opened at the top of the file, blocks never nest, and an
    // inner gate whose condition is false on the active node gets commented out, emitting a block
    // comment terminator that closes the outer branch early ("Unclosed scope"). ⚠️ For the same
    // reason, never type that two-character sequence in PROSE inside a gated branch either: it
    // truncates the branch exactly as a real one would, and the error is reported against the
    // branch's opening line, tens of lines away. The method is spliced in ahead of the
    // signature below by the !mc263-setuv3-fanout replacement rule instead. The parameters are named
    // packedU/packedV purely so that line is not a substring of the two 8-space-indented setUv2
    // signatures the sibling !mc263-setuv3-discard rule targets; replacement matching is
    // boundary-checked on neither edge, so the shorter line would otherwise match inside them.
    @Override
    public VertexConsumer setUv2(int packedU, int packedV) {
        this.first.setUv2(packedU, packedV);
        this.second.setUv2(packedU, packedV);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.first.setNormal(x, y, z);
        this.second.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        this.first.setLineWidth(width);
        this.second.setLineWidth(width);
        return this;
    }
}
*///?}
