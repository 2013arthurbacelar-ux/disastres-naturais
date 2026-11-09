package com.overgrid.disastres.disaster;

import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Um desastre em andamento no mundo. Estado mínimo; a lógica fica em {@link DisasterManager}. */
public final class ActiveDisaster {
    public final DisasterType type;
    public final BlockPos center;
    public final float intensity;
    public final double radius;
    /** Direção horizontal em que a onda do tsunami viaja (null para os outros desastres). */
    public final Vec3d dir;
    public int ticksLeft;
    /** Fase do funil/tremor/vento, avançando a cada tick. */
    public double phase;
    /** Ticks desde o início. */
    public int age;
    /** Nuclear: bomba já foi lançada. */
    public boolean started;
    /** Nuclear: bomba já explodiu. */
    public boolean exploded;
    /** Nuclear: a bomba caindo do céu. */
    public FallingBlockEntity bomb;
    /** Nuclear: última posição conhecida da bomba. */
    public BlockPos bombPos;
    /** Blocos de água colocados pelo tsunami, removidos quando ele termina. */
    public final List<BlockPos> placedWater = new ArrayList<>();

    public ActiveDisaster(DisasterPreset preset, BlockPos center, Vec3d dir) {
        this.type = preset.mechanic();
        this.center = center;
        this.intensity = preset.intensity();
        this.radius = preset.radius();
        this.dir = dir;
        this.ticksLeft = preset.durationTicks();
        this.phase = 0.0;
        this.age = 0;
    }

    public Vec3d centerPos() {
        return Vec3d.ofCenter(center);
    }

    public boolean isOver() {
        return ticksLeft <= 0;
    }
}
