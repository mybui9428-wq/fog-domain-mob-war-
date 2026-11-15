
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

            // Tự chiến đấu và triệu hồi Thây Ma Linh Hồn.
            FogDomainMinionAI.tickMinion(this);

            // Giữ cơ thể bay lơ lửng.
            this.setDeltaMovement(
                    this.getDeltaMovement().x * 0.95D,
                    Math.sin(this.tickCount * 0.08D) * 0.025D,
                    this.getDeltaMovement().z * 0.95D
            );
        }
    }
}
