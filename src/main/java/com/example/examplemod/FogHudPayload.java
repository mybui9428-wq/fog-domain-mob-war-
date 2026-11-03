
package com.example.examplemod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FogHudPayload(String data)
        implements CustomPacketPayload {

    public static final Type<FogHudPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            ExampleMod.MODID,
                            "fog_hud"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, FogHudPayload>
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
