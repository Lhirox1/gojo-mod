package com.gojo.mod.ability;

import com.gojo.mod.GojoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-side logic for all Gojo abilities. */
@Mod.EventBusSubscriber(modid = GojoMod.MODID)
public final class AbilityManager {

    public static final int BLUE = 0;
    public static final int RED = 1;
    public static final int PURPLE = 2;
    public static final int DOMAIN = 3;

    /** Cooldowns in ticks (20 ticks = 1 second). */
    private static final int[] COOLDOWN = {100, 80, 300, 1200};
    private static final double DOMAIN_RADIUS = 15.0;
    private static final int DOMAIN_DURATION = 200;

    private static long ticks = 0;
    private static final Map<UUID, long[]> READY = new HashMap<>();
    private static final List<Orb> ORBS = new ArrayList<>();
    private static final List<Domain> DOMAINS = new ArrayList<>();

    private AbilityManager() {}

    // ------------------------------------------------------------------ input

    public static void use(ServerPlayer p, int id) {
        if (id < 0 || id > 3) return;

        long[] ready = READY.computeIfAbsent(p.getUUID(), k -> new long[4]);
        if (ticks < ready[id]) {
            long secs = (ready[id] - ticks) / 20 + 1;
            p.displayClientMessage(Component.literal("Cooldown: " + secs + "s"), true);
            return;
        }

        switch (id) {
            case BLUE -> blue(p);
            case RED -> red(p);
            case PURPLE -> purple(p);
            case DOMAIN -> domain(p);
            default -> { }
        }
        ready[id] = ticks + COOLDOWN[id];
    }

    // --------------------------------------------------------------- abilities

    /** Blue: pulls nearby entities toward the caster. */
    private static void blue(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 center = p.getEyePosition().add(p.getLookAngle().scale(8));
        AABB box = new AABB(center, center).inflate(7);
        Vec3 origin = p.position();

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != p && x.isAlive())) {
            Vec3 pull = origin.subtract(e.position());
            if (pull.length() < 0.5) continue;
            e.hurt(p.damageSources().playerAttack(p), 4f);
            e.setDeltaMovement(pull.normalize().scale(1.6).add(0, 0.3, 0));
            e.hurtMarked = true;
        }

        level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z,
                60, 2, 2, 2, 0.5);
        level.playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.5f);
    }

    /** Red: blasts entities in front of the caster away. */
    private static void red(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 look = p.getLookAngle();
        AABB box = p.getBoundingBox().expandTowards(look.scale(6)).inflate(2);

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != p && x.isAlive())) {
            Vec3 push = e.position().subtract(p.position());
            if (push.length() < 0.01) push = look;
            e.hurt(p.damageSources().playerAttack(p), 8f);
            e.setDeltaMovement(push.normalize().scale(2.2).add(0, 0.5, 0));
            e.hurtMarked = true;
        }

        Vec3 blast = p.getEyePosition().add(look.scale(3));
        level.sendParticles(ParticleTypes.EXPLOSION, blast.x, blast.y, blast.z,
                3, 1, 1, 1, 0.1);
        level.playSound(null, blast.x, blast.y, blast.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1f, 1.2f);
    }

    /** Hollow Purple: a slow orb that flies forward and damages what it passes. */
    private static void purple(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 look = p.getLookAngle().normalize();
        Vec3 start = p.getEyePosition().add(look.scale(1.5));
        ORBS.add(new Orb(level, p.getUUID(), start, look));

        level.playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.6f);
    }

    /** Domain Expansion: a 10 second zone that blinds and confuses enemies. */
    private static void domain(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        DOMAINS.add(new Domain(level, p.position(), p.getUUID(), ticks + DOMAIN_DURATION));

        p.displayClientMessage(Component.literal("Domain Expansion: Infinite Void"), true);
        level.playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.5f);
    }

    // ----------------------------------------------------------------- ticking

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        tickOrbs();
        tickDomains();
    }

    private static void tickOrbs() {
        Iterator<Orb> it = ORBS.iterator();
        while (it.hasNext()) {
            Orb o = it.next();
            o.pos = o.pos.add(o.dir.scale(1.2));
            o.age++;

            boolean blocked = o.level.getBlockState(BlockPos.containing(o.pos)).blocksMotion();
            if (blocked || o.age > 80) {
                o.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        o.pos.x, o.pos.y, o.pos.z, 1, 0, 0, 0, 0);
                o.level.playSound(null, o.pos.x, o.pos.y, o.pos.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2f, 0.8f);
                it.remove();
                continue;
            }

            AABB box = new AABB(o.pos, o.pos).inflate(2.5);
            for (LivingEntity e : o.level.getEntitiesOfClass(LivingEntity.class, box,
                    x -> x.isAlive() && !x.getUUID().equals(o.owner) && !o.hit.contains(x.getUUID()))) {
                o.hit.add(e.getUUID());
                e.hurt(o.level.damageSources().magic(), 20f);
            }

            o.level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    o.pos.x, o.pos.y, o.pos.z, 15, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private static void tickDomains() {
        Iterator<Domain> it = DOMAINS.iterator();
        while (it.hasNext()) {
            Domain d = it.next();
            if (ticks >= d.end) {
                it.remove();
                continue;
            }
            if (ticks % 20 != 0) continue;

            AABB box = new AABB(d.center, d.center).inflate(DOMAIN_RADIUS);
            for (LivingEntity e : d.level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (e.getUUID().equals(d.owner)) {
                    e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
                } else {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60));
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60));
                }
            }

            d.level.sendParticles(ParticleTypes.PORTAL,
                    d.center.x, d.center.y + 1, d.center.z,
                    80, DOMAIN_RADIUS / 2, 2, DOMAIN_RADIUS / 2, 0.2);
        }
    }

    // ----------------------------------------------------------------- state

    private static final class Orb {
        final ServerLevel level;
        final UUID owner;
        final Vec3 dir;
        final Set<UUID> hit = new HashSet<>();
        Vec3 pos;
        int age;

        Orb(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir) {
            this.level = level;
            this.owner = owner;
            this.pos = pos;
            this.dir = dir;
        }
    }

    private record Domain(ServerLevel level, Vec3 center, UUID owner, long end) {}
}
