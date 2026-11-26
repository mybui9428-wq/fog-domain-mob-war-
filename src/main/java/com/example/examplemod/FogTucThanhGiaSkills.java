package com.example.examplemod;

import java.util.*;

import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Runtime effects and per-player selector for Túc Thanh Giả skills. */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class FogTucThanhGiaSkills {
    private static final List<List<String>> GROUPS = List.of(
            List.of("long_nhot", "song_am", "anh_bo", "tam_linh_ho_the"),
            List.of("thien_hoa", "chan_thuc_chi_nhan"),
            List.of("vo_vat_bat_tram", "bat_huu_chien", "nguoc_toa_tu_long", "huyen_tinh_chu_the", "sinh_tu_luan_chuyen"),
            List.of("cuop_doat"),
            List.of("ket_hop")
    );
    private static final String[] GROUP_NAMES = {"Kỹ năng thường", "Động Sát Chi Nhãn", "Kỹ năng độc quyền", "Cướp Đoạt", "Kết Hợp"};
    private static final Map<UUID, Integer> groupIndex = new HashMap<>();
    private static final Map<UUID, Integer> skillIndex = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private static final Map<UUID, Long> bloodSurgeUntil = new HashMap<>();
    private static final Map<UUID, Double> bloodSurgeBonus = new HashMap<>();
    private static final Set<UUID> frenzyEnabled = new HashSet<>();
    private static final Map<UUID, Integer> slashCharges = new HashMap<>();
    private static final Map<UUID, Integer> deathSealTicks = new HashMap<>();
    private static final Map<UUID, DeathMark> deathMarks = new HashMap<>();
    private static final Map<UUID, PrisonZone> prisonZones = new HashMap<>();
    private static final Map<UUID, BurningMark> unquenchableBurns = new HashMap<>();
    private static final Map<UUID, List<String>> stealCandidates = new HashMap<>();
    private static final Map<UUID, UUID> stealTargets = new HashMap<>();
    private static final Set<UUID> slashImbued = new HashSet<>();
    private static final Map<UUID, UUID> truthTargets = new HashMap<>();
    private static final Map<UUID, Long> truthEyeUntil = new HashMap<>();
    private static final Map<UUID, UUID> deathSealTargets = new HashMap<>();
    private static final Set<UUID> deathSealSelf = new HashSet<>();
    private static final Identifier BLOOD_ATTACK = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_blood_attack");
    private static final Identifier BLOOD_SPEED = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_blood_speed");
    private static final Identifier BLOOD_ARMOR = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_blood_armor");
    private static final Identifier BODY_HEALTH = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_body_health");
    private static final Identifier DEATH_FORM_ARMOR = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_death_form_armor");
    private static final Identifier DEATH_FORM_ARMOR_SURGE = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_death_form_armor_surge");
    private static final Identifier DEATH_FORM_ATTACK = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_death_form_attack");
    private static final Identifier DEATH_FORM_SPEED = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_death_form_speed");
    private static final Identifier DEATH_FORM_HEALTH = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_death_form_health");

    private record DeathMark(UUID target, long expiresAt, float initialHealth, int level) {}
    private record PrisonZone(ServerLevel level, Vec3 center, double radius, double halfHeight, long expiresAt) {}
    private record BurningMark(LivingEntity entity, long expiresAt) {}

    private FogTucThanhGiaSkills() {}

    private static boolean owner(ServerPlayer p) {
        FogDomainAbilityData d = FogDomainAbilityData.get(p.level().getServer());
        boolean isOwner = d.isTucThanhGia(p.getUUID());
        if (isOwner) d.initializeTucThanhGiaSkills(p.getUUID());
        return isOwner;
    }
    private static FogDomainAbilityData data(ServerPlayer p) {
        return FogDomainAbilityData.get(p.level().getServer());
    }
    private static int level(ServerPlayer p, String id) {
        return Math.max(1, data(p).getSkillLevel(p.getUUID(), id));
    }
    private static String selected(ServerPlayer p) {
        int g = groupIndex.getOrDefault(p.getUUID(), 0);
        int s = skillIndex.getOrDefault(p.getUUID(), -1);
        return s < 0 ? "" : GROUPS.get(g).get(Math.floorMod(s, GROUPS.get(g).size()));
    }
    public static String selectedName(ServerPlayer p) {
        if (!owner(p)) return FogDomainSkills.getSelectedSkillName();
        int g = groupIndex.getOrDefault(p.getUUID(), 0);
        String skill = selected(p);
        return skill.isEmpty() ? GROUP_NAMES[g] : GROUP_NAMES[g] + " → " + skill;
    }
    public static boolean cycle(ServerPlayer p) {
        if (!owner(p)) return false;
        UUID id = p.getUUID();
        int g = groupIndex.getOrDefault(id, 0);
        if (!skillIndex.containsKey(id)) {
            skillIndex.put(id, -1);
            groupIndex.put(id, 0);
            p.sendSystemMessage(Component.literal("§dĐã chọn nhóm: " + GROUP_NAMES[0] + " §7| dùng cycle để chọn kỹ năng."));
            return true;
        }
        int s = skillIndex.getOrDefault(id, -1);
        if (s < 0) {
            s = 0;
        } else if (s + 1 < GROUPS.get(g).size()) {
            s++;
        } else {
            g = (g + 1) % GROUPS.size();
            s = -1;
        }
        groupIndex.put(id, g);
        skillIndex.put(id, s);
        p.sendSystemMessage(Component.literal("§d" + selectedName(p)));
        return true;
    }

    public static int toggleFrenzy(ServerPlayer p) {
        if (!owner(p)) return 0;
        UUID id = p.getUUID();
        if (!frenzyEnabled.add(id)) frenzyEnabled.remove(id);
        p.sendSystemMessage(Component.literal(frenzyEnabled.contains(id)
                ? "§cCuồng Hóa đã bật: +" + (level(p, "cuong_hoa") * 5) + "% thuộc tính tổng thể."
                : "§7Cuồng Hóa đã tắt."));
        return 1;
    }

    public static int cast(ServerPlayer p) {
        if (!owner(p)) return -1;
        String id = selected(p);
        if (id.isEmpty()) {
            p.sendSystemMessage(Component.literal("§eBạn đang chọn nhóm. Dùng /fogskill cycle để chọn kỹ năng bên trong nhóm."));
            return 0;
        }
        FogDomainAbilityData d = data(p);
        int lv = level(p, id);
        long now = p.level().getGameTime();
        long ready = cooldowns.getOrDefault(p.getUUID(), Map.of()).getOrDefault(id, 0L);
        if (now < ready) {
            p.sendSystemMessage(Component.literal("§cKỹ năng còn hồi chiêu: " + ((ready-now+19)/20) + " giây."));
            return 0;
        }
        switch (id) {
            case "long_nhot", "song_am", "anh_bo", "tam_linh_ho_the" ->
                    { return FogDomainSkills.castLegacy(p, id); }
            case "thien_hoa" -> {
                if (!d.consumeSpiritPoints(p.getUUID(), 10)) return needSpirit(p, 10);
                ServerLevel level = (ServerLevel) p.level();
                List<LivingEntity> victims = visibleTargets(p, 32.0D, 0.68D);
                if (victims.isEmpty()) {
                    d.addSpiritPoints(p.getUUID(), 10);
                    p.sendSystemMessage(Component.literal("§cKhông có sinh vật nào trong tầm nhìn."));
                    return 0;
                }
                long burnUntil = now + 100L;
                for (LivingEntity victim : victims) {
                    unquenchableBurns.put(victim.getUUID(), new BurningMark(victim, burnUntil));
                    victim.igniteForSeconds(5);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), 18, .45, .55, .45, .025);
                }
                setCooldown(p, id, now + 200L);
                p.sendSystemMessage(Component.literal("§6Thiên Hỏa thiêu đốt " + victims.size() + " sinh vật trong 5 giây. Hồi chiêu 10 giây."));
                return 1;
            }
            case "chan_thuc_chi_nhan" -> {
                if (!d.consumeSpiritPoints(p.getUUID(), 5)) return needSpirit(p, 5);
                LivingEntity target = lookTarget(p, 32);
                if (target == null) { d.addSpiritPoints(p.getUUID(), 5); p.sendSystemMessage(Component.literal("§cKhông có mục tiêu trong tầm nhìn.")); return 0; }
                truthTargets.put(p.getUUID(), target.getUUID());
                truthEyeUntil.put(p.getUUID(), now + 200L);
                showTruth(p, target);
                p.sendSystemMessage(Component.literal("§bChân Thực Chi Nhãn duy trì 10 giây."));
                return 1;
            }
            case "vo_vat_bat_tram" -> {
                Integer stored = slashCharges.get(p.getUUID());
                if (stored != null) {
                    LivingEntity target = lookTarget(p, 96.0D);
                    if (target == null) { p.sendSystemMessage(Component.literal("§cHãy ngắm mục tiêu ở tầm xa (tối đa 96 khối) để phóng trảm kích.")); return 0; }
                    slashCharges.remove(p.getUUID());
                    releaseSlashes(p, target, stored, slashImbued.remove(p.getUUID()));
                    return 1;
                }
                int count = slashCount(lv);
                int cost = count * 10;
                if (!d.consumeSpiritPoints(p.getUUID(), cost)) return needSpirit(p, cost);
                slashCharges.put(p.getUUID(), count);
                p.sendSystemMessage(Component.literal("§eĐã ngưng tụ " + count + " đạo trảm kích trong cơ thể. Dùng /fogskill imbue để gắn Tịch Diệt Chi Hỏa (nếu đã mở khóa), rồi dùng /fogskill cast khi ngắm mục tiêu để phóng trảm kích tầm xa."));
                return 1;
            }
            case "bat_huu_chien" -> {
                double hp = p.getHealth()/p.getMaxHealth()*100.0;
                double threshold = Math.min(50, lv*5);
                if (hp > threshold) { p.sendSystemMessage(Component.literal("§cMáu phải ở mức " + threshold + "% hoặc thấp hơn.")); return 0; }
                int cost = Math.max(100, 1100 - lv*100);
                if (!d.consumeSpiritPoints(p.getUUID(), cost)) return needSpirit(p, cost);
                double savedBloodBonus = Math.max(0.0D, 1.0D - p.getHealth()/p.getMaxHealth());
                p.setHealth(p.getMaxHealth());
                bloodSurgeBonus.put(p.getUUID(), savedBloodBonus);
                bloodSurgeUntil.put(p.getUUID(), Long.MAX_VALUE);
                p.sendSystemMessage(Component.literal("§cBất Hưu Chiến! Hồi đầy máu, giữ sức mạnh Huyết Chiến trong 10 giây. Chi phí: " + cost));
                return 1;
            }
            case "nguoc_toa_tu_long" -> {
                ServerLevel level = (ServerLevel) p.level();
                Vec3 center = p.position().add(p.getLookAngle().scale(8.0D));
                prisonZones.put(p.getUUID(), new PrisonZone(level, center, 7.0D, 4.0D, now + 160L));
                setCooldown(p, id, now + 20L * 30L);
                p.sendSystemMessage(Component.literal("§5Ngục Tỏa Tù Long dựng lên một vùng giam cầm bán kính 7 khối, duy trì 8 giây."));
                return 1;
            }
            case "huyen_tinh_chu_the" -> {
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20*15, Math.min(4, lv-1), false, true, true));
                setCooldown(p, id, now+20L*30);
                p.sendSystemMessage(Component.literal("§bHuyền Tinh Chú Thể: tăng chống chịu trong 15 giây.")); return 1;
            }
            case "sinh_tu_luan_chuyen" -> {
                LivingEntity target = lookTarget(p, 32.0D);
                if (target == null) {
                    float cost = Math.max(1.0F, p.getMaxHealth() * 0.01F);
                    if (p.getHealth() <= cost) { p.sendSystemMessage(Component.literal("§cKhông đủ máu để kích hoạt Tử Vong Sinh Cơ Ẩn Phục.")); return 0; }
                    p.setHealth(p.getHealth() - cost);
                    deathSealSelf.add(p.getUUID());
                    deathSealTicks.put(p.getUUID(), 20 * 60);
                    applyModifier(p, Attributes.ARMOR, DEATH_FORM_ARMOR, -0.50D);
                    p.sendSystemMessage(Component.literal("§dTử Vong Sinh Cơ Ẩn Phục: phòng ngự giảm 50%, chống đỡ một đòn chí mạng; khi hấp hối tăng 50% thuộc tính."));
                    setCooldown(p, id, now + 20L * 60L);
                    return 1;
                }
                DeathMark oldMark = deathMarks.get(p.getUUID());
                if (oldMark != null && oldMark.target().equals(target.getUUID()) && now < oldMark.expiresAt()) {
                    deathMarks.remove(p.getUUID());
                    float decayedLife = Math.max(0.0F, oldMark.initialHealth() - target.getHealth());
                    float burst = Math.min(target.getHealth(), Math.max(4.0F, target.getMaxHealth() * (0.20F * lv) + decayedLife));
                    target.hurtServer((ServerLevel) p.level(), p.damageSources().magic(), burst);
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 45, .7, .7, .7, .08);
                    p.sendSystemMessage(Component.literal("§dSinh Chi Ấn Ký kích nổ sinh mệnh bên trong mục tiêu, gây " + (int) burst + " sát thương."));
                    setCooldown(p, id, now + 20L * 60L);
                    return 1;
                }
                deathMarks.put(p.getUUID(), new DeathMark(target.getUUID(), now + 20L * 15L, target.getHealth(), lv));
                p.sendSystemMessage(Component.literal("§8Tử Chi Ấn Ký đã khắc lên mục tiêu. Sinh cơ sẽ suy kiệt trong 15 giây; dùng lại kỹ năng lên chính mục tiêu để kích hoạt Sinh Chi Ấn Ký."));
                return 1;
            }
            case "cuop_doat" -> {
                LivingEntity target = lookTarget(p, 32.0D);
                if (target == null) { p.sendSystemMessage(Component.literal("§cHãy ngắm đối thủ trong tầm 32 khối.")); return 0; }
                List<String> candidates;
                if (target instanceof ServerPlayer targetPlayer) {
                    FogDomainAbilityData targetData = FogDomainAbilityData.get(targetPlayer.level().getServer());
                    candidates = new ArrayList<>(targetData.getCreatureSkills(targetPlayer.getUUID()));
                    for (String innate : targetData.getInnateAbilities(targetPlayer.getUUID())) {
                        if (!candidates.contains(innate)) candidates.add(innate);
                    }
                } else {
                    String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
                    candidates = new ArrayList<>(FogDomainCreatureSkills.getSkills(type));
                }
                if (candidates.isEmpty()) { p.sendSystemMessage(Component.literal("§cĐối thủ không có năng lực có thể cướp.")); return 0; }
                stealCandidates.put(p.getUUID(), List.copyOf(candidates));
                stealTargets.put(p.getUUID(), target.getUUID());
                String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
                PacketDistributor.sendToPlayer(p, new FogCharacterPayload("stealmenu|" + target.getUUID() + "|" + String.join(",", candidates)));
                p.sendSystemMessage(Component.literal("§dChọn chính xác 1 hoặc 2 năng lực trong giao diện Cướp Đoạt."));
                return 1;
            }
            case "ket_hop" -> {
                p.sendSystemMessage(Component.literal("§dKết Hợp cấp " + lv + ": mở " + (lv*2) + " ô kết hợp. Hệ thống ghép vật phẩm sẽ được nối ở phần vật phẩm.")); return 1;
            }
            default -> { return 0; }
        }
    }

    private static void showTruth(ServerPlayer viewer, LivingEntity target) {
        viewer.sendSystemMessage(Component.literal("§b[Chân Thực Chi Nhãn] Máu: " + (int)target.getHealth() + "/" + (int)target.getMaxHealth()));
        viewer.sendSystemMessage(Component.literal("§bLoài: " + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())));
        if (target instanceof ServerPlayer sp) {
            FogDomainAbilityData td = FogDomainAbilityData.get(sp.level().getServer());
            viewer.sendSystemMessage(Component.literal("§bLinh Tính: " + td.getSpiritPoints(sp.getUUID())));
            viewer.sendSystemMessage(Component.literal("§bThuộc tính: " + td.getAttributes(sp.getUUID())));
            viewer.sendSystemMessage(Component.literal("§bNăng lực: " + td.getAbilities(sp.getUUID())));
        } else {
            viewer.sendSystemMessage(Component.literal("§bKỹ năng sinh vật: " + FogDomainCreatureSkills.getSkills(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString())));
        }
    }

    private static void setCooldown(ServerPlayer player, String skillId, long until) {
        cooldowns.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(skillId, until);
    }

    private static int needSpirit(ServerPlayer p, int cost) {
        p.sendSystemMessage(Component.literal("§cKhông đủ Linh Tính. Cần " + cost + ".")); return 0;
    }
    private static int slashCount(int lv) {
        if (lv==1) return 10; if (lv==2) return 20; if (lv==3) return 40; if (lv==4) return 80;
        return 100 + (lv-5)*20;
    }
    private static void releaseSlashes(ServerPlayer p, LivingEntity target, int count, boolean imbued) {
        int applied = Math.max(0, count);
        ServerLevel level = (ServerLevel) p.level();
        for (int i = 0; i < applied && target.isAlive(); i++) {
            float damage = 2.0F;
            if (imbued && data(p).getSkillLevel(p.getUUID(), "tich_diet_chi_hoa") > 0) damage += 2.0F;
            target.hurtServer(level, p.damageSources().playerAttack(p), damage);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), Math.min(50, applied), .7, .7, .7, .08);
        p.sendSystemMessage(Component.literal("§fVô Vật Bất Trảm phóng " + applied + " trảm kích tầm xa" + (imbued ? " kèm Tịch Diệt Chi Hỏa." : ".")));
    }

    private static List<LivingEntity> visibleTargets(ServerPlayer p, double range, double minDot) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle().normalize();
        AABB search = p.getBoundingBox().inflate(range);
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search, x -> x != p && x.isAlive())) {
            Vec3 center = e.getBoundingBox().getCenter();
            Vec3 delta = center.subtract(eye);
            double distance = delta.length();
            if (distance > range || distance < 0.1D || delta.normalize().dot(look) < minDot) continue;
            if (hasClearSight(level, p, eye, center)) result.add(e);
        }
        result.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return result;
    }

    private static boolean hasClearSight(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceTo(end) < 0.75D;
    }

    private static LivingEntity lookTarget(ServerPlayer p, double range) {
        List<LivingEntity> targets = visibleTargets(p, range, 0.96D);
        return targets.isEmpty() ? null : targets.get(0);
    }

    public static void toggleSlashImbue(ServerPlayer p) {
        if (!owner(p)) return;
        if (data(p).getSkillLevel(p.getUUID(), "tich_diet_chi_hoa") <= 0) {
            p.sendSystemMessage(Component.literal("§cBạn chưa mở khóa Tịch Diệt Chi Hỏa."));
            return;
        }
        if (slashCharges.getOrDefault(p.getUUID(), 0) <= 0) {
            p.sendSystemMessage(Component.literal("§cHãy ngưng tụ Vô Vật Bất Trảm trước."));
            return;
        }
        if (!slashImbued.add(p.getUUID())) slashImbued.remove(p.getUUID());
        p.sendSystemMessage(Component.literal(slashImbued.contains(p.getUUID()) ? "§cĐã gắn Tịch Diệt Chi Hỏa lên trảm kích." : "§7Đã tháo Tịch Diệt Chi Hỏa khỏi trảm kích."));
    }

    public static boolean completeSteal(ServerPlayer p, UUID targetId, List<String> selected) {
        if (!owner(p) || targetId == null || selected == null || selected.isEmpty() || selected.size() > 2) return false;
        if (!targetId.equals(stealTargets.get(p.getUUID()))) return false;
        List<String> candidates = stealCandidates.getOrDefault(p.getUUID(), List.of());
        if (selected.stream().anyMatch(id -> !candidates.contains(id))) return false;
        String source = "unknown";
        Entity target = ((ServerLevel) p.level()).getEntity(targetId);
        if (target != null) source = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        int count = 0;
        for (String skill : new LinkedHashSet<>(selected)) if (data(p).addStolenCreatureSkill(p.getUUID(), skill, source)) count++;
        stealCandidates.remove(p.getUUID());
        stealTargets.remove(p.getUUID());
        p.sendSystemMessage(Component.literal("§dCướp Đoạt thành công: nhận " + count + " năng lực."));
        return count > 0;
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("tucskill")
                .then(Commands.literal("cycle").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException(); if(!cycle(p)){p.sendSystemMessage(Component.literal("§cBạn chưa thức tỉnh Túc Thanh Giả."));return 0;} return 1;}))
                .then(Commands.literal("cast").executes(c->cast(c.getSource().getPlayerOrException())))
                .then(Commands.literal("imbue").executes(c->{ toggleSlashImbue(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("cuonghoa").executes(c->{
                    ServerPlayer p=c.getSource().getPlayerOrException(); if(!owner(p)) return 0;
                    if(!frenzyEnabled.add(p.getUUID())) frenzyEnabled.remove(p.getUUID());
                    p.sendSystemMessage(Component.literal(frenzyEnabled.contains(p.getUUID()) ? "§cCuồng Hóa đã bật." : "§7Cuồng Hóa đã tắt.")); return 1;
                })));
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        long now=event.getServer().overworld().getGameTime();
        for(ServerPlayer p:event.getServer().getPlayerList().getPlayers()) {
            if(!owner(p)) continue;
            FogDomainAbilityData d=data(p);
            Iterator<Map.Entry<UUID, BurningMark>> burnIterator = unquenchableBurns.entrySet().iterator();
            while (burnIterator.hasNext()) {
                BurningMark mark = burnIterator.next().getValue();
                if (!mark.entity().isAlive() || mark.entity().isRemoved() || now >= mark.expiresAt()) {
                    burnIterator.remove();
                } else {
                    mark.entity().igniteForSeconds(1);
                }
            }
            PrisonZone zone = prisonZones.get(p.getUUID());
            if (zone != null) {
                if (now >= zone.expiresAt() || zone.level() != p.level()) {
                    prisonZones.remove(p.getUUID());
                } else {
                    AABB area = new AABB(zone.center().x - zone.radius(), zone.center().y - zone.halfHeight(), zone.center().z - zone.radius(), zone.center().x + zone.radius(), zone.center().y + zone.halfHeight(), zone.center().z + zone.radius());
                    for (LivingEntity victim : zone.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != p && e.isAlive())) {
                        double dx = victim.getX() - zone.center().x, dz = victim.getZ() - zone.center().z;
                        double radial = Math.sqrt(dx * dx + dz * dz);
                        if (radial > zone.radius() - 0.5D) {
                            Vec3 inward = new Vec3(zone.center().x - victim.getX(), 0.0D, zone.center().z - victim.getZ()).normalize().scale(0.45D);
                            victim.setDeltaMovement(inward.x, Math.min(0.0D, victim.getDeltaMovement().y), inward.z);
                            victim.hurtMarked = true;
                        } else {
                            victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.15D, 0.0D, 0.15D));
                            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 6, false, false, true));
                            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5, 1, false, false, true));
                        }
                    }
                    if (now % 5L == 0L) {
                        for (int i = 0; i < 32; i++) {
                            double angle = Math.PI * 2.0D * i / 32.0D;
                            zone.level().sendParticles(ParticleTypes.END_ROD, zone.center().x + Math.cos(angle) * zone.radius(), zone.center().y, zone.center().z + Math.sin(angle) * zone.radius(), 1, 0, 0.2, 0, 0.0D);
                        }
                    }
                }
            }
            DeathMark mark = deathMarks.get(p.getUUID());
            if (mark != null) {
                Entity marked = ((ServerLevel) p.level()).getEntity(mark.target());
                if (now >= mark.expiresAt() || !(marked instanceof LivingEntity living) || !living.isAlive()) {
                    deathMarks.remove(p.getUUID());
                } else if (now % 20L == 0L) {
                    living.hurtServer((ServerLevel) p.level(), p.damageSources().magic(), Math.max(1.0F, living.getMaxHealth() * 0.01F * mark.level()));
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.SOUL, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(), 5, .25, .35, .25, .02);
                }
            }
            int bloodLv=d.getSkillLevel(p.getUUID(),"huyet_chien");
            double lost = bloodLv > 0 ? Math.max(0.0D, 1.0D-p.getHealth()/p.getMaxHealth()) : 0.0D;
            lost = Math.max(lost, bloodSurgeBonus.getOrDefault(p.getUUID(), 0.0D));
            applyModifier(p, Attributes.ATTACK_DAMAGE, BLOOD_ATTACK, lost);
            applyModifier(p, Attributes.MOVEMENT_SPEED, BLOOD_SPEED, lost);
            applyModifier(p, Attributes.ARMOR, BLOOD_ARMOR, lost);
            int bodyLv=d.getSkillLevel(p.getUUID(),"than_cuong");
            applyModifier(p, Attributes.MAX_HEALTH, BODY_HEALTH, bodyLv > 0 ? 0.30D : 0.0D);
            if (bodyLv > 0 && p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
            if(p.getHealth()<p.getMaxHealth()*.90F) {
                int lv=d.getSkillLevel(p.getUUID(),"than_cuong");
                if(lv>0) { /* Incoming-damage cap is enforced in damage event below. */ }
            }
            Long eyeUntil = truthEyeUntil.get(p.getUUID());
            if (eyeUntil != null) {
                if (now >= eyeUntil) { truthEyeUntil.remove(p.getUUID()); truthTargets.remove(p.getUUID()); }
                else if (now % 20 == 0) {
                    Entity seen = ((ServerLevel)p.level()).getEntity(truthTargets.get(p.getUUID()));
                    if (seen instanceof LivingEntity living && living.isAlive()) showTruth(p, living);
                }
            }
            Integer ticks=deathSealTicks.get(p.getUUID());
            if(ticks!=null) { if(ticks<=1){deathSealTicks.remove(p.getUUID());deathSealTargets.remove(p.getUUID());deathSealSelf.remove(p.getUUID());} else deathSealTicks.put(p.getUUID(),ticks-1); }
            if (deathSealSelf.contains(p.getUUID())) {
                boolean nearDeath = p.getHealth() <= p.getMaxHealth() * 0.10F;
                applyModifier(p, Attributes.ATTACK_DAMAGE, DEATH_FORM_ATTACK, nearDeath ? 0.50D : 0.0D);
                applyModifier(p, Attributes.MOVEMENT_SPEED, DEATH_FORM_SPEED, nearDeath ? 0.50D : 0.0D);
                applyModifier(p, Attributes.ARMOR, DEATH_FORM_ARMOR, -0.50D);
                applyModifier(p, Attributes.ARMOR, DEATH_FORM_ARMOR_SURGE, nearDeath ? 0.50D : 0.0D);
                applyModifier(p, Attributes.MAX_HEALTH, DEATH_FORM_HEALTH, nearDeath ? 0.50D : 0.0D);
            } else {
                applyModifier(p, Attributes.ATTACK_DAMAGE, DEATH_FORM_ATTACK, 0.0D);
                applyModifier(p, Attributes.MOVEMENT_SPEED, DEATH_FORM_SPEED, 0.0D);
                applyModifier(p, Attributes.ARMOR, DEATH_FORM_ARMOR, 0.0D);
                applyModifier(p, Attributes.ARMOR, DEATH_FORM_ARMOR_SURGE, 0.0D);
                applyModifier(p, Attributes.MAX_HEALTH, DEATH_FORM_HEALTH, 0.0D);
            }
            if (d.getSkillLevel(p.getUUID(),"cam_tuyet_khong_che") > 0) {
                p.removeEffect(MobEffects.SLOWNESS);
                p.removeEffect(MobEffects.WEAKNESS);
                p.removeEffect(MobEffects.BLINDNESS);
                p.removeEffect(MobEffects.POISON);
                p.removeEffect(MobEffects.WITHER);
                p.removeEffect(MobEffects.LEVITATION);
            }
            int regen=d.getSkillLevel(p.getUUID(),"hoi_thanh");
            if(regen>0 && now%20==0 && p.getHealth()<p.getMaxHealth()) p.heal(5.0F*regen);
            int frenzy=d.getSkillLevel(p.getUUID(),"cuong_hoa");
            double frenzyBonus = frenzyEnabled.contains(p.getUUID()) ? (frenzy * 0.05D) : 0.0D;
            applyModifier(p, Attributes.ATTACK_DAMAGE, Identifier.fromNamespaceAndPath(ExampleMod.MODID,"tuc_frenzy_attack"), frenzyBonus);
            applyModifier(p, Attributes.MOVEMENT_SPEED, Identifier.fromNamespaceAndPath(ExampleMod.MODID,"tuc_frenzy_speed"), frenzyBonus);
            applyModifier(p, Attributes.ARMOR, Identifier.fromNamespaceAndPath(ExampleMod.MODID,"tuc_frenzy_armor"), frenzyBonus);
            applyModifier(p, Attributes.MAX_HEALTH, Identifier.fromNamespaceAndPath(ExampleMod.MODID,"tuc_frenzy_health"), frenzyBonus);
        }
    }

    private static void applyModifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, Identifier id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (amount <= 0.0D) instance.removeModifier(id);
        else instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    @SubscribeEvent public static void incoming(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && owner(attacker)
                && attacker != event.getEntity()) {
            FogDomainAbilityData ad = data(attacker);
            int executeLevel = ad.getSkillLevel(attacker.getUUID(), "tuyet_menh_nhat_dao");
            if (executeLevel > 0 && event.getEntity().getHealth() / event.getEntity().getMaxHealth() * 100.0F <= Math.min(100, executeLevel * 5)) {
                event.setNewDamage(Math.max(event.getNewDamage(), event.getEntity().getHealth()));
            }
            int tichDietLevel = ad.getSkillLevel(attacker.getUUID(), "tich_diet_chi_hoa");
            if (tichDietLevel > 0) {
                LivingEntity victim = event.getEntity();
                victim.igniteForSeconds(5);
                event.setNewDamage(event.getNewDamage() + Math.max(1.0F, tichDietLevel * 0.5F));
                if (victim instanceof ServerPlayer victimPlayer) {
                    FogDomainAbilityData victimData = FogDomainAbilityData.get(victimPlayer.level().getServer());
                    int drain = Math.min(victimData.getSpiritPoints(victimPlayer.getUUID()), Math.max(1, tichDietLevel * 2));
                    if (drain > 0 && victimData.consumeSpiritPoints(victimPlayer.getUUID(), drain)) ad.addSpiritPoints(attacker.getUUID(), drain);
                } else {
                    ad.addSpiritPoints(attacker.getUUID(), Math.max(1, tichDietLevel));
                }
            }
        }
        if(!(event.getEntity() instanceof ServerPlayer p) || !owner(p)) return;
        FogDomainAbilityData d=data(p);
        int body=d.getSkillLevel(p.getUUID(),"than_cuong");
        if(body>0 && p.getHealth()<p.getMaxHealth()*.90F) event.setNewDamage(Math.min(event.getNewDamage(),p.getMaxHealth()*.01F));
        int counter=d.getSkillLevel(p.getUUID(),"phan_sat_thuong");
        if(counter>0 && event.getSource().getEntity() instanceof LivingEntity attacker) {
            int own=d.getAttributes(p.getUUID()).values().stream().mapToInt(Integer::intValue).sum();
            int enemy=attacker instanceof ServerPlayer sp ? FogDomainAbilityData.get(sp.level().getServer()).getAttributes(sp.getUUID()).values().stream().mapToInt(Integer::intValue).sum() : 0;
            if(enemy<own) attacker.hurtServer((ServerLevel)p.level(),p.damageSources().thorns(p),event.getNewDamage()*Math.min(1.0F,counter*.05F));
        }
        int unbreak=d.getSkillLevel(p.getUUID(),"bat_khuat");
        if(unbreak>0) {
            double threshold=Math.max(0.0,50.0-(unbreak-1)*5.0);
            if(p.getHealth()/p.getMaxHealth()*100.0 < threshold) {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, Math.min(4, unbreak-1), false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, Math.min(4, unbreak-1), false, false, true));
            }
        }
        if (deathSealSelf.contains(p.getUUID()) && p.getHealth() - event.getNewDamage() <= 0.0F) {
            event.setNewDamage(Math.max(0.0F, p.getHealth() - 1.0F));
            deathSealSelf.remove(p.getUUID());
            deathSealTicks.remove(p.getUUID());
            p.sendSystemMessage(Component.literal("§dTử Vong Sinh Cơ Ẩn Phục đã chống đỡ một đòn chí mạng!"));
        }
        int neverDie=d.getSkillLevel(p.getUUID(),"ta_se_khong_chet");
        if(neverDie>0 && p.getHealth()-event.getNewDamage()<=0.0F) {
            float keep=(float)(p.getMaxHealth()*Math.min(0.95, neverDie*0.05));
            event.setNewDamage(Math.max(0.0F,p.getHealth()-Math.max(1.0F,keep)));
        }
    }

    @SubscribeEvent public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer defender) || !owner(defender)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == defender) return;
        int skillLevel = data(defender).getSkillLevel(defender.getUUID(), "cuong_liet");
        if (skillLevel <= 0 || attacker.getHealth() > attacker.getMaxHealth() * 0.50F) return;
        int chance = Math.min(90, 10 + skillLevel * 10);
        if (defender.getRandom().nextInt(100) < chance) {
            event.setAmount(0.0F);
            stealRandomEquipment(defender, attacker);
            defender.sendSystemMessage(Component.literal("§6Cường Liệt né đòn và cướp vật phẩm của kẻ địch!"));
        }
    }

    @SubscribeEvent public static void onShieldBlock(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event) {
        if (!(event.getDamageSource().getEntity() instanceof ServerPlayer attacker) || !owner(attacker)) return;
        LivingEntity blocker = event.getEntity();
        if (blocker == attacker || blocker.getHealth() > blocker.getMaxHealth() * 0.50F) return;
        int level = data(attacker).getSkillLevel(attacker.getUUID(), "cuong_liet");
        if (level <= 0 || attacker.getRandom().nextInt(100) >= Math.min(90, 15 + level * 10)) return;
        stealRandomEquipment(attacker, blocker);
    }

    private static void stealRandomEquipment(ServerPlayer player, LivingEntity victim) {
        List<net.minecraft.world.entity.EquipmentSlot> slots = new ArrayList<>(List.of(
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET,
                net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                net.minecraft.world.entity.EquipmentSlot.OFFHAND));
        Collections.shuffle(slots);
        for (net.minecraft.world.entity.EquipmentSlot slot : slots) {
            net.minecraft.world.item.ItemStack stack = victim.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.item.ItemStack stolen = stack.copy();
                stack.setCount(0);
                if (!player.getInventory().add(stolen)) player.drop(stolen, false);
                player.sendSystemMessage(Component.literal("§6Cường Liệt cướp được: " + stolen.getHoverName().getString()));
                return;
            }
        }
    }

    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer deadPlayer) {
            bloodSurgeBonus.remove(deadPlayer.getUUID());
            bloodSurgeUntil.remove(deadPlayer.getUUID());
            frenzyEnabled.remove(deadPlayer.getUUID());
            slashCharges.remove(deadPlayer.getUUID());
            slashImbued.remove(deadPlayer.getUUID());
            prisonZones.remove(deadPlayer.getUUID());
            deathMarks.remove(deadPlayer.getUUID());
            applyModifier(deadPlayer, Attributes.ARMOR, DEATH_FORM_ARMOR, 0.0D);
            applyModifier(deadPlayer, Attributes.ARMOR, DEATH_FORM_ARMOR_SURGE, 0.0D);
            applyModifier(deadPlayer, Attributes.ATTACK_DAMAGE, DEATH_FORM_ATTACK, 0.0D);
            applyModifier(deadPlayer, Attributes.MOVEMENT_SPEED, DEATH_FORM_SPEED, 0.0D);
            applyModifier(deadPlayer, Attributes.MAX_HEALTH, DEATH_FORM_HEALTH, 0.0D);
        }
        if(!(event.getSource().getEntity() instanceof ServerPlayer p) || !owner(p) || event.getEntity() instanceof ServerPlayer) return;
        FogDomainAbilityData d=data(p);
        int steal=d.getSkillLevel(p.getUUID(),"cuop_doat");
        if(steal>0) {
            String type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
            d.stealAllCreatureAbilities(p.getUUID(), event.getEntity().getUUID(), type);
        }
        int fierceLevel = d.getSkillLevel(p.getUUID(), "cuong_liet");
        if (fierceLevel > 0) {
            if (p.getRandom().nextInt(100) < Math.min(100, 60 + fierceLevel * 5)) {
                stealRandomEquipment(p, event.getEntity());
            }
            if (p.getRandom().nextInt(100) < Math.min(95, 70 + fierceLevel * 3)) {
                String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
                List<String> stolenSkills = d.stealRandomCreatureAbilities(p.getUUID(), type);
                if (!stolenSkills.isEmpty()) p.sendSystemMessage(Component.literal("§6Cường Liệt cướp được năng lực: " + String.join(", ", stolenSkills)));
            }
        }
    }
}
