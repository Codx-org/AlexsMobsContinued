package com.github.alexthe666.alexsmobs.citadel.server.entity.pathfinding.raycoms.pathjobs;

public interface ICustomSizeNavigator {

    boolean isSmallerThanBlock();

    float getXZNavSize();

    int getYNavSize();
}
