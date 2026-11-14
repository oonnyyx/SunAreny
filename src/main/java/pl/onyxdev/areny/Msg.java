package pl.onyxdev.areny;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.onyxdev.suncore.Messages;
import pl.onyxdev.suncore.SunCore;

import java.time.Duration;

public final class Msg {
    private Msg() {
    }

    public static final String ANNOUNCE = "areny.ogloszenie";
    public static final String GAME = "areny.gra";
    public static final String TITLE = "areny.tytul";
    public static final String BAR = "areny.pasek";
    public static final String INFO = "areny.info";

    public static void register(Plugin p) {
        Messages m = SunCore.messages();
        m.register(p, ANNOUNCE, "Ogłoszenia o zapisach na areny i zwycięzcach", true, true);
        m.register(p, GAME, "Wiadomości w trakcie gry na arenie (dla uczestników)", true, false);
        m.register(p, TITLE, "Napisy na ekranie: odliczanie, start, wynik", true, false);
        m.register(p, BAR, "Pasek z czasem i stanem gry na arenie", true, false);
        m.register(p, INFO, "Odpowiedzi na komendy /arena", true, false);
    }

    public static void chat(Player p, String key, Component c) {
        SunCore.messages().send(p, key, c);
    }

    public static void broadcast(Component c) {
        SunCore.messages().broadcast(ANNOUNCE, c);
    }

    public static void actionBar(Player p, Component c) {
        SunCore.messages().actionBar(p, BAR, c);
    }

    public static void title(Player p, Component title, Component sub, int fadeIn, int stay, int fadeOut) {
        SunCore.messages().title(p, TITLE, Title.title(title, sub, Title.Times.times(
                Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
    }

    public static BossBar bossBar(Player p, BossBar bar) {
        return SunCore.messages().bossBar(p, BAR, bar, 20 * 60 * 30);
    }
}
