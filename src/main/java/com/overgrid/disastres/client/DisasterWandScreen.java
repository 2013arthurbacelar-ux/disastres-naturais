package com.overgrid.disastres.client;

import com.overgrid.disastres.DisasterNetworking;
import com.overgrid.disastres.disaster.DisasterPreset;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Menu da varinha: os desastres que a varinha pode ativar, paginados de 8 em 8. */
public class DisasterWandScreen extends Screen {
    private static final int PAGE_SIZE = 8;
    private static final List<DisasterPreset> PRESETS = DisasterPreset.wandPresets();
    private static final int TOTAL_PAGES = (PRESETS.size() + PAGE_SIZE - 1) / PAGE_SIZE;
    private static final int BUTTON_WIDTH = 240;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;

    private int page = 0;

    public DisasterWandScreen() {
        super(Text.literal("Varinha de Desastres"));
    }

    private int listStartY() {
        return this.height / 2 - (PAGE_SIZE * SPACING) / 2;
    }

    private int navY() {
        return listStartY() + PAGE_SIZE * SPACING + 10;
    }

    @Override
    protected void init() {
        int first = page * PAGE_SIZE;
        int last = Math.min(first + PAGE_SIZE, PRESETS.size());
        int x = this.width / 2 - BUTTON_WIDTH / 2;

        for (int i = first; i < last; i++) {
            DisasterPreset preset = PRESETS.get(i);
            int y = listStartY() + (i - first) * SPACING;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(preset.name()), button -> {
                        ClientPlayNetworking.send(new DisasterNetworking.SelectDisasterPayload(preset.id()));
                        this.close();
                    })
                    .dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                    page = Math.max(0, page - 1);
                    this.clearAndInit();
                })
                .dimensions(x, navY(), 40, BUTTON_HEIGHT)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                    page = Math.min(TOTAL_PAGES - 1, page + 1);
                    this.clearAndInit();
                })
                .dimensions(x + BUTTON_WIDTH - 40, navY(), 40, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, listStartY() - 16, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Página " + (page + 1) + "/" + TOTAL_PAGES),
                this.width / 2, navY() + 6, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
