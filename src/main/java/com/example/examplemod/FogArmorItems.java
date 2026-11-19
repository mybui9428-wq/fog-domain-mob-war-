
package com.example.examplemod;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FogArmorItems {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MODID);

    public static final DeferredItem<Item> CROWN =
            ITEMS.registerItem(
                    "fog_crown",
                    p -> new Item(
                            p.humanoidArmor(
                                    ArmorMaterials.NETHERITE,
                                    ArmorType.HELMET
                            ).stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> CHESTPLATE =
            ITEMS.registerItem(
                    "fog_chestplate",
                    p -> new Item(
                            p.humanoidArmor(
                                    ArmorMaterials.NETHERITE,
                                    ArmorType.CHESTPLATE
                            ).stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> LEGGINGS =
            ITEMS.registerItem(
                    "fog_leggings",
                    p -> new Item(
                            p.humanoidArmor(
                                    ArmorMaterials.NETHERITE,
                                    ArmorType.LEGGINGS
                            ).stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> BOOTS =
            ITEMS.registerItem(
                    "fog_boots",
                    p -> new Item(
                            p.humanoidArmor(
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
