
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

    private static String message = "";

    public FogCharacterScreen() {
        super(Component.literal("Bảng thuộc tính nhân vật"));
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

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.gui.screen() instanceof FogCharacterScreen screen) {
            screen.refreshScreen();
        } else {
            minecraft.gui.setScreen(new FogCharacterScreen());
        }
    }

    private void refreshScreen() {
        clearWidgets();
        init();
    }

    private void refreshScreen() {
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = Math.max(10, this.height / 2 - 170);

        addLabel(
                "BẢNG THUỘC TÍNH NHÂN VẬT",
                centerX - 145, startY, 290
        );

        addLabel(
                "Cấp nhân vật: " + level,
                centerX - 130, startY + 25, 260
        );

        addLabel(
                "Điểm Linh Tính: " + spiritPoints,
                centerX - 130, startY + 48, 260
        );

        addLabel(
                "Điểm thuộc tính: " + freePoints,
                centerX - 130, startY + 71, 260
        );

        addLabel(
                "Mảnh Vỡ Quy Tắc: " + ruleFragments,
                centerX - 130, startY + 94, 260
        );

        addLabel(
                "Tinh Thạch Linh Hồn: " + soulCrystals,
                centerX - 130, startY + 117, 260
        );

        int cost = level * 10;

        addLabel(
                "Giá tăng mỗi thuộc tính: " + cost + " Điểm Linh Tính",
                centerX - 155, startY + 143, 310
        );

        addLabel(
                "Mỗi điểm trên 10 sẽ mất 1 máu hiện tại",
                centerX - 155, startY + 164, 310
        );

        addAttribute("Nhanh nhẹn", "agility",
                agility, centerX, startY + 190);

        addAttribute("Thể lực", "stamina",
                stamina, centerX, startY + 216);

        addAttribute("Sức mạnh", "strength",
                strength, centerX, startY + 242);

        addAttribute("Tinh thần", "spirit",
                spirit, centerX, startY + 268);

        addAttribute("Mị lực", "charisma",
                charisma, centerX, startY + 294);

        addLabel(
                "Máu: " + (int) health + "/" + (int) maxHealth,
                centerX - 120, startY + 321, 240
        );

        addLabel(
                getWeakStatus(),
                centerX - 160, startY + 344, 320
        );

        addLabel(
                message,
                centerX - 160, startY + 367, 320
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Đóng"),
                        button -> onClose()
                )
                .bounds(centerX - 50, startY + 392, 100, 20)
                .build()
        );
    }

    private String getWeakStatus() {
        if (level == 1) {
            return "Thuộc tính yếu khi dưới 5";
        }

        if (level == 2) {
            return "Thuộc tính yếu khi dưới 10";
        }

        return "Ngưỡng thuộc tính yếu chưa được thiết lập";
    }

    private void addLabel(String text, int x, int y, int width) {
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
