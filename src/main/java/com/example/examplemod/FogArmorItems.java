
package com.example.examplemod;

import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ArmorType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FogArmorItems {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MODID);

    // Vương miện - ô đầu
    public static final DeferredItem<Item> CROWN =
            ITEMS.registerItem(
                    "fog_crown",
                    properties -> new Item(
                            properties.humanoidArmor(
                                    ArmorMaterials.NETHERITE,
                                    ArmorType.HELMET
                            ).stacksTo(1)
                    )
            );

    // Áo - ô thân
    public static final DeferredItem<Item> CHESTPLATE =
            ITEMS.registerItem(
                    "fog_chestplate",
                    properties -> new Item(
                            properties.humanoidArmor(
                                    ArmorMaterials.NETHERITE,
                                    ArmorType.CHESTPLATE
                            ).stacksTo(1)
                    )
            );

    // Quần - ô chân
    public static final DeferredItem<Item> LEGGINGS =
            ITEMS.registerItem(
                    "fog_leggings",
                    properties -> new Item(
                            properties.humanoidArmor(
                                    ArmorMaterials.NETHERITE,
                                    ArmorType.LEGGINGS
                            ).stacksTo(1)
                    )
            );

    // Giày - ô bàn chân
    public static final DeferredItem<Item> BOOTS =
            ITEMS.registerItem(
                    "fog_boots",
                    properties -> new Item(
                            properties.humanoidArmor(
                                    ArmorMaterials.NETHERITE,
                                    ArmorType.BOOTS
                            ).stacksTo(1)
                    )
            );

    private FogArmorItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
