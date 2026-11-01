
package com.example.examplemod;

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

    // Bốn kỹ năng có thể lựa chọn.
    public static final String SKILL_ENERGY = "energy";
    public static final String SKILL_CAGE = "cage";
    public static final String SKILL_TAME = "tame";
    public static final String SKILL_DEVOUR = "devour";

    public static final List<String> SKILLS = List.of(
            SKILL_ENERGY,
            SKILL_CAGE,
            SKILL_TAME,
            SKILL_DEVOUR
    );

    // Danh sách năng lực đã hấp thụ.
    private static final Codec<Map<String, List<String>>> ABILITIES_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf());

    // Sức mạnh được lưu riêng theo UUID và từng kỹ năng.
    private static final Codec<Map<String, Map<String, Double>>> POWERS_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
            );

    public static final SavedDataType<FogDomainAbilityData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath("examplemod", DATA_NAME),
                    FogDomainAbilityData::new,
                    RecordCodecBuilder.create(instance ->
                            instance.group(
                                    ABILITIES_CODEC.fieldOf("abilities")
                                            .forGetter(data -> data.abilities),

                                    POWERS_CODEC.optionalFieldOf("powers", Map.of())
                                            .forGetter(data -> data.powers)
                            ).apply(instance, FogDomainAbilityData::new)
                    )
            );

    private final Map<String, List<String>> abilities = new HashMap<>();

    private final Map<String, Map<String, Double>> powers =
            new HashMap<>();

    public FogDomainAbilityData() {
    }

    private FogDomainAbilityData(
            Map<String, List<String>> loadedAbilities,
            Map<String, Map<String, Double>> loadedPowers
    ) {
        if (loadedAbilities != null) {
            loadedAbilities.forEach((id, list) -> {
                if (id != null && list != null) {
                    abilities.put(id, new ArrayList<>(list));
                }
            });
        }

        if (loadedPowers != null) {
            loadedPowers.forEach((id, skillMap) -> {
                if (id != null && skillMap != null) {
                    powers.put(id, new HashMap<>(skillMap));
                }
            });
        }
    }

    public static FogDomainAbilityData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private String key(UUID entityId) {
        return entityId.toString();
    }

    /**
     * Khởi tạo sức mạnh cho một người chơi hoặc sinh vật.
     * Controller tubi2531 bắt đầu 10 điểm mỗi kỹ năng.
     * Những đối tượng khác bắt đầu 100 điểm mỗi kỹ năng.
     */
    public void initializePower(UUID entityId, boolean isController) {
        String id = key(entityId);

        Map<String, Double> skillMap =
                powers.computeIfAbsent(id, ignored -> new HashMap<>());

        double startingPower = isController ? 10.0D : 100.0D;
        boolean changed = false;

        for (String skill : SKILLS) {
            if (!skillMap.containsKey(skill)) {
                skillMap.put(skill, startingPower);
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    /**
     * Lấy sức mạnh của một kỹ năng.
     */
    public double getPower(UUID entityId, String skill) {
        Map<String, Double> skillMap = powers.get(key(entityId));

        if (skillMap == null) {
            return 0.0D;
        }

        return skillMap.getOrDefault(skill, 0.0D);
    }

    /**
     * Cộng sức mạnh. Không giới hạn số lần cộng dồn.
     */
    public void addPower(UUID entityId, String skill, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        Map<String, Double> skillMap =
                powers.computeIfAbsent(key(entityId), ignored -> new HashMap<>());

        double current = skillMap.getOrDefault(skill, 0.0D);
        skillMap.put(skill, current + amount);

        setDirty();
    }

    /**
     * Đặt sức mạnh của một kỹ năng.
     */
    public void setPower(UUID entityId, String skill, double amount) {
        Map<String, Double> skillMap =
                powers.computeIfAbsent(key(entityId), ignored -> new HashMap<>());

        skillMap.put(skill, Math.max(0.0D, amount));
        setDirty();
    }

    /**
     * Người chơi bị hấp thụ:
     * Người đó giữ lại một nửa sức mạnh từng kỹ năng.
     * tubi2531 nhận nửa còn lại.
     */
    public void transferPlayerPowers(UUID ownerId, UUID targetId) {
        initializePower(ownerId, true);
        initializePower(targetId, false);

        for (String skill : SKILLS) {
            double targetPower = getPower(targetId, skill);
            double transferred = targetPower / 2.0D;

            setPower(targetId, skill, targetPower - transferred);
            addPower(ownerId, skill, transferred);
        }

        setDirty();
    }

    /**
     * Sinh vật bị hấp thụ:
     * tubi2531 nhận toàn bộ sức mạnh của sinh vật.
     */
    public void transferCreaturePowers(UUID ownerId, UUID creatureId) {
        initializePower(ownerId, true);
        initializePower(creatureId, false);

        for (String skill : SKILLS) {
            double creaturePower = getPower(creatureId, skill);
            addPower(ownerId, skill, creaturePower);
        }

        setDirty();
    }

    /**
     * Lưu năng lực đã hấp thụ.
     * Cho phép trùng lặp để năng lực có thể cộng dồn.
     */
    public boolean addAbility(UUID playerId, String entityTypeId) {
        if (playerId == null || entityTypeId == null || entityTypeId.isBlank()) {
            return false;
        }

        List<String> owned =
                abilities.computeIfAbsent(key(playerId), ignored -> new ArrayList<>());

        owned.add(entityTypeId);
        setDirty();

        return true;
    }

    public boolean hasAbility(UUID playerId, String entityTypeId) {
        List<String> owned = abilities.get(key(playerId));

        return owned != null && owned.contains(entityTypeId);
    }

    public List<String> getAbilities(UUID playerId) {
        List<String> owned = abilities.get(key(playerId));

        if (owned == null) {
            return List.of();
        }

        return List.copyOf(owned);
    }

    public int getAbilityCount(UUID playerId) {
        return getAbilities(playerId).size();
    }

    /**
     * Tính tổng sức mạnh của bốn kỹ năng.
     */
    public double getTotalPower(UUID entityId) {
        double total = 0.0D;

        for (String skill : SKILLS) {
            total += getPower(entityId, skill);
        }

        return total;
    }
}
