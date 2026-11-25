
package com.example.examplemod;

import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
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

    private static boolean hasKyVat(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(FogWeaponItems.SWORD.get())
                    || stack.is(FogWeaponItems.SHIELD.get())
                    || stack.is(FogWeaponItems.LONG_SPEAR.get())
                    || stack.is(FogWeaponItems.BOW.get())
                    || stack.is(FogWeaponItems.BAT_DIET_LUU_ANH.get())) {
                return true;
            }
        }
        return false;
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
                                    } else if (!data.canAwakenTucThanhGia(
                                            player.getUUID(), hasKyVat(player))) {
                                        player.sendSystemMessage(Component.literal(
                                                "Khong du dieu kien: can cap duoi 5, bon thuoc tinh nhanh nhen/the luc/suc manh/tinh than tren 30, mi luc duoi 20, 10.000 linh tinh va it nhat mot Ky Vat. The gioi chi co mot Tuc Thanh Gia."));
                                    } else if (data.awakenTucThanhGia(player.getUUID())) {
                                        player.sendSystemMessage(Component.literal(
                                                "Thuc tinh Tuc Thanh Gia thanh cong! Mi luc da bien mat va he ky nang rieng da mo khoa."));
                                        FogCharacterPayload.openCharacterScreen(player);
                                    } else {
                                        player.sendSystemMessage(Component.literal(
                                                "The gioi da co mot Tuc Thanh Gia. Khong the thuc tinh nguoi thu hai."));
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("tuc")
                                .then(Commands.literal("skills")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            FogDomainAbilityData data = FogDomainAbilityData.get(player.level().getServer());
                                            if (!data.isTucThanhGia(player.getUUID())) {
                                                player.sendSystemMessage(Component.literal("Ban chua thuc tinh Tuc Thanh Gia."));
                                                return 0;
                                            }
                                            player.sendSystemMessage(Component.literal("=== KY NANG TUC THANH GIA ==="));
                                            player.sendSystemMessage(Component.literal("Dong Sat Chi Nhan: thien_hoa (chu dong), chan_thuc_chi_nhan (chu dong)"));
                                            player.sendSystemMessage(Component.literal("Ky nang doc quyen: vo_vat_bat_tram (chu dong), tuyet_menh_nhat_dao (bi dong), hoi_thanh (bi dong), ta_se_khong_chet (bi dong), bat_huu_chien (bi dong), cuong_hoa (chu dong)"));
                                            player.sendSystemMessage(Component.literal("Cac ky nang bi dong khac: cam_tuyet_khong_che, tich_diet_chi_hoa"));
                                            player.sendSystemMessage(Component.literal("Cuop Doat: cuop_doat_passive (bi dong), cuop_doat_active (chu dong); Ket Hop: ket_hop (chu dong)"));
                                            for (String skill : FogDomainAbilityData.TUC_THANH_GIA_SKILLS) {
                                                player.sendSystemMessage(Component.literal(skill + " - cap " + data.getSkillLevel(player.getUUID(), skill)));
                                            }
                                            player.sendSystemMessage(Component.literal("Nang cap: /fog tuc upgrade <skill_id>"));
                                            return 1;
                                        }))
                                .then(Commands.literal("upgrade")
                                        .then(Commands.argument("skill", StringArgumentType.word())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    FogDomainAbilityData data = FogDomainAbilityData.get(player.level().getServer());
                                                    String skill = StringArgumentType.getString(context, "skill");
                                                    if (!data.isTucThanhGia(player.getUUID())
                                                            || !FogDomainAbilityData.TUC_THANH_GIA_SKILLS.contains(skill)) {
                                                        player.sendSystemMessage(Component.literal("Ky nang khong hop le hoac ban chua la Tuc Thanh Gia."));
                                                        return 0;
                                                    }
                                                    int oldLevel = data.getSkillLevel(player.getUUID(), skill);
                                                    if (data.upgradeSkill(player.getUUID(), skill, 0)) {
                                                        player.sendSystemMessage(Component.literal("Da nang " + skill + " len cap " + data.getSkillLevel(player.getUUID(), skill)));
                                                        return 1;
                                                    }
                                                    player.sendSystemMessage(Component.literal("Khong du tai nguyen. Can " + (oldLevel * 100) + " linh tinh va " + oldLevel + " tinh thach linh hon."));
                                                    return 0;
                                                }))))
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
