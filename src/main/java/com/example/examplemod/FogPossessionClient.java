package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterClientPayloadHandlersEvent;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class FogPossessionClient {

    private FogPossessionClient() {}

    @SubscribeEvent
    public static void registerPayloadHandlers(
            RegisterClientPayloadHandlersEvent event) {

        event.register(
                FogPossessionPayload.TYPE,
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
