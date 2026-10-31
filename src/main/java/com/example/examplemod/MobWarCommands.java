
package com.example.examplemod;

import com.mojang.brigadier.Command;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarCommands {

    public static boolean enabled = false;

    private MobWarCommands() {
    }

    private static boolean isController(ServerPlayer player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("mobwar")
                        .requires(source ->
                                source.getEntity() instanceof ServerPlayer
                        )

                        .then(Commands.literal("on")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();

                                    if (!isController(player)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Chi tubi2531 moi duoc bat Mob War!"
                                                )
                                        );
                                        return 0;
                                    }

                                    enabled = true;

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Mob War: ON"
                                            ),
                                            true
                                    );

                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        .then(Commands.literal("off")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();

                                    if (!isController(player)) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "Chi tubi2531 moi duoc tat Mob War!"
                                                )
                                        );
                                        return 0;
                                    }

                                    enabled = false;

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Mob War: OFF"
                                            ),
                                            true
                                    );

                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                        .then(Commands.literal("status")
                                .executes(context -> {

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Mob War: "
                                                            + (enabled ? "ON" : "OFF")
                                            ),
                                            false
                                    );

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
        );
    }
}
