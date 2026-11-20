
package com.example.examplemod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    public static final String MODID = "examplemod";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK =
            BLOCKS.registerSimpleBlock(
                    "example_block",
                    p -> p.mapColor(MapColor.STONE)
            );

    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "example_block",
                    EXAMPLE_BLOCK
            );

    public static final DeferredItem<Item> EXAMPLE_ITEM =
            ITEMS.registerSimpleItem(
                    "example_item",
                    p -> p.food(
                            new FoodProperties.Builder()
                                    .alwaysEdible()
                                    .nutrition(1)
                                    .saturationModifier(2f)
                                    .build()
                    )
            );

    // Vật phẩm mới: Tinh Thạch Linh Hồn.
    public static final DeferredItem<Item> SOUL_CRYSTAL =
            ITEMS.registerSimpleItem("soul_crystal");

    // Vật phẩm mới: Mảnh Vỡ Quy Tắc.
    public static final DeferredItem<Item> RULE_FRAGMENT =
            ITEMS.registerSimpleItem("rule_fragment");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB =
            CREATIVE_MODE_TABS.register(
                    "example_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.examplemod"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> SOUL_CRYSTAL.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(EXAMPLE_ITEM.get());
                                output.accept(SOUL_CRYSTAL.get());
                                output.accept(RULE_FRAGMENT.get());
                                output.accept(FogArmorItems.CROWN.get());
                                output.accept(FogArmorItems.CHESTPLATE.get());
                                output.accept(FogArmorItems.LEGGINGS.get());
                                output.accept(FogArmorItems.BOOTS.get());
                                output.accept(FogWeaponItems.SWORD.get());
                                output.accept(FogWeaponItems.SPEAR.get());
                                output.accept(FogWeaponItems.LONG_SPEAR.get());
                                output.accept(FogWeaponItems.SHIELD.get());
                                output.accept(FogWeaponItems.BOW.get());
                            })
                            .build()
            );

    public ExampleMod(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        modEventBus.addListener(this::commonSetup);

        // Đăng ký payload nhập hồn.
        modEventBus.addListener(FogPossessionPayload::register);
        modEventBus.addListener(FogPossessionControlPayload::register);
        modEventBus.addListener(FogPossessionSyncPayload::register);

        // Đăng ký payload HUD.
        modEventBus.addListener(FogHudPayload::register);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        FogArmorItems.register(modEventBus);
        FogWeaponItems.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        FogDomainEntities.register(modEventBus);
        modEventBus.addListener(this::createEntityAttributes);
    
        NeoForge.EVENT_BUS.register(this);

        // Đăng ký lệnh nhập hồn.
        NeoForge.EVENT_BUS.addListener(
                FogPossessionCommands::register
        );

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );
    }
    
     private void createEntityAttributes(EntityAttributeCreationEvent event) {

    // Linh Hồn Băng
    event.put(
            FogDomainEntities.ICE_SPIRIT.get(),
            Mob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, 30.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.30D)
                    .add(Attributes.ATTACK_DAMAGE, 6.0D)
                    .add(Attributes.FOLLOW_RANGE, 32.0D)
                    .build()
    );

    // Thây Ma Băng
    event.put(
            FogDomainEntities.ICE_ZOMBIE.get(),
            Mob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, 50.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.28D)
                    .add(Attributes.ATTACK_DAMAGE, 8.0D)
                    .add(Attributes.FOLLOW_RANGE, 32.0D)
                    .build()
    );

    // Bộ Xương Nhiều Tay
    event.put(
            FogDomainEntities.MULTI_ARMED_SKELETON.get(),
            Mob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, 40.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.30D)
                    .add(Attributes.ATTACK_DAMAGE, 7.0D)
                    .add(Attributes.FOLLOW_RANGE, 32.0D)
                    .build()
    );

    // Thây Ma Linh Hồn
    event.put(
            FogDomainEntities.SOUL_ZOMBIE.get(),
            Mob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, 20.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.32D)
                    .add(Attributes.ATTACK_DAMAGE, 4.0D)
                    .add(Attributes.FOLLOW_RANGE, 32.0D)
                    .build()
    );
}
    
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info(
                    "DIRT BLOCK >> {}",
                    BuiltInRegistries.BLOCK.getKey(Blocks.DIRT)
            );
        }

        LOGGER.info(
                "{}{}",
                Config.MAGIC_NUMBER_INTRODUCTION.get(),
                Config.MAGIC_NUMBER.getAsInt()
        );

        Config.ITEM_STRINGS.get().forEach(
                item -> LOGGER.info("ITEM >> {}", item)
        );
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    // Áp dụng máu tối đa theo Thể lực.
    private void applyPlayerStaminaHealth(ServerPlayer player) {
        if (player == null) return;

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        FogDomainAbilityData data =
                FogDomainAbilityData.get(serverLevel.getServer());

        data.initializeCharacter(player.getUUID());
        data.applyStaminaMaxHealth(player);
    }

    // Khi người chơi đăng nhập.
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyPlayerStaminaHealth(player);
        }
    }

    // Khi người chơi hồi sinh.
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyPlayerStaminaHealth(player);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
