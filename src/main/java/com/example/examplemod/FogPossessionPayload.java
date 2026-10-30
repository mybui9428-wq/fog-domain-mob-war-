
package com.example.examplemod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FogPossessionPayload(int action, int targetId)
        implements CustomPacketPayload {

    public static final Type<FogPossessionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    ExampleMod.MODID, "possession"));

    public static final StreamCodec<ByteBuf, FogPossessionPayload>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    FogPossessionPayload::action,
                    ByteBufCodecs.VAR_INT,
                    FogPossessionPayload::targetId,
                    FogPossessionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                TYPE,
                STREAM_CODEC,
                FogPossessionPayload::handleServer
        );
    }

    private static void handleServer(
            FogPossessionPayload payload,
            IPayloadContext context) {

        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!FogDomainPossession.isController(player)) {
                return;
            }

            if (payload.action() == 0) {
                FogDomainPossession.stop(player);
                return;
            }

            if (payload.action() != 1) {
                return;
            }

            Entity target = player.level().getEntity(payload.targetId());

            if (!(target instanceof LivingEntity)
                    || target == player
                    || !target.isAlive()
                    || player.distanceToSqr(target) > 625.0D) {
                return;
            }

            FogDomainPossession.start(player, target);
        });
    }
}
