
package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
public final class FogPossessionClient {

    private static boolean lastJump = false;
    private static boolean lastAttack = false;
    private static int sendCounter = 0;

    public FogPossessionClient(IEventBus modEventBus) {
        modEventBus.addListener(
                FogPossessionClient::registerPayloadHandlers
        );

        NeoForge.EVENT_BUS.addListener(
                FogPossessionClient::onClientTick
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

        if (payload.action() == 0) {
            minecraft.setCameraEntity(minecraft.player);
            return;
        }

        if (payload.action() == 1 && minecraft.level != null) {
            Entity target = minecraft.level.getEntity(payload.targetId());

            if (target != null && target.isAlive()) {
                minecraft.setCameraEntity(target);
            }
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null) {
            return;
        }

        if (minecraft.getCameraEntity() == minecraft.player) {
            return;
        }

        if (++sendCounter < 2) {
            return;
        }

        sendCounter = 0;

        float forward = 0.0F;
        float strafe = 0.0F;

        if (minecraft.options.keyUp.isDown()) forward += 1.0F;
        if (minecraft.options.keyDown.isDown()) forward -= 1.0F;
        if (minecraft.options.keyLeft.isDown()) strafe += 1.0F;
        if (minecraft.options.keyRight.isDown()) strafe -= 1.0F;

        boolean jump = minecraft.options.keyJump.isDown();
        boolean attack = minecraft.options.keyAttack.isDown();

        Entity camera = minecraft.getCameraEntity();

        float yaw = camera.getYRot();
        float pitch = camera.getXRot();

        PacketDistributor.sendToServer(
                new FogPossessionControlPayload(
                        forward,
                        strafe,
                        yaw,
                        pitch,
                        jump,
                        attack
                )
        );
    }
}
