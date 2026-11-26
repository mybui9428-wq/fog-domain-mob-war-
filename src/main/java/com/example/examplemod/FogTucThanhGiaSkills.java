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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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
    private static final Map<UUID, UUID> truthTargets = new HashMap<>();
    private static final Map<UUID, Long> truthEyeUntil = new HashMap<>();
    private static final Map<UUID, UUID> deathSealTargets = new HashMap<>();
    private static final Set<UUID> deathSealSelf = new HashSet<>();
    private static final Identifier BLOOD_ATTACK = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_blood_attack");
    private static final Identifier BLOOD_SPEED = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_blood_speed");
    private static final Identifier BLOOD_ARMOR = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_blood_armor");
    private static final Identifier BODY_HEALTH = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "tuc_body_health");

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
                ServerLevel level = (ServerLevel)p.level();
                Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
                AABB box = p.getBoundingBox().inflate(32);
                List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != p && e.isAlive() && e.getBoundingBox().inflate(0.2).clip(eye, eye.add(look.scale(32))).isPresent());
                for (LivingEntity e : victims) {
                    e.igniteForSeconds(5);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), e.getY()+e.getBbHeight()/2, e.getZ(), 12, .4,.5,.4,.02);
                }
                setCooldown(p, id, now + 200);
                p.sendSystemMessage(Component.literal("§6Thiên Hỏa thiêu đốt " + victims.size() + " mục tiêu."));
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
                    LivingEntity target = lookTarget(p, 25);
                    if (target == null) { p.sendSystemMessage(Component.literal("§cHãy ngắm mục tiêu để giải phóng trảm kích.")); return 0; }
                    slashCharges.remove(p.getUUID());
                    releaseSlashes(p, target, stored);
                    return 1;
                }
                int count = slashCount(lv);
                int cost = count * 10;
                if (!d.consumeSpiritPoints(p.getUUID(), cost)) return needSpirit(p, cost);
                slashCharges.put(p.getUUID(), count);
                p.sendSystemMessage(Component.literal("§eĐã ngưng tụ " + count + " trảm kích. Dùng /fogskill cast lần nữa khi ngắm mục tiêu để giải phóng."));
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
                LivingEntity target = lookTarget(p, 25);
                if (target == null) { p.sendSystemMessage(Component.literal("§cHãy ngắm mục tiêu.")); return 0; }
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20*8, 8, false, true, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20*8, 2, false, true, true));
                target.setDeltaMovement(Vec3.ZERO); target.hurtMarked = true;
                setCooldown(p, id, now+20L*30);
                ((ServerLevel)p.level()).sendParticles(ParticleTypes.CRIT, target.getX(),target.getY()+1,target.getZ(),35,.8,.8,.8,.03);
                p.sendSystemMessage(Component.literal("§5Ngục Tỏa Tù Long đã giam cầm mục tiêu.")); return 1;
            }
            case "huyen_tinh_chu_the" -> {
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20*15, Math.min(4, lv-1), false, true, true));
                setCooldown(p, id, now+20L*30);
                p.sendSystemMessage(Component.literal("§bHuyền Tinh Chú Thể: tăng chống chịu trong 15 giây.")); return 1;
            }
            case "sinh_tu_luan_chuyen" -> {
                float cost = Math.max(1.0F, p.getMaxHealth()*0.01F);
                if (p.getHealth() <= cost) { p.sendSystemMessage(Component.literal("§cKhông đủ máu để ngưng luyện ấn ký.")); return 0; }
                p.setHealth(p.getHealth()-cost);
                LivingEntity target = lookTarget(p, 25);
                if (target == null) {
                    deathSealSelf.add(p.getUUID()); deathSealTicks.put(p.getUUID(), 20*20);
                    p.sendSystemMessage(Component.literal("§dTử Vong Sinh Cơ Ẩn Phục bao phủ cơ thể bạn."));
                } else {
                    deathSealTargets.put(p.getUUID(), target.getUUID()); deathSealTicks.put(p.getUUID(), 20*15);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20*15, Math.min(4,lv-1), false,true,true));
                    p.sendSystemMessage(Component.literal("§dSinh Tử Ấn Ký đã khắc lên mục tiêu."));
                }
                setCooldown(p, id, now+20L*60); return 1;
            }
            case "cuop_doat" -> {
                LivingEntity target = lookTarget(p, 25);
                if (target == null) { p.sendSystemMessage(Component.literal("§cHãy ngắm một sinh vật.")); return 0; }
                String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
                int unlocked = d.unlockCreatureSkills(p.getUUID(), type);
                p.sendSystemMessage(Component.literal("§dCướp Đoạt: nhận " + unlocked + " kỹ năng từ " + type)); return 1;
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
    private static LivingEntity lookTarget(ServerPlayer p, double range) {
        Vec3 a=p.getEyePosition(), b=a.add(p.getLookAngle().scale(range));
        AABB box=p.getBoundingBox().expandTowards(p.getLookAngle().scale(range)).inflate(1.2);
        return p.level().getEntitiesOfClass(LivingEntity.class,box,e->e!=p && e.isAlive() && e.getBoundingBox().inflate(.25).clip(a,b).isPresent())
                .stream().min(Comparator.comparingDouble(e->e.distanceToSqr(p))).orElse(null);
    }
    private static void releaseSlashes(ServerPlayer p, LivingEntity target, int count) {
        int applied=Math.min(count,100);
        for(int i=0;i<applied && target.isAlive();i++) target.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),2.0F);
        ((ServerLevel)p.level()).sendParticles(ParticleTypes.SWEEP_ATTACK,target.getX(),target.getY()+1,target.getZ(),Math.min(40,applied),.5,.5,.5,.05);
        p.sendSystemMessage(Component.literal("§fVô Vật Bất Trảm giải phóng " + applied + " trảm kích."));
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("tucskill")
                .then(Commands.literal("cycle").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException(); if(!cycle(p)){p.sendSystemMessage(Component.literal("§cBạn chưa thức tỉnh Túc Thanh Giả."));return 0;} return 1;}))
                .then(Commands.literal("cast").executes(c->cast(c.getSource().getPlayerOrException())))
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
            if(deathSealSelf.contains(p.getUUID()) && p.getHealth()<=p.getMaxHealth()*.10F) {
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH,100,1,false,true,true));
                p.addEffect(new MobEffectInstance(MobEffects.SPEED,100,1,false,true,true));
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,100,1,false,true,true));
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
            if (ad.getSkillLevel(attacker.getUUID(), "tich_diet_chi_hoa") > 0) {
                event.getEntity().igniteForSeconds(5);
                int gain = Math.max(1, (int)Math.ceil(event.getNewDamage() * 0.5F));
                ad.addSpiritPoints(attacker.getUUID(), gain);
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
        int neverDie=d.getSkillLevel(p.getUUID(),"ta_se_khong_chet");
        if(neverDie>0 && p.getHealth()-event.getNewDamage()<=0.0F) {
            float keep=(float)(p.getMaxHealth()*Math.min(0.95, neverDie*0.05));
            event.setNewDamage(Math.max(0.0F,p.getHealth()-Math.max(1.0F,keep)));
        }
    }

    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer deadPlayer) {
            bloodSurgeBonus.remove(deadPlayer.getUUID());
            bloodSurgeUntil.remove(deadPlayer.getUUID());
            frenzyEnabled.remove(deadPlayer.getUUID());
            slashCharges.remove(deadPlayer.getUUID());
        }
        if(!(event.getSource().getEntity() instanceof ServerPlayer p) || !owner(p) || event.getEntity() instanceof ServerPlayer) return;
        FogDomainAbilityData d=data(p);
        int steal=d.getSkillLevel(p.getUUID(),"cuop_doat");
        if(steal>0) {
            String type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
            d.unlockCreatureSkills(p.getUUID(),type);
        }
        if (d.getSkillLevel(p.getUUID(), "cuong_liet") > 0) {
            LivingEntity victim = event.getEntity();
            for (net.minecraft.world.entity.EquipmentSlot slot : List.of(
                    net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET,
                    net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    net.minecraft.world.entity.EquipmentSlot.OFFHAND)) {
                net.minecraft.world.item.ItemStack stack = victim.getItemBySlot(slot);
                if (!stack.isEmpty()) { p.getInventory().add(stack.copy()); stack.setCount(0); break; }
            }
        }
    }
}
