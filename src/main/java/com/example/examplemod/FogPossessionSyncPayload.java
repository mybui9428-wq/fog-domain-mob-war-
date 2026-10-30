
package com.example.examplemod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record FogPossessionSyncPayload(int action, int targetId)
        implements CustomPacketPayload {

    public static final Type<FogPossessionSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    ExampleMod.MODID, "possession_sync"));

    public static final StreamCodec<ByteBuf, FogPossessionSyncPayload>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    FogPossessionSyncPayload::action,
                    ByteBufCodecs.VAR_INT,
                    FogPossessionSyncPayload::targetId,
                    FogPossessionSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                TYPE,
                STREAM_CODEC,
                (FogPossessionSyncPayload payload,
                 net.neoforged.neoforge.network.handling.IPayloadContext context) -> {
                    context.enqueueWork(() ->
                            DistExecutor.unsafeRunWhenOn(
                                    Dist.CLIENT,
                                    () -> () ->
                                            FogPossessionClient.handlePayload(payload)
                            )
                    );
                }
        );
    }
}
