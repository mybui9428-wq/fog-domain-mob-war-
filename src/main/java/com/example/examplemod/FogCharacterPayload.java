
package com.example.examplemod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = ExampleMod.MODID)
public record FogCharacterPayload(String data)
        implements CustomPacketPayload {

    public static final Type<FogCharacterPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            ExampleMod.MODID,
                            "character_data"
                    )
            );

    public static final StreamCodec<
            io.netty.buffer.ByteBuf,
            FogCharacterPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    FogCharacterPayload::data,
                    FogCharacterPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playBidirectional(
                TYPE,
                STREAM_CODEC,
                FogCharacterPayload::handleServer
        );
    }

    // MỞ BẢNG CHỈ SỐ
    public static void openCharacterScreen(ServerPlayer player) {
        FogDomainAbilityData data =
                FogDomainAbilityData.get(player.level().getServer());

        data.initializeCharacter(player.getUUID());

        data.initializeInnateAbilities(
                player.getUUID(),
                player.getName().getString()
        );

        PacketDistributor.sendToPlayer(
                player,
                createUpdate(data, player, "Da mo bang chi so.")
        );
    }

    // KIỂM TRA KỸ NĂNG ĐƯỢC PHÉP NÂNG CẤP
    private static boolean isUpgradeableSkill(String skillId) {
        return switch (skillId) {
            case FogDomainAbilityData.SKILL_ENERGY,
                 FogDomainAbilityData.SKILL_CAGE,
                 FogDomainAbilityData.SKILL_TAME,
                 FogDomainAbilityData.SKILL_COMBINE,
                 FogDomainAbilityData.SKILL_STEAL,
                 FogDomainAbilityData.SKILL_TRIPLE_SPIRIT -> true;
            default -> false;
        };
    }

    // XỬ LÝ YÊU CẦU TỪ CLIENT
    private static void handleServer(
            FogCharacterPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        String request = payload.data();

        if (request == null || request.length() > 64) {
            return;
        }

        FogDomainAbilityData data =
                FogDomainAbilityData.get(player.level().getServer());

        data.initializeCharacter(player.getUUID());

        data.initializeInnateAbilities(
                player.getUUID(),
                player.getName().getString()
        );

        String message = "Du lieu da cap nhat.";

        if (request.equals("open")) {

            message = "Da mo bang nhan vat.";

        } else if (request.startsWith("add|")) {

            String attribute = request.substring(4);

            if (!FogDomainAbilityData.ATTRIBUTES.contains(attribute)) {
                return;
            }

            boolean success = data.increaseAttribute(
                    player,
                    attribute,
                    1
            );

            message = success
                    ? "Da tang " + attribute + "."
                    : "Khong du Diem Linh Tinh hoac HP.";

        } else if (request.equals("levelup")) {

            int playerLevel = data.getCharacterLevel(player.getUUID());

            if (playerLevel >= FogDomainAbilityData.MAX_CHARACTER_LEVEL) {

                message = "Ban da dat cap toi da.";

            } else if (!data.canLevelUpByAttributes(player.getUUID())) {

                int required =
                        data.getRequiredAttributeForLevelUp(player.getUUID());

                message = "Ca 5 thuoc tinh phai dat toi thieu "
                        + required + " moi duoc tang cap.";

            } else {

                int spiritCost =
                        data.getLevelUpSpiritPointCost(player.getUUID());

                int fragmentCost =
                        data.getLevelUpRuleFragmentCost(player.getUUID());

                if (data.getSpiritPoints(player.getUUID()) < spiritCost) {

                    message = "Khong du Diem Linh Tinh! Can "
                            + spiritCost + ".";

                } else if (data.getRuleFragments(player.getUUID())
                        < fragmentCost) {

                    message = "Khong du Manh Vo Quy Tac! Can "
                            + fragmentCost + ".";

                } else {

                    boolean success = data.levelUp(player.getUUID());

                    if (success) {
                        message = "Tang cap thanh cong! Cap moi: "
                                + data.getCharacterLevel(player.getUUID());
                    } else {
                        message = "Tang cap that bai. Hay kiem tra lai dieu kien.";
                    }
                }
            }

        } else if (request.startsWith("upgrade|")) {

            String skillId = request.substring("upgrade|".length());

            if (!isUpgradeableSkill(skillId)) {
                return;
            }

            int oldSkillLevel = data.getSkillLevel(
                    player.getUUID(),
                    skillId
            );

            boolean success = data.upgradeSkill(
                    player.getUUID(),
                    skillId,
                    0
            );

            if (success) {
                message = "Nang cap ky nang thanh cong! Cap moi: "
                        + data.getSkillLevel(player.getUUID(), skillId);
            } else {
                message = "Khong du tai nguyen! Can "
                        + (oldSkillLevel * 100)
                        + " Diem Linh Tinh va "
                        + oldSkillLevel
                        + " Tinh Thach Linh Hon.";
            }

        } else {
            return;
        }

        context.reply(createUpdate(data, player, message));
    }

    // TẠO DỮ LIỆU GỬI VỀ CLIENT
    private static FogCharacterPayload createUpdate(
            FogDomainAbilityData data,
            ServerPlayer player,
            String message
    ) {
        var id = player.getUUID();

        String response = String.join("|",
                "update",

                String.valueOf(data.getCharacterLevel(id)),
                String.valueOf(data.getSpiritPoints(id)),
                String.valueOf(data.getFreeStatPoints(id)),
                String.valueOf(data.getRuleFragments(id)),
                String.valueOf(data.getSoulCrystals(id)),

                String.valueOf(data.getAttribute(id, "agility")),
                String.valueOf(data.getAttribute(id, "stamina")),
                String.valueOf(data.getAttribute(id, "strength")),
                String.valueOf(data.getAttribute(id, "spirit")),
                String.valueOf(data.getAttribute(id, "charisma")),

                String.valueOf(player.getHealth()),
                String.valueOf(player.getMaxHealth()),

                message,

                String.valueOf(data.getSkillLevel(
                        id, FogDomainAbilityData.SKILL_ENERGY
                )),
                String.valueOf(data.getSkillLevel(
                        id, FogDomainAbilityData.SKILL_CAGE
                )),
                String.valueOf(data.getSkillLevel(
                        id, FogDomainAbilityData.SKILL_TAME
                )),
                String.valueOf(data.getSkillLevel(
                        id, FogDomainAbilityData.SKILL_COMBINE
                )),
                String.valueOf(data.getSkillLevel(
                        id, FogDomainAbilityData.SKILL_STEAL
                )),
                String.valueOf(data.getSkillLevel(
                        id, FogDomainAbilityData.SKILL_TRIPLE_SPIRIT
                ))
        );

        return new FogCharacterPayload(response);
    }
}
