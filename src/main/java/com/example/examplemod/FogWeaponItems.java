
package com.example.examplemod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.UseEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Các vũ khí Kỳ Vật của Fog Domain.
 */
public final class FogWeaponItems {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MODID);

    // Trường Đao: không thể bị phá hủy.
    public static final DeferredItem<Item> SWORD =
            ITEMS.registerItem(
                    "fog_sword",
                    p -> new Item(
                            p.sword(
                                    ToolMaterial.NETHERITE,
                                    3.0F,
                                    -2.4F
                            )
                            .component(
                                    DataComponents.UNBREAKABLE,
                                    Unit.INSTANCE
                            )
                            .stacksTo(1)
                    )
            );

    // Trường Thương: sát thương 10, tốc độ đánh tăng.
    public static final DeferredItem<Item> LONG_SPEAR =
            ITEMS.registerItem(
                    "fog_long_spear",
                    p -> new Item(
                            p.sword(
                                    ToolMaterial.NETHERITE,
                                    10.0F,
                                    2.0F
                            )
                            .component(
                                    DataComponents.ATTACK_RANGE,
                                    new AttackRange(
                                            0.0F,
                                            4.5F,
                                            0.0F,
                                            5.0F,
                                            0.3F,
                                            1.0F
                                    )
                            )
                            .component(
                                    DataComponents.UNBREAKABLE,
                                    Unit.INSTANCE
                            )
                            .stacksTo(1)
                    )
            );

    // Khiên: không thể bị phá hủy.
    public static final DeferredItem<ShieldItem> SHIELD =
            ITEMS.registerItem(
                    "fog_shield",
                    p -> new ShieldItem(
                            p.durability(1000)
                                    .component(
                                            DataComponents.UNBREAKABLE,
                                            Unit.INSTANCE
                                    )
                                    // Không bị giảm tốc độ khi đang giơ khiên.
                                    .component(
                                            DataComponents.USE_EFFECTS,
                                            new UseEffects(
                                                    true,
                                                    false,
                                                    1.0F
                                            )
                                    )
                                    .stacksTo(1)
                    )
            );

    // Cung: không thể bị phá hủy.
    public static final DeferredItem<BowItem> BOW =
            ITEMS.registerItem(
                    "fog_bow",
                    p -> new BowItem(
                            p.durability(800)
                                    .component(
                                            DataComponents.UNBREAKABLE,
                                            Unit.INSTANCE
                                    )
                                    .stacksTo(1)
                    )
            );

    private FogWeaponItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
