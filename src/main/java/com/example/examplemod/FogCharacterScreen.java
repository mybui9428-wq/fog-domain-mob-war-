
package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

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

    private static final int MAX_LEVEL = 20;

    private boolean skillPage = false;

    public FogCharacterScreen() {
        super(Component.literal("Bảng nhân vật"));
    }

    // Đồng bộ dữ liệu cũ.
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
                newLevel,
                newSpiritPoints,
                newFreePoints,
                newRuleFragments,
                newSoulCrystals,
                newAgility,
                newStamina,
                newStrength,
                newSpirit,
                newCharisma,
                newHealth,
                newMaxHealth,
                newMessage
        );
    }

    // Đồng bộ dữ liệu mới, bao gồm cấp độ kỹ năng.
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
            int newTripleSpiritSkillLevel
    ) {
        updateCommonData(
                newLevel,
                newSpiritPoints,
                newFreePoints,
                newRuleFragments,
                newSoulCrystals,
                newAgility,
                newStamina,
                newStrength,
                newSpirit,
                newCharisma,
                newHealth,
                newMaxHealth,
                newMessage
        );

        energySkillLevel = Math.max(1, newEnergySkillLevel);
        cageSkillLevel = Math.max(1, newCageSkillLevel);
        tameSkillLevel = Math.max(1, newTameSkillLevel);
        combineSkillLevel = Math.max(1, newCombineSkillLevel);
        stealSkillLevel = Math.max(1, newStealSkillLevel);
        tripleSpiritSkillLevel = Math.max(1, newTripleSpiritSkillLevel);

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

        if (skillPage) {
            initSkillPage(centerX);
        } else {
            initAttributePage(centerX);
        }
    }

    // =====================================================
    // TRANG THUỘC TÍNH
    // =====================================================

    private void initAttributePage(int centerX) {
        int startY = Math.max(4, (this.height - 440) / 2);

        addLabel(
                "BẢNG THUỘC TÍNH NHÂN VẬT",
                centerX - 145, startY, 290
        );

        addLabel(
                "Cấp nhân vật: " + level + "/" + MAX_LEVEL,
                centerX - 130, startY + 22, 260
        );

        addLabel(
                "Điểm Linh Tính: " + spiritPoints,
                centerX - 130, startY + 43, 260
        );

        addLabel(
                "Điểm thuộc tính: " + freePoints,
                centerX - 130, startY + 64, 260
        );

        addLabel(
                "Mảnh Vỡ Quy Tắc: " + ruleFragments,
                centerX - 130, startY + 85, 260
        );

        addLabel(
                "Tinh Thạch Linh Hồn: " + soulCrystals,
                centerX - 130, startY + 106, 260
        );

        int attributeCost = level * 10;

        addLabel(
                "Giá tăng thuộc tính: " + attributeCost + " Điểm Linh Tính",
                centerX - 155, startY + 130, 310
        );

        addLabel(
                "Mỗi điểm trên 10 mất 1 máu hiện tại",
                centerX - 155, startY + 151, 310
        );

        addAttribute("Nhanh nhẹn", "agility",
                agility, centerX, startY + 175);

        addAttribute("Thể lực", "stamina",
                stamina, centerX, startY + 198);

        addAttribute("Sức mạnh", "strength",
                strength, centerX, startY + 221);

        addAttribute("Tinh thần", "spirit",
                spirit, centerX, startY + 244);

        addAttribute("Mị lực", "charisma",
                charisma, centerX, startY + 267);

        addLabel(
                "Máu: " + (int) health + "/" + (int) maxHealth,
                centerX - 120, startY + 291, 240
        );

        addLabel(
                getWeakStatus(),
                centerX - 160, startY + 313, 320
        );

        addLabel(
                message,
                centerX - 160, startY + 335, 320
        );

        int levelUpSpiritCost = level * 100;
        int levelUpFragmentCost = level;

        boolean maxLevel = level >= MAX_LEVEL;

        String levelUpText;

        if (maxLevel) {
            levelUpText = "Đã đạt cấp tối đa";
        } else {
            levelUpText = "Tăng cấp: "
                    + levelUpSpiritCost
                    + " Linh Tính + "
                    + levelUpFragmentCost
                    + " Mảnh";
        }

        Button levelUpButton = Button.builder(
                Component.literal(levelUpText),
                button -> ClientPacketDistributor.sendToServer(
                        new FogCharacterPayload("levelup")
                )
        )
        .bounds(centerX - 110, startY + 359, 220, 20)
        .build();

        levelUpButton.active =
                !maxLevel
                && spiritPoints >= levelUpSpiritCost
                && ruleFragments >= levelUpFragmentCost;

        addRenderableWidget(levelUpButton);

        addRenderableWidget(
                Button.builder(
                        Component.literal("Kỹ năng >>"),
                        button -> {
                            skillPage = true;
                            refreshScreen();
                        }
                )
                .bounds(centerX - 110, startY + 383, 105, 20)
                .build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Đóng"),
                        button -> onClose()
                )
                .bounds(centerX + 5, startY + 383, 105, 20)
                .build()
        );
    }

    // =====================================================
    // TRANG KỸ NĂNG
    // =====================================================

    private void initSkillPage(int centerX) {
        int startY = Math.max(8, (this.height - 390) / 2);

        addLabel(
                "BẢNG NÂNG CẤP KỸ NĂNG",
                centerX - 145, startY, 290
        );

        addLabel(
                "Điểm Linh Tính: " + spiritPoints,
                centerX - 125, startY + 25, 250
        );

        addLabel(
                "Tinh Thạch Linh Hồn: " + soulCrystals,
                centerX - 125, startY + 47, 250
        );

        addLabel(
                "Chi phí nâng cấp dựa trên cấp kỹ năng hiện tại",
                centerX - 165, startY + 72, 330
        );

        addSkill(
                "Năng Lượng",
                "energy",
                energySkillLevel,
                centerX,
                startY + 105
        );

        addSkill(
                "Nhà Tù",
                "cage",
                cageSkillLevel,
                centerX,
                startY + 143
        );

        addSkill(
                "Thuần Hóa",
                "tame",
                tameSkillLevel,
                centerX,
                startY + 181
        );

        addSkill(
                "Kết Hợp",
                "combine",
                combineSkillLevel,
                centerX,
                startY + 219
        );

        addSkill(
                "Cướp Đoạt",
                "steal",
                stealSkillLevel,
                centerX,
                startY + 257
        );

        addSkill(
                "Tam Linh Hộ Thể",
                "triple_spirit_guard",
                tripleSpiritSkillLevel,
                centerX,
                startY + 295
        );

        addLabel(
                message,
                centerX - 165, startY + 337, 330
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("<< Thuộc tính"),
                        button -> {
                            skillPage = false;
                            refreshScreen();
                        }
                )
                .bounds(centerX - 110, startY + 365, 105, 20)
                .build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Đóng"),
                        button -> onClose()
                )
                .bounds(centerX + 5, startY + 365, 105, 20)
                .build()
        );
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
                name + " - Cấp " + skillLevel,
                centerX - 155,
                y,
                155
        );

        Button upgradeButton = Button.builder(
                Component.literal(
                        "Nâng cấp (" + spiritCost + " LT, "
                                + crystalCost + " Tinh Thạch)"
                ),
                button -> ClientPacketDistributor.sendToServer(
                        new FogCharacterPayload("upgrade|" + skillId)
                )
        )
        .bounds(centerX + 5, y, 175, 20)
        .build();

        upgradeButton.active =
                spiritPoints >= spiritCost
                && soulCrystals >= crystalCost;

        addRenderableWidget(upgradeButton);
    }

    // =====================================================
    // THUỘC TÍNH
    // =====================================================

    private String getWeakStatus() {
        if (level == 1) {
            return "Thuộc tính yếu khi dưới 5";
        }

        if (level == 2) {
            return "Thuộc tính yếu khi dưới 10";
        }

        return "Không có ngưỡng thuộc tính yếu ở cấp này";
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
        .bounds(x, y, width, 20)
        .build();

        label.active = false;
        addRenderableWidget(label);
    }

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
                Component.literal("+"),
                button -> ClientPacketDistributor.sendToServer(
                        new FogCharacterPayload("add|" + key)
                )
        )
        .bounds(centerX + 55, y, 35, 20)
        .build();

        increaseButton.active = spiritPoints >= level * 10;

        addRenderableWidget(increaseButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
