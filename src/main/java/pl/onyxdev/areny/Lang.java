package pl.onyxdev.areny;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Lang {
    public static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Set<String> PARSED = Set.of("wynik", "dodatki");

    private final JavaPlugin plugin;
    private YamlConfiguration yml;
    private YamlConfiguration defaults;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File f = new File(plugin.getDataFolder(), "wiadomosci.yml");
        if (!f.exists()) plugin.saveResource("wiadomosci.yml", false);
        yml = YamlConfiguration.loadConfiguration(f);
        var in = plugin.getResource("wiadomosci.yml");
        if (in != null) defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public String raw(String path) {
        String s = yml.getString(path);
        if (s == null && defaults != null) s = defaults.getString(path);
        if (s == null) {
            plugin.getLogger().warning("Brak tekstu: " + path);
            return "<red>[" + path + "]";
        }
        return s;
    }

    public Component get(String path, String... kv) {
        return parse(raw(path), kv);
    }

    public Component withPrefix(String path, String... kv) {
        return parse(raw("prefiks") + raw(path), kv);
    }

    public static Component parse(String text, String... kv) {
        List<TagResolver> res = new ArrayList<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = kv[i], v = kv[i + 1] == null ? "" : kv[i + 1];
            if (k.equals("kolor")) text = text.replace("<kolor>", v.matches("#[0-9a-fA-F]{6}") ? v : "#ffffff");
            else if (PARSED.contains(k)) res.add(Placeholder.parsed(k, v));
            else res.add(Placeholder.unparsed(k, v));
        }
        return MM.deserialize(text, res.toArray(TagResolver[]::new));
    }

    public String modeName(Mode m) {
        return raw("tryby." + m.id + ".nazwa");
    }

    public String modeColor(Mode m) {
        return raw("tryby." + m.id + ".kolor");
    }

    public String modeDesc(Mode m) {
        return raw("tryby." + m.id + ".opis");
    }

    public String[] modeKv(Mode m, String... more) {
        String[] base = {"tryb", modeName(m), "kolor", modeColor(m), "opis", modeDesc(m)};
        String[] out = new String[base.length + more.length];
        System.arraycopy(base, 0, out, 0, base.length);
        System.arraycopy(more, 0, out, base.length, more.length);
        return out;
    }
}
