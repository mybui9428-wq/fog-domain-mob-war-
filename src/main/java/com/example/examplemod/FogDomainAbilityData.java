
package com.example.examplemod;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FogDomainAbilityData extends SavedData {

    private static final String DATA_NAME = "fog_domain_abilities";

    private static final Codec<Map<String, List<String>>> ABILITIES_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.STRING.listOf()
            );

    public static final SavedDataType<FogDomainAbilityData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            "examplemod",
                            DATA_NAME
                    ),
                    FogDomainAbilityData::new,
                    RecordCodecBuilder.create(instance ->
                            instance.group(
                                    ABILITIES_CODEC
                                            .fieldOf("abilities")
                                            .forGetter(data -> data.abilities)
                            ).apply(
                                    instance,
                                    FogDomainAbilityData::new
                            )
                    )
            );

    private final Map<String, List<String>> abilities =
            new HashMap<>();

    public FogDomainAbilityData() {
    }

    private FogDomainAbilityData(
            Map<String, List<String>> loadedAbilities) {

        if (loadedAbilities != null) {
            loadedAbilities.forEach((playerId, list) -> {
                if (playerId != null && list != null) {
                    abilities.put(
                            playerId,
                            new ArrayList<>(list)
                    );
                }
            });
        }
    }

    public static FogDomainAbilityData get(
            MinecraftServer server) {

        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean addAbility(
            UUID playerId,
            String entityTypeId) {

        if (playerId == null
                || entityTypeId == null
                || entityTypeId.isBlank()) {
            return false;
        }

        List<String> owned = abilities.computeIfAbsent(
                playerId.toString(),
                key -> new ArrayList<>()
        );

        if (owned.contains(entityTypeId)) {
            return false;
        }

        owned.add(entityTypeId);
        setDirty();

        return true;
    }

    public boolean hasAbility(
            UUID playerId,
            String entityTypeId) {

        if (playerId == null || entityTypeId == null) {
            return false;
        }

        List<String> owned = abilities.get(
                playerId.toString()
        );

        return owned != null && owned.contains(entityTypeId);
    }

    public List<String> getAbilities(UUID playerId) {

        if (playerId == null) {
            return List.of();
        }

        List<String> owned = abilities.get(
                playerId.toString()
        );

        if (owned == null) {
            return List.of();
        }

        return List.copyOf(owned);
    }

    public int getAbilityCount(UUID playerId) {
        return getAbilities(playerId).size();
    }
}
