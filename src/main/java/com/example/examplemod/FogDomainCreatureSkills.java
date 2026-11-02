
package com.example.examplemod;

import java.util.List;
import java.util.Map;

/**
 * Danh mục kỹ năng được mở khóa theo loài sinh vật.
 * Mỗi lần hấp thụ sinh vật sẽ mở khóa các kỹ năng tương ứng.
 */
public final class FogDomainCreatureSkills {

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

    private static final Map<String, List<String>> SKILLS =
            Map.ofEntries(
                    Map.entry("minecraft:creeper",
                            List.of(CREEPER_EXPLOSION)),

                    Map.entry("minecraft:enderman",
                            List.of(ENDERMAN_TELEPORT)),

                    Map.entry("minecraft:warden",
                            List.of(WARDEN_SONIC_BOOM, WARDEN_SONAR)),

                    Map.entry("minecraft:iron_golem",
                            List.of(IRON_GOLEM_SMASH)),

                    Map.entry("minecraft:witch",
                            List.of(WITCH_POTION)),

                    Map.entry("minecraft:blaze",
                            List.of(BLAZE_FIREBALL)),

                    Map.entry("minecraft:ghast",
                            List.of(GHAST_FIREBALL)),

                    Map.entry("minecraft:spider",
                            List.of(SPIDER_WEB)),

                    Map.entry("minecraft:ender_dragon",
                            List.of(ENDER_DRAGON_BREATH))
            );

    private FogDomainCreatureSkills() {
    }

    /**
     * Lấy danh sách kỹ năng của loài sinh vật.
     */
    public static List<String> getSkills(String entityTypeId) {
        if (entityTypeId == null) {
            return List.of();
        }

        return SKILLS.getOrDefault(entityTypeId, List.of());
    }

    /**
     * Kiểm tra sinh vật có kỹ năng được hỗ trợ hay không.
     */
    public static boolean hasSkills(String entityTypeId) {
        return !getSkills(entityTypeId).isEmpty();
    }

    /**
     * Lấy tên hiển thị của kỹ năng.
     */
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
            default -> "Kỹ năng chưa xác định";
        };
    }
}
