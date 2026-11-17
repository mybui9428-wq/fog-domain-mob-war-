
package com.example.examplemod;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomain5 {

    private static final Map<UUID, Boolean> ACTIVE =
            new HashMap<>();

    private FogDomain5() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    // Giữ phương thức tương thích với những file cũ còn gọi đến.
    // Lĩnh vực hiện tại không còn hệ thống tầng.
    public static int getTier(UUID playerId) {
        return 1;
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.getOrDefault(playerId, false);
    }

    public static void setActive(UUID playerId, boolean active) {
        ACTIVE.put(playerId, active);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("fogdomain5")

                        .then(Commands.literal("character")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    PacketDistributor.sendToPlayer(
                                            player,
                                            new FogCharacterPayload("open")
                                    );

                                    return Command.SINGLE_SUCCESS;
                                }))

                        .requires(source ->
                                source.getEntity() instanceof ServerPlayer player
                                        && isController(player))

                        // Bật lĩnh vực, không tạo hồ nước.
                        .then(Commands.literal("start")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    UUID id = player.getUUID();

                                    if (isActive(id)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Lĩnh vực đã được bật."));
                                        return 0;
                                    }

                                    setActive(id, true);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Đã bật Lĩnh Vực Thôn Phệ."),
                                            false
                                    );

                                    return Command.SINGLE_SUCCESS;
                                }))

                        // Tắt lĩnh vực.
                        .then(Commands.literal("stop")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    UUID id = player.getUUID();

                                    if (!isActive(id)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Lĩnh vực hiện không hoạt động."));
                                        return 0;
                                    }

                                    setActive(id, false);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Đã tắt Lĩnh Vực Thôn Phệ."),
                                            false
                                    );

                                    return Command.SINGLE_SUCCESS;
                                }))

                        // Kiểm tra trạng thái.
                        .then(Commands.literal("status")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    UUID id = player.getUUID();

                                    String status = isActive(id)
                                            ? "ĐANG BẬT"
                                            : "ĐANG TẮT";

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "LĨNH VỰC THÔN PHỆ"
                                                            + "\nTrạng thái: "
                                                            + status),
                                            false
                                    );

                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }
}
