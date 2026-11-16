
package com.example.examplemod;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.Identifier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = ExampleMod.MODID,
        value = Dist.CLIENT
)
public class FogDomainClientEvents {

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {

        // LINH HỒN BĂNG - mô hình skeleton, texture stray.
        event.registerEntityRenderer(
                FogDomainEntities.ICE_SPIRIT.get(),
                context -> new FogDomainSkeletonRenderer<>(
                        context,
                        Identifier.parse(
                                "minecraft:textures/entity/skeleton/stray.png"
                        )
                )
        );

        // THÂY MA BĂNG - mô hình zombie.
        event.registerEntityRenderer(
                FogDomainEntities.ICE_ZOMBIE.get(),
                context -> new FogDomainZombieRenderer<>(
                        context,
                        ModelLayers.ZOMBIE,
                        Identifier.parse(
                                "minecraft:textures/entity/zombie/zombie.png"
                        )
                )
        );

        // BỘ XƯƠNG NHIỀU TAY - mô hình skeleton.
        event.registerEntityRenderer(
                FogDomainEntities.MULTI_ARMED_SKELETON.get(),
                context -> new FogDomainSkeletonRenderer<>(
                        context,
                        Identifier.parse(
                                "minecraft:textures/entity/skeleton/skeleton.png"
                        )
                )
        );

        // THÂY MA LINH HỒN - mô hình zombie husk.
        event.registerEntityRenderer(
                FogDomainEntities.SOUL_ZOMBIE.get(),
                context -> new FogDomainZombieRenderer<>(
                        context,
                        ModelLayers.ZOMBIE,
                        Identifier.parse(
                                "minecraft:textures/entity/zombie/husk.png"
                        )
                )
        );

        ExampleMod.LOGGER.info(
                "FOG DOMAIN CUSTOM ENTITY RENDERERS REGISTERED"
        );
    }
}
