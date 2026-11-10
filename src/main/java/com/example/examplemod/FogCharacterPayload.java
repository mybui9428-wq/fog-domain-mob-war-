
package com.example.examplemod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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

    // ================= SERVER =================

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

        // Cấp năng lực bẩm sinh nếu nhân vật chưa có.
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
                    : "Khong du dieu kien tang thuoc tinh.";
        } else {
            return;
        }

        context.reply(createUpdate(data, player, message));
    }

    // ================= SEND CHARACTER DATA =================

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
                message
        );

        return new FogCharacterPayload(response);
    }
}
