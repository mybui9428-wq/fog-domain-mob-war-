
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

        if (!this.level().isClientSide()) {
            FogDomainMinionAI.tickMinion(this);
        }
    }
}
