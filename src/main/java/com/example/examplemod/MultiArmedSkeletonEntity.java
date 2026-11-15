
package com.example.examplemod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class MultiArmedSkeletonEntity extends PathfinderMob {

    public MultiArmedSkeletonEntity(
            EntityType<? extends MultiArmedSkeletonEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Giữ chuyển động bay nhẹ.
            this.setDeltaMovement(
                    this.getDeltaMovement().x * 0.95,
                    Math.sin(this.tickCount * 0.08) * 0.025,
                    this.getDeltaMovement().z * 0.95
            );
        }
    }
}
