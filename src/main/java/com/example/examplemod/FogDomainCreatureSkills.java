
package com.example.examplemod;

import java.util.List;
import java.util.Map;

/**
 * Danh mục kỹ năng riêng của từng loài sinh vật.
 * Không đưa kỹ năng bị động vào danh sách có thể lựa chọn.
 */
public final class FogDomainCreatureSkills {

    // Kỹ năng cũ
    public static final String CREEPER_EXPLOSION = "creeper_explosion";
    public static final String ENDERMAN_TELEPORT = "enderman_teleport";
    public static final String WARDEN_SONIC_BOOM = "warden_sonic_boom";
    public static final String WARDEN_SONAR = "warden_sonar";
    public static final String IRON_GOLEM_SMASH = "iron_golem_smash";
    public static final String WITCH_POTION = "witch_potion";
    public static final String BLAZE_FIREBALL = "blaze_fireball";
    public static final String GHAST_FIREBALL = "ghast_fireball";
    public static final String SPIDER_WEB = "spider_web";
    public static final String ENDER_DRAGON_BREATH = "ender_dragon_breath";

    // Kỹ năng mới
    public static final String WITHER_STORM = "wither_storm";
    public static final String WITHER_SKELETON_SLASH = "wither_skeleton_slash";
    public static final String SKELETON_ARROW = "skeleton_arrow";
    public static final String ZOMBIE_HORDE = "zombie_horde";
    public static final String DROWNED_TIDAL = "drowned_tidal";
    public static final String SLIME_BOUNCE = "slime_bounce";
    public static final String BEE_STING = "bee_sting";
    public static final String CAVE_SPIDER_POISON = "cave_spider_poison";
    public static final String DOLPHIN_DASH = "dolphin_dash";
    public static final String FOX_AMBUSH = "fox_ambush";
    public static final String GOAT_RAM = "goat_ram";
    public static final String LLAMA_SPIT = "llama_spit";
    public static final String PANDA_ROLL = "panda_roll";
    public static final String PIGLIN_GOLD = "piglin_gold";
    public static final String POLAR_BEAR_SMASH = "polar_bear_smash";
    public static final String PUFFERFISH_POISON = "pufferfish_poison";
    public static final String WOLF_BITE = "wolf_bite";
    public static final String SHEEP_WOOL = "sheep_wool";
    public static final String HORSE_CHARGE = "horse_charge";
    public static final String CAT_SCRATCH = "cat_scratch";
    public static final String OCELOT_STEALTH = "ocelot_stealth";
    public static final String BOGGED_ARROW = "bogged_arrow";
    public static final String BREEZE_WIND = "breeze_wind";
    public static final String CREAKING_STRIKE = "creaking_strike";
    public static final String ELDER_GUARDIAN_LASER = "elder_guardian_laser";
    public static final String GUARDIAN_LASER = "guardian_laser";
    public static final String EVOKER_FANGS = "evoker_fangs";
    public static final String HOGLIN_CHARGE = "hoglin_charge";
    public static final String HUSK_SAND = "husk_sand";
    public static final String MAGMA_CUBE_BURN = "magma_cube_burn";
    public static final String PHANTOM_DIVE = "phantom_dive";
    public static final String PIGLIN_BRUTE_SMASH = "piglin_brute_smash";
    public static final String PILLAGER_CROSSBOW = "pillager_crossbow";
    public static final String RAVAGER_RAM = "ravager_ram";
    public static final String SHULKER_PROJECTILE = "shulker_projectile";
    public static final String SILVERFISH_SWARM = "silverfish_swarm";
    public static final String STRAY_FROST = "stray_frost";
    public static final String VEX_DASH = "vex_dash";
    public static final String VINDICATOR_AXE = "vindicator_axe";
    public static final String ZOGLIN_CHARGE = "zoglin_charge";
    public static final String ZOMBIE_VILLAGER_CALL = "zombie_villager_call";
    public static final String ALLAY_SUPPORT = "allay_support";
    public static final String ARMADILLO_ROLL = "armadillo_roll";
    public static final String AXOLOTL_REGEN = "axolotl_regen";
    public static final String BAT_SONAR = "bat_sonar";
    public static final String CAMEL_STOMP = "camel_stomp";
    public static final String CAT_POUNCE = "cat_pounce";
    public static final String CHICKEN_FLUTTER = "chicken_flutter";
    public static final String COPPER_GOLEM_SPIN = "copper_golem_spin";
    public static final String COW_RUSH = "cow_rush";
    public static final String DONKEY_KICK = "donkey_kick";
    public static final String FROG_TONGUE = "frog_tongue";
    public static final String GLOW_SQUID_BLIND = "glow_squid_blind";
    public static final String HAPPY_GHAST_WIND = "happy_ghast_wind";
    public static final String MOOSHROOM_SPORES = "mooshroom_spores";
    public static final String MULE_KICK = "mule_kick";
    public static final String PARROT_SONIC = "parrot_sonic";
    public static final String PIG_CHARGE = "pig_charge";
    public static final String RABBIT_JUMP = "rabbit_jump";
    public static final String SALMON_SPLASH = "salmon_splash";
    public static final String SNIFFER_SCENT = "sniffer_scent";
    public static final String SNOW_GOLEM_SNOW = "snow_golem_snow";
    public static final String SQUID_INK = "squid_ink";
    public static final String STRIDER_LAVA = "strider_lava";
    public static final String TURTLE_SHELL = "turtle_shell";
    public static final String VILLAGER_TRADE = "villager_trade";
    public static final String WANDERING_TRADER_POTION = "wandering_trader_potion";
    public static final String NAUTILUS_WATER = "nautilus_water";
    public static final String ZOMBIE_NAUTILUS_TIDE = "zombie_nautilus_tide";
    public static final String SULFUR_CUBE_BLAST = "sulfur_cube_blast";

    private static final Map<String, List<String>> SKILLS =
            Map.ofEntries(
                    Map.entry("minecraft:creeper", List.of(CREEPER_EXPLOSION)),
                    Map.entry("minecraft:enderman", List.of(ENDERMAN_TELEPORT)),
                    Map.entry("minecraft:warden", List.of(WARDEN_SONIC_BOOM)),
                    Map.entry("minecraft:iron_golem", List.of(IRON_GOLEM_SMASH)),
                    Map.entry("minecraft:witch", List.of(WITCH_POTION)),
                    Map.entry("minecraft:blaze", List.of(BLAZE_FIREBALL)),
                    Map.entry("minecraft:ghast", List.of(GHAST_FIREBALL)),
                    Map.entry("minecraft:spider", List.of(SPIDER_WEB)),
                    Map.entry("minecraft:ender_dragon", List.of(ENDER_DRAGON_BREATH)),

                    Map.entry("minecraft:wither", List.of(WITHER_STORM)),
                    Map.entry("minecraft:wither_skeleton", List.of(WITHER_SKELETON_SLASH)),
                    Map.entry("minecraft:skeleton", List.of(SKELETON_ARROW)),
                    Map.entry("minecraft:zombie", List.of(ZOMBIE_HORDE)),
                    Map.entry("minecraft:drowned", List.of(DROWNED_TIDAL)),
                    Map.entry("minecraft:slime", List.of(SLIME_BOUNCE)),
                    Map.entry("minecraft:bee", List.of(BEE_STING)),
                    Map.entry("minecraft:cave_spider", List.of(CAVE_SPIDER_POISON)),
                    Map.entry("minecraft:dolphin", List.of(DOLPHIN_DASH)),
                    Map.entry("minecraft:fox", List.of(FOX_AMBUSH)),
                    Map.entry("minecraft:goat", List.of(GOAT_RAM)),
                    Map.entry("minecraft:llama", List.of(LLAMA_SPIT)),
                    Map.entry("minecraft:panda", List.of(PANDA_ROLL)),
                    Map.entry("minecraft:piglin", List.of(PIGLIN_GOLD)),
                    Map.entry("minecraft:polar_bear", List.of(POLAR_BEAR_SMASH)),
                    Map.entry("minecraft:pufferfish", List.of(PUFFERFISH_POISON)),
                    Map.entry("minecraft:wolf", List.of(WOLF_BITE)),
                    Map.entry("minecraft:sheep", List.of(SHEEP_WOOL)),
                    Map.entry("minecraft:horse", List.of(HORSE_CHARGE)),
                    Map.entry("minecraft:cat", List.of(CAT_SCRATCH)),
                    Map.entry("minecraft:ocelot", List.of(OCELOT_STEALTH)),
                    Map.entry("minecraft:bogged", List.of(BOGGED_ARROW)),
                    Map.entry("minecraft:breeze", List.of(BREEZE_WIND)),
                    Map.entry("minecraft:creaking", List.of(CREAKING_STRIKE)),
                    Map.entry("minecraft:elder_guardian", List.of(ELDER_GUARDIAN_LASER)),
                    Map.entry("minecraft:guardian", List.of(GUARDIAN_LASER)),
                    Map.entry("minecraft:evoker", List.of(EVOKER_FANGS)),
                    Map.entry("minecraft:hoglin", List.of(HOGLIN_CHARGE)),
                    Map.entry("minecraft:husk", List.of(HUSK_SAND)),
                    Map.entry("minecraft:magma_cube", List.of(MAGMA_CUBE_BURN)),
                    Map.entry("minecraft:phantom", List.of(PHANTOM_DIVE)),
                    Map.entry("minecraft:piglin_brute", List.of(PIGLIN_BRUTE_SMASH)),
                    Map.entry("minecraft:pillager", List.of(PILLAGER_CROSSBOW)),
                    Map.entry("minecraft:ravager", List.of(RAVAGER_RAM)),
                    Map.entry("minecraft:shulker", List.of(SHULKER_PROJECTILE)),
                    Map.entry("minecraft:silverfish", List.of(SILVERFISH_SWARM)),
                    Map.entry("minecraft:stray", List.of(STRAY_FROST)),
                    Map.entry("minecraft:vex", List.of(VEX_DASH)),
                    Map.entry("minecraft:vindicator", List.of(VINDICATOR_AXE)),
                    Map.entry("minecraft:zoglin", List.of(ZOGLIN_CHARGE)),
                    Map.entry("minecraft:zombie_villager", List.of(ZOMBIE_VILLAGER_CALL)),

                    Map.entry("minecraft:allay", List.of(ALLAY_SUPPORT)),
                    Map.entry("minecraft:armadillo", List.of(ARMADILLO_ROLL)),
                    Map.entry("minecraft:axolotl", List.of(AXOLOTL_REGEN)),
                    Map.entry("minecraft:bat", List.of(BAT_SONAR)),
                    Map.entry("minecraft:camel", List.of(CAMEL_STOMP)),
                    Map.entry("minecraft:chicken", List.of(CHICKEN_FLUTTER)),
                    Map.entry("minecraft:cow", List.of(COW_RUSH)),
                    Map.entry("minecraft:donkey", List.of(DONKEY_KICK)),
                    Map.entry("minecraft:frog", List.of(FROG_TONGUE)),
                    Map.entry("minecraft:glow_squid", List.of(GLOW_SQUID_BLIND)),
                    Map.entry("minecraft:happy_ghast", List.of(HAPPY_GHAST_WIND)),
                    Map.entry("minecraft:mooshroom", List.of(MOOSHROOM_SPORES)),
                    Map.entry("minecraft:mule", List.of(MULE_KICK)),
                    Map.entry("minecraft:parrot", List.of(PARROT_SONIC)),
                    Map.entry("minecraft:pig", List.of(PIG_CHARGE)),
                    Map.entry("minecraft:rabbit", List.of(RABBIT_JUMP)),
                    Map.entry("minecraft:salmon", List.of(SALMON_SPLASH)),
                    Map.entry("minecraft:sniffer", List.of(SNIFFER_SCENT)),
                    Map.entry("minecraft:snow_golem", List.of(SNOW_GOLEM_SNOW)),
                    Map.entry("minecraft:squid", List.of(SQUID_INK)),
                    Map.entry("minecraft:strider", List.of(STRIDER_LAVA)),
                    Map.entry("minecraft:turtle", List.of(TURTLE_SHELL)),
                    Map.entry("minecraft:villager", List.of(VILLAGER_TRADE)),
                    Map.entry("minecraft:wandering_trader", List.of(WANDERING_TRADER_POTION)),

                    // Các sinh vật có thể có trong phiên bản/mod mới.
                    Map.entry("minecraft:nautilus", List.of(NAUTILUS_WATER)),
                    Map.entry("minecraft:zombie_nautilus", List.of(ZOMBIE_NAUTILUS_TIDE)),
                    Map.entry("minecraft:sulfur_cube", List.of(SULFUR_CUBE_BLAST))
            );

    private FogDomainCreatureSkills() {
    }

    public static List<String> getSkills(String entityTypeId) {
        if (entityTypeId == null) {
            return List.of();
        }
        return SKILLS.getOrDefault(entityTypeId, List.of());
    }

    public static boolean hasSkills(String entityTypeId) {
        return !getSkills(entityTypeId).isEmpty();
    }

    public static String getSkillName(String skillId) {
        return switch (skillId) {
            case CREEPER_EXPLOSION -> "Nổ Creeper";
            case ENDERMAN_TELEPORT -> "Dịch chuyển Enderman";
            case WARDEN_SONIC_BOOM -> "Sóng âm Warden";
            case WARDEN_SONAR -> "Cảm nhận Warden";
            case IRON_GOLEM_SMASH -> "Đấm sắt";
            case WITCH_POTION -> "Ném thuốc phù thủy";
            case BLAZE_FIREBALL -> "Cầu lửa Blaze";
            case GHAST_FIREBALL -> "Cầu lửa Ghast";
            case SPIDER_WEB -> "Phóng tơ nhện";
            case ENDER_DRAGON_BREATH -> "Hơi thở Rồng Ender";

            case WITHER_STORM -> "Bão hủy diệt";
            case WITHER_SKELETON_SLASH -> "Chém xương Wither";
            case SKELETON_ARROW -> "Bắn mũi tên";
            case ZOMBIE_HORDE -> "Gọi đồng loại";
            case DROWNED_TIDAL -> "Thủy triều";
            case SLIME_BOUNCE -> "Nhảy nảy";
            case BEE_STING -> "Đốt chích";
            case CAVE_SPIDER_POISON -> "Tơ nhện độc";
            case DOLPHIN_DASH -> "Lướt sóng";
            case FOX_AMBUSH -> "Núp lùm";
            case GOAT_RAM -> "Húc mạnh";
            case LLAMA_SPIT -> "Phun hơi";
            case PANDA_ROLL -> "Bám dính";
            case PIGLIN_GOLD -> "Ném vàng";
            case POLAR_BEAR_SMASH -> "Cú đấm băng";
            case PUFFERFISH_POISON -> "Độc tố phồng";
            case WOLF_BITE -> "Cắn xé";
            case SHEEP_WOOL -> "Đập lông";
            case HORSE_CHARGE -> "Tấn công bầy";
            case CAT_SCRATCH -> "Đá vồ";
            case OCELOT_STEALTH -> "Tàng hình";
            case BOGGED_ARROW -> "Tên độc Bogged";
            case BREEZE_WIND -> "Lốc gió Breeze";
            case CREAKING_STRIKE -> "Đòn đánh Creaking";
            case ELDER_GUARDIAN_LASER -> "Tia laser cổ đại";
            case GUARDIAN_LASER -> "Tia laser Guardian";
            case EVOKER_FANGS -> "Răng nanh Evoker";
            case HOGLIN_CHARGE -> "Húc Hoglin";
            case HUSK_SAND -> "Bão cát";
            case MAGMA_CUBE_BURN -> "Dung nham nóng";
            case PHANTOM_DIVE -> "Lao xuống";
            case PIGLIN_BRUTE_SMASH -> "Đập Piglin";
            case PILLAGER_CROSSBOW -> "Nỏ Pillager";
            case RAVAGER_RAM -> "Húc Ravager";
            case SHULKER_PROJECTILE -> "Đạn Shulker";
            case SILVERFISH_SWARM -> "Đàn cá bạc";
            case STRAY_FROST -> "Mũi tên băng";
            case VEX_DASH -> "Lao xuyên";
            case VINDICATOR_AXE -> "Rìu Vindicator";
            case ZOGLIN_CHARGE -> "Húc Zoglin";
            case ZOMBIE_VILLAGER_CALL -> "Triệu hồi dân làng xác sống";

            case ALLAY_SUPPORT -> "Hỗ trợ Allay";
            case ARMADILLO_ROLL -> "Cuộn giáp";
            case AXOLOTL_REGEN -> "Hồi phục Axolotl";
            case BAT_SONAR -> "Sóng dò đường";
            case CAMEL_STOMP -> "Giậm chân Lạc đà";
            case CHICKEN_FLUTTER -> "Bay lượn";
            case COW_RUSH -> "Húc bò";
            case DONKEY_KICK -> "Đá lừa";
            case FROG_TONGUE -> "Lưỡi ếch";
            case GLOW_SQUID_BLIND -> "Mực phát sáng";
            case HAPPY_GHAST_WIND -> "Gió Ghast vui vẻ";
            case MOOSHROOM_SPORES -> "Bào tử nấm";
            case MULE_KICK -> "Đá la";
            case PARROT_SONIC -> "Tiếng hét vẹt";
            case PIG_CHARGE -> "Lao tới";
            case RABBIT_JUMP -> "Nhảy thỏ";
            case SALMON_SPLASH -> "Cú quẫy nước";
            case SNIFFER_SCENT -> "Đánh hơi";
            case SNOW_GOLEM_SNOW -> "Ném cầu tuyết";
            case SQUID_INK -> "Phun mực";
            case STRIDER_LAVA -> "Bước chân dung nham";
            case TURTLE_SHELL -> "Mai rùa";
            case VILLAGER_TRADE -> "Trao đổi";
            case WANDERING_TRADER_POTION -> "Thuốc thương nhân";
            case NAUTILUS_WATER -> "Lướt nước Nautilus";
            case ZOMBIE_NAUTILUS_TIDE -> "Sóng xác sống";
            case SULFUR_CUBE_BLAST -> "Vụ nổ Sulfur Cube";

            default -> "Kỹ năng chưa xác định";
        };
    }
}
