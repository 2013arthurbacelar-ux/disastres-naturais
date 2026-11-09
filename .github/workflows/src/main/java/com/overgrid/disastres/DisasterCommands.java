package com.overgrid.disastres;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.overgrid.disastres.disaster.DisasterManager;
import com.overgrid.disastres.disaster.DisasterPreset;
import com.overgrid.disastres.disaster.DisasterType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Comandos (nível 2):
 *  /desastre tornado | terremoto | tempestade      -> também disponíveis na varinha
 *  /desastre tsunami | radiacao | nuclear | buraconegro  -> só por comando
 *  /desastre preset <0-699>                         -> qualquer um dos 700
 *  /desastre parar
 *
 * O tsunami nasce 40 blocos à frente de quem digitou (na direção que o jogador olha)
 * e avança de volta em direção ao jogador.
 */
public final class DisasterCommands {
    private DisasterCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("desastre")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .then(literal("tornado").executes(ctx -> spawnType(ctx.getSource(), DisasterType.TORNADO)))
                .then(literal("terremoto").executes(ctx -> spawnType(ctx.getSource(), DisasterType.EARTHQUAKE)))
                .then(literal("tempestade").executes(ctx -> spawnType(ctx.getSource(), DisasterType.WIND_STORM)))
                .then(literal("tsunami").executes(ctx -> spawnType(ctx.getSource(), DisasterType.TSUNAMI)))
                .then(literal("radiacao").executes(ctx -> spawnType(ctx.getSource(), DisasterType.RADIATION)))
                .then(literal("nuclear").executes(ctx -> spawnType(ctx.getSource(), DisasterType.NUCLEAR)))
                .then(literal("buraconegro").executes(ctx -> spawnType(ctx.getSource(), DisasterType.BLACK_HOLE)))
                .then(literal("preset")
                        .then(argument("id", IntegerArgumentType.integer(0, DisasterPreset.COUNT - 1))
                                .executes(ctx -> spawnPreset(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "id")))))
                .then(literal("parar").executes(ctx -> {
                    DisasterManager.get(ctx.getSource().getWorld()).stopAll();
                    ctx.getSource().sendFeedback(() -> Text.literal("Desastres encerrados."), true);
                    return 1;
                })));
    }

    private static int spawnType(ServerCommandSource src, DisasterType type) {
        return spawnPreset(src, type.ordinal() * 100);
    }

    private static int spawnPreset(ServerCommandSource src, int presetId) {
        ServerWorld world = src.getWorld();
        DisasterPreset preset = DisasterPreset.get(presetId);
        DisasterManager manager = DisasterManager.get(world);

        if (preset.mechanic() == DisasterType.TSUNAMI && src.getEntity() instanceof PlayerEntity player) {
            Vec3d facing = Vec3d.fromPolar(0, player.getYaw()).normalize();
            Vec3d origin = player.getEntityPos().add(facing.multiply(40));
            manager.start(preset, BlockPos.ofFloored(origin), facing.multiply(-1));
        } else {
            manager.start(preset, BlockPos.ofFloored(src.getPosition()));
        }

        src.sendFeedback(() -> Text.literal("Desastre iniciado: " + preset.name()), true);
        return 1;
    }
}
