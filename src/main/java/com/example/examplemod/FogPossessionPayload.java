
package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
public final class FogPossessionClient {

    public FogPossessionClient(IEventBus modEventBus) {
        modEventBus.addListener(
            FogPossessionClient::registerPayloadHandlers
        );
    }

    private static void registerPayloadHandlers(
        RegisterPayloadHandlersEvent event
    ) {
        event.registrar("1").playToClient(
            FogPossessionPayload.TYPE,
            FogPossessionPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> handlePayload(payload)
            )
        );
    }

    public static void handlePayload(FogPossessionPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        if (payload.action() == 0) {
            minecraft.setCameraEntity(minecraft.player);
            return;
        }

        if (payload.action() == 1 && minecraft.level != null) {
            Entity target = minecraft.level.getEntity(
                payload.targetId()
            );

            if (target != null && target.isAlive()) {
                minecraft.setCameraEntity(target);
            }
        }
    }
}
