
package com.example.examplemod;

import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainLootDrops {

    private FogDomainLootDrops() {
    }

    private enum CreatureRank {
        WEAK,
        MEDIUM,
        STRONG,
        NONE
    }

    // Các sinh vật trung lập, có thể phản công khi bị khiêu khích.
    private static final Set<String> NEUTRAL_CREATURES = Set.of(
            "minecraft:bee",
            "minecraft:dolphin",
            "minecraft:enderman",
            "minecraft:fox",
            "minecraft:goat",
            "minecraft:iron_golem",
            "minecraft:llama",
            "minecraft:panda",
            "minecraft:piglin",
            "minecraft:polar_bear",
            "minecraft:trader_llama",
            "minecraft:wolf",
            "minecraft:zombified_piglin"
    );

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        CreatureRank rank = getCreatureRank(mob);

        switch (rank) {
            case WEAK -> dropWeakRewards(event, serverLevel, mob);
            case MEDIUM -> dropMediumRewards(event, serverLevel, mob);
            case STRONG -> dropStrongRewards(event, serverLevel, mob);
            case NONE -> {
            }
        }
    }

    private static CreatureRank getCreatureRank(Mob mob) {
        EntityType<?> type = mob.getType();

        // Lấy ID loài từ registry thay vì gọi hằng EntityType.
        String entityId = BuiltInRegistries.ENTITY_TYPE
                .getKey(type)
                .toString();

        // Sinh vật mạnh.
        if (entityId.equals("minecraft:wither")
                || entityId.equals("minecraft:ender_dragon")
                || entityId.equals("minecraft:warden")
                || entityId.equals("minecraft:elder_guardian")) {
            return CreatureRank.STRONG;
        }

        // Sinh vật trung lập không thuộc nhóm yếu hoặc trung bình.
        if (NEUTRAL_CREATURES.contains(entityId)) {
            return CreatureRank.NONE;
        }

        // Sinh vật có AI thù địch.
        if (mob instanceof Enemy) {
            return CreatureRank.MEDIUM;
        }

        // Các Mob còn lại được xếp vào nhóm yếu.
        return CreatureRank.WEAK;
    }

    // Sinh vật yếu: 10% rơi 1 Tinh Thạch Linh Hồn.
    private static void dropWeakRewards(
            LivingDropsEvent event,
            ServerLevel level,
            Mob mob
    ) {
        if (level.getRandom().nextFloat() < 0.10F) {
            addDrop(
                    event,
                    level,
                    mob,
                    new ItemStack(ExampleMod.SOUL_CRYSTAL.get())
            );
        }
    }

    // Sinh vật trung bình:
    // 50% rơi 1 Tinh Thạch Linh Hồn.
    // 10% rơi 1 Mảnh Vỡ Quy Tắc.
    private static void dropMediumRewards(
            LivingDropsEvent event,
            ServerLevel level,
            Mob mob
    ) {
        if (level.getRandom().nextFloat() < 0.50F) {
            addDrop(
                    event,
                    level,
                    mob,
                    new ItemStack(ExampleMod.SOUL_CRYSTAL.get())
            );
        }

        if (level.getRandom().nextFloat() < 0.10F) {
            addDrop(
                    event,
                    level,
                    mob,
                    new ItemStack(ExampleMod.RULE_FRAGMENT.get())
            );
        }
    }

    // Sinh vật mạnh:
    // Chắc chắn rơi 3-5 Tinh Thạch Linh Hồn.
    // Chắc chắn rơi 1-2 Mảnh Vỡ Quy Tắc.
    private static void dropStrongRewards(
            LivingDropsEvent event,
            ServerLevel level,
            Mob mob
    ) {
        int crystalCount = 3 + level.getRandom().nextInt(3);
        int fragmentCount = 1 + level.getRandom().nextInt(2);

        addDrop(
                event,
                level,
                mob,
                new ItemStack(
                        ExampleMod.SOUL_CRYSTAL.get(),
                        crystalCount
                )
        );

        addDrop(
                event,
                level,
                mob,
                new ItemStack(
                        ExampleMod.RULE_FRAGMENT.get(),
                        fragmentCount
                )
        );
    }

    private static void addDrop(
            LivingDropsEvent event,
            ServerLevel level,
            Mob mob,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEntity itemEntity = new ItemEntity(
                level,
                mob.getX(),
                mob.getY() + 0.2D,
                mob.getZ(),
                stack
        );

        event.getDrops().add(itemEntity);
    }
}
