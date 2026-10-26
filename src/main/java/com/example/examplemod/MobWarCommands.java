
package com.example.examplemod;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class MobWarCommands {

    public static boolean enabled = false;

    // Mob War version currently selected.
    public static int version = 2;

    private MobWarCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("fogmobwar")
                .requires(Commands.hasPermission(
                    Commands.LEVEL_GAMEMASTERS))

                .then(Commands.literal("on")
                    .executes(context -> {
                        enabled = true;

                        context.getSource().sendSuccess(
                            () -> Component.literal(
                                "Linh Vuc Suong Mu: ON | Mob War V"
                                    + version),
                            true
                        );

                        return Command.SINGLE_SUCCESS;
                    })
                )

                .then(Commands.literal("off")
                    .executes(context -> {
                        enabled = false;

                        context.getSource().sendSuccess(
                            () -> Component.literal(
                                "Linh Vuc Suong Mu: OFF"),
                            true
                        );

                        return Command.SINGLE_SUCCESS;
                    })
                )

                .then(Commands.literal("v2")
                    .executes(context -> {
                        version = 2;

                        context.getSource().sendSuccess(
                            () -> Component.literal(
                                "Da chon Mob War V2"),
                            true
                        );

                        return Command.SINGLE_SUCCESS;
                    })
                )

                .then(Commands.literal("status")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                            () -> Component.literal(
                                "Linh Vuc: "
                                    + (enabled ? "ON" : "OFF")
                                    + " | Mob War V" + version),
                            false
                        );

                        return Command.SINGLE_SUCCESS;
                    })
                )
        );
    }
}
