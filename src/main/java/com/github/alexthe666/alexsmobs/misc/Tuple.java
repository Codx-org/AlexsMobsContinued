package com.github.alexthe666.alexsmobs.misc;

// MC 26.2 deleted net.minecraft.util.Tuple. Nothing replaced it — vanilla's own former callers
// either use a record or com.mojang.datafixers.util.Pair now. Four files here still want it (the
// banana-slug slime spread queue and the vendored Citadel raycoms path jobs), so it is vendored
// verbatim, and the `!mc262-tuple` replacement points the FULLY QUALIFIED name at this class,
// which covers both the two `import net.minecraft.util.Tuple;` lines and the two inline
// fully-qualified uses in AbstractAdvancedPathNavigate / AdvancedPathNavigate.
//
// Below 26.2 this compilation unit is just a package declaration, so the vanilla class keeps
// being the one every node resolves. Precedent: message/AMNeoSend.java.
//? if >=26.2 {
/*public class Tuple<A, B> {
    private A a;
    private B b;

    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return this.a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public B getB() {
        return this.b;
    }

    public void setB(B b) {
        this.b = b;
    }
}
*///?}
