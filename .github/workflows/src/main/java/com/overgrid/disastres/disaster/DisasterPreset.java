package com.overgrid.disastres.disaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Catálogo de 700 desastres: 7 mecanismos de {@link DisasterType} com 100 variações cada.
 * A geração é determinística (mesma lista no servidor e no cliente).
 *
 * ids 0-99: Tornado | 100-199: Terremoto | 200-299: Tempestade | 300-399: Tsunami |
 * 400-499: Chuva ácida | 500-599: Explosão nuclear | 600-699: Buraco negro
 */
public record DisasterPreset(int id, DisasterType mechanic, String name, double radius, int durationTicks, float intensity) {
    public static final int COUNT = 700;
    private static final String[] ADJECTIVES = {
            "Carmesim", "Gélido", "Sombrio", "Dourado", "Faiscante",
            "Ártico", "Ventoso", "Silencioso", "Furioso", "Antigo"
    };
    private static final List<DisasterPreset> ALL = build();
    /** Desastres que a varinha pode ativar (sem os de comando). */
    private static final List<DisasterPreset> WAND = ALL.stream()
            .filter(p -> !p.mechanic().commandOnly)
            .toList();

    private static List<DisasterPreset> build() {
        List<DisasterPreset> list = new ArrayList<>(COUNT);
        DisasterType[] mechanics = DisasterType.values();
        for (int id = 0; id < COUNT; id++) {
            DisasterType mech = mechanics[id / 100];
            int k = id % 100;
            Random rnd = new Random(id * 31L + 7L);
            double radius = mech.radius * (0.6 + 0.8 * k / 99.0);
            int duration = (int) (mech.durationTicks * (0.7 + 0.6 * rnd.nextDouble()));
            float intensity = (float) (0.6 + 0.9 * rnd.nextDouble());
            String name = mech.displayName + " " + ADJECTIVES[k % 10] + " " + (k / 10 + 1);
            list.add(new DisasterPreset(id, mech, name, radius, duration, intensity));
        }
        return list;
    }

    public static DisasterPreset get(int id) {
        return ALL.get(Math.floorMod(id, COUNT));
    }

    public static List<DisasterPreset> wandPresets() {
        return WAND;
    }

    /** Próximo desastre da varinha depois do id dado (cicla pela lista da varinha). */
    public static int nextWandId(int current) {
        for (int i = 0; i < WAND.size(); i++) {
            if (WAND.get(i).id() == current) {
                return WAND.get((i + 1) % WAND.size()).id();
            }
        }
        return WAND.get(0).id();
    }
}
