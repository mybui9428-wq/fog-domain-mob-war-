package com.example.examplemod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Inventory selection board for the Túc Thanh Giả Kết Hợp ability. */
public final class FogTucCombineScreen extends Screen {
    private final List<Integer> selectedSlots = new ArrayList<>(2);

    public FogTucCombineScreen() {
        super(Component.literal("Kết Hợp"));
    }

    @Override
    protected void init() {
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.literal("BẢNG KẾT HỢP"), b -> {})
                .bounds(center - 110, 12, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Chọn tối đa 2 vật phẩm"), b -> {})
                .bounds(center - 110, 35, 220, 20).build());

        var player = Minecraft.getInstance().player;
        if (player != null) {
            int visible = Math.min(27, player.getInventory().getContainerSize());
            for (int slot = 0; slot < visible; slot++) {
                final int inventorySlot = slot;
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.isEmpty()) continue;
                String prefix = selectedSlots.contains(slot) ? "✓ " : "□ ";
                String label = prefix + stack.getHoverName().getString();
                int col = slot / 9;
                int row = slot % 9;
                addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    if (selectedSlots.contains(inventorySlot)) selectedSlots.remove(Integer.valueOf(inventorySlot));
                    else if (selectedSlots.size() < 2) selectedSlots.add(inventorySlot);
                    rebuild();
                }).bounds(center - 210 + row * 47, 65 + col * 25, 45, 21).build());
            }
        }

        String selection = selectedSlots.isEmpty() ? "Chưa chọn vật phẩm"
                : "Đã chọn ô: " + selectedSlots.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(" và "));
        addRenderableWidget(Button.builder(Component.literal(selection), b -> {})
                .bounds(center - 150, height - 55, 300, 20).build());
        addRenderableWidget(Button.builder(Component.literal("ĐÓNG"), b -> onClose())
                .bounds(center - 45, height - 30, 90, 20).build());
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
