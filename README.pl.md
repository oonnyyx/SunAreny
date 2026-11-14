# SunAreny

[English](README.md) · **Polski**

Minigry na zapisy na Paper 26.2: Spleef, TNT Run i Król Wyspy. Plugin sam tworzy pusty świat, stawia areny i odbudowuje je po każdej rundzie.

## Tryby

- **Spleef**: wielopiętrowa śnieżna arena. Zewnętrzny pierścień najwyższego piętra z czasem się zapada, a z rozkopanego śniegu może wypaść śnieżka, która niszczy blok, w który trafi.
- **TNT Run**: bloki znikają chwilę po tym, jak na nie wejdziesz. Na starcie jest kilka sekund ochrony.
- **Król Wyspy**: stój na szczycie, zbieraj punkty i zrzucaj innych. Za upadek tracisz punkty.

## Jak to działa

- Jedna gra naraz: 60 sekund zapisów z klikalnym ogłoszeniem na czacie, odliczanie, gra, ekran wyników i powrót tam, skąd się przyszło
- Serwer sam otwiera zapisy co 20 minut, gdy online są co najmniej 3 osoby. Gracze mogą je też otworzyć z menu `/arena`.
- Ekwipunek, XP i pozycja zapisują się na dysku przed grą i wracają po niej. Uszkodzony zapis nigdy nie czyści gracza.
- Nagrody za miejsca 1-3 i za udział, z dziennym limitem
- W trakcie gry działają tylko dozwolone komendy
- Statystyki graczy w `statystyki.yml`: wygrane, rozegrane i seria zwycięstw

## Wymagania

- Paper **26.2**
- Java **25**
- [SunCore](https://github.com/oonnyyx/SunCore)

## Instalacja

1. Pobierz `SunAreny.jar` i `SunCore.jar` z zakładki [Releases](../../releases/latest).
2. Wrzuć oba pliki do `plugins/` i zrestartuj serwer. Świat `areny` i wszystkie trzy areny powstaną same.

## Komendy

Aliasy: `/areny`, `/minigry`.

| Komenda | Opis | Uprawnienie |
|---|---|---|
| `/arena` | Menu: tryby, zapisy, statystyki | `sunareny.menu` |
| `/arena dolacz` | Dołącza do trwających zapisów | `sunareny.dolacz` |
| `/arena opusc` | Wychodzi z areny | `sunareny.opusc` |
| `/arena statystyki [gracz]` | Pokazuje statystyki | `sunareny.statystyki` |
| `/arena start <tryb>` | Otwiera zapisy na tryb | `sunareny.admin.start` |
| `/arena wymus` | Startuje od razu, bez czekania na koniec zapisów | `sunareny.admin.wymus` |
| `/arena stop` | Przerywa grę | `sunareny.admin.stop` |
| `/arena odbuduj` | Stawia areny od nowa | `sunareny.admin.odbuduj` |
| `/arena tp <tryb>` | Teleport na arenę | `sunareny.admin.tp` |
| `/arena reload` | Przeładowuje config | `sunareny.admin.reload` |

Uprawnienia gracza ma domyślnie każdy, a admina tylko op. `sunareny.*` daje wszystkie.

## Konfiguracja

W `config.yml` ustawisz świat, czas zapisów, limity graczy, automat, nagrody (komendy z konsoli) i opcje każdego trybu. Wiadomości są w `wiadomosci.yml`.

## Budowanie

```bash
git clone https://github.com/oonnyyx/SunCore && cd SunCore && mvn install && cd ..
git clone https://github.com/oonnyyx/SunAreny && cd SunAreny && mvn package
```

## Licencja

[MIT](LICENSE)
