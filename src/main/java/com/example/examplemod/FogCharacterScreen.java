
package com.example.examplemod;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
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

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = Math.max(25, this.height / 2 - 115);

        addLabel(
                "CẤP NHÂN VẬT: " + level,
                centerX - 110, startY, 220
        );

        addLabel(
                "Điểm Linh Tính: " + spiritPoints,
                centerX - 110, startY + 25, 220
        );

        addLabel(
                "Thuộc tính có thể phân bổ: " + freePoints,
                centerX - 110, startY + 48, 220
        );

        addLabel(
                "Mảnh Vỡ Quy Tắc: " + ruleFragments,
                centerX - 110, startY + 71, 220
        );

        addLabel(
                "Tinh Thạch Linh Hồn: " + soulCrystals,
                centerX - 110, startY + 94, 220
        );

        addAttribute(
                "Nhanh nhẹn",
                "agility",
                agility,
                centerX,
                startY + 125
        );

        addAttribute(
                "Thể lực",
                "stamina",
                stamina,
                centerX,
                startY + 153
        );

        addAttribute(
                "Sức mạnh",
                "strength",
                strength,
                centerX,
                startY + 181
        );

        addAttribute(
                "Tinh thần",
                "spirit",
                spirit,
                centerX,
                startY + 209
        );

        addAttribute(
                "Mị lực",
                "charisma",
                charisma,
                centerX,
                startY + 237
        );

        addLabel(
                "Máu: " + (int) health + "/" + (int) maxHealth,
                centerX - 110, startY + 270, 220
        );

        addLabel(
                message,
                centerX - 140, startY + 295, 280
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Đóng"),
                        button -> onClose()
                )
                .bounds(centerX - 50, startY + 320, 100, 20)
                .build()
        );
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
                centerX - 110,
                y,
                165
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("+"),
                        button -> ClientPacketDistributor.sendToServer(
        new FogCharacterPayload("add|" + key)
)
                )
                .bounds(centerX + 60, y, 35, 20)
                .build()
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
