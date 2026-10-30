
package com.example.examplemod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FogPossessionControlPayload(
        float forward,
        float strafe,
        float yaw,
        float pitch,
        boolean jump,
        boolean attack
) implements CustomPacketPayload {

    public static final Type<FogPossessionControlPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    ExampleMod.MODID,
                    "possession_control"
            ));

    public static final StreamCodec<ByteBuf, FogPossessionControlPayload>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    FogPossessionControlPayload::forward,
                    ByteBufCodecs.FLOAT,
                    FogPossessionControlPayload::strafe,
                    ByteBufCodecs.FLOAT,
                    FogPossessionControlPayload::yaw,
                    ByteBufCodecs.FLOAT,
                    FogPossessionControlPayload::pitch,
                    ByteBufCodecs.BOOL,
                    FogPossessionControlPayload::jump,
                    ByteBufCodecs.BOOL,
                    FogPossessionControlPayload::attack,
                    FogPossessionControlPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                TYPE,
                STREAM_CODEC,
                FogPossessionControlPayload::handleServer
        );
    }

    private static void handleServer(
            FogPossessionControlPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!FogDomainPossession.isController(player)
                    || !FogDomainPossession.isPossessing(player)) {
                return;
            }

            FogDomainPossession.control(
                    player,
                    payload.forward(),
                    payload.strafe(),
                    payload.yaw(),
                    payload.pitch(),
                    payload.jump(),
                    payload.attack()
            );
        });
    }
}
