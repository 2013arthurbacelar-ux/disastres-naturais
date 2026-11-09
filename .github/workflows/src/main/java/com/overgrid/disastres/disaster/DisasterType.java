package com.overgrid.disastres.disaster;

/**
 * Mecanismos de desastre. Cada um gera 100 variações em {@link DisasterPreset}.
 * Os marcados como {@code commandOnly} só podem ser ativados por comando, não pela varinha.
 */
public enum DisasterType {
    /** Funil que gira, puxa blocos leves e os arremessa. */
    TORNADO(1200, 14, "Tornado", false),
    /** Tremor no chão: sacode a superfície e faz destroços caírem. */
    EARTHQUAKE(900, 10, "Terremoto", false),
    /** Rajadas de vento constantes que derrubam e empurram blocos. */
    WIND_STORM(1600, 22, "Tempestade de vento", false),
    /** Onda gigante vinda do mar: avança em linha reta, arrasta tudo e deixa água por onde passa. */
    TSUNAMI(200, 30, "Tsunami", true),
    /** Nuvens se fecham, chove ácido e as folhas e plantas somem; zona de radiação envenena quem está dentro. */
    RADIATION(1200, 18, "Chuva ácida", true),
    /** Bomba cai do céu, explode, cega os jogadores e deixa zona de radiação com wither. */
    NUCLEAR(800, 12, "Explosão nuclear", true),
    /** Vórtice que puxa blocos e entidades para o centro e destrói o que chega perto. */
    BLACK_HOLE(1000, 16, "Buraco negro", true);

    public final int durationTicks;
    public final double radius;
    public final String displayName;
    public final boolean commandOnly;

    DisasterType(int durationTicks, double radius, String displayName, boolean commandOnly) {
        this.durationTicks = durationTicks;
        this.radius = radius;
        this.displayName = displayName;
        this.commandOnly = commandOnly;
    }

    public static DisasterType fromId(int id) {
        return values()[Math.floorMod(id, values().length)];
    }
}
