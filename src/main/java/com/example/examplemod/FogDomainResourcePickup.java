
package com.example.examplemod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainResourcePickup {

    private FogDomainResourcePickup() {
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack original = event.getOriginalStack();
        ItemStack remaining = event.getCurrentStack();

        if (original.isEmpty()) {
            return;
        }

        int pickedCount = original.getCount()
                - (remaining.isEmpty() ? 0 : remaining.getCount());

        if (pickedCount <= 0) {
            return;
        }

        boolean isSoulCrystal =
                original.is(ExampleMod.SOUL_CRYSTAL.get());

        boolean isRuleFragment =
                original.is(ExampleMod.RULE_FRAGMENT.get());

        if (!isSoulCrystal && !isRuleFragment) {
            return;
        }

        // Xóa đúng số vật phẩm vừa nhặt khỏi túi.
        int removedCount = removeFromInventory(
                player.getInventory(),
                original,
                pickedCount
        );

        if (removedCount <= 0) {
            return;
        }

        FogDomainAbilityData data =
                FogDomainAbilityData.get(serverLevel.getServer());

        data.initializeCharacter(player.getUUID());

        if (isSoulCrystal) {
            data.addSoulCrystals(player.getUUID(), removedCount);

            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§b+" + removedCount
                                    + " Tinh Thạch Linh Hồn"
                    )
            );
        } else {
            data.addRuleFragments(player.getUUID(), removedCount);

            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§d+" + removedCount
                                    + " Mảnh Vỡ Quy Tắc"
                    )
            );
        }
    }

    private static int removeFromInventory(
            Inventory inventory,
            ItemStack reference,
            int amount
    ) {
        int remaining = amount;

        for (int slot = 0;
                slot < inventory.getContainerSize() && remaining > 0;
                slot++) {

            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty() || !ItemStack.isSameItem(stack, reference)) {
                continue;
            }

            int removed = Math.min(stack.getCount(), remaining);

            stack.shrink(removed);
            remaining -= removed;
        }

        return amount - remaining;
    }
}
