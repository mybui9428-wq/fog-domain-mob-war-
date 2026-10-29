package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(
modid = ExampleMod.MODID,
value = Dist.CLIENT,
bus = EventBusSubscriber.Bus.MOD
)
public final class FogPossessionClient {

private FogPossessionClient() {}

@SubscribeEvent
public static void registerPayloadHandlers(
        RegisterPayloadHandlersEvent event) {

    event.registrar("1").playToClient(
            FogPossessionPayload.TYPE,
            FogPossessionPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();

                if (minecraft.player == null) {
                    return;
                }

                if (payload.action() == 0) {
                    minecraft.setCameraEntity(minecraft.player);
                    return;
                }

                if (payload.action() == 1
                        && minecraft.level != null) {
                    Entity target = minecraft.level.getEntity(
                            payload.targetId());

                    if (target != null && target.isAlive()) {
                        minecraft.setCameraEntity(target);
                    }
                }
            })
    );
}

}
