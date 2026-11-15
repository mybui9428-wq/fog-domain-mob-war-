
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
        this.setMaxUpStep(1.0F);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {

            // AI tự tìm mục tiêu, chiến đấu và quay về chủ nhân.
            FogDomainMinionAI.tickMinion(this);

            // Giữ Linh Hồn Băng bay lơ lửng.
            double verticalMovement =
                    Math.sin(this.tickCount * 0.08D) * 0.025D;

            this.setDeltaMovement(
                    this.getDeltaMovement().x * 0.9D,
                    verticalMovement,
                    this.getDeltaMovement().z * 0.9D
            );
        }
    }
}
