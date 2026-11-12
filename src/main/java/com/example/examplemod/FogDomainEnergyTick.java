
package com.example.examplemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainEnergyTick {

    private FogDomainEnergyTick() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        // Chỉ xử lý một lần mỗi giây.
        if (server.getTickCount() % 20 != 0) {
            return;
        }

        FogDomainAbilityData data =
                FogDomainAbilityData.get(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            // Giữ nguyên cơ chế hồi năng lượng riêng cho tubi2531.
            if (!player.getName().getString()
                    .equalsIgnoreCase("tubi2531")) {
                continue;
            }

            data.initializeCharacter(player.getUUID());

            // Hồi 1 năng lượng mỗi giây.
            // Giới hạn tối đa tự động phụ thuộc vào chỉ số Tinh thần.
            data.regenerateEnergy(player.getUUID(), 1.0D);
        }
    }
}
