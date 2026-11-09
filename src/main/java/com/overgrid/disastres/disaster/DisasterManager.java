package com.overgrid.disastres.disaster;

import com.overgrid.disastres.DisasterNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Controla os desastres de um mundo. Cada tick de servidor aplica o efeito do mecanismo.
 * Blocos que se movem viram {@link FallingBlockEntity} para parecer físicos.
 */
public final class DisasterManager {
    private static final WeakHashMap<ServerWorld, DisasterManager> MANAGERS = new WeakHashMap<>();
    /** Altura da onda do tsunami, em blocos de água. */
    private static final int WAVE_HEIGHT = 8;
    /** Velocidade da frente da onda, em blocos por tick. */
    private static final double WAVE_SPEED = 0.8;
    /** Altura de onde a bomba nuclear é solta. */
    private static final int BOMB_DROP_HEIGHT = 80;

    private final ServerWorld world;
    private final List<ActiveDisaster> active = new ArrayList<>();
    private final Random random;

    private DisasterManager(ServerWorld world) {
        this.world = world;
        this.random = Random.create(world.getRandom().nextLong());
    }

    public static DisasterManager get(ServerWorld world) {
        return MANAGERS.computeIfAbsent(world, DisasterManager::new);
    }

    public void start(DisasterPreset preset, BlockPos center) {
        start(preset, center, null);
    }

    /** dir: para o tsunami, direção horizontal da onda; nos outros, null. */
    public void start(DisasterPreset preset, BlockPos center, Vec3d dir) {
        ActiveDisaster d = new ActiveDisaster(preset, center, dir);
        active.add(d);
        DisasterNetworking.broadcast(world, new DisasterNetworking.DisasterStartPayload(
                preset.id(), center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                preset.intensity(), preset.durationTicks()));
    }

    public void stopAll() {
        for (ActiveDisaster d : active) {
            cleanup(d);
        }
        active.clear();
    }

    public void tick() {
        if (active.isEmpty()) return;
        active.removeIf(d -> {
            d.phase += 0.15;
            d.age++;
            applyEffects(d);
            d.ticksLeft--;
            if (d.isOver()) {
                cleanup(d);
                return true;
            }
            return false;
        });
    }

    private void applyEffects(ActiveDisaster d) {
        switch (d.type) {
            case TORNADO -> tornado(d);
            case EARTHQUAKE -> earthquake(d);
            case WIND_STORM -> windStorm(d);
            case TSUNAMI -> tsunami(d);
            case RADIATION -> {
                if (d.age == 1) world.setWeather(0, d.ticksLeft, true, true);
                acidRain(d);
                radiation(d, false);
            }
            case NUCLEAR -> nuclear(d);
            case BLACK_HOLE -> blackHole(d);
        }
    }

    private void tornado(ActiveDisaster d) {
        double r = d.radius * d.intensity;
        int samples = (int) (12 * d.intensity);
        for (int i = 0; i < samples; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * r;
            BlockPos p = surface(d.center.add((int) (Math.cos(ang) * dist), 0, (int) (Math.sin(ang) * dist)));
            BlockState st = world.getBlockState(p);
            if (!isMovable(p, st)) continue;
            double swirl = d.phase + dist * 0.4;
            double vx = -Math.sin(swirl) * 0.6 * d.intensity;
            double vz = Math.cos(swirl) * 0.6 * d.intensity;
            launch(p, st, new Vec3d(vx, 0.9 * d.intensity, vz));
        }
    }

    private void earthquake(ActiveDisaster d) {
        double r = d.radius * d.intensity;
        int samples = (int) (10 * d.intensity);
        for (int i = 0; i < samples; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * r;
            BlockPos p = surface(d.center.add((int) (Math.cos(ang) * dist), 0, (int) (Math.sin(ang) * dist)));
            BlockState st = world.getBlockState(p);
            if (!isMovable(p, st)) continue;
            double sx = Math.sin(d.phase * 3 + i) * 0.25 * d.intensity;
            double sz = Math.cos(d.phase * 3 + i) * 0.25 * d.intensity;
            launch(p, st, new Vec3d(sx, 0.2 * d.intensity, sz));
        }
    }

    private void windStorm(ActiveDisaster d) {
        double r = d.radius * d.intensity;
        int samples = (int) (20 * d.intensity);
        double windAngle = Math.sin(d.phase * 0.2) * Math.PI;
        double wx = Math.cos(windAngle);
        double wz = Math.sin(windAngle);
        for (int i = 0; i < samples; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * r;
            BlockPos p = surface(d.center.add((int) (Math.cos(ang) * dist), 0, (int) (Math.sin(ang) * dist)));
            BlockState st = world.getBlockState(p);
            if (!isMovable(p, st)) continue;
            double gust = 0.5 + random.nextDouble() * 0.5;
            launch(p, st, new Vec3d(wx * 0.7 * gust * d.intensity, 0.15, wz * 0.7 * gust * d.intensity));
        }
    }

    /**
     * Onda gigante: a frente é uma linha perpendicular à direção, que avança pela terra.
     * Atrás da frente fica uma parede de água, e os blocos e jogadores na frente são arrastados.
     */
    private void tsunami(ActiveDisaster d) {
        if (d.dir == null) return;
        double front = d.age * WAVE_SPEED;
        double halfWidth = d.radius * d.intensity;
        Vec3d perp = new Vec3d(-d.dir.z, 0, d.dir.x);
        Vec3d origin = d.centerPos();

        int samples = 80;
        for (int i = 0; i < samples; i++) {
            double s = (random.nextDouble() * 2 - 1) * halfWidth;
            double f = front - random.nextDouble() * 4.0;
            Vec3d pt = origin.add(d.dir.multiply(f)).add(perp.multiply(s));
            BlockPos base = surface(BlockPos.ofFloored(pt));
            BlockState st = world.getBlockState(base);
            if (isMovable(base, st)) {
                launch(base, st, d.dir.multiply(0.9 * d.intensity).add(0, 0.35 * d.intensity, 0));
            }
            for (int h = 1; h <= WAVE_HEIGHT; h++) {
                BlockPos w = base.up(h);
                if (world.getBlockState(w).isAir() && d.placedWater.size() < 3000) {
                    world.setBlockState(w, Blocks.WATER.getDefaultState());
                    d.placedWater.add(w);
                }
            }
        }

        // jogadores que estão na frente da onda são empurrados para a terra
        for (ServerPlayerEntity player : world.getPlayers()) {
            Vec3d rel = player.getEntityPos().subtract(origin);
            double along = rel.dotProduct(d.dir);
            double across = Math.abs(rel.dotProduct(perp));
            if (along >= front - 4.0 && along <= front && across <= halfWidth) {
                player.addVelocity(d.dir.x * 1.1, 0.4, d.dir.z * 1.1);
            }
        }
    }

    /** Chuva ácida: remove folhas e plantas dentro da área, deixando troncos e terra. */
    private void acidRain(ActiveDisaster d) {
        double r = d.radius * d.intensity;
        for (int i = 0; i < 25; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * r;
            BlockPos p = surface(d.center.add((int) (Math.cos(ang) * dist), 0, (int) (Math.sin(ang) * dist)));
            for (int h = 0; h <= 12; h++) {
                BlockPos q = p.up(h);
                BlockState s = world.getBlockState(q);
                if (s.isIn(BlockTags.LEAVES) || s.isOf(Blocks.SHORT_GRASS) || s.isOf(Blocks.TALL_GRASS)
                        || s.isOf(Blocks.FERN) || s.isOf(Blocks.LARGE_FERN)) {
                    world.setBlockState(q, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    /** Zona de radiação: envenena jogadores próximos a cada 20 ticks. Mais forte na explosão nuclear. */
    private void radiation(ActiveDisaster d, boolean nuclear) {
        if (d.age % 20 != 0) return;
        double r = d.radius * d.intensity;
        Vec3d c = d.centerPos();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getEntityPos().distanceTo(c) > r) continue;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
            if (nuclear) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0));
            }
        }
    }

    /** Bomba cai do céu; ao cair, explode, cega os jogadores e deixa a zona de radiação. */
    private void nuclear(ActiveDisaster d) {
        if (!d.started) {
            d.started = true;
            BlockPos drop = d.center.up(BOMB_DROP_HEIGHT);
            d.bombPos = drop;
            d.bomb = FallingBlockEntity.spawnFromBlock(world, drop, Blocks.TNT.getDefaultState());
            if (d.bomb == null) {
                d.exploded = true;
                detonate(d, d.center);
            }
            return;
        }
        if (!d.exploded) {
            if (d.bomb != null && !d.bomb.isRemoved()) {
                d.bombPos = d.bomb.getBlockPos();
                return;
            }
            d.exploded = true;
            detonate(d, d.bombPos != null ? d.bombPos : d.center);
            return;
        }
        radiation(d, true);
    }

    private void detonate(ActiveDisaster d, BlockPos at) {
        world.setBlockState(at, Blocks.AIR.getDefaultState());
        world.createExplosion(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
                6.0f * d.intensity, World.ExplosionSourceType.TNT);
        Vec3d c = Vec3d.ofCenter(at);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getEntityPos().distanceTo(c) <= 120) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0));
            }
        }
    }

    /** Vórtice: puxa blocos e entidades para o centro e apaga o núcleo. */
    private void blackHole(ActiveDisaster d) {
        double r = d.radius * d.intensity;
        Vec3d c = d.centerPos();

        int samples = (int) (16 * d.intensity);
        for (int i = 0; i < samples; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * r;
            BlockPos p = surface(d.center.add((int) (Math.cos(ang) * dist), 0, (int) (Math.sin(ang) * dist)));
            BlockState st = world.getBlockState(p);
            if (!isMovable(p, st)) continue;
            if (dist < 2.5) {
                world.setBlockState(p, Blocks.AIR.getDefaultState());
            } else {
                double dx = -Math.cos(ang);
                double dz = -Math.sin(ang);
                launch(p, st, new Vec3d(dx * 0.6 * d.intensity - dz * 0.3, 0.3, dz * 0.6 * d.intensity + dx * 0.3));
            }
        }

        Box box = new Box(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
        for (Entity e : world.getOtherEntities(null, box)) {
            Vec3d pull = c.subtract(e.getEntityPos());
            if (pull.lengthSquared() < 1e-6) continue;
            Vec3d n = pull.normalize().multiply(0.12 * d.intensity);
            e.addVelocity(n.x, n.y, n.z);
        }
    }

    /** Desce pela coluna até achar o primeiro bloco não-ar. */
    private BlockPos surface(BlockPos p) {
        BlockPos.Mutable m = new BlockPos.Mutable(p.getX(), world.getTopYInclusive(), p.getZ());
        while (m.getY() > world.getBottomY() && world.getBlockState(m).isAir()) {
            m.move(0, -1, 0);
        }
        return m.toImmutable();
    }

    /** Só move blocos frágeis: nada de bedrock, obsidiana ou blocos inquebráveis. */
    private boolean isMovable(BlockPos p, BlockState st) {
        if (st.isAir()) return false;
        if (st.isOf(Blocks.BEDROCK) || st.isOf(Blocks.OBSIDIAN)) return false;
        float hardness = st.getHardness(world, p);
        return hardness >= 0 && hardness < 3.0f;
    }

    private void launch(BlockPos p, BlockState st, Vec3d velocity) {
        world.setBlockState(p, Blocks.AIR.getDefaultState());
        FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.up(), st);
        if (fb != null) {
            fb.setVelocity(velocity);
            fb.dropItem = false;
        }
    }

    /** Remove a água colocada pelo tsunami quando ele termina ou é parado. */
    private void cleanup(ActiveDisaster d) {
        for (BlockPos p : d.placedWater) {
            if (world.getBlockState(p).isOf(Blocks.WATER)) {
                world.setBlockState(p, Blocks.AIR.getDefaultState());
            }
        }
        d.placedWater.clear();
    }
}
