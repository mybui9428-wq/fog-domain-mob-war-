
package com.example.examplemod;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogCharacterCommands {

    private FogCharacterCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("fog")
                        .then(Commands.literal("stats")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    FogCharacterPayload.openCharacterScreen(
                                            player
                                    );

                                    return 1;
                                })
                        )
                        .then(Commands.literal("help")
                                .executes(context -> {

                                    ServerPlayer player =
                                            context.getSource()
                                                    .getPlayerOrException();

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "===== FOG DOMAIN ====="
                                            )
                                    );

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "/fog stats - Mo bang chi so"
                                            )
                                    );

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "/fog help - Xem danh sach lenh"
                                            )
                                    );

                                    return 1;
                                })
                        )
        );
    }
}
