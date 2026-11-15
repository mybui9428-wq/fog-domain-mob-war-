
package com.example.examplemod;

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

        // Linh Hồn Băng
        event.registerEntityRenderer(
                FogDomainEntities.ICE_SPIRIT.get(),
                context -> new FogDomainEntityRenderer<>(
                        context,
                        Identifier.parse(
                                "minecraft:textures/entity/skeleton/stray.png"
                        )
                )
        );

        // Thây Ma Băng
        event.registerEntityRenderer(
                FogDomainEntities.ICE_ZOMBIE.get(),
                context -> new FogDomainEntityRenderer<>(
                        context,
                        Identifier.parse(
                                "minecraft:textures/entity/zombie/zombie.png"
                        )
                )
        );

        // Bộ Xương Nhiều Tay
        event.registerEntityRenderer(
                FogDomainEntities.MULTI_ARMED_SKELETON.get(),
                context -> new FogDomainEntityRenderer<>(
                        context,
                        Identifier.parse(
                                "minecraft:textures/entity/skeleton/skeleton.png"
                        )
                )
        );

        // Thây Ma Linh Hồn
        event.registerEntityRenderer(
                FogDomainEntities.SOUL_ZOMBIE.get(),
                context -> new FogDomainEntityRenderer<>(
                        context,
                        Identifier.parse(
                                "minecraft:textures/entity/zombie/husk.png"
                        )
                )
        );

        ExampleMod.LOGGER.info(
                "FOG DOMAIN ENTITY RENDERERS REGISTERED"
        );
    }
}
