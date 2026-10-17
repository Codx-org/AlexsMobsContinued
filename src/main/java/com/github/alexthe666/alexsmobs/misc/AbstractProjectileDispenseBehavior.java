package com.github.alexthe666.alexsmobs.misc;

// 1.20.5 deleted net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior in favour of
// ProjectileDispenseBehavior(Item), which requires the item itself to implement ProjectileItem.
// Alex's Mobs registers five anonymous subclasses over items it does not want to change, so we
// keep the old shape here instead. Same name, different package: the import line in
// AMItemRegistry is swapped by a stonecutter replacement on >=1.20.5 nodes.
//
// Below 1.20.5 the whole file is commented out and vanilla's class is used unchanged.
//? if >=1.20.5 {
/*import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public abstract class AbstractProjectileDispenseBehavior extends DefaultDispenseItemBehavior {

    public ItemStack execute(BlockSource source, ItemStack stack) {
        Level level = source.level();
        Position position = DispenserBlock.getDispensePosition(source);
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        Projectile projectile = this.getProjectile(level, position, stack);
        projectile.shoot(direction.getStepX(), (float) direction.getStepY() + 0.1F, direction.getStepZ(), this.getPower(), this.getUncertainty());
        level.addFreshEntity(projectile);
        stack.shrink(1);
        return stack;
    }

    protected void playSound(BlockSource source) {
        source.level().levelEvent(1002, source.pos(), 0);
    }

    protected abstract Projectile getProjectile(Level level, Position position, ItemStack stack);

    protected float getUncertainty() {
        return 6.0F;
    }

    protected float getPower() {
        return 1.1F;
    }
}
*///?}
