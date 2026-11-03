
package com.example.examplemod;

import net.minecraft.network.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import io.netty.buffer.ByteBuf;

public record FogHudPayload(String data)
        implements CustomPacketPayload {

    public static final Type<FogHudPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            ExampleMod.MODID,
                            "fog_hud"
                    )
            );

    public static final StreamCodec<ByteBuf, FogHudPayload>
            STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    FogHudPayload::new,
                    FogHudPayload::data
            );

    @Override
    public Type<FogHudPayload> type() {
        return TYPE;
    }
}
