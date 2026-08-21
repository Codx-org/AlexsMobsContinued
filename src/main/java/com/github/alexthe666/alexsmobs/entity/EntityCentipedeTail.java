package com.github.alexthe666.alexsmobs.entity;

import net.minecraft.world.entity.EntityType;
//? if <1.20.5
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;

public class EntityCentipedeTail extends EntityCentipedeBody {

    protected EntityCentipedeTail(EntityType type, Level worldIn) {
        super(type, worldIn);
    }

    //? if <1.20.5 {
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }
    //?}

}
