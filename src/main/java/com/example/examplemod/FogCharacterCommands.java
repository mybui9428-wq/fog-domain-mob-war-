
package com.example.examplemod;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.StringArgumentType;
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
                        .then(Commands.literal("scroll")
                                .then(Commands.literal("list")
                                        .executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();

                                            FogDomainAbilityData data =
                                                    FogDomainAbilityData.get(player.level().getServer());

                                            player.sendSystemMessage(Component.literal(
                                                    "===== KY NANG CO THE PHA GIOI ====="
                                            ));

                                            for (String skillId : data.getKnownSkillIds(player.getUUID())) {
                                                player.sendSystemMessage(Component.literal(
                                                        "- " + skillId
                                                ));
                                            }

                                            player.sendSystemMessage(Component.literal(
                                                    "Dung /fog scroll apply <skill_id> khi cam Cuon Truc Pha Gioi tren tay."
                                            ));
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("apply")
                                        .then(Commands.argument(
                                                "skill_id",
                                                StringArgumentType.word()
                                        ).executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();

                                            if (!player.getMainHandItem().is(ExampleMod.BREAKTHROUGH_SCROLL.get())) {
                                                player.sendSystemMessage(Component.literal(
                                                        "Hay cam Cuon Truc Pha Gioi tren tay chinh."
                                                ));
                                                return 0;
                                            }

                                            String skillId = StringArgumentType.getString(
                                                    context, "skill_id"
                                            );

                                            FogDomainAbilityData data =
                                                    FogDomainAbilityData.get(player.level().getServer());

                                            if (data.hasSkillLimitBreak(player.getUUID(), skillId)) {
                                                player.sendSystemMessage(Component.literal(
                                                        "Ky nang nay da duoc pha gioi."
                                                ));
                                                return 0;
                                            }

                                            if (!data.unlockSkillLimitBreak(player.getUUID(), skillId)) {
                                                player.sendSystemMessage(Component.literal(
                                                        "Khong tim thay ky nang nay trong danh sach ky nang ban so huu."
                                                ));
                                                return 0;
                                            }

                                            player.getMainHandItem().shrink(1);
                                            player.sendSystemMessage(Component.literal(
                                                    "Pha gioi thanh cong! Ky nang " + skillId
                                                            + " co the vuot qua cap 10."
                                            ));
                                            return 1;
                                        }))
                                )
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
                                                    "/fog scroll list - Xem ky nang co the pha gioi"
                                            )
                                    );

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "/fog scroll apply <skill_id> - Dung cuon truc cho mot ky nang"
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
