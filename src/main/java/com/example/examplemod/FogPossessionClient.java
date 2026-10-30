
package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
public class FogPossessionClient {

    private static int tickCounter = 0;

    public FogPossessionClient() {
        NeoForge.EVENT_BUS.addListener(
            FogPossessionClient::onClientTick
        );

        ClientPacketDistributor.class.getName();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        Entity camera = minecraft.getCameraEntity();

        if (camera == null || camera == minecraft.player) {
            return;
        }

        tickCounter++;

        // Gửi điều khiển mỗi 2 tick để giảm tải mạng
        if (tickCounter < 2) {
            return;
        }

        tickCounter = 0;

        float forward = 0.0F;
        float strafe = 0.0F;

        if (minecraft.options.keyUp.isDown()) {
            forward += 1.0F;
        }

        if (minecraft.options.keyDown.isDown()) {
            forward -= 1.0F;
        }

        if (minecraft.options.keyLeft.isDown()) {
            strafe += 1.0F;
        }

        if (minecraft.options.keyRight.isDown()) {
            strafe -= 1.0F;
        }

        float yaw = camera.getYRot();
        float pitch = camera.getXRot();

        boolean jump = minecraft.options.keyJump.isDown();
        boolean attack = minecraft.options.keyAttack.isDown();

        ClientPacketDistributor.sendToServer(
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
