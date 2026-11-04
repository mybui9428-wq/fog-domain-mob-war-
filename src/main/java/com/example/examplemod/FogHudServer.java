package com.example.examplemod;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogHudServer {

    private static int tickCounter = 0;

    private FogHudServer() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        if (tickCounter < 10) {
            return;
        }

        tickCounter = 0;

        for (ServerPlayer player :
                event.getServer().getPlayerList().getPlayers()) {

            if (!player.getName().getString()
                    .equalsIgnoreCase("tubi2531")) {
                continue;
            }

            sendHud(player);
        }
    }

    private static void sendHud(ServerPlayer player) {
        FogDomainAbilityData data =
                FogDomainAbilityData.get(player.getServer());

        String status;

        if (FogDomainPossession.isSoulMode(player)) {
            status = "LINH HON";
        } else if (FogDomainPossession.isPossessing(player)) {
            status = "DANG NHAP HON";
        } else {
            status = "BINH THUONG";
        }

        StringBuilder hud = new StringBuilder();

        hud.append("LINH VUC SUONG MU\n");

        hud.append("Mob War: ")
                .append(MobWarCommands.enabled ? "ON" : "OFF")
                .append('\n');

        hud.append("Trang thai: ")
                .append(status)
                .append('\n');

        hud.append("Nang luong: ")
                .append(data.getEnergy(player.getUUID()))
                .append("/100\n");

        hud.append("Tong suc manh: ")
                .append(data.getTotalPower(player.getUUID()))
                .append('\n');

        hud.append("Nang luc da hap thu: ")
                .append(data.getAbilityCount(player.getUUID()))
                .append('\n');

        hud.append("Sinh vat no dich: ")
                .append(FogDomainSkills.getTamedCount(player.getUUID()))
                .append("/5\n");

        hud.append("Ky nang: ")
                .append(FogDomainSkills.getSelectedSkillName());

        PacketDistributor.sendToPlayer(
                player,
                new FogHudPayload(hud.toString())
        );
    }
}
