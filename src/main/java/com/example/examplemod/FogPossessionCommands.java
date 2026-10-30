
package com.example.examplemod;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class FogPossessionCommands {

    private FogPossessionCommands() {}

    public static void register(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("fogpossess")
                        .then(Commands.literal("start")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!FogDomainPossession.isController(player)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Bạn không có quyền sử dụng lệnh này."
                                                )
                                        );
                                        return 0;
                                    }

                                    Entity target = player.getCamera();

                                    if (target == player
                                            || target == null
                                            || !FogDomainPossession.start(
                                                    player,
                                                    target
                                            )) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Không thể nhập xác. Hãy đứng gần một sinh vật trong phạm vi 25 block."
                                                )
                                        );
                                        return 0;
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Đã bắt đầu nhập xác!"
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )
                        .then(Commands.literal("stop")
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    if (!FogDomainPossession.isController(player)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Bạn không có quyền sử dụng lệnh này."
                                                )
                                        );
                                        return 0;
                                    }

                                    if (!FogDomainPossession.stop(player)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Bạn hiện không nhập xác."
                                                )
                                        );
                                        return 0;
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Đã kết thúc nhập xác!"
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )
        );
    }
}
