
package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(
        modid = ExampleMod.MODID,
        value = Dist.CLIENT
)
public class ExampleModClient {

    private static String hudData = "";

    public ExampleModClient(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                ConfigurationScreen::new
        );
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ExampleMod.LOGGER.info("FOG DOMAIN CLIENT READY");
    }

    @SubscribeEvent
    public static void registerClientPayloads(
            RegisterClientPayloadHandlersEvent event
    ) {

        // Nhận dữ liệu điều khiển Nhập Hồn.
        event.register(
                FogPossessionSyncPayload.TYPE,
                (payload, context) -> {

                    Minecraft minecraft = Minecraft.getInstance();

                    if (minecraft.player == null
                            || minecraft.level == null) {
                        return;
                    }

                    if (payload.action() == 1) {

                        Entity target = minecraft.level.getEntity(
                                payload.targetId()
                        );

                        if (target != null) {
                            FogPossessionClient.soulMode = false;
                            minecraft.setCameraEntity(target);
                        }
                    }

                    else if (payload.action() == 0) {

                        FogPossessionClient.soulMode = false;
                        minecraft.setCameraEntity(minecraft.player);
                    }

                    else if (payload.action() == 2) {

                        FogPossessionClient.soulMode = true;
                        minecraft.setCameraEntity(minecraft.player);
                    }

                    else if (payload.action() == 3) {

                        FogPossessionClient.soulMode = false;
                        minecraft.setCameraEntity(minecraft.player);
                    }
                }
        );

        // Nhận dữ liệu HUD cũ từ máy chủ.
        event.register(
                FogHudPayload.TYPE,
                (payload, context) -> {
                    hudData = payload.data();
                }
        );

        // Nhận dữ liệu bảng thuộc tính nhân vật.
        event.register(
                FogCharacterPayload.TYPE,
                (payload, context) -> {

                    String[] parts = payload.data().split("\\|", -1);

                    if (parts.length < 14
                            || !parts[0].equals("update")) {
                        return;
                    }

                    try {
                        int level = Integer.parseInt(parts[1]);
                        int spiritPoints = Integer.parseInt(parts[2]);
                        int freePoints = Integer.parseInt(parts[3]);
                        int ruleFragments = Integer.parseInt(parts[4]);
                        int soulCrystals = Integer.parseInt(parts[5]);

                        int agility = Integer.parseInt(parts[6]);
                        int stamina = Integer.parseInt(parts[7]);
                        int strength = Integer.parseInt(parts[8]);
                        int spirit = Integer.parseInt(parts[9]);
                        int charisma = Integer.parseInt(parts[10]);

                        float health = Float.parseFloat(parts[11]);
                        float maxHealth = Float.parseFloat(parts[12]);

                        String message = parts[13];

                        Minecraft.getInstance().execute(() -> {
                            FogCharacterScreen.updateData(
                                    level,
                                    spiritPoints,
                                    freePoints,
                                    ruleFragments,
                                    soulCrystals,
                                    agility,
                                    stamina,
                                    strength,
                                    spirit,
                                    charisma,
                                    health,
                                    maxHealth,
                                    message
                            );
                        });

                    } catch (NumberFormatException exception) {
                        ExampleMod.LOGGER.error(
                                "Du lieu bang nhan vat khong hop le",
                                exception
                        );
                    }
                }
        );
    }

    // Vẽ bảng trạng thái cũ trên màn hình.
    @SubscribeEvent
    public static void renderHud(RenderGuiEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            return;
        }

        if (!minecraft.player.getName().getString()
                .equalsIgnoreCase("tubi2531")) {
            return;
        }

        if (hudData == null || hudData.isBlank()) {
            return;
        }

        GuiGraphicsExtractor graphics =
                event.getGuiGraphics();

        int x = 8;
        int y = 8;
        int width = 190;

        String[] lines = hudData.split("\\n");

        int visibleLines = Math.min(lines.length, 12);
        int height = 14 + visibleLines * 12;

        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                0xCC101820
        );

        graphics.fill(
                x,
                y,
                x + width,
                y + 2,
                0xFF55DDEE
        );

        var textRenderer = graphics.textRenderer(
                GuiGraphicsExtractor.HoveredTextEffects.NONE
        );

        textRenderer.accept(
                TextAlignment.LEFT,
                x + 7,
                y + 6,
                textRenderer.defaultParameters(),
                Component.literal("LINH VUC SUONG MU")
        );

        for (int i = 0; i < visibleLines; i++) {

            textRenderer.accept(
                    TextAlignment.LEFT,
                    x + 7,
                    y + 20 + i * 12,
                    textRenderer.defaultParameters(),
                    Component.literal(lines[i])
            );
        }
    }
}
