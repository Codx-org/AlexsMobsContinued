package com.github.alexthe666.alexsmobs.misc;

// MC 26.2 deleted net.minecraft.world.entity.animal.FlyingAnimal. Vanilla read it in exactly two
// places in LivingEntity — the air-friction branch of travel() and the limb-animation call — and
// both now ask `protected boolean omnidirectionalAirMover()` on Entity instead (Entity:879;
// Parrot:463, Bee:620 and SulfurCube:427 are vanilla's overriders).
//
// The interface itself is still worth having: this mod queries it directly (TameableAIRide asks
// whether its mount is a flier), so it is vendored and the `!mc262-flyinganimal` replacement
// points the fully-qualified vanilla name here. The 13 mobs that implement it also gain a gated
// `omnidirectionalAirMover()` override so they keep vanilla's flight friction and wing-flap
// limb animation on 26.2.
//
// Below 26.2 this compilation unit is just a package declaration and the vanilla interface is
// what every node resolves. Precedent: message/AMNeoSend.java.
//? if >=26.2 {
/*public interface FlyingAnimal {
    boolean isFlying();
}
*///?}
