
package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class FogPossessionClient {

    private FogPossessionClient() {
    }

    public static void handlePayload(
            FogPossessionSyncPayload payload) {

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
