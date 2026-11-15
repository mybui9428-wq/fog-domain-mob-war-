
package com.example.examplemod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class IceSpiritEntity extends PathfinderMob {

    public IceSpiritEntity(
            EntityType<? extends IceSpiritEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    @Override
    public void tick() {
        super.tick();

        // Linh Hồn Băng luôn bay lơ lửng.
        if (!this.level().isClientSide()) {
            this.setDeltaMovement(
                    this.getDeltaMovement().x * 0.9,
                    Math.sin(this.tickCount * 0.08) * 0.025,
                    this.getDeltaMovement().z * 0.9
            );
        }
    }
}
