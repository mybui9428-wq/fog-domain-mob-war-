
package com.example.examplemod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FogDomainEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(ExampleMod.MODID);

    // Linh Hồn Băng
    public static final DeferredHolder<EntityType<?>, EntityType<IceSpiritEntity>>
            ICE_SPIRIT = ENTITY_TYPES.registerEntityType(
                    "ice_spirit",
                    IceSpiritEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.8F, 1.8F)
                            .clientTrackingRange(8)
                            .updateInterval(2)
            );

    // Thây Ma Băng
    public static final DeferredHolder<EntityType<?>, EntityType<IceZombieEntity>>
            ICE_ZOMBIE = ENTITY_TYPES.registerEntityType(
                    "ice_zombie",
                    IceZombieEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.0F, 2.4F)
                            .clientTrackingRange(8)
                            .updateInterval(2)
            );

    // Bộ Xương Nhiều Tay
    public static final DeferredHolder<EntityType<?>, EntityType<MultiArmedSkeletonEntity>>
            MULTI_ARMED_SKELETON = ENTITY_TYPES.registerEntityType(
                    "multi_armed_skeleton",
                    MultiArmedSkeletonEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.9F, 2.2F)
                            .clientTrackingRange(8)
                            .updateInterval(2)
            );

    // Thây Ma Linh Hồn
    public static final DeferredHolder<EntityType<?>, EntityType<SoulZombieEntity>>
            SOUL_ZOMBIE = ENTITY_TYPES.registerEntityType(
                    "soul_zombie",
                    SoulZombieEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(8)
                            .updateInterval(2)
            );

    private FogDomainEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
