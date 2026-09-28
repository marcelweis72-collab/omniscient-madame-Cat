# Omniscient Madame Cat

Android-App: Eine Comic-Katze im Wahrsagerzelt auf dem Jahrmarkt verrät Sprüche, halb Glückskeks, halb Wahrsagerin.
Kotlin, Jetpack Compose, keine Material-Komponenten. Sprache der App: Deutsch, Du-Form.

## Ablauf pro Tag
1. Tippen auf die Katze: Miauen, Augen zu, Nachdenken, Spruch 1.
2. Knopf "Noch ein Blick in die Kugel": Belohnungs-Werbung, dann Spruch 2.
3. Danach schläft die Katze: "Die Katze braucht Ruhe bis morgen." mit Countdown bis Mitternacht (Ortszeit).
- Ohne verfügbare Werbung trotzdem Spruch 2 geben. Die Katze straft nicht.
- Sprüche kommen aus einem gemischten Stapel, keine Wiederholung, bevor alle dran waren.

## Design-Regeln

| Nicht | Stattdessen |
|---|---|
| Lila-blaue Verläufe, Glassmorphism | Flächige Farben aus `ui/Palette.kt`: Samtrot, Tintenblau, Bernstein, Papier |
| Weiche Schatten auf jeder Karte | Trennung durch Linien und Ornamente; Schatten nur, wo etwas wirklich auf dem Tisch liegt |
| Inter/Poppins überall | Rye für Überschriften, IM Fell English für Sprüche und Fließtext |
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
- Kurz: höchstens zwei Sätze, passt auf eine Karte.

## Austauschbare Assets (ohne Code-Änderung)
- `assets/cat/<zustand>.webp`: Blender-Animationen, siehe `docs/ASSETS.md`.
- `assets/audio/meow.*`, `assets/audio/music.*`: echte Aufnahmen statt der synthetischen Platzhalter.
- `assets/fonts/`: wird von `tools/fetch-fonts.sh` befüllt.

## Bauen
- Debug-APK: `gradle assembleDebug` (JDK 17, Gradle 8.11). Die GitHub Action baut bei jedem Push und stellt die APK als Artefakt bereit.
