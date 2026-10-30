
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
                FogPossessionSyncPayload.TYPE,
                FogPossessionSyncPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> handlePayload(payload))
        );
    }

    private static void handlePayload(
            FogPossessionSyncPayload payload
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        // Kết thúc nhập xác: trả camera về người chơi
        if (payload.action() == 0) {
            minecraft.setCameraEntity(minecraft.player);
            return;
        }

        // Bắt đầu nhập xác: chuyển camera sang sinh vật
        if (payload.action() == 1 && minecraft.level != null) {
            Entity target = minecraft.level.getEntity(payload.targetId());

            if (target != null && target.isAlive()) {
                minecraft.setCameraEntity(target);
            }
        }
    }
}
