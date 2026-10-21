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

    private MobWarCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("fogmobwar")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("on")
                    .executes(context -> {
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
        );
    }
}
