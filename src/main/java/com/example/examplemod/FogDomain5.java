
package com.example.examplemod;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomain5 {

    private static final int MAX_TIER = 7;

    private static final Map<UUID, Integer> TIERS =
            new HashMap<>();

    private static final Map<UUID, Boolean> ACTIVE =
            new HashMap<>();

    private FogDomain5() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getGameProfile().getName()
                .equalsIgnoreCase("tubi2531");
    }

    public static int getTier(UUID playerId) {
        return TIERS.getOrDefault(playerId, 1);
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
                        .requires(source ->
                                source.getEntity() instanceof ServerPlayer player
                                        && isController(player))

                        .then(Commands.literal("ability")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();

                                    int current = getTier(player.getUUID());

                                    if (current >= MAX_TIER) {
                                        context.getSource().sendSuccess(
                                                () -> Component.literal(
                                                        "Lĩnh vực đã đạt tầng tối đa 7."),
                                                false
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    int next = current + 1;
                                    TIERS.put(player.getUUID(), next);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Đã chọn Lĩnh Vực Thôn Phệ tầng "
                                                            + next + "/7."),
                                            false
                                    );

                                    return Command.SINGLE_SUCCESS;
                                }))

                        .then(Commands.literal("start")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();

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
                                                    "Đã bật Lĩnh Vực Thôn Phệ tầng "
                                                            + getTier(id) + "/7."),
                                            false
                                    );

                                    return Command.SINGLE_SUCCESS;
                                }))

                        .then(Commands.literal("stop")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();

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

                        .then(Commands.literal("status")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();

                                    UUID id = player.getUUID();

                                    String status = isActive(id)
                                            ? "ĐANG BẬT"
                                            : "ĐANG TẮT";

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "LĨNH VỰC THÔN PHỆ"
                                                            + "\nTầng: "
                                                            + getTier(id) + "/7"
                                                            + "\nTrạng thái: "
                                                            + status),
                                            false
                                    );

                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }
}
