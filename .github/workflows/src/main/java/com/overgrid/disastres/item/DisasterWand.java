package com.overgrid.disastres.item;

import com.overgrid.disastres.disaster.DisasterManager;
import com.overgrid.disastres.disaster.DisasterPreset;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Varinha de desastres (só tornado, terremoto e tempestade; os de comando não entram aqui).
 *  - Botão direito no ar: abre o menu (paginado).
 *  - Botão direito mirando no chão: dispara o desastre selecionado ali.
 *  - Agachado + botão direito: avança para o próximo desastre (mensagem na barra de ação).
 */
public class DisasterWand extends Item {
    private static final double REACH = 128.0;
    private static final Map<UUID, Integer> SELECTED = new HashMap<>();

    public DisasterWand(Settings settings) {
        super(settings);
    }

    private static int selectedId(PlayerEntity player) {
        return SELECTED.getOrDefault(player.getUuid(), DisasterPreset.wandPresets().get(0).id());
    }

    /** Chamado pelo servidor quando o jogador escolhe no menu. Ignora desastres de comando. */
    public static void setSelected(PlayerEntity player, int presetId) {
        DisasterPreset preset = DisasterPreset.get(presetId);
        if (preset.mechanic().commandOnly) return;
        SELECTED.put(player.getUuid(), preset.id());
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            HitResult clientHit = user.raycast(REACH, 1.0f, false);
            if (clientHit.getType() != HitResult.Type.BLOCK && !user.isSneaking()) {
                com.overgrid.disastres.client.DisasterWandClient.openMenu();
            }
            return ActionResult.SUCCESS;
        }
        ServerWorld serverWorld = (ServerWorld) world;

        if (user.isSneaking()) {
            int next = DisasterPreset.nextWandId(selectedId(user));
            SELECTED.put(user.getUuid(), next);
            user.sendMessage(Text.literal("Desastre selecionado: " + DisasterPreset.get(next).name()), true);
            return ActionResult.SUCCESS;
        }

        HitResult hit = user.raycast(REACH, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            user.sendMessage(Text.literal("Mire em um bloco no chão."), true);
            return ActionResult.FAIL;
        }

        BlockPos target = ((BlockHitResult) hit).getBlockPos().up();
        DisasterPreset preset = DisasterPreset.get(selectedId(user));
        DisasterManager.get(serverWorld).start(preset, target);
        user.sendMessage(Text.literal(preset.name() + " em " + target.toShortString()), true);
        return ActionResult.SUCCESS;
    }
}
