
package com.example.examplemod;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarCommands {

    public static boolean enabled = false;

    private MobWarCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("mobwar")
                        .requires(source -> source.getEntity() instanceof net.minecraft.server.level.ServerPlayer)
                        .then(Commands.literal("on")
                                .executes(context -> {
                                    enabled = true;
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Mob War: ON"),
                                            true
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("off")
                                .executes(context -> {
                                    enabled = false;
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Mob War: OFF"),
                                            true
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Mob War: " + (enabled ? "ON" : "OFF")
                                            ),
                                            false
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }
}
