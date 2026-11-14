package pl.onyxdev.areny;

import org.bukkit.Material;

import java.util.Locale;

public enum Mode {
    SPLEEF("spleef", Material.DIAMOND_SHOVEL),
    TNTRUN("tntrun", Material.TNT),
    KROL("krol", Material.GOLDEN_HELMET);

    public final String id;
    public final Material icon;

    Mode(String id, Material icon) {
        this.id = id;
        this.icon = icon;
    }

    public static Mode of(String s) {
        if (s == null) return null;
        String l = s.toLowerCase(Locale.ROOT);
        for (Mode m : values()) if (m.id.equals(l)) return m;
        return null;
    }
}
