
package com.example.examplemod;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogHudServer {

    private static int tickCounter = 0;

    private FogHudServer() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        // Cập nhật mỗi 10 tick.
        if (tickCounter < 10) {
            return;
        }

        tickCounter = 0;

        MinecraftServer server = event.getServer();

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {

            // Chỉ tubi2531 nhận dữ liệu HUD.
            if (!player.getName().getString()
                    .equalsIgnoreCase("tubi2531")) {
                continue;
            }

            sendHud(player, server);
        }
    }

    private static void sendHud(
            ServerPlayer player,
            MinecraftServer server) {

        FogDomainAbilityData data =
                FogDomainAbilityData.get(server);

        data.initializePower(player.getUUID(), true);

        String status;

        if (FogDomainPossession.isSoulMode(player)) {
            status = "LINH HON";
        } else if (FogDomainPossession.isPossessing(player)) {
            status = "DANG NHAP HON";
        } else {
            status = "BINH THUONG";
        }

        double energy = data.getEnergy(player.getUUID());
        double power = data.getTotalPower(player.getUUID());

        int abilityCount =
                data.getAbilityCount(player.getUUID());

        int tamedCount =
                FogDomainSkills.getTamedCount(player.getUUID());

        StringBuilder hud = new StringBuilder();

        hud.append("Mob War: ")
                .append(MobWarCommands.enabled ? "ON" : "OFF")
                .append('\n');

        hud.append("Trang thai: ")
                .append(status)
                .append('\n');

        hud.append("Nang luong: ")
                .append(String.format("%.0f/100", energy))
                .append('\n');

        hud.append("Tong suc manh: ")
                .append(String.format("%.1f", power))
                .append('\n');

        hud.append("Nang luc da hap thu: ")
                .append(abilityCount)
                .append('\n');

        hud.append("Sinh vat no dich: ")
                .append(tamedCount)
                .append("/5")
                .append('\n');

        hud.append("Ky nang: ")
                .append(FogDomainSkills.getSelectedSkillName())
                .append('\n');

        List<String> abilities =
                data.getAbilities(player.getUUID());

        hud.append("Nang luc:");

        if (abilities.isEmpty()) {
            hud.append(" Chua co");
        } else {
            int count = Math.min(abilities.size(), 4);

            for (int i = 0; i < count; i++) {
                hud.append('\n')
                        .append("- ")
                        .append(shortName(abilities.get(i)));
            }

            if (abilities.size() > count) {
                hud.append('\n')
                        .append("Va ")
                        .append(abilities.size() - count)
                        .append(" nang luc khac");
            }
        }

        PacketDistributor.sendToPlayer(
                player,
                new FogHudPayload(hud.toString())
        );
    }

    private static String shortName(String id) {
        if (id == null || id.isBlank()) {
            return "Khong ro";
        }

        int separator = id.indexOf(':');

        String name = separator >= 0
                ? id.substring(separator + 1)
                : id;

        return name.replace('_', ' ');
    }
}
