
package com.example.examplemod;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
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
                        .then(Commands.literal("awakentuc")
                                .requires(Commands.hasPermission(
    new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)
))
                                .executes(context -> {
                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();

                                    FogDomainAbilityData data =
                                            FogDomainAbilityData.get(player.level().getServer());

                                    if (data.isTucThanhGia(player.getUUID())) {
                                        player.sendSystemMessage(Component.literal(
                                                "Ban da la Tuc Thanh Gia duy nhat."));
                                    } else if (data.awakenTucThanhGia(player.getUUID())) {
                                        player.sendSystemMessage(Component.literal(
                                                "Thuc tinh Tuc Thanh Gia thanh cong. Ban la nguoi duy nhat."));
                                        FogCharacterPayload.openCharacterScreen(player);
                                    } else {
                                        player.sendSystemMessage(Component.literal(
                                                "The gioi da co mot Tuc Thanh Gia. Khong the thuc tinh nguoi thu hai."));
                                    }
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
