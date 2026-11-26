package com.example.examplemod;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Selection screen for stealing exactly one or two abilities. */
public final class FogTucStealScreen extends Screen {
    private final UUID targetId;
    private final List<String> candidates;
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();

    public FogTucStealScreen(UUID targetId, List<String> candidates) {
        super(Component.literal("Cướp Đoạt"));
        this.targetId = targetId;
        this.candidates = List.copyOf(candidates);
    }

    @Override
    protected void init() {
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.literal("CHỌN 1 HOẶC 2 NĂNG LỰC"), b -> {})
                .bounds(center - 130, 18, 260, 20).build());
        int visible = Math.min(candidates.size(), 20);
        for (int i = 0; i < visible; i++) {
            String id = candidates.get(i);
            boolean checked = selected.contains(id);
            String label = (checked ? "✓ " : "□ ") + displayName(id);
            int col = i / 10;
            int row = i % 10;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                if (selected.contains(id)) selected.remove(id);
                else if (selected.size() < 2) selected.add(id);
                rebuild();
            }).bounds(center - 145 + col * 150, 48 + row * 23, 145, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("XÁC NHẬN (" + selected.size() + "/2)"), b -> {
            if (selected.isEmpty() || selected.size() > 2) return;
            String request = "stealpick|" + targetId + "|" + String.join(",", selected);
            ClientPacketDistributor.sendToServer(new FogCharacterPayload(request));
            Minecraft.getInstance().gui.setScreen(null);
        }).bounds(center - 145, height - 42, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("HỦY"), b -> Minecraft.getInstance().gui.setScreen(null))
                .bounds(center + 5, height - 42, 140, 20).build());
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private static String displayName(String id) {
        return id.replace('_', ' ');
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
