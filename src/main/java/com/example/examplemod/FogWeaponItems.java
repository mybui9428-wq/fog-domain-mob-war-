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
 * Kiếm và Trường Thương không có kỹ năng kích hoạt riêng.
 */
public final class FogWeaponItems {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MODID);

    public static final DeferredItem<Item> SWORD =
            ITEMS.registerItem(
                    "fog_sword",
                    p -> new Item(
                            p.sword(ToolMaterial.NETHERITE, 3.0F, -2.4F)
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> SPEAR =
            ITEMS.registerItem(
                    "fog_spear",
                    p -> new Item(
                            p.spear(
                                    ToolMaterial.NETHERITE,
                                    15.0F,  // thời lượng đòn thương
                                    1.2F,   // hệ số sát thương lao tới
                                    0.0F,   // độ trễ
                                    20.0F,  // thời gian xét hất khỏi thú cưỡi
                                    9.0F,   // tốc độ tối thiểu để hất
                                    20.0F,  // thời gian xét đẩy lùi
                                    5.1F,   // tốc độ tối thiểu để đẩy lùi
                                    20.0F,  // thời gian xét sát thương
                                    4.6F    // tốc độ tối thiểu để gây sát thương
                            ).stacksTo(1)
                    )
            );

    public static final DeferredItem<ShieldItem> SHIELD =
            ITEMS.registerItem(
                    "fog_shield",
                    p -> new ShieldItem(p.durability(1000).stacksTo(1))
            );

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
