
package com.example.examplemod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class SoulZombieEntity extends PathfinderMob {

    public SoulZombieEntity(
            EntityType<? extends SoulZombieEntity> entityType,
            Level level
    ) {
        super(entityType, level);

        this.setPersistenceRequired();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            FogDomainMinionAI.tickMinion(this);
        }
    }
}
