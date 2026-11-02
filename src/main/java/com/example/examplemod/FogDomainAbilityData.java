
package com.example.examplemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FogDomainAbilityData extends SavedData {

    private static final String DATA_NAME = "fog_domain_abilities";

    public static final String SKILL_ENERGY = "energy";
    public static final String SKILL_CAGE = "cage";
    public static final String SKILL_TAME = "tame";
    public static final String SKILL_DEVOUR = "devour";

    // Giữ nguyên các nhóm sức mạnh cũ để tương thích dữ liệu.
    public static final List<String> SKILLS = List.of(
            SKILL_ENERGY,
            SKILL_CAGE,
            SKILL_TAME,
            SKILL_DEVOUR
    );

    public static final double MAX_ENERGY = 100.0D;

    private static final Codec<Map<String, List<String>>> ABILITIES_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf());

    private static final Codec<Map<String, Map<String, Double>>> POWERS_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
            );

    private static final Codec<Map<String, List<String>>> CREATURE_SKILLS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf());

    // Lưu nguồn gốc kỹ năng:
    // UUID người chơi -> mã kỹ năng -> danh sách loại sinh vật cung cấp.
    private static final Codec<Map<String, Map<String, List<String>>>>
            CREATURE_SKILL_SOURCES_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.unboundedMap(
                            Codec.STRING,
                            Codec.STRING.listOf()
                    )
            );

    private static final Codec<Map<String, Double>> ENERGY_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE);

    private static final Codec<Map<String, Map<String, Long>>> COOLDOWNS_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.unboundedMap(Codec.STRING, Codec.LONG)
            );

    private static final Map<String, List<String>> EMPTY_SKILL_MAP =
            Map.of();

    private static final Map<String, Map<String, List<String>>>
            EMPTY_SKILL_SOURCE_MAP = Map.of();

    private static final Map<String, Double> EMPTY_ENERGY_MAP =
            Map.of();

    private static final Map<String, Map<String, Long>> EMPTY_COOLDOWN_MAP =
            Map.of();

    public static final SavedDataType<FogDomainAbilityData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath("examplemod", DATA_NAME),
                    FogDomainAbilityData::new,
                    RecordCodecBuilder.create(instance ->
                            instance.group(
                                    ABILITIES_CODEC.fieldOf("abilities")
                                            .forGetter(data -> data.abilities),

                                    POWERS_CODEC.optionalFieldOf(
                                            "powers", Map.of())
                                            .forGetter(data -> data.powers),

                                    CREATURE_SKILLS_CODEC.optionalFieldOf(
                                            "creature_skills",
                                            EMPTY_SKILL_MAP)
                                            .forGetter(data -> data.creatureSkills),

                                    CREATURE_SKILL_SOURCES_CODEC.optionalFieldOf(
                                            "creature_skill_sources",
                                            EMPTY_SKILL_SOURCE_MAP)
                                            .forGetter(data -> data.creatureSkillSources),

                                    ENERGY_CODEC.optionalFieldOf(
                                            "energy",
                                            EMPTY_ENERGY_MAP)
                                            .forGetter(data -> data.energy),

                                    COOLDOWNS_CODEC.optionalFieldOf(
                                            "cooldowns",
                                            EMPTY_COOLDOWN_MAP)
                                            .forGetter(data -> data.cooldowns)

                            ).apply(instance, FogDomainAbilityData::new)
                    )
            );

    private final Map<String, List<String>> abilities =
            new HashMap<>();

    private final Map<String, Map<String, Double>> powers =
            new HashMap<>();

    private final Map<String, List<String>> creatureSkills =
            new HashMap<>();

    private final Map<String, Map<String, List<String>>>
            creatureSkillSources = new HashMap<>();

    private final Map<String, Double> energy =
            new HashMap<>();

    private final Map<String, Map<String, Long>> cooldowns =
            new HashMap<>();

    public FogDomainAbilityData() {
    }

    private FogDomainAbilityData(
            Map<String, List<String>> loadedAbilities,
            Map<String, Map<String, Double>> loadedPowers,
            Map<String, List<String>> loadedCreatureSkills,
            Map<String, Map<String, List<String>>> loadedSkillSources,
            Map<String, Double> loadedEnergy,
            Map<String, Map<String, Long>> loadedCooldowns
    ) {
        copyLists(loadedAbilities, abilities);
        copyLists(loadedCreatureSkills, creatureSkills);

        if (loadedPowers != null) {
            loadedPowers.forEach((id, map) -> {
                if (id != null && map != null) {
                    powers.put(id, new HashMap<>(map));
                }
            });
        }

        if (loadedSkillSources != null) {
            loadedSkillSources.forEach((playerId, skillMap) -> {
                if (playerId == null || skillMap == null) {
                    return;
                }

                Map<String, List<String>> copiedSkills =
                        new HashMap<>();

                skillMap.forEach((skillId, sources) -> {
                    if (skillId != null && sources != null) {
                        copiedSkills.put(
                                skillId,
                                new ArrayList<>(
                                        new LinkedHashSet<>(sources)
                                )
                        );
                    }
                });

                creatureSkillSources.put(playerId, copiedSkills);
            });
        }

        if (loadedEnergy != null) {
            energy.putAll(loadedEnergy);
        }

        if (loadedCooldowns != null) {
            loadedCooldowns.forEach((id, map) -> {
                if (id != null && map != null) {
                    cooldowns.put(id, new HashMap<>(map));
                }
            });
        }

        // Làm sạch các kỹ năng cũ bị trùng trong dữ liệu đã lưu.
        creatureSkills.replaceAll((id, list) ->
                new ArrayList<>(new LinkedHashSet<>(list))
        );
    }

    private static void copyLists(
            Map<String, List<String>> source,
            Map<String, List<String>> destination
    ) {
        if (source == null) {
            return;
        }

        source.forEach((id, list) -> {
            if (id != null && list != null) {
                destination.put(
                        id,
                        new ArrayList<>(list)
                );
            }
        });
    }

    public static FogDomainAbilityData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private String key(UUID entityId) {
        return entityId.toString();
    }

    // -------------------- SỨC MẠNH --------------------

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

    public double getPower(UUID entityId, String skill) {
        Map<String, Double> skillMap = powers.get(key(entityId));

        if (skillMap == null) {
            return 0.0D;
        }

        return skillMap.getOrDefault(skill, 0.0D);
    }

    public void addPower(UUID entityId, String skill, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        Map<String, Double> skillMap =
                powers.computeIfAbsent(
                        key(entityId),
                        ignored -> new HashMap<>()
                );

        skillMap.put(
                skill,
                skillMap.getOrDefault(skill, 0.0D) + amount
        );

        setDirty();
    }

    public void setPower(UUID entityId, String skill, double amount) {
        Map<String, Double> skillMap =
                powers.computeIfAbsent(
                        key(entityId),
                        ignored -> new HashMap<>()
                );

        skillMap.put(skill, Math.max(0.0D, amount));
        setDirty();
    }

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

    public void transferCreaturePowers(UUID ownerId, UUID creatureId) {
        initializePower(ownerId, true);
        initializePower(creatureId, false);

        for (String skill : SKILLS) {
            addPower(ownerId, skill, getPower(creatureId, skill));
        }

        setDirty();
    }

    // -------------------- NĂNG LỰC ĐÃ HẤP THỤ --------------------

    public boolean addAbility(UUID playerId, String entityTypeId) {
        if (playerId == null
                || entityTypeId == null
                || entityTypeId.isBlank()) {
            return false;
        }

        List<String> owned = abilities.computeIfAbsent(
                key(playerId),
                ignored -> new ArrayList<>()
        );

        if (owned.contains(entityTypeId)) {
            return false;
        }

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

        return owned == null
                ? List.of()
                : List.copyOf(owned);
    }

    public int getAbilityCount(UUID playerId) {
        return getAbilities(playerId).size();
    }

    // -------------------- KỸ NĂNG SINH VẬT --------------------

    /**
     * Mở khóa kỹ năng của sinh vật.
     * Không thêm kỹ năng trùng vào danh sách sử dụng.
     * Vẫn lưu lại nguồn gốc của kỹ năng.
     */
    public int unlockCreatureSkills(
            UUID playerId,
            String entityTypeId
    ) {
        if (playerId == null
                || entityTypeId == null
                || entityTypeId.isBlank()) {
            return 0;
        }

        List<String> unlocked =
                FogDomainCreatureSkills.getSkills(entityTypeId);

        if (unlocked.isEmpty()) {
            return 0;
        }

        String playerKey = key(playerId);

        List<String> owned = creatureSkills.computeIfAbsent(
                playerKey,
                ignored -> new ArrayList<>()
        );

        Map<String, List<String>> sources =
                creatureSkillSources.computeIfAbsent(
                        playerKey,
                        ignored -> new HashMap<>()
                );

        int newlyUnlocked = 0;

        for (String skillId : unlocked) {
            if (skillId == null || skillId.isBlank()) {
                continue;
            }

            // Ghi nhận sinh vật đã cung cấp kỹ năng này.
            List<String> skillSources = sources.computeIfAbsent(
                    skillId,
                    ignored -> new ArrayList<>()
            );

            if (!skillSources.contains(entityTypeId)) {
                skillSources.add(entityTypeId);
            }

            // Chỉ thêm một lần vào danh sách kỹ năng sử dụng.
            if (!owned.contains(skillId)) {
                owned.add(skillId);
                newlyUnlocked++;
            }
        }

        setDirty();
        return newlyUnlocked;
    }

    public List<String> getCreatureSkills(UUID playerId) {
        List<String> owned = creatureSkills.get(key(playerId));

        return owned == null
                ? List.of()
                : List.copyOf(owned);
    }

    public int getCreatureSkillCount(UUID playerId) {
        return getCreatureSkills(playerId).size();
    }

    public String getCreatureSkillAt(UUID playerId, int index) {
        List<String> owned = getCreatureSkills(playerId);

        if (index < 0 || index >= owned.size()) {
            return null;
        }

        return owned.get(index);
    }

    /**
     * Trả về các loại sinh vật đã cung cấp một kỹ năng.
     */
    public List<String> getCreatureSkillSources(
            UUID playerId,
            String skillId
    ) {
        Map<String, List<String>> sources =
                creatureSkillSources.get(key(playerId));

        if (sources == null) {
            return List.of();
        }

        List<String> result = sources.get(skillId);

        return result == null
                ? List.of()
                : List.copyOf(result);
    }

    // -------------------- NĂNG LƯỢNG --------------------

    public double getEnergy(UUID playerId) {
        return energy.getOrDefault(
                key(playerId),
                MAX_ENERGY
        );
    }

    public void setEnergy(UUID playerId, double amount) {
        energy.put(
                key(playerId),
                Math.max(0.0D, Math.min(MAX_ENERGY, amount))
        );

        setDirty();
    }

    public boolean consumeEnergy(UUID playerId, double amount) {
        if (amount <= 0.0D) {
            return true;
        }

        double current = getEnergy(playerId);

        if (current < amount) {
            return false;
        }

        setEnergy(playerId, current - amount);
        return true;
    }

    public void regenerateEnergy(UUID playerId, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        setEnergy(
                playerId,
                getEnergy(playerId) + amount
        );
    }

    // -------------------- HỒI CHIÊU --------------------

    public boolean isOnCooldown(
            UUID playerId,
            String skillId,
            long currentGameTime
    ) {
        return getCooldownRemaining(
                playerId,
                skillId,
                currentGameTime
        ) > 0L;
    }

    public long getCooldownRemaining(
            UUID playerId,
            String skillId,
            long currentGameTime
    ) {
        Map<String, Long> playerCooldowns =
                cooldowns.get(key(playerId));

        if (playerCooldowns == null) {
            return 0L;
        }

        long expiry = playerCooldowns.getOrDefault(
                skillId,
                0L
        );

        return Math.max(
                0L,
                expiry - currentGameTime
        );
    }

    public void startCooldown(
            UUID playerId,
            String skillId,
            long currentGameTime,
            long durationTicks
    ) {
        if (playerId == null
                || skillId == null
                || durationTicks <= 0L) {
            return;
        }

        Map<String, Long> playerCooldowns =
                cooldowns.computeIfAbsent(
                        key(playerId),
                        ignored -> new HashMap<>()
                );

        playerCooldowns.put(
                skillId,
                currentGameTime + durationTicks
        );

        setDirty();
    }

    public double getTotalPower(UUID entityId) {
        double total = 0.0D;

        for (String skill : SKILLS) {
            total += getPower(entityId, skill);
        }

        return total;
    }
}
