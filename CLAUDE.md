# Omniscient Madame Cat

Android-App: Eine Comic-Katze im Wahrsagerzelt auf dem Jahrmarkt verrät Sprüche, halb Glückskeks, halb Wahrsagerin.
Kotlin, Jetpack Compose, keine Material-Komponenten. Sprache der App: Deutsch, Du-Form.

## Ablauf pro Tag
1. Tippen auf die Katze: Miauen, Augen zu, Pfoten an die Kugel, Spruch. Der erste Spruch am Tag ist gratis.
2. Nach dem Spruch schläft die Katze ein: "Die Katze schläft." mit Countdown bis zum nächsten Gratis-Spruch (Mitternacht, Ortszeit).
3. Knopf "Katze wecken": Belohnungs-Werbung, dann ein neuer Spruch, danach schläft sie wieder. Beliebig oft.
- Ohne verfügbare Werbung trotzdem wecken. Die Katze straft nicht.
- Sprüche kommen aus einem gemischten Stapel, keine Wiederholung, bevor alle dran waren.

## Design-Regeln

| Nicht | Stattdessen |
|---|---|
| Lila-blaue Verläufe, Glassmorphism | Flächige Farben aus `ui/Palette.kt`: Samtrot, Tintenblau, Bernstein, Papier |
| Weiche Schatten auf jeder Karte | Trennung durch Linien und Ornamente; Schatten nur, wo etwas wirklich auf dem Tisch liegt |
| Inter/Poppins überall | Lavishly Yours (Schreibschrift) für den Namen und kurze Zeilen, Rye für Plakat-Elemente, IM Fell English für Sprüche |
| Einheitlicher Radius überall | Karten fast eckig (6dp), Knöpfe eckig mit Doppelrahmen und Rauten |
| Funkel-Icons, Emojis als Icons, Glitzerpartikel | Handgezeichnete Symbole: Mondsichel, fünfzackiger Stern, Pfote, Raute |
| "Entdecke", "Mühelos", "smarter Begleiter" | Konkrete, verschmitzte Sätze in der Stimme der Katze |
| Onboarding mit gesichtslosen Illustrationen | Kein Onboarding. Die App startet direkt im Zelt |
| Dashboards mit Deko-Statistiken | Keine Statistiken |
| Viele halbe Features | Eine Kernfunktion: der tägliche Spruch |

- Licht kommt immer von der Kerze rechts: warmes Bernstein, flackernd, nie kaltes Weiß.
- Papierkorn (`rememberGrainBrush`) auf großen Flächen, damit nichts glatt nach Computer aussieht.
- Neue Farben nur in `Palette.kt`.

## Sprüche (`app/src/main/assets/sprueche.json`)
- Drei Gruppen: `glueckskeks`, `wahrsager`, `katze`.
- Keine Vorhersagen zu Krankheit, Tod, Trennung oder Geldverlust.
- Kurz: meist zwei, höchstens drei kurze Sätze, passt auf eine Karte.
- Jeder Spruch muss sich ins eigene Leben hineindeuten lassen (Barnum-Effekt):
  - handelt von Innerem: Wunsch, Zweifel, Entscheidung, Geduld, ein Mensch, eine Veränderung
  - bleibt offen: "etwas", "jemand", "eine Sache" statt konkreter Dinge wie Bus, Socke, Parkplatz
  - lässt sich nicht widerlegen: keine Zahlen, Uhrzeiten, Wochentage, Buchstaben
  - Pointen sind erlaubt, brauchen aber einen deutbaren Kern (Beispiel: "Der Turm steht noch" ist nur Pointe;
    "Was nur aus Gewohnheit steht, darf fallen, damit Neues Platz hat" lässt sich deuten)

## Austauschbare Assets (ohne Code-Änderung)
- `assets/cat/<zustand>.webp`: Blender-Animationen, siehe `docs/ASSETS.md`.
- `assets/audio/meow.*`, `assets/audio/music.*`: echte Aufnahmen statt der synthetischen Platzhalter.
- `assets/fonts/`: wird von `tools/fetch-fonts.sh` befüllt.

## Bauen
- Debug-APK: `gradle assembleDebug` (JDK 17, Gradle 8.11). Die GitHub Action baut bei jedem Push und stellt die APK als Artefakt bereit.
