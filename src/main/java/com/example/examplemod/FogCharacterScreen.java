
package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import java.util.HashMap;
import java.util.Map;

public class FogCharacterScreen extends Screen {

    private static int level = 1;
    private static int spiritPoints = 0;
    private static int freePoints = 0;
    private static int ruleFragments = 0;
    private static int soulCrystals = 0;

    private static int agility = 5;
    private static int stamina = 5;
    private static int strength = 5;
    private static int spirit = 5;
    private static int charisma = 5;

    private static float health = 20;
    private static float maxHealth = 20;

    private static int energySkillLevel = 1;
    private static int cageSkillLevel = 1;
    private static int tameSkillLevel = 1;
    private static int combineSkillLevel = 1;
    private static int stealSkillLevel = 1;
    private static int tripleSpiritSkillLevel = 1;

    private static String message = "";
    private static boolean tucThanhGia = false;
    private static int attributeMaximum = 15;
    private static boolean canAwakenTucThanhGia = false;

    private static final int MAX_LEVEL = 20;

    private boolean skillPage = false;
    private boolean tucSkillPage = false;
    private static final Map<String, Integer> tucSkillLevels = new HashMap<>();
    private static final String[][] TUC_SKILLS = {
            {"thien_hoa", "Thiên Hỏa"}, {"chan_thuc_chi_nhan", "Chân Thực Chi Nhãn"},
            {"cam_tuyet_khong_che", "Cấm Tuyệt Khống Chế"}, {"tich_diet_chi_hoa", "Tịch Diệt Chi Hỏa"},
            {"vo_vat_bat_tram", "Vô Vật Bất Trảm"}, {"tuyet_menh_nhat_dao", "Tuyệt Mệnh Nhất Đao"},
            {"hoi_thanh", "Hồi Thanh"}, {"ta_se_khong_chet", "Ta Sẽ Không Chết"},
            {"bat_huu_chien", "Bất Hưu Chiến"}, {"cuong_hoa", "Cuồng Hóa"},
            {"huyet_chien", "Huyết Chiến"}, {"than_cuong", "Thân Cường"},
            {"bat_khuat", "Bất Khuất"}, {"nguoc_toa_tu_long", "Ngục Tỏa Tù Long"},
            {"huyen_tinh_chu_the", "Huyền Tinh Chú Thể"}, {"cuong_liet", "Cường Liệt"},
            {"phan_sat_thuong", "Phản Sát Thương"}, {"sinh_tu_luan_chuyen", "Sinh Tử Luân Chuyển"},
            {"cuop_doat", "Cướp Đoạt"}, {"ket_hop", "Kết Hợp"}
    };

    public FogCharacterScreen() {
        super(Component.literal("Bảng nhân vật"));
    }

    public static void updateData(
            int newLevel,
            int newSpiritPoints,
            int newFreePoints,
            int newRuleFragments,
            int newSoulCrystals,
            int newAgility,
            int newStamina,
            int newStrength,
            int newSpirit,
            int newCharisma,
            float newHealth,
            float newMaxHealth,
            String newMessage
    ) {
        updateCommonData(
                newLevel, newSpiritPoints, newFreePoints,
                newRuleFragments, newSoulCrystals,
                newAgility, newStamina, newStrength,
                newSpirit, newCharisma,
                newHealth, newMaxHealth, newMessage
        );
    }

    public static void updateData(
            int newLevel,
            int newSpiritPoints,
            int newFreePoints,
            int newRuleFragments,
            int newSoulCrystals,
            int newAgility,
            int newStamina,
            int newStrength,
            int newSpirit,
            int newCharisma,
            float newHealth,
            float newMaxHealth,
            String newMessage,
            int newEnergySkillLevel,
            int newCageSkillLevel,
            int newTameSkillLevel,
            int newCombineSkillLevel,
            int newStealSkillLevel,
            int newTripleSpiritSkillLevel,
            boolean newTucThanhGia,
            int newAttributeMaximum,
            boolean newCanAwakenTucThanhGia,
            String newTucSkillLevels
    ) {
        updateCommonData(
                newLevel, newSpiritPoints, newFreePoints,
                newRuleFragments, newSoulCrystals,
                newAgility, newStamina, newStrength,
                newSpirit, newCharisma,
                newHealth, newMaxHealth, newMessage
        );

        energySkillLevel = Math.max(1, newEnergySkillLevel);
        cageSkillLevel = Math.max(1, newCageSkillLevel);
        tameSkillLevel = Math.max(1, newTameSkillLevel);
        combineSkillLevel = Math.max(1, newCombineSkillLevel);
        stealSkillLevel = Math.max(1, newStealSkillLevel);
        tripleSpiritSkillLevel = Math.max(1, newTripleSpiritSkillLevel);
        tucThanhGia = newTucThanhGia;
        attributeMaximum = Math.max(1, newAttributeMaximum);
        canAwakenTucThanhGia = newCanAwakenTucThanhGia;
        tucSkillLevels.clear();
        if (newTucSkillLevels != null && !newTucSkillLevels.isBlank()) {
            for (String pair : newTucSkillLevels.split(",")) {
                String[] parts = pair.split(":", 2);
                if (parts.length == 2) {
                    try { tucSkillLevels.put(parts[0], Math.max(1, Integer.parseInt(parts[1]))); }
                    catch (NumberFormatException ignored) { }
                }
            }
        }

        refreshOpenScreen();
    }

    private static void updateCommonData(
            int newLevel,
            int newSpiritPoints,
            int newFreePoints,
            int newRuleFragments,
            int newSoulCrystals,
            int newAgility,
            int newStamina,
            int newStrength,
            int newSpirit,
            int newCharisma,
            float newHealth,
            float newMaxHealth,
            String newMessage
    ) {
        level = newLevel;
        spiritPoints = newSpiritPoints;
        freePoints = newFreePoints;
        ruleFragments = newRuleFragments;
        soulCrystals = newSoulCrystals;

        agility = newAgility;
        stamina = newStamina;
        strength = newStrength;
        spirit = newSpirit;
        charisma = newCharisma;

        health = newHealth;
        maxHealth = newMaxHealth;
        message = newMessage;

        refreshOpenScreen();
    }

    private static void refreshOpenScreen() {
        Minecraft minecraft = Minecraft.getInstance();

        minecraft.execute(() -> {
            if (minecraft.gui.screen() instanceof FogCharacterScreen screen) {
                screen.refreshScreen();
            } else {
                minecraft.gui.setScreen(new FogCharacterScreen());
            }
        });
    }

    private void refreshScreen() {
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        if (tucSkillPage) {
            initTucThanhGiaSkillPage(centerX);
        } else if (skillPage) {
            initSkillPage(centerX);
        } else {
            initAttributePage(centerX);
        }
    }

    // =================================================
    // TRANG THUỘC TÍNH - TỐI ƯU ĐIỆN THOẠI
    // =================================================

    private void initAttributePage(int centerX) {

        int startY = Math.max(2, (this.height - 340) / 2);

        addLabel("BẢNG THUỘC TÍNH NHÂN VẬT",
                centerX - 145, startY, 290);

        addLabel("Cấp nhân vật: " + level + "/" + MAX_LEVEL,
                centerX - 130, startY + 18, 260);

        addLabel("Điểm Linh Tính: " + spiritPoints,
                centerX - 130, startY + 36, 260);

        addLabel("Điểm thuộc tính: " + freePoints,
                centerX - 130, startY + 54, 260);

        addLabel("Mảnh Vỡ Quy Tắc: " + ruleFragments,
                centerX - 130, startY + 72, 260);

        addLabel("Tinh Thạch Linh Hồn: " + soulCrystals,
                centerX - 130, startY + 90, 260);

        addLabel("Giới hạn mỗi chỉ số: " + attributeMaximum
                        + (tucThanhGia ? " (Túc Thanh Giả: cấp x 30)" : ""),
                centerX - 155, startY + 108, 310);

        int requiredAttribute = level * 10;
        boolean hasLevelUpAttribute = agility >= requiredAttribute
                || stamina >= requiredAttribute
                || strength >= requiredAttribute
                || spirit >= requiredAttribute;
        addLabel("Tăng cấp: 1/4 chỉ số chính đạt " + requiredAttribute
                        + " | Giá tăng: " + (level * 100) + " LT + " + level + " Mảnh",
                centerX - 165, startY + 126, 330);

        addAttribute("Nhanh nhẹn", "agility",
                agility, centerX, startY + 146);

        addAttribute("Thể lực", "stamina",
                stamina, centerX, startY + 164);

        addAttribute("Sức mạnh", "strength",
                strength, centerX, startY + 182);

        addAttribute("Tinh thần", "spirit",
                spirit, centerX, startY + 200);

        addAttribute("Mị lực", "charisma",
                charisma, centerX, startY + 218);

        addLabel("Máu: " + (int) health + "/" + (int) maxHealth,
                centerX - 120, startY + 238, 240);

        addLabel(getWeakStatus(),
                centerX - 155, startY + 256, 310);

        addLabel(message,
                centerX - 165, startY + 274, 330);

        int spiritCost = level * 100;
        int fragmentCost = level;

        boolean maxLevel = level >= MAX_LEVEL;

        String levelUpText = maxLevel
                ? "Đã đạt cấp tối đa"
                : "Tăng cấp: " + spiritCost
                + " Linh Tính + " + fragmentCost + " Mảnh";

        Button levelUpButton = Button.builder(
                Component.literal(levelUpText),
                button -> ClientPacketDistributor.sendToServer(
                        new FogCharacterPayload("levelup")
                )
        )
        .bounds(centerX - 110, startY + 294, 220, 20)
        .build();

        levelUpButton.active =
                !maxLevel
                && hasLevelUpAttribute
                && spiritPoints >= spiritCost
                && ruleFragments >= fragmentCost;

        addRenderableWidget(levelUpButton);

        if (!tucThanhGia && canAwakenTucThanhGia) {
            addRenderableWidget(
                    Button.builder(
                            Component.literal("THỨC TỈNH TÚC THANH GIẢ"),
                            button -> ClientPacketDistributor.sendToServer(
                                    new FogCharacterPayload("awaken_tuc")
                            )
                    )
                    .bounds(centerX - 110, startY + 316, 220, 20)
                    .build()
            );
        } else {
            addRenderableWidget(
                    Button.builder(
                            Component.literal("KỸ NĂNG >>"),
                            button -> {
                                skillPage = true;
                                refreshScreen();
                            }
                    )
                    .bounds(centerX - 110, startY + 316, 105, 20)
                    .build()
            );

            addRenderableWidget(
                    Button.builder(
                            Component.literal("Đóng"),
                            button -> onClose()
                    )
                    .bounds(centerX + 5, startY + 316, 105, 20)
                    .build()
            );
        }
    }

    // =================================================
    // TRANG KỸ NĂNG
    // =================================================

    private void initSkillPage(int centerX) {

        int startY = Math.max(2, (this.height - 340) / 2);

        addLabel("BẢNG NÂNG CẤP KỸ NĂNG",
                centerX - 145, startY, 290);

        addLabel("Điểm Linh Tính: " + spiritPoints,
                centerX - 125, startY + 20, 250);

        addLabel("Tinh Thạch Linh Hồn: " + soulCrystals,
                centerX - 125, startY + 38, 250);

        addLabel("Chi phí: cấp kỹ năng x 100 Linh Tính",
                centerX - 160, startY + 56, 320);

        addSkill("Năng Lượng", "energy",
                energySkillLevel, centerX, startY + 82);

        addSkill("Nhà Tù", "cage",
                cageSkillLevel, centerX, startY + 114);

        addSkill("Thuần Hóa", "tame",
                tameSkillLevel, centerX, startY + 146);

        addSkill("Kết Hợp", "combine",
                combineSkillLevel, centerX, startY + 178);

        addSkill("Cướp Đoạt", "steal",
                stealSkillLevel, centerX, startY + 210);

        addSkill("Tam Linh Hộ Thể", "triple_spirit_guard",
                tripleSpiritSkillLevel, centerX, startY + 242);

        addLabel(message,
                centerX - 165, startY + 274, 330);

        addRenderableWidget(
                Button.builder(Component.literal("<< THUỘC TÍNH"), button -> {
                    skillPage = false;
                    refreshScreen();
                }).bounds(centerX - 110, startY + 304, 105, 20).build()
        );

        if (tucThanhGia) {
            addRenderableWidget(Button.builder(Component.literal("TÚC THANH GIẢ >>"), button -> {
                tucSkillPage = true;
                refreshScreen();
            }).bounds(centerX + 5, startY + 304, 105, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Đóng"), button -> onClose())
                    .bounds(centerX + 5, startY + 304, 105, 20).build());
        }
    }

    private void initTucThanhGiaSkillPage(int centerX) {
        int startY = Math.max(2, (this.height - 340) / 2);
        addLabel("NÂNG CẤP TOÀN BỘ KỸ NĂNG TÚC THANH GIẢ", centerX - 220, startY, 440);
        addLabel("Linh Tính: " + spiritPoints + " | Tinh Thạch: " + soulCrystals,
                centerX - 190, startY + 22, 380);
        addLabel("Chi phí mỗi lần: cấp hiện tại x 100 LT + cấp hiện tại Tinh Thạch",
                centerX - 220, startY + 40, 440);
        for (int i = 0; i < TUC_SKILLS.length; i++) {
            int col = i / 10;
            int row = i % 10;
            int x = centerX - 230 + col * 235;
            int y = startY + 64 + row * 23;
            addTucSkill(TUC_SKILLS[i][1], TUC_SKILLS[i][0], x, y);
        }
        addRenderableWidget(Button.builder(Component.literal("<< BẢNG KỸ NĂNG"), button -> {
            tucSkillPage = false;
            skillPage = true;
            refreshScreen();
        }).bounds(centerX - 110, startY + 302, 105, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Đóng"), button -> onClose())
                .bounds(centerX + 5, startY + 302, 105, 20).build());
    }

    private void addTucSkill(String name, String skillId, int x, int y) {
        int current = tucSkillLevels.getOrDefault(skillId, 1);
        int spiritCost = current * 100;
        int crystalCost = current;
        addLabel(name + " Cấp " + current, x, y, 130);
        Button upgrade = Button.builder(Component.literal("+1 (" + spiritCost + "/" + crystalCost + ")"), button ->
                ClientPacketDistributor.sendToServer(new FogCharacterPayload("upgrade|" + skillId)))
                .bounds(x + 132, y, 95, 18).build();
        upgrade.active = spiritPoints >= spiritCost && soulCrystals >= crystalCost;
        addRenderableWidget(upgrade);
    }

    private void addSkill(
            String name,
            String skillId,
            int skillLevel,
            int centerX,
            int y
    ) {
        int spiritCost = skillLevel * 100;
        int crystalCost = skillLevel;

        addLabel(
                name + ": Cấp " + skillLevel,
                centerX - 205,
                y,
                200
        );

        Button upgradeButton = Button.builder(
                Component.literal(
                        "Nâng cấp: " + spiritCost + " LT, "
                                + crystalCost + " Tinh Thạch"
                ),
                button -> ClientPacketDistributor.sendToServer(
                        new FogCharacterPayload("upgrade|" + skillId)
                )
        )
        .bounds(centerX + 5, y, 210, 20)
        .build();

        upgradeButton.active =
                spiritPoints >= spiritCost
                && soulCrystals >= crystalCost;

        addRenderableWidget(upgradeButton);
    }

    // =================================================
    // NÚT TĂNG THUỘC TÍNH
    // =================================================

    private void addAttribute(
            String name,
            String key,
            int value,
            int centerX,
            int y
    ) {
        addLabel(
                name + ": " + value,
                centerX - 130,
                y,
                180
        );

        Button increaseButton = Button.builder(
                Component.literal("Tăng"),
                button -> ClientPacketDistributor.sendToServer(
                        new FogCharacterPayload("add|" + key)
                )
        )
        .bounds(centerX + 55, y, 55, 18)
        .build();

        increaseButton.active =
                value < attributeMaximum
                && spiritPoints >= level * 10;

        addRenderableWidget(increaseButton);
    }

    private String getWeakStatus() {
        return "Thuộc tính yếu khi dưới " + (level * 5);
    }

    private void addLabel(
            String text,
            int x,
            int y,
            int width
    ) {
        Button label = Button.builder(
                Component.literal(text),
                button -> {}
        )
        .bounds(x, y, width, 18)
        .build();

        label.active = false;
        addRenderableWidget(label);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
