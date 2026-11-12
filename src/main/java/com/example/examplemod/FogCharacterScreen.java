
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
            int newTripleSpiritSkillLevel
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

        addLabel("Giá tăng thuộc tính: " + (level * 10) + " Linh Tính",
                centerX - 150, startY + 108, 300);

        addLabel("Điểm trên 10 mất 1 máu hiện tại",
                centerX - 150, startY + 126, 300);

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
                && spiritPoints >= spiritCost
                && ruleFragments >= fragmentCost;

        addRenderableWidget(levelUpButton);

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
                Button.builder(
                        Component.literal("<< THUỘC TÍNH"),
                        button -> {
                            skillPage = false;
                            refreshScreen();
                        }
                )
                .bounds(centerX - 110, startY + 304, 105, 20)
                .build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Đóng"),
                        button -> onClose()
                )
                .bounds(centerX + 5, startY + 304, 105, 20)
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

        increaseButton.active = spiritPoints >= level * 10;

        addRenderableWidget(increaseButton);
    }

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
