
package com.example.examplemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import java.util.concurrent.ThreadLocalRandom;

public final class FogDomainAbilityData extends SavedData {

    private static final String DATA_NAME = "fog_domain_abilities";

    public static final String SKILL_ENERGY = "energy";
    public static final String SKILL_CAGE = "cage";
    public static final String SKILL_TAME = "tame";

    @Deprecated
    public static final String SKILL_DEVOUR = "devour";

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

    // ==================== 10 NĂNG LỰC BẨM SINH ====================

    public static final String INNATE_SPIRIT_RETURN = "spirit_return";
    public static final String INNATE_SHADOW_STEP = "shadow_step";
    public static final String INNATE_SPIRIT_ARMOR = "spirit_armor";
    public static final String INNATE_BLOOD_RAGE = "blood_rage";
    public static final String INNATE_SPIRIT_SENSE = "spirit_sense";
    public static final String INNATE_THUNDER = "thunder";
    public static final String INNATE_ICE_HEART = "ice_heart";
    public static final String INNATE_LIFE_SOURCE = "life_source";
    public static final String INNATE_SPACE_JUMP = "space_jump";
    public static final String INNATE_SOUL_PRESSURE = "soul_pressure";

    public static final List<String> RANDOM_INNATE_ABILITIES = List.of(
            INNATE_SPIRIT_RETURN,
            INNATE_SHADOW_STEP,
            INNATE_SPIRIT_ARMOR,
            INNATE_BLOOD_RAGE,
            INNATE_SPIRIT_SENSE,
            INNATE_THUNDER,
            INNATE_ICE_HEART,
            INNATE_LIFE_SOURCE,
            INNATE_SPACE_JUMP,
            INNATE_SOUL_PRESSURE
    );

    public static final String PLAYER_TUBI = "tubi2531";

    public static final List<String> ATTRIBUTES = List.of(
            "agility",
            "stamina",
            "strength",
            "spirit",
            "charisma"
    );

    public static final int STARTING_LEVEL = 1;
    public static final int MAX_CHARACTER_LEVEL = 20;
    public static final int STARTING_ATTRIBUTE = 5;
    public static final int STARTING_FREE_POINTS = 0;

    public static final double MAX_ENERGY = 100.0D;

    // ==================== CODEC ====================

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
                    Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
            );

    private static final Codec<Map<String, String>> SELECTED_CREATURE_SKILLS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private static final Codec<Map<String, Double>> ENERGY_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE);

    private static final Codec<Map<String, Map<String, Long>>> COOLDOWNS_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.unboundedMap(Codec.STRING, Codec.LONG)
            );

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

    private static final Map<String, List<String>> EMPTY_SKILL_MAP = Map.of();
    private static final Map<String, Map<String, List<String>>> EMPTY_SKILL_SOURCE_MAP = Map.of();
    private static final Map<String, String> EMPTY_SELECTED_SKILLS_MAP = Map.of();
    private static final Map<String, Double> EMPTY_ENERGY_MAP = Map.of();
    private static final Map<String, Map<String, Long>> EMPTY_COOLDOWN_MAP = Map.of();
    private static final Map<String, Integer> EMPTY_INTEGER_MAP = Map.of();
    private static final Map<String, Map<String, Integer>> EMPTY_NESTED_INTEGER_MAP = Map.of();
    private static final Map<String, String> EMPTY_STRING_MAP = Map.of();

    public static final SavedDataType<FogDomainAbilityData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath("examplemod", DATA_NAME),
                    FogDomainAbilityData::new,
                    RecordCodecBuilder.create(instance ->
                            instance.group(
                                    ABILITIES_CODEC.fieldOf("abilities")
                                            .forGetter(data -> data.abilities),

                                    POWERS_CODEC.optionalFieldOf("powers", Map.of())
                                            .forGetter(data -> data.powers),

                                    CREATURE_SKILLS_CODEC.optionalFieldOf(
                                            "creature_skills", EMPTY_SKILL_MAP)
                                            .forGetter(data -> data.creatureSkills),

                                    CREATURE_SKILL_SOURCES_CODEC.optionalFieldOf(
                                            "creature_skill_sources", EMPTY_SKILL_SOURCE_MAP)
                                            .forGetter(data -> data.creatureSkillSources),

                                    SELECTED_CREATURE_SKILLS_CODEC.optionalFieldOf(
                                            "selected_creature_skills", EMPTY_SELECTED_SKILLS_MAP)
                                            .forGetter(data -> data.selectedCreatureSkills),

                                    ENERGY_CODEC.optionalFieldOf("energy", EMPTY_ENERGY_MAP)
                                            .forGetter(data -> data.energy),

                                    COOLDOWNS_CODEC.optionalFieldOf("cooldowns", EMPTY_COOLDOWN_MAP)
                                            .forGetter(data -> data.cooldowns),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "character_levels", EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.characterLevels),

                                    NESTED_INTEGER_MAP_CODEC.optionalFieldOf(
                                            "attributes", EMPTY_NESTED_INTEGER_MAP)
                                            .forGetter(data -> data.attributes),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "free_stat_points", EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.freeStatPoints),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "spirit_points", EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.spiritPoints),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "rule_fragments", EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.ruleFragments),

                                    INTEGER_MAP_CODEC.optionalFieldOf(
                                            "soul_crystals", EMPTY_INTEGER_MAP)
                                            .forGetter(data -> data.soulCrystals),

                                    NESTED_INTEGER_MAP_CODEC.optionalFieldOf(
                                            "skill_levels", EMPTY_NESTED_INTEGER_MAP)
                                            .forGetter(data -> data.skillLevels),

                                    STRING_MAP_CODEC.optionalFieldOf(
                                            "innate_abilities", EMPTY_STRING_MAP)
                                            .forGetter(data -> data.innateAbilities)

                            ).apply(instance, FogDomainAbilityData::new)
                    )
            );

    // ==================== DỮ LIỆU CŨ ====================

    private final Map<String, List<String>> abilities = new HashMap<>();
    private final Map<String, Map<String, Double>> powers = new HashMap<>();
    private final Map<String, List<String>> creatureSkills = new HashMap<>();

    private final Map<String, Map<String, List<String>>> creatureSkillSources =
            new HashMap<>();

    private final Map<String, String> selectedCreatureSkills = new HashMap<>();
    private final Map<String, Double> energy = new HashMap<>();
    private final Map<String, Map<String, Long>> cooldowns = new HashMap<>();

    // ==================== DỮ LIỆU NHÂN VẬT ====================

    private final Map<String, Integer> characterLevels = new HashMap<>();
    private final Map<String, Map<String, Integer>> attributes = new HashMap<>();
    private final Map<String, Integer> freeStatPoints = new HashMap<>();
    private final Map<String, Integer> spiritPoints = new HashMap<>();
    private final Map<String, Integer> ruleFragments = new HashMap<>();
    private final Map<String, Integer> soulCrystals = new HashMap<>();
    private final Map<String, Map<String, Integer>> skillLevels = new HashMap<>();
    private final Map<String, String> innateAbilities = new HashMap<>();

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
                if (playerId == null || skillMap == null) return;

                Map<String, List<String>> copied = new HashMap<>();

                skillMap.forEach((skillId, sources) -> {
                    if (skillId != null && sources != null) {
                        copied.put(
                                skillId,
                                new ArrayList<>(new LinkedHashSet<>(sources))
                        );
                    }
                });

                creatureSkillSources.put(playerId, copied);
            });
        }

        if (loadedSelectedSkills != null) {
            loadedSelectedSkills.forEach((playerId, skillId) -> {
                if (playerId != null && skillId != null && !skillId.isBlank()) {
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

        // Chuyển dữ liệu intelligence cũ sang stamina.
        boolean migrated = false;

        for (Map<String, Integer> playerAttributes : attributes.values()) {
            if (playerAttributes.containsKey("intelligence")) {
                if (!playerAttributes.containsKey("stamina")) {
                    playerAttributes.put(
                            "stamina",
                            playerAttributes.get("intelligence")
                    );
                }

                playerAttributes.remove("intelligence");
                migrated = true;
            }
        }

        creatureSkills.replaceAll((id, list) ->
                new ArrayList<>(new LinkedHashSet<>(list))
        );

        if (migrated) {
            setDirty();
        }
    }

    private static void copyLists(
            Map<String, List<String>> source,
            Map<String, List<String>> destination
    ) {
        if (source == null) return;

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
        if (source == null) return;

        source.forEach((id, value) -> {
            if (id != null && value != null) {
                destination.put(id, value);
            }
        });
    }

    private static void copyNestedIntegerMap(
            Map<String, Map<String, Integer>> source,
            Map<String, Map<String, Integer>> destination
    ) {
        if (source == null) return;

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

    // ==================== KHỞI TẠO NHÂN VẬT ====================

    public void initializeCharacter(UUID playerId) {
        if (playerId == null) return;

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

        if (playerAttributes.containsKey("intelligence")) {
            playerAttributes.putIfAbsent(
                    "stamina",
                    playerAttributes.get("intelligence")
            );
            playerAttributes.remove("intelligence");
            changed = true;
        }

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

    // ==================== CẤP ĐỘ NHÂN VẬT ====================

    public int getCharacterLevel(UUID playerId) {
        if (playerId == null) return STARTING_LEVEL;

        initializeCharacter(playerId);

        return characterLevels.getOrDefault(
                key(playerId),
                STARTING_LEVEL
        );
    }

    public void setCharacterLevel(UUID playerId, int level) {
        if (playerId == null) return;

        characterLevels.put(
                key(playerId),
                Math.max(1, Math.min(MAX_CHARACTER_LEVEL, level))
        );

        setDirty();
    }

    // ==================== TĂNG CẤP NHÂN VẬT ====================

    public int getLevelUpSpiritPointCost(UUID playerId) {
        int level = getCharacterLevel(playerId);

        if (level >= MAX_CHARACTER_LEVEL) return 0;

        return level * 100;
    }

    public int getLevelUpRuleFragmentCost(UUID playerId) {
        int level = getCharacterLevel(playerId);

        if (level >= MAX_CHARACTER_LEVEL) return 0;

        return level;
    }

    public boolean levelUp(UUID playerId) {
        if (playerId == null) return false;

        initializeCharacter(playerId);

        int currentLevel = getCharacterLevel(playerId);

        if (currentLevel >= MAX_CHARACTER_LEVEL) {
            return false;
        }

        int spiritCost = getLevelUpSpiritPointCost(playerId);
        int fragmentCost = getLevelUpRuleFragmentCost(playerId);

        if (getSpiritPoints(playerId) < spiritCost
                || getRuleFragments(playerId) < fragmentCost) {
            return false;
        }

        if (!consumeSpiritPoints(playerId, spiritCost)) {
            return false;
        }

        if (!consumeRuleFragments(playerId, fragmentCost)) {
            addSpiritPoints(playerId, spiritCost);
            return false;
        }

        setCharacterLevel(playerId, currentLevel + 1);

        return true;
    }

    // ==================== THUỘC TÍNH ====================

    public int getAttribute(UUID playerId, String attribute) {
        if (playerId == null || !ATTRIBUTES.contains(attribute)) return 0;

        initializeCharacter(playerId);

        return attributes.get(key(playerId))
                .getOrDefault(attribute, STARTING_ATTRIBUTE);
    }

    public Map<String, Integer> getAttributes(UUID playerId) {
        if (playerId == null) return Map.of();

        initializeCharacter(playerId);
        return Map.copyOf(attributes.get(key(playerId)));
    }

    // ==================== MÁU TỐI ĐA TỪ THỂ LỰC ====================

    public double getStaminaMaxHealth(UUID playerId) {
        if (playerId == null) return 20.0D;

        int stamina = getAttribute(playerId, "stamina");

        return Math.max(1.0D, 20.0D + (stamina - 5) * 5.0D);
    }

    public void applyStaminaMaxHealth(LivingEntity entity) {
        if (entity == null) return;

        double maxHealth = getStaminaMaxHealth(entity.getUUID());

        AttributeInstance attribute =
                entity.getAttribute(Attributes.MAX_HEALTH);

        if (attribute == null) return;

        if (Math.abs(attribute.getBaseValue() - maxHealth) > 0.001D) {
            attribute.setBaseValue(maxHealth);
        }
    }

    // ==================== KIỂM TRA THUỘC TÍNH YẾU ====================

    public int getWeakAttributeThreshold(UUID playerId) {
        int level = getCharacterLevel(playerId);

        if (level == 1) return 5;
        if (level == 2) return 10;

        return -1;
    }

    public boolean isAttributeWeak(UUID playerId, String attribute) {
        if (playerId == null || !ATTRIBUTES.contains(attribute)) {
            return false;
        }

        int threshold = getWeakAttributeThreshold(playerId);

        if (threshold < 0) return false;

        return getAttribute(playerId, attribute) < threshold;
    }

    public int getAttributePointCost(UUID playerId) {
        return getCharacterLevel(playerId) * 10;
    }

    public boolean increaseAttribute(
            LivingEntity player,
            String attribute,
            int amount
    ) {
        if (player == null
                || !player.isAlive()
                || !ATTRIBUTES.contains(attribute)
                || amount <= 0) {
            return false;
        }

        UUID playerId = player.getUUID();
        initializeCharacter(playerId);

        String id = key(playerId);

        int currentAttribute = getAttribute(playerId, attribute);
        int newAttribute = currentAttribute + amount;

        if (newAttribute < currentAttribute) return false;

        int costPerPoint = getAttributePointCost(playerId);
        long totalCost = (long) costPerPoint * amount;

        if (totalCost > Integer.MAX_VALUE) return false;

        int availablePoints = getSpiritPoints(playerId);

        if (availablePoints < totalCost) return false;

        int healthCost = Math.max(
                0,
                newAttribute - Math.max(currentAttribute, 10)
        );

        float currentHealth = player.getHealth();

        if (currentHealth <= healthCost) return false;

        spiritPoints.put(id, availablePoints - (int) totalCost);
        attributes.get(id).put(attribute, newAttribute);

        if (healthCost > 0) {
            player.setHealth(currentHealth - healthCost);
        }

        if ("stamina".equals(attribute)) {
            applyStaminaMaxHealth(player);
        }

        setDirty();
        return true;
    }

    @Deprecated
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

        int current = getAttribute(playerId, attribute);
        int newValue = current + amount;

        int healthCost = Math.max(
                0,
                newValue - Math.max(current, 10)
        );

        if (healthCost > 0) return false;

        int costPerPoint = getAttributePointCost(playerId);
        long totalCost = (long) costPerPoint * amount;

        if (totalCost > Integer.MAX_VALUE
                || getSpiritPoints(playerId) < totalCost) {
            return false;
        }

        String id = key(playerId);

        spiritPoints.put(
                id,
                getSpiritPoints(playerId) - (int) totalCost
        );

        attributes.get(id).put(attribute, newValue);

        setDirty();
        return true;
    }

    public int getFreeStatPoints(UUID playerId) {
        if (playerId == null) return 0;

        initializeCharacter(playerId);
        return freeStatPoints.getOrDefault(key(playerId), 0);
    }

    public void addFreeStatPoints(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) return;

        initializeCharacter(playerId);

        String id = key(playerId);

        freeStatPoints.put(
                id,
                freeStatPoints.getOrDefault(id, 0) + amount
        );

        setDirty();
    }

    // ==================== TÀI NGUYÊN ====================

    public int getSpiritPoints(UUID playerId) {
        if (playerId == null) return 0;

        initializeCharacter(playerId);
        return spiritPoints.getOrDefault(key(playerId), 0);
    }

    public void addSpiritPoints(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) return;

        initializeCharacter(playerId);

        String id = key(playerId);

        spiritPoints.put(
                id,
                getSpiritPoints(playerId) + amount
        );

        setDirty();
    }

    public boolean consumeSpiritPoints(UUID playerId, int amount) {
        if (playerId == null || amount < 0) return false;

        initializeCharacter(playerId);

        int current = getSpiritPoints(playerId);
        if (current < amount) return false;

        spiritPoints.put(key(playerId), current - amount);
        setDirty();
        return true;
    }

    public int getRuleFragments(UUID playerId) {
        if (playerId == null) return 0;

        initializeCharacter(playerId);
        return ruleFragments.getOrDefault(key(playerId), 0);
    }

    public void addRuleFragments(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) return;

        initializeCharacter(playerId);

        String id = key(playerId);
        ruleFragments.put(id, getRuleFragments(playerId) + amount);
        setDirty();
    }

    public boolean consumeRuleFragments(UUID playerId, int amount) {
        if (playerId == null || amount < 0) return false;

        initializeCharacter(playerId);

        int current = getRuleFragments(playerId);
        if (current < amount) return false;

        ruleFragments.put(key(playerId), current - amount);
        setDirty();
        return true;
    }

    public int getSoulCrystals(UUID playerId) {
        if (playerId == null) return 0;

        initializeCharacter(playerId);
        return soulCrystals.getOrDefault(key(playerId), 0);
    }

    public void addSoulCrystals(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) return;

        initializeCharacter(playerId);

        String id = key(playerId);
        soulCrystals.put(id, getSoulCrystals(playerId) + amount);
        setDirty();
    }

    public boolean consumeSoulCrystals(UUID playerId, int amount) {
        if (playerId == null || amount < 0) return false;

        initializeCharacter(playerId);

        int current = getSoulCrystals(playerId);
        if (current < amount) return false;

        soulCrystals.put(key(playerId), current - amount);
        setDirty();
        return true;
    }

    // ==================== NĂNG LỰC BẨM SINH ====================

    public void initializeInnateAbilities(
            UUID playerId,
            String playerName
    ) {
        if (playerId == null) return;

        String id = key(playerId);

        boolean isTubi = playerName != null
                && PLAYER_TUBI.equalsIgnoreCase(playerName);

        if (isTubi) {
            String specialAbilities = SKILL_STEAL + "," + SKILL_COMBINE;

            if (!specialAbilities.equals(innateAbilities.get(id))) {
                innateAbilities.put(id, specialAbilities);
                setDirty();
            }

            Map<String, Integer> levels =
                    skillLevels.computeIfAbsent(
                            id,
                            ignored -> new HashMap<>()
                    );

            boolean changed = false;

            if (!levels.containsKey(SKILL_STEAL)) {
                levels.put(SKILL_STEAL, 1);
                changed = true;
            }

            if (!levels.containsKey(SKILL_COMBINE)) {
                levels.put(SKILL_COMBINE, 1);
                changed = true;
            }

            if (changed) setDirty();
            return;
        }

        if (innateAbilities.containsKey(id)) return;

        int index = ThreadLocalRandom.current().nextInt(
                RANDOM_INNATE_ABILITIES.size()
        );

        String ability = RANDOM_INNATE_ABILITIES.get(index);

        innateAbilities.put(id, ability);

        skillLevels.computeIfAbsent(
                id,
                ignored -> new HashMap<>()
        ).putIfAbsent(ability, 1);

        setDirty();
    }

    public List<String> getInnateAbilities(UUID playerId) {
        if (playerId == null) return List.of();

        String stored = innateAbilities.get(key(playerId));

        if (stored == null || stored.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();

        for (String ability : stored.split(",")) {
            String cleaned = ability.trim();

            if (!cleaned.isEmpty() && !result.contains(cleaned)) {
                result.add(cleaned);
            }
        }

        return List.copyOf(result);
    }

    public String getInnateAbility(UUID playerId) {
        List<String> owned = getInnateAbilities(playerId);

        return owned.isEmpty() ? null : owned.get(0);
    }

    public boolean hasInnateAbility(
            UUID playerId,
            String abilityId
    ) {
        if (playerId == null || abilityId == null) return false;

        return getInnateAbilities(playerId).contains(abilityId);
    }

    public boolean hasInnateAbility(UUID playerId) {
        return playerId != null
                && !getInnateAbilities(playerId).isEmpty();
    }

    public boolean setInnateAbility(
            UUID playerId,
            String abilityId
    ) {
        if (playerId == null
                || abilityId == null
                || abilityId.isBlank()) {
            return false;
        }

        String id = key(playerId);

        if (innateAbilities.containsKey(id)) return false;

        innateAbilities.put(id, abilityId);

        skillLevels.computeIfAbsent(
                id,
                ignored -> new HashMap<>()
        ).putIfAbsent(abilityId, 1);

        setDirty();
        return true;
    }

    public int getInnateAbilityLevel(
            UUID playerId,
            String abilityId
    ) {
        if (!hasInnateAbility(playerId, abilityId)) return 0;

        return getSkillLevel(playerId, abilityId);
    }

    public boolean upgradeInnateAbility(
            UUID playerId,
            String abilityId,
            int soulCrystalCost
    ) {
        if (!hasInnateAbility(playerId, abilityId)) {
            return false;
        }

        return upgradeSkill(playerId, abilityId, soulCrystalCost);
    }

    // ==================== CẤP ĐỘ KỸ NĂNG ====================

    public int getSkillLevel(UUID playerId, String skillId) {
        if (playerId == null || skillId == null || skillId.isBlank()) {
            return 1;
        }

        initializeCharacter(playerId);

        return skillLevels.get(key(playerId)).getOrDefault(skillId, 1);
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

    /**
     * Nâng cấp kỹ năng:
     * Điểm Linh Tính = cấp kỹ năng hiện tại x 100.
     * Tinh Thạch Linh Hồn = cấp kỹ năng hiện tại.
     *
     * Ví dụ cấp 1 lên cấp 2:
     * 100 Điểm Linh Tính và 1 Tinh Thạch Linh Hồn.
     */
    public boolean upgradeSkill(
            UUID playerId,
            String skillId,
            int soulCrystalCost
    ) {
        if (playerId == null
                || skillId == null
                || skillId.isBlank()) {
            return false;
        }

        initializeCharacter(playerId);

        int currentSkillLevel = Math.max(
                1,
                getSkillLevel(playerId, skillId)
        );

        int requiredSoulCrystals = currentSkillLevel;
        int requiredSpiritPoints = currentSkillLevel * 100;

        if (getSoulCrystals(playerId) < requiredSoulCrystals
                || getSpiritPoints(playerId) < requiredSpiritPoints) {
            return false;
        }

        if (!consumeSoulCrystals(playerId, requiredSoulCrystals)) {
            return false;
        }

        if (!consumeSpiritPoints(playerId, requiredSpiritPoints)) {
            addSoulCrystals(playerId, requiredSoulCrystals);
            return false;
        }

        return setSkillLevel(
                playerId,
                skillId,
                currentSkillLevel + 1
        );
    }

    // ==================== SỨC MẠNH KỸ NĂNG CŨ ====================

    public void initializePower(UUID entityId, boolean isController) {
        if (entityId == null) return;

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

        if (changed) setDirty();
    }

    public double getPower(UUID entityId, String skill) {
        if (entityId == null || skill == null) return 0.0D;

        Map<String, Double> skillMap = powers.get(key(entityId));

        return skillMap == null ? 0.0D : skillMap.getOrDefault(skill, 0.0D);
    }

    public void addPower(UUID entityId, String skill, double amount) {
        if (entityId == null || skill == null || amount <= 0.0D) return;

        Map<String, Double> skillMap =
                powers.computeIfAbsent(key(entityId), ignored -> new HashMap<>());

        skillMap.put(
                skill,
                skillMap.getOrDefault(skill, 0.0D) + amount
        );

        setDirty();
    }

    public void setPower(UUID entityId, String skill, double amount) {
        if (entityId == null || skill == null) return;

        Map<String, Double> skillMap =
                powers.computeIfAbsent(key(entityId), ignored -> new HashMap<>());

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

    // ==================== NĂNG LỰC ĐÃ HẤP THỤ ====================

    public boolean addAbility(UUID playerId, String entityTypeId) {
        if (playerId == null || entityTypeId == null || entityTypeId.isBlank()) {
            return false;
        }

        List<String> owned = abilities.computeIfAbsent(
                key(playerId),
                ignored -> new ArrayList<>()
        );

        if (owned.contains(entityTypeId)) return false;

        owned.add(entityTypeId);
        setDirty();
        return true;
    }

    public boolean hasAbility(UUID playerId, String entityTypeId) {
        if (playerId == null || entityTypeId == null) return false;

        List<String> owned = abilities.get(key(playerId));
        return owned != null && owned.contains(entityTypeId);
    }

    public List<String> getAbilities(UUID playerId) {
        if (playerId == null) return List.of();

        List<String> owned = abilities.get(key(playerId));
        return owned == null ? List.of() : List.copyOf(owned);
    }

    public int getAbilityCount(UUID playerId) {
        return getAbilities(playerId).size();
    }

    // ==================== KỸ NĂNG SINH VẬT ====================

    public int unlockCreatureSkills(UUID playerId, String entityTypeId) {
        if (playerId == null || entityTypeId == null || entityTypeId.isBlank()) {
            return 0;
        }

        List<String> unlocked = FogDomainCreatureSkills.getSkills(entityTypeId);
        if (unlocked.isEmpty()) return 0;

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
            if (skillId == null || skillId.isBlank()) continue;

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

        if (!owned.isEmpty() && !selectedCreatureSkills.containsKey(playerKey)) {
            selectedCreatureSkills.put(playerKey, owned.get(0));
        }

        setDirty();
        return newlyUnlocked;
    }

    public List<String> getCreatureSkills(UUID playerId) {
        if (playerId == null) return List.of();

        List<String> owned = creatureSkills.get(key(playerId));
        return owned == null ? List.of() : List.copyOf(owned);
    }

    public int getCreatureSkillCount(UUID playerId) {
        return getCreatureSkills(playerId).size();
    }

    public String getCreatureSkillAt(UUID playerId, int index) {
        List<String> owned = getCreatureSkills(playerId);

        if (index < 0 || index >= owned.size()) return null;
        return owned.get(index);
    }

    public List<String> getCreatureSkillSources(UUID playerId, String skillId) {
        if (playerId == null || skillId == null) return List.of();

        Map<String, List<String>> sources =
                creatureSkillSources.get(key(playerId));

        if (sources == null) return List.of();

        List<String> result = sources.get(skillId);
        return result == null ? List.of() : List.copyOf(result);
    }

    // ==================== KỸ NĂNG ĐANG ĐƯỢC CHỌN ====================

    public String getSelectedCreatureSkill(UUID playerId) {
        if (playerId == null) return null;

        String playerKey = key(playerId);
        String selected = selectedCreatureSkills.get(playerKey);

        List<String> owned = creatureSkills.get(playerKey);

        if (owned == null || owned.isEmpty()) return null;

        if (selected != null && owned.contains(selected)) return selected;

        selected = owned.get(0);
        selectedCreatureSkills.put(playerKey, selected);
        setDirty();

        return selected;
    }

    public boolean setSelectedCreatureSkill(UUID playerId, String skillId) {
        if (playerId == null || skillId == null || skillId.isBlank()) {
            return false;
        }

        if (!getCreatureSkills(playerId).contains(skillId)) return false;

        selectedCreatureSkills.put(key(playerId), skillId);
        setDirty();
        return true;
    }

    public String cycleCreatureSkill(UUID playerId) {
        if (playerId == null) return null;

        List<String> owned = getCreatureSkills(playerId);
        if (owned.isEmpty()) return null;

        String current = getSelectedCreatureSkill(playerId);
        int currentIndex = owned.indexOf(current);

        String nextSkill = owned.get((currentIndex + 1) % owned.size());
        setSelectedCreatureSkill(playerId, nextSkill);

        return nextSkill;
    }

    // ==================== NĂNG LƯỢNG ====================

    /**
     * Tinh thần 5: năng lượng tối đa 100.
     * Mỗi điểm Tinh thần trên 5 tăng 2% năng lượng tối đa.
     */
    public double getMaxEnergy(UUID playerId) {
        if (playerId == null) return MAX_ENERGY;

        int spirit = getAttribute(playerId, "spirit");

        double bonus = Math.max(0, spirit - 5) * 0.02D;

        return MAX_ENERGY * (1.0D + bonus);
    }

    public double getEnergy(UUID playerId) {
        if (playerId == null) return MAX_ENERGY;

        return energy.getOrDefault(
                key(playerId),
                getMaxEnergy(playerId)
        );
    }

    public void setEnergy(UUID playerId, double amount) {
        if (playerId == null) return;

        double maxEnergy = getMaxEnergy(playerId);

        energy.put(
                key(playerId),
                Math.max(0.0D, Math.min(maxEnergy, amount))
        );

        setDirty();
    }

    public boolean consumeEnergy(UUID playerId, double amount) {
        if (playerId == null) return false;

        if (amount <= 0.0D) return true;

        double current = getEnergy(playerId);

        if (current < amount) return false;

        setEnergy(playerId, current - amount);

        return true;
    }

    public void regenerateEnergy(UUID playerId, double amount) {
        if (playerId == null || amount <= 0.0D) return;

        setEnergy(
                playerId,
                getEnergy(playerId) + amount
        );
    }

    // ==================== HỒI CHIÊU ====================

    public boolean isOnCooldown(
            UUID playerId,
            String skillId,
            long currentGameTime
    ) {
        return getCooldownRemaining(playerId, skillId, currentGameTime) > 0L;
    }

    public long getCooldownRemaining(
            UUID playerId,
            String skillId,
            long currentGameTime
    ) {
        if (playerId == null || skillId == null) return 0L;

        Map<String, Long> playerCooldowns = cooldowns.get(key(playerId));
        if (playerCooldowns == null) return 0L;

        long expiry = playerCooldowns.getOrDefault(skillId, 0L);

        return Math.max(0L, expiry - currentGameTime);
    }

    public void startCooldown(
            UUID playerId,
            String skillId,
            long currentGameTime,
            long durationTicks
    ) {
        if (playerId == null || skillId == null || durationTicks <= 0L) {
            return;
        }

        Map<String, Long> playerCooldowns =
                cooldowns.computeIfAbsent(key(playerId), ignored -> new HashMap<>());

        playerCooldowns.put(skillId, currentGameTime + durationTicks);
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
