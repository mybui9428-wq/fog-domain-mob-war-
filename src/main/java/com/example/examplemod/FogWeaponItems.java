
package com.example.examplemod;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Các vũ khí Kỳ Vật của Fog Domain.
 * Trường Đao và Trường Thương không có kỹ năng kích hoạt riêng.
 */
public final class FogWeaponItems {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MODID);

    // Trường Đao: giữ ID cũ để không làm mất vật phẩm đã tồn tại.
    public static final DeferredItem<Item> SWORD =
            ITEMS.registerItem(
                    "fog_sword",
                    p -> new Item(
                            p.sword(ToolMaterial.NETHERITE, 3.0F, -2.4F)
                                    .stacksTo(1)
                    )
            );

    // Thương cũ: giữ nguyên.
    public static final DeferredItem<Item> SPEAR =
            ITEMS.registerItem(
                    "fog_spear",
                    p -> new Item(
                            p.spear(
                                    ToolMaterial.NETHERITE,
                                    15.0F,
                                    1.2F,
                                    0.0F,
                                    20.0F,
                                    9.0F,
                                    20.0F,
                                    5.1F,
                                    20.0F,
                                    4.6F
                            ).stacksTo(1)
                    )
            );

    // Trường Thương mới: vật phẩm riêng, không thay thế Thương cũ.
    public static final DeferredItem<Item> LONG_SPEAR =
            ITEMS.registerItem(
                    "fog_long_spear",
                    p -> new Item(
                            p.spear(
                                    ToolMaterial.NETHERITE,
                                    15.0F,
                                    1.2F,
                                    0.0F,
                                    20.0F,
                                    9.0F,
                                    20.0F,
                                    5.1F,
                                    20.0F,
                                    4.6F
                            ).stacksTo(1)
                    )
            );

    // Khiên.
    public static final DeferredItem<ShieldItem> SHIELD =
            ITEMS.registerItem(
                    "fog_shield",
                    p -> new ShieldItem(p.durability(1000).stacksTo(1))
            );

    // Cung.
    public static final DeferredItem<BowItem> BOW =
            ITEMS.registerItem(
                    "fog_bow",
                    p -> new BowItem(p.durability(800).stacksTo(1))
            );

    private FogWeaponItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
