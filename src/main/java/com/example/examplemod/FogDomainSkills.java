package com.example.examplemod;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogDomainSkills {

    private static final double RADIUS = 30.0D;
    private static final double SKILL_RANGE = 25.0D;
    private static final int MAX_ENSLAVED = 5;

    private static final String COMPANION_TAG = "fog_domain_companion";
    private static final String TEMP_TAG = "fog_skill_temp";

    private static final Map<UUID, Integer> SELECTED_SKILL =
            new HashMap<>();

    private static final Map<UUID, List<Mob>> ENSLAVED =
            new HashMap<>();

    private static final Map<UUID, LivingEntity> ORDERS =
            new HashMap<>();

    private static final Map<UUID, Integer> DOMAIN_SECONDS =
            new HashMap<>();

    private static final Map<UUID, ArmorStand> COMPANIONS =
            new HashMap<>();

    // Dùng UUID thay cho getTags() để nhận diện Armor Stand.
    private static final Set<UUID> COMPANION_STAND_IDS =
            new HashSet<>();

    private static final Set<UUID> TEMP_STAND_IDS =
            new HashSet<>();

    private static final List<BeamTask> BEAM_TASKS =
            new ArrayList<>();

    private static final List<CageTask> CAGE_TASKS =
            new ArrayList<>();

    private static int tickCounter = 0;
    private static boolean wasEnabled = false;

    private FogDomainSkills() {
    }

    private static boolean isController(Player player) {
        return player.getName().getString()
                .equalsIgnoreCase("tubi2531");
    }

    public static boolean isEnslaved(Mob mob) {
        for (List<Mob> list : ENSLAVED.values()) {
            for (Mob slave : list) {
                if (slave == mob) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isEnslavedBy(
            UUID owner, LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return false;
        }

        List<Mob> list = ENSLAVED.get(owner);
        return list != null && list.contains(mob);
    }

    // ---------------- COMMANDS ----------------

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("fogskill")
                        .then(Commands.literal("cycle")
                                .executes(context ->
                                        cycleSkill(context.getSource())))
                        .then(Commands.literal("cast")
                                .executes(context ->
                                        castSkill(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context ->
                                        showStatus(context.getSource())))
        );
    }

    private static ServerPlayer getController(
            CommandSourceStack source)
            throws CommandSyntaxException {

        ServerPlayer player = source.getPlayerOrException();

        if (!isController(player)) {
            source.sendFailure(Component.literal(
                    "Chi tubi2531 moi su dung duoc ky nang."
            ));
            return null;
        }

        return player;
    }

    private static int cycleSkill(
            CommandSourceStack source)
            throws CommandSyntaxException {

        ServerPlayer player = getController(source);
        if (player == null) {
            return 0;
        }

        int current = SELECTED_SKILL.getOrDefault(
                player.getUUID(), 1);

        int next = current % 3 + 1;
        SELECTED_SKILL.put(player.getUUID(), next);

        String name = switch (next) {
            case 1 -> "Skill 1 - Tia nang luong";
            case 2 -> "Skill 2 - Lao giam 10 giay";
            default -> "Skill 3 - No dich";
        };

        source.sendSuccess(
                () -> Component.literal("Ky nang da chon: " + name),
                false
        );

        return 1;
    }

    private static int showStatus(
            CommandSourceStack source)
            throws CommandSyntaxException {

        ServerPlayer player = getController(source);
        if (player == null) {
            return 0;
        }

        int skill = SELECTED_SKILL.getOrDefault(
                player.getUUID(), 1);

        List<Mob> slaves = ENSLAVED.getOrDefault(
                player.getUUID(), new ArrayList<>());

        source.sendSuccess(
                () -> Component.literal(
                        "Ky nang dang chon: " + skill
                                + " | Sinh vat no dich: "
                                + slaves.size() + "/" + MAX_ENSLAVED
                ),
                false
        );

        return 1;
    }

    private static int castSkill(
            CommandSourceStack source)
            throws CommandSyntaxException {

        ServerPlayer player = getController(source);
        if (player == null) {
            return 0;
        }

        if (!MobWarCommands.enabled) {
            source.sendFailure(Component.literal(
                    "Hay bat linh vuc bang /fogmobwar on truoc."
            ));
            return 0;
        }

        int skill = SELECTED_SKILL.getOrDefault(
                player.getUUID(), 1);

        LivingEntity target = findLookTarget(player, SKILL_RANGE);

        if (skill == 3) {
            if (!(target instanceof Mob mob)
                    || mob instanceof Warden
                    || !mob.isAlive()) {
                source.sendFailure(Component.literal(
                        "Hay nham vao mot sinh vat co the no dich."
                ));
                return 0;
            }

            List<Mob> slaves = ENSLAVED.computeIfAbsent(
                    player.getUUID(), ignored -> new ArrayList<>());

            slaves.removeIf(old -> old == null
                    || old.isRemoved()
                    || !old.isAlive());

            if (slaves.contains(mob)) {
                source.sendFailure(Component.literal(
                        "Sinh vat nay da bi no dich."
                ));
                return 0;
            }

            if (slaves.size() >= MAX_ENSLAVED) {
                source.sendFailure(Component.literal(
                        "Da dat gioi han 5 sinh vat no dich."
                ));
                return 0;
            }

            slaves.add(mob);
            mob.setTarget(null);

            source.sendSuccess(
                    () -> Component.literal(
                            "No dich thanh cong! "
                                    + slaves.size() + "/5"
                    ),
                    false
            );

            return 1;
        }

        if (target == null) {
            source.sendFailure(Component.literal(
                    "Hay nham vao mot sinh vat trong tam 25 block."
            ));
            return 0;
        }

        if (skill == 1) {
            startEnergyBeam(player, target);
            source.sendSuccess(
                    () -> Component.literal(
                            "Skill 1: Tia nang luong!"
                    ),
                    false
            );
            return 1;
        }

        startCage(player, target);
        source.sendSuccess(
                () -> Component.literal(
                        "Skill 2: Lao giam da kich hoat!"
                ),
                false
        );
        return 1;
    }

    private static LivingEntity findLookTarget(
            ServerPlayer player, double range) {

        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        AABB box = player.getBoundingBox().inflate(range);

        LivingEntity best = null;
        double bestProjection = Double.MAX_VALUE;

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                candidate -> candidate != player
                        && candidate.isAlive()
                        && !(candidate instanceof ArmorStand)
                        && !(candidate instanceof Player p
                                && isController(p)))) {

            Vec3 center = entity.position().add(
                    0.0D, entity.getBbHeight() * 0.5D, 0.0D);

            Vec3 difference = center.subtract(eye);
            double projection = difference.dot(look);

            if (projection <= 0.0D || projection > range) {
                continue;
            }

            Vec3 side = difference.subtract(
                    look.scale(projection));

            if (side.lengthSqr() > 2.25D) {
                continue;
            }

            if (!player.hasLineOfSight(entity)) {
                continue;
            }

            if (projection < bestProjection) {
                bestProjection = projection;
                best = entity;
            }
        }

        return best;
    }

    // ---------------- ARMOR STANDS ----------------

    private static ArmorStand createStand(
            ServerLevel level,
            double x,
            double y,
            double z,
            String tag,
            boolean diamondArmor,
            boolean netheriteArmor,
            float yaw) {

        ArmorStand stand =
                net.minecraft.world.entity.EntityType.ARMOR_STAND
                        .create(level, EntitySpawnReason.COMMAND);

        if (stand == null) {
            return null;
        }

        stand.setPos(x, y, z);
        stand.setYRot(yaw);
        stand.setXRot(0.0F);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setShowArms(true);
        stand.addTag(tag);

        if (COMPANION_TAG.equals(tag)) {
            COMPANION_STAND_IDS.add(stand.getUUID());
        } else if (TEMP_TAG.equals(tag)) {
            TEMP_STAND_IDS.add(stand.getUUID());
        }

        if (diamondArmor) {
            stand.setItemSlot(EquipmentSlot.HEAD,
                    new ItemStack(Items.DIAMOND_HELMET));
            stand.setItemSlot(EquipmentSlot.CHEST,
                    new ItemStack(Items.DIAMOND_CHESTPLATE));
            stand.setItemSlot(EquipmentSlot.LEGS,
                    new ItemStack(Items.DIAMOND_LEGGINGS));
            stand.setItemSlot(EquipmentSlot.FEET,
                    new ItemStack(Items.DIAMOND_BOOTS));
        }

        if (netheriteArmor) {
            stand.setItemSlot(EquipmentSlot.HEAD,
                    new ItemStack(Items.NETHERITE_HELMET));
            stand.setItemSlot(EquipmentSlot.CHEST,
                    new ItemStack(Items.NETHERITE_CHESTPLATE));
            stand.setItemSlot(EquipmentSlot.LEGS,
                    new ItemStack(Items.NETHERITE_LEGGINGS));
            stand.setItemSlot(EquipmentSlot.FEET,
                    new ItemStack(Items.NETHERITE_BOOTS));
        }

        level.addFreshEntity(stand);
        return stand;
    }

    private static ArmorStand ensureCompanion(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID id = player.getUUID();

        ArmorStand stand = COMPANIONS.get(id);

        if (stand != null
                && (!stand.isAlive() || stand.level() != level)) {
            COMPANION_STAND_IDS.remove(stand.getUUID());
            stand.discard();
            COMPANIONS.remove(id);
            stand = null;
        }

        if (stand == null) {
            List<ArmorStand> found = level.getEntitiesOfClass(
                    ArmorStand.class,
                    player.getBoundingBox().inflate(RADIUS + 10.0D),
                    candidate -> COMPANION_STAND_IDS
                            .contains(candidate.getUUID()));

            if (!found.isEmpty()) {
                stand = found.get(0);

                for (int i = 1; i < found.size(); i++) {
                    ArmorStand extra = found.get(i);
                    COMPANION_STAND_IDS.remove(extra.getUUID());
                    extra.discard();
                }
            } else {
                stand = createStand(
                        level,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        COMPANION_TAG,
                        false,
                        true,
                        player.getYRot()
                );
            }

            if (stand != null) {
                COMPANIONS.put(id, stand);
            }
        }

        if (stand != null) {
            double yaw = Math.toRadians(player.getYRot());
            double x = player.getX() + Math.sin(yaw) * 1.3D;
            double z = player.getZ() - Math.cos(yaw) * 1.3D;

            stand.setPos(x, player.getY(), z);
            stand.setYRot(player.getYRot());
        }

        return stand;
    }

    private static void removeOtherArmorStands(
            ServerLevel level, ServerPlayer player) {

        List<ArmorStand> stands = level.getEntitiesOfClass(
                ArmorStand.class,
                player.getBoundingBox().inflate(RADIUS),
                stand -> player.distanceToSqr(stand)
                        <= RADIUS * RADIUS);

        for (ArmorStand stand : stands) {
            UUID id = stand.getUUID();

            if (COMPANION_STAND_IDS.contains(id)
                    || TEMP_STAND_IDS.contains(id)) {
                continue;
            }

            // Luu y: thao tac nay xoa ca Armor Stand trang tri
            // khong do mod tao ra trong pham vi linh vuc.
            stand.discard();
        }
    }

    // ---------------- SKILL 1: ENERGY BEAM ----------------

    private static void startEnergyBeam(
            ServerPlayer owner, LivingEntity target) {

        ServerLevel level = (ServerLevel) owner.level();
        List<ArmorStand> stands = new ArrayList<>();

        double[][] corners = {
                { 1.7D,  1.7D},
                { 1.7D, -1.7D},
                {-1.7D,  1.7D},
                {-1.7D, -1.7D}
        };

        for (double[] corner : corners) {
            ArmorStand stand = createStand(
                    level,
                    owner.getX() + corner[0],
                    owner.getY(),
                    owner.getZ() + corner[1],
                    TEMP_TAG,
                    true,
                    false,
                    owner.getYRot()
            );

            if (stand != null) {
                stands.add(stand);
            }
        }

        BEAM_TASKS.add(new BeamTask(owner, target, stands));
    }

    private static void drawBeam(
            ServerLevel level, Vec3 start, Vec3 end) {

        Vec3 difference = end.subtract(start);
        double distance = difference.length();

        int steps = Math.max(
                1, Math.min(40, (int) (distance * 2.0D)));

        for (int i = 0; i <= steps; i++) {
            double fraction = i / (double) steps;
            Vec3 point = start.add(difference.scale(fraction));

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    point.x, point.y, point.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D
            );
        }
    }

    private static void tickBeamTasks() {
        Iterator<BeamTask> iterator = BEAM_TASKS.iterator();

        while (iterator.hasNext()) {
            BeamTask task = iterator.next();
            task.age++;

            ServerPlayer owner = task.owner;
            LivingEntity target = task.target;

            if (owner == null || !owner.isAlive()
                    || target == null || !target.isAlive()
                    || owner.level() != target.level()
                    || !MobWarCommands.enabled) {
                removeStands(task.stands);
                iterator.remove();
                continue;
            }

            if (task.age >= 10 && task.age <= 17
                    && task.age % 2 == 0) {

                ArmorStand companion = COMPANIONS.get(
                        owner.getUUID());

                Vec3 start = companion != null
                        ? companion.position().add(0.0D, 1.4D, 0.0D)
                        : owner.getEyePosition();

                Vec3 end = target.position().add(
                        0.0D, target.getBbHeight() * 0.5D, 0.0D);

                drawBeam((ServerLevel) owner.level(), start, end);
            }

            if (task.age >= 20) {
                removeStands(task.stands);

                if (target.isAlive()) {
                    target.hurt(
                            owner.damageSources().playerAttack(owner),
                            14.0F
                    );
                }

                iterator.remove();
            }
        }
    }

    // ---------------- SKILL 2: 10-SECOND CAGE ----------------

    private static void startCage(
            ServerPlayer owner, LivingEntity target) {

        ServerLevel level = (ServerLevel) owner.level();
        List<ArmorStand> stands = new ArrayList<>();

        double[][] corners = {
                { 1.5D,  1.5D},
                { 1.5D, -1.5D},
                {-1.5D,  1.5D},
                {-1.5D, -1.5D}
        };

        for (double[] corner : corners) {
            ArmorStand stand = createStand(
                    level,
                    target.getX() + corner[0],
                    target.getY(),
                    target.getZ() + corner[1],
                    TEMP_TAG,
                    false,
                    false,
                    0.0F
            );

            if (stand != null) {
                stands.add(stand);
            }
        }

        CAGE_TASKS.add(new CageTask(owner, target, stands));
    }

    private static void tickCageTasks() {
        Iterator<CageTask> iterator = CAGE_TASKS.iterator();

        while (iterator.hasNext()) {
            CageTask task = iterator.next();
            task.age++;

            ServerPlayer owner = task.owner;
            LivingEntity target = task.target;

            if (owner == null || !owner.isAlive()
                    || target == null || !target.isAlive()
                    || owner.level() != target.level()
                    || !MobWarCommands.enabled) {
                removeStands(task.stands);
                iterator.remove();
                continue;
            }

            if (task.age % 20 == 0 && task.age < 200) {
                target.hurt(
                        owner.damageSources().playerAttack(owner),
                        2.0F
                );

                ServerLevel level = (ServerLevel) owner.level();

                level.sendParticles(
                        ParticleTypes.SMOKE,
                        target.getX(), target.getY() + 1.0D,
                        target.getZ(),
                        8, 0.4D, 0.5D, 0.4D, 0.02D
                );

                level.sendParticles(
                        ParticleTypes.WHITE_ASH,
                        target.getX(), target.getY() + 1.0D,
                        target.getZ(),
                        8, 0.4D, 0.5D, 0.4D, 0.02D
                );
            }

            if (task.age >= 200) {
                if (target.isAlive()) {
                    target.hurt(
                            owner.damageSources().playerAttack(owner),
                            target.getHealth() + 100.0F
                    );
                }

                removeStands(task.stands);
                iterator.remove();
            }
        }
    }

    private static void removeStands(List<ArmorStand> stands) {
        for (ArmorStand stand : stands) {
            if (stand != null) {
                UUID id = stand.getUUID();
                COMPANION_STAND_IDS.remove(id);
                TEMP_STAND_IDS.remove(id);

                if (stand.isAlive()) {
                    stand.discard();
                }
            }
        }

        stands.clear();
    }

    // ---------------- FIELD PARTICLES AND FIRE ----------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        if (!MobWarCommands.enabled) {
            if (wasEnabled) {
                clearDomainVisuals(server);
            }

            wasEnabled = false;
            tickEnslaved(server);
            return;
        }

        wasEnabled = true;
        tickCounter++;

        Set<UUID> insideNow = new HashSet<>();

        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (!isController(player) || !player.isAlive()) {
                    continue;
                }

                if (tickCounter % 5 == 0) {
                    ensureCompanion(player);
                }

                if (tickCounter % 20 == 0) {
                    removeOtherArmorStands(level, player);
                    tickDomainSeconds(level, player, insideNow);
                }

                if (tickCounter % 10 == 0) {
                    spawnDomainParticles(level, player);
                }
            }
        }

        if (tickCounter % 20 == 0) {
            DOMAIN_SECONDS.keySet().retainAll(insideNow);
        }

        tickBeamTasks();
        tickCageTasks();

        if (tickCounter % 10 == 0) {
            tickEnslaved(server);
        }
    }

    private static void tickDomainSeconds(
            ServerLevel level,
            ServerPlayer owner,
            Set<UUID> insideNow) {

        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                owner.getBoundingBox().inflate(RADIUS),
                entity -> entity.isAlive()
                        && owner.distanceToSqr(entity)
                                <= RADIUS * RADIUS);

        for (LivingEntity entity : entities) {
            if (entity instanceof Player player
                    && isController(player)) {
                continue;
            }

            insideNow.add(entity.getUUID());

            int seconds = DOMAIN_SECONDS.getOrDefault(
                    entity.getUUID(), 0) + 1;

            DOMAIN_SECONDS.put(entity.getUUID(), seconds);

            if (seconds >= 10) {
                entity.igniteForSeconds(3.0F);
            }
        }
    }

    private static void spawnDomainParticles(
            ServerLevel level, ServerPlayer owner) {

        for (int i = 0; i < 6; i++) {
            double angle = level.getRandom().nextDouble()
                    * Math.PI * 2.0D;

            double radius = Math.sqrt(
                    level.getRandom().nextDouble()) * RADIUS;

            double x = owner.getX() + Math.cos(angle) * radius;
            double z = owner.getZ() + Math.sin(angle) * radius;
            double y = owner.getY() + 0.5D
                    + level.getRandom().nextDouble() * 5.0D;

            level.sendParticles(
                    ParticleTypes.FLAME,
                    x, y, z,
                    1, 0.08D, 0.1D, 0.08D, 0.01D
            );
        }

        for (int i = 0; i < 7; i++) {
            double angle = level.getRandom().nextDouble()
                    * Math.PI * 2.0D;

            double radius = Math.sqrt(
                    level.getRandom().nextDouble()) * RADIUS;

            double x = owner.getX() + Math.cos(angle) * radius;
            double z = owner.getZ() + Math.sin(angle) * radius;
            double y = owner.getY() + 8.0D
                    + level.getRandom().nextDouble() * 8.0D;

            level.sendParticles(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    1, 0.1D, 0.1D, 0.1D, 0.015D
            );

            level.sendParticles(
                    ParticleTypes.WHITE_ASH,
                    x, y, z,
                    1, 0.1D, 0.1D, 0.1D, 0.015D
            );
        }
    }

    private static void clearDomainVisuals(MinecraftServer server) {
        for (ArmorStand stand :
                new ArrayList<>(COMPANIONS.values())) {
            if (stand != null) {
                COMPANION_STAND_IDS.remove(stand.getUUID());

                if (stand.isAlive()) {
                    stand.discard();
                }
            }
        }

        COMPANIONS.clear();

        for (BeamTask task : BEAM_TASKS) {
            removeStands(task.stands);
        }
        BEAM_TASKS.clear();

        for (CageTask task : CAGE_TASKS) {
            removeStands(task.stands);
        }
        CAGE_TASKS.clear();

        for (ServerLevel level : server.getAllLevels()) {
            List<ArmorStand> tagged = level.getEntitiesOfClass(
                    ArmorStand.class,
                    new AABB(
                            -3.0E7D, level.getMinY(), -3.0E7D,
                            3.0E7D, level.getMaxY(), 3.0E7D),
                    stand -> COMPANION_STAND_IDS
                            .contains(stand.getUUID())
                            || TEMP_STAND_IDS
                            .contains(stand.getUUID()));

            for (ArmorStand stand : tagged) {
                stand.discard();
            }
        }

        COMPANION_STAND_IDS.clear();
        TEMP_STAND_IDS.clear();
        DOMAIN_SECONDS.clear();
    }

    // ---------------- ENSLAVED MOBS ----------------

    @SubscribeEvent
    public static void enslavedProtectOwner(
            LivingIncomingDamageEvent event) {

        Entity source = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();

        if (source instanceof ServerPlayer owner
                && isController(owner)
                && victim != owner
                && !isEnslavedBy(owner.getUUID(), victim)) {

            ORDERS.put(owner.getUUID(), victim);
            orderSlaves(owner, victim);
            return;
        }

        if (victim instanceof ServerPlayer owner
                && isController(owner)
                && source instanceof LivingEntity attacker
                && attacker != owner
                && !isEnslavedBy(owner.getUUID(), attacker)) {

            ORDERS.put(owner.getUUID(), attacker);
            orderSlaves(owner, attacker);
        }
    }

    private static void orderSlaves(
            ServerPlayer owner, LivingEntity target) {

        List<Mob> slaves = ENSLAVED.get(owner.getUUID());

        if (slaves == null) {
            return;
        }

        for (Mob slave : slaves) {
            if (slave == null || !slave.isAlive()
                    || slave == target
                    || slave.level() != owner.level()) {
                continue;
            }

            slave.setTarget(target);
        }
    }

    private static void tickEnslaved(MinecraftServer server) {
        for (Map.Entry<UUID, List<Mob>> entry
                : ENSLAVED.entrySet()) {

            UUID ownerId = entry.getKey();
            List<Mob> slaves = entry.getValue();

            slaves.removeIf(mob -> mob == null
                    || mob.isRemoved()
                    || !mob.isAlive());

            ServerPlayer owner = server.getPlayerList()
                    .getPlayer(ownerId);

            if (owner == null) {
                continue;
            }

            LivingEntity order = ORDERS.get(ownerId);

            if (order != null
                    && (!order.isAlive()
                    || order.level() != owner.level()
                    || order == owner
                    || isEnslavedBy(ownerId, order))) {
                ORDERS.remove(ownerId);
                order = null;
            }

            for (Mob slave : slaves) {
                if (slave.level() != owner.level()) {
                    continue;
                }

                if (order != null && order != slave) {
                    slave.setTarget(order);
                } else if (slave.getTarget() == owner) {
                    slave.setTarget(null);
                }

                if (slave instanceof PathfinderMob pathfinder
                        && slave.getTarget() == null) {

                    double distance = slave.distanceToSqr(owner);

                    if (distance > 16.0D) {
                        pathfinder.getNavigation()
                                .moveTo(owner, 1.1D);
                    } else if (distance < 6.0D) {
                        pathfinder.getNavigation().stop();
                    }
                }
            }
        }
    }

    // ---------------- TASK DATA ----------------

    private static final class BeamTask {
        private final ServerPlayer owner;
        private final LivingEntity target;
        private final List<ArmorStand> stands;
        private int age = 0;

        private BeamTask(
                ServerPlayer owner,
                LivingEntity target,
                List<ArmorStand> stands) {
            this.owner = owner;
            this.target = target;
            this.stands = stands;
        }
    }

    private static final class CageTask {
        private final ServerPlayer owner;
        private final LivingEntity target;
        private final List<ArmorStand> stands;
        private int age = 0;

        private CageTask(
                ServerPlayer owner,
                LivingEntity target,
                List<ArmorStand> stands) {
            this.owner = owner;
            this.target = target;
            this.stands = stands;
        }
    }
    }
