
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
import java.util.UUID;

public final class FogDomainAbilityData extends SavedData {

    private static final String DATA_NAME = "fog_domain_abilities";

    // Kỹ năng cũ còn được giữ lại để tương thích dữ liệu.
    public static final String SKILL_ENERGY = "energy";
    public static final String SKILL_CAGE = "cage";
    public static final String SKILL_TAME = "tame";

    // Giữ tên hằng số để tránh lỗi ở các file cũ đang tham chiếu.
    // Không còn nằm trong danh sách kỹ năng hoạt động.
    @Deprecated
    public static final String SKILL_DEVOUR = "devour";

    // Kỹ năng mới.
    public static final String SKILL_COMBINE = "combine";
    public static final String SKILL_STEAL = "steal";
    public static final String SKILL_TRIPLE_SPIRIT = "triple_spirit_guard";

    public static final List<String> SKILLS = List.of(
            SKILL_ENERGY,
            SKILL_CAGE,
            SKILL_TAME,
            SKILL_COMBINE,
            SKILL_STEAL,
            SKILL_TRIPLE_SPIRIT
    );

    public static final List<String> ATTRIBUTES = List.of(
            "agility",
            "strength",
            "spirit",
            "intelligence",
            "charisma"
    );

    public static final int STARTING_LEVEL = 1;
    public static final int STARTING_ATTRIBUTE = 5;
    public static final int STARTING_FREE_POINTS = 0;

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

    private static final Codec<Map<String, Map<String, List<String>>>>
            CREATURE_SKILL_SOURCES_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.unboundedMap(
                            Codec.STRING,
                            Codec.STRING.listOf()
                    )
            );

    private static final Codec<Map<String, String>>
            SELECTED_CREATURE_SKILLS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private static final Codec<Map<String, Double>> ENERGY_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE);

    private static final Codec<Map<String, Map<String, Long>>> COOLDOWNS_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.unboundedMap(Codec.STRING, Codec.LONG)
            );

    // Dữ liệu nhân vật mới.
    private static final Codec<Map<String, Integer>> INTEGER_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT);

    private static final Codec<Map<String, Map<String, Integer>>>
            NESTED_INTEGER_MAP_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
            );

    private static final Codec<Map<String, String>> STRING_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private static final Map<String, List<String>> EMPTY_SKILL_MAP =
            Map.of();

    private static final Map<String, Map<String, List<String>>>
            EMPTY_SKILL_SOURCE_MAP = Map.of();

    private static final Map<String, String> EMPTY_SELECTED_SKILLS_MAP =
            Map.of();

    private static final Map<String, Double> EMPTY_ENERGY_MAP =
            Map.of();

    private static final Map<String, Map<String, Long>> EMPTY_COOLDOWN_MAP =
            Map.of();

    private static final Map<String, Integer> EMPTY_INTEGER_MAP =
            Map.of();

    private static final Map<String, Map<String, Integer>>
            EMPTY_NESTED_INTEGER_MAP = Map.of();

    private static final Map<String, String> EMPTY_STRING_MAP =
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

                                    SELECTED_CREATURE_SKILLS_CODEC.optionalFieldOf(
                                            "selected_creature_skills",
                                            EMPTY_SELECTED_SKILLS_MAP)
                                            .forGetter(data -> data.selectedCreatureSkills),

                                    ENERGY_CODEC.optionalFieldOf(
                                            "energy",
                                            EMPTY_ENERGY_MAP)
                                            .forGetter(data -> data.energy),

                                    COOLDOWNS_CODEC.optionalFieldOf(
                                            "cooldowns",
                                            EMPTY_COOLDOWN_MAP)
                                            .forGetter(data -> data.cooldowns),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "character_levels",
                                            EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.characterLevels),

                                    NESTED_INTEGER_MAP_CODEC.optionalFieldOf(
                                            "attributes",
                                            EMPTY_NESTED_INTEGER_MAP)
                                            .forGetter(data -> data.attributes),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "free_stat_points",
                                            EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.freeStatPoints),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "spirit_points",
                                            EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.spiritPoints),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "rule_fragments",
                                            EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.ruleFragments),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "soul_crystals",
                                            EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.soulCrystals),

                                    NESTED_INTEGER_MAP_CODEC.optionalFieldOf(
                                            "skill_levels",
                                            EMPTY_NESTED_INTEGER_MAP)
                                            .forGetter(data -> data.skillLevels),

                                    STRING_MAP_CODEC.optionalFieldOf(
                                            "innate_abilities",
                                            EMPTY_STRING_MAP)
                                            .forGetter(data -> data.innateAbilities)

                            ).apply(instance, FogDomainAbilityData::new)
                    )
            );

    // -------------------- DỮ LIỆU CŨ --------------------

    private final Map<String, List<String>> abilities =
            new HashMap<>();

    private final Map<String, Map<String, Double>> powers =
            new HashMap<>();

    private final Map<String, List<String>> creatureSkills =
            new HashMap<>();

    private final Map<String, Map<String, List<String>>>
            creatureSkillSources = new HashMap<>();

    private final Map<String, String> selectedCreatureSkills =
            new HashMap<>();

    private final Map<String, Double> energy =
            new HashMap<>();

    private final Map<String, Map<String, Long>> cooldowns =
            new HashMap<>();

    // -------------------- DỮ LIỆU NHÂN VẬT MỚI --------------------

    private final Map<String, Integer> characterLevels =
            new HashMap<>();

    private final Map<String, Map<String, Integer>> attributes =
            new HashMap<>();

    private final Map<String, Integer> freeStatPoints =
            new HashMap<>();

    private final Map<String, Integer> spiritPoints =
            new HashMap<>();

    private final Map<String, Integer> ruleFragments =
            new HashMap<>();

    private final Map<String, Integer> soulCrystals =
            new HashMap<>();

    private final Map<String, Map<String, Integer>> skillLevels =
            new HashMap<>();

    private final Map<String, String> innateAbilities =
            new HashMap<>();

    public FogDomainAbilityData() {
    }

    private FogDomainAbilityData(
            Map<String, List<String>> loadedAbilities,
            Map<String, Map<String, Double>> loadedPowers,
            Map<String, List<String>> loadedCreatureSkills,
            Map<String, Map<String, List<String>>> loadedSkillSources,
            Map<String, String> loadedSelectedSkills,
            Map<String, Double> loadedEnergy,
            Map<String, Map<String, Long>> loadedCooldowns,
            Map<String, Integer> loadedCharacterLevels,
            Map<String, Map<String, Integer>> loadedAttributes,
            Map<String, Integer> loadedFreeStatPoints,
            Map<String, Integer> loadedSpiritPoints,
            Map<String, Integer> loadedRuleFragments,
            Map<String, Integer> loadedSoulCrystals,
            Map<String, Map<String, Integer>> loadedSkillLevels,
            Map<String, String> loadedInnateAbilities
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

        if (loadedSelectedSkills != null) {
            loadedSelectedSkills.forEach((playerId, skillId) -> {
                if (playerId != null
                        && skillId != null
                        && !skillId.isBlank()) {
                    selectedCreatureSkills.put(playerId, skillId);
                }
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

        copyIntegerMap(loadedCharacterLevels, characterLevels);
        copyNestedIntegerMap(loadedAttributes, attributes);
        copyIntegerMap(loadedFreeStatPoints, freeStatPoints);
        copyIntegerMap(loadedSpiritPoints, spiritPoints);
        copyIntegerMap(loadedRuleFragments, ruleFragments);
        copyIntegerMap(loadedSoulCrystals, soulCrystals);
        copyNestedIntegerMap(loadedSkillLevels, skillLevels);

        if (loadedInnateAbilities != null) {
            loadedInnateAbilities.forEach((id, ability) -> {
                if (id != null && ability != null && !ability.isBlank()) {
                    innateAbilities.put(id, ability);
                }
            });
        }

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
                        new ArrayList<>(new LinkedHashSet<>(list))
                );
            }
        });
    }

    private static void copyIntegerMap(
            Map<String, Integer> source,
            Map<String, Integer> destination
    ) {
        if (source != null) {
            source.forEach((id, value) -> {
                if (id != null && value != null) {
                    destination.put(id, value);
                }
            });
        }
    }

    private static void copyNestedIntegerMap(
            Map<String, Map<String, Integer>> source,
            Map<String, Map<String, Integer>> destination
    ) {
        if (source == null) {
            return;
        }

        source.forEach((id, map) -> {
            if (id != null && map != null) {
                destination.put(id, new HashMap<>(map));
            }
        });
    }

    public static FogDomainAbilityData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private String key(UUID entityId) {
        return entityId.toString();
    }

    // ============================================================
    // KHỞI TẠO NHÂN VẬT
    // ============================================================

    public void initializeCharacter(UUID playerId) {
        if (playerId == null) {
            return;
        }

        String id = key(playerId);
        boolean changed = false;

        if (!characterLevels.containsKey(id)) {
            characterLevels.put(id, STARTING_LEVEL);
            changed = true;
        }

        if (!freeStatPoints.containsKey(id)) {
            freeStatPoints.put(id, STARTING_FREE_POINTS);
            changed = true;
        }

        if (!spiritPoints.containsKey(id)) {
            spiritPoints.put(id, 0);
            changed = true;
        }

        if (!ruleFragments.containsKey(id)) {
            ruleFragments.put(id, 0);
            changed = true;
        }

        if (!soulCrystals.containsKey(id)) {
            soulCrystals.put(id, 0);
            changed = true;
        }

        Map<String, Integer> playerAttributes =
                attributes.computeIfAbsent(id, ignored -> new HashMap<>());

        for (String attribute : ATTRIBUTES) {
            if (!playerAttributes.containsKey(attribute)) {
                playerAttributes.put(attribute, STARTING_ATTRIBUTE);
                changed = true;
            }
        }

        Map<String, Integer> playerSkills =
                skillLevels.computeIfAbsent(id, ignored -> new HashMap<>());

        for (String skill : SKILLS) {
            if (!playerSkills.containsKey(skill)) {
                playerSkills.put(skill, 1);
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    public int getCharacterLevel(UUID playerId) {
        initializeCharacter(playerId);
        return characterLevels.getOrDefault(key(playerId), STARTING_LEVEL);
    }

    public void setCharacterLevel(UUID playerId, int level) {
        if (playerId == null) {
            return;
        }

        characterLevels.put(key(playerId), Math.max(1, level));
        setDirty();
    }

    // ============================================================
    // THUỘC TÍNH NHÂN VẬT
    // ============================================================

    public int getAttribute(UUID playerId, String attribute) {
        if (playerId == null || !ATTRIBUTES.contains(attribute)) {
            return 0;
        }

        initializeCharacter(playerId);

        return attributes.get(key(playerId))
                .getOrDefault(attribute, STARTING_ATTRIBUTE);
    }

    public Map<String, Integer> getAttributes(UUID playerId) {
        if (playerId == null) {
            return Map.of();
        }

        initializeCharacter(playerId);

        return Map.copyOf(attributes.get(key(playerId)));
    }

    public boolean increaseAttribute(
            UUID playerId,
            String attribute,
            int amount
    ) {
        if (playerId == null
                || !ATTRIBUTES.contains(attribute)
                || amount <= 0) {
            return false;
        }

        initializeCharacter(playerId);

        String id = key(playerId);
        int available = freeStatPoints.getOrDefault(id, 0);

        if (available < amount) {
            return false;
        }

        Map<String, Integer> playerAttributes =
                attributes.get(id);

        playerAttributes.put(
                attribute,
                playerAttributes.getOrDefault(
                        attribute,
                        STARTING_ATTRIBUTE
                ) + amount
        );

        freeStatPoints.put(id, available - amount);
        setDirty();

        return true;
    }

    public int getFreeStatPoints(UUID playerId) {
        if (playerId == null) {
            return 0;
        }

        initializeCharacter(playerId);
        return freeStatPoints.getOrDefault(key(playerId), 0);
    }

    public void addFreeStatPoints(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) {
            return;
        }

        initializeCharacter(playerId);

        String id = key(playerId);

        freeStatPoints.put(
                id,
                freeStatPoints.getOrDefault(id, 0) + amount
        );

        setDirty();
    }

    // ============================================================
    // TÀI NGUYÊN NHÂN VẬT
    // ============================================================

    public int getSpiritPoints(UUID playerId) {
        return playerId == null
                ? 0
                : spiritPoints.getOrDefault(key(playerId), 0);
    }

    public void addSpiritPoints(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) {
            return;
        }

        String id = key(playerId);
        spiritPoints.put(id, getSpiritPoints(playerId) + amount);
        setDirty();
    }

    public boolean consumeSpiritPoints(UUID playerId, int amount) {
        if (playerId == null || amount < 0) {
            return false;
        }

        int current = getSpiritPoints(playerId);

        if (current < amount) {
            return false;
        }

        spiritPoints.put(key(playerId), current - amount);
        setDirty();
        return true;
    }

    public int getRuleFragments(UUID playerId) {
        return playerId == null
                ? 0
                : ruleFragments.getOrDefault(key(playerId), 0);
    }

    public void addRuleFragments(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) {
            return;
        }

        String id = key(playerId);
        ruleFragments.put(id, getRuleFragments(playerId) + amount);
        setDirty();
    }

    public boolean consumeRuleFragments(UUID playerId, int amount) {
        if (playerId == null || amount < 0) {
            return false;
        }

        int current = getRuleFragments(playerId);

        if (current < amount) {
            return false;
        }

        ruleFragments.put(key(playerId), current - amount);
        setDirty();
        return true;
    }

    public int getSoulCrystals(UUID playerId) {
        return playerId == null
                ? 0
                : soulCrystals.getOrDefault(key(playerId), 0);
    }

    public void addSoulCrystals(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) {
            return;
        }

        String id = key(playerId);
        soulCrystals.put(id, getSoulCrystals(playerId) + amount);
        setDirty();
    }

    public boolean consumeSoulCrystals(UUID playerId, int amount) {
        if (playerId == null || amount < 0) {
            return false;
        }

        int current = getSoulCrystals(playerId);

        if (current < amount) {
            return false;
        }

        soulCrystals.put(key(playerId), current - amount);
        setDirty();
        return true;
    }

    // ============================================================
    // NĂNG LỰC BẨM SINH NGẪU NHIÊN
    // ============================================================

    public String getInnateAbility(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        return innateAbilities.get(key(playerId));
    }

    public boolean setInnateAbility(UUID playerId, String abilityId) {
        if (playerId == null
                || abilityId == null
                || abilityId.isBlank()) {
            return false;
        }

        String id = key(playerId);

        // Không cho thay đổi năng lực đã nhận lần đầu.
        if (innateAbilities.containsKey(id)) {
            return false;
        }

        innateAbilities.put(id, abilityId);
        setDirty();

        return true;
    }

    public boolean hasInnateAbility(UUID playerId) {
        return playerId != null
                && innateAbilities.containsKey(key(playerId));
    }

    // ============================================================
    // CẤP ĐỘ KỸ NĂNG
    // ============================================================

    public int getSkillLevel(UUID playerId, String skillId) {
        if (playerId == null
                || skillId == null
                || skillId.isBlank()) {
            return 1;
        }

        initializeCharacter(playerId);

        return skillLevels.get(key(playerId))
                .getOrDefault(skillId, 1);
    }

    public boolean setSkillLevel(
            UUID playerId,
            String skillId,
            int level
    ) {
        if (playerId == null
                || skillId == null
                || skillId.isBlank()
                || level < 1) {
            return false;
        }

        initializeCharacter(playerId);

        skillLevels.get(key(playerId)).put(skillId, level);
        setDirty();

        return true;
    }

    public boolean upgradeSkill(
            UUID playerId,
            String skillId,
            int soulCrystalCost
    ) {
        if (playerId == null
                || skillId == null
                || skillId.isBlank()
                || soulCrystalCost <= 0) {
            return false;
        }

        initializeCharacter(playerId);

        if (!consumeSoulCrystals(playerId, soulCrystalCost)) {
            return false;
        }

        int currentLevel = getSkillLevel(playerId, skillId);

        setSkillLevel(playerId, skillId, currentLevel + 1);

        return true;
    }

    // ============================================================
    // SỨC MẠNH KỸ NĂNG CŨ
    // ============================================================

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

    // ============================================================
    // NĂNG LỰC ĐÃ HẤP THỤ
    // ============================================================

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

    // ============================================================
    // KỸ NĂNG SINH VẬT
    // ============================================================

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

            List<String> skillSources = sources.computeIfAbsent(
                    skillId,
                    ignored -> new ArrayList<>()
            );

            if (!skillSources.contains(entityTypeId)) {
                skillSources.add(entityTypeId);
            }

            if (!owned.contains(skillId)) {
                owned.add(skillId);
                newlyUnlocked++;
            }
        }

        if (!owned.isEmpty()
                && !selectedCreatureSkills.containsKey(playerKey)) {
            selectedCreatureSkills.put(playerKey, owned.get(0));
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

    // ============================================================
    // KỸ NĂNG ĐANG ĐƯỢC CHỌN
    // ============================================================

    public String getSelectedCreatureSkill(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        String playerKey = key(playerId);
        String selected = selectedCreatureSkills.get(playerKey);

        List<String> owned = creatureSkills.get(playerKey);

        if (owned == null || owned.isEmpty()) {
            return null;
        }

        if (selected != null && owned.contains(selected)) {
            return selected;
        }

        selected = owned.get(0);
        selectedCreatureSkills.put(playerKey, selected);
        setDirty();

        return selected;
    }

    public boolean setSelectedCreatureSkill(
            UUID playerId,
            String skillId
    ) {
        if (playerId == null
                || skillId == null
                || skillId.isBlank()) {
            return false;
        }

        List<String> owned = getCreatureSkills(playerId);

        if (!owned.contains(skillId)) {
            return false;
        }

        selectedCreatureSkills.put(key(playerId), skillId);
        setDirty();

        return true;
    }

    public String cycleCreatureSkill(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        List<String> owned = getCreatureSkills(playerId);

        if (owned.isEmpty()) {
            return null;
        }

        String current = getSelectedCreatureSkill(playerId);
        int currentIndex = owned.indexOf(current);

        int nextIndex = (currentIndex + 1) % owned.size();
        String nextSkill = owned.get(nextIndex);

        setSelectedCreatureSkill(playerId, nextSkill);

        return nextSkill;
    }

    // ============================================================
    // NĂNG LƯỢNG
    // ============================================================

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

    // ============================================================
    // HỒI CHIÊU
    // ============================================================

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
