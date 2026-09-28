# Die Blender-Katze einbauen

Die App spielt für jeden Zustand der Katze eine eigene Animation ab. Fehlt eine Datei, zeichnet sie
stattdessen die Platzhalter-Katze. Du kannst also Zustand für Zustand austauschen.

## Die sechs Zustände

| Datei in `app/src/main/assets/cat/` | Was passiert | Schleife? | Länge ungefähr |
|---|---|---|---|
| `idle.webp` | atmen, blinzeln, Schwanz bewegen | ja | 3 bis 4 s |
| `tapped.webp` | Ohr zuckt, Maul öffnet sich zum Miauen | nein | 0,6 s |
| `thinking.webp` | Augen zu, Pfote über der Kugel, leichtes Wiegen | ja | 2 bis 3 s |
| `revealing.webp` | Augen gehen weit auf, Blick zum Betrachter | nein | 1,4 s |
| `falling_asleep.webp` | gähnen, zusammenrollen | nein | 2,2 s |
| `sleeping.webp` | ruhiges Atmen, ab und zu ein Ohrzucken | ja | 4 bis 6 s |

Die Zeiten passen zum Ablauf in `OracleViewModel.divine()`. Werden deine Animationen länger oder
kürzer, sag Bescheid, dann passe ich die Zeiten an.

## Blender-Einstellungen

- **Kamera:** frontal, leicht von unten, quadratisch 1024 x 1024 Pixel.
- **Hintergrund transparent:** Render Properties, Film, Transparent einschalten.
- **Licht:** Hauptlicht warm (etwa 2000 K, Bernstein) von rechts unten, wie von einer Kerze.
  Schwaches kühles Fülllicht von links. Neutral beleuchtet wirkt die Katze im Zelt hineinkopiert.
- **Stil:** Toon- oder Cel-Shading, kein realistisches Fell. Große Bernstein-Augen, Samthalsband mit
  Mondsichel-Anhänger, damit sie zur Platzhalter-Katze passt.
- **Turban:** Drapierter Samtturban (Samtrot), Falten laufen zu einer Messingbrosche mit Perlenkranz
  zusammen, darüber eine schwarze Feder. Die Ohren schauen links und rechts deutlich darunter hervor.
- **Kugel und Pfoten:** Die Kristallkugel zeichnet die App selbst, sie leuchtet beim Nachdenken auf.
  Sie liegt vor der Katze. Beim Nachdenken muss die Katze die Pfoten an die Kugel legen, die Pfoten
  liegen dann aber hinter der gezeichneten Kugel. Wie wir das lösen (Pfoten als eigene Ebene rendern
  oder die Kugel mit in Blender bauen), klären wir, wenn deine Katze so weit ist.
- **Bildrate:** 24 fps reichen.
- **Position:** Die Katze sitzt im unteren Bildrand. Die unteren 5 % verdeckt die Tischkante.
- **Ausgabe:** PNG-Bildfolge mit RGBA.

## PNG-Folge in animiertes WebP umwandeln

Mit `img2webp` (aus den libwebp-Werkzeugen von Google):

```
img2webp -loop 0 -lossy -q 80 -d 42 frame_*.png -o idle.webp
```

`-d 42` ist die Dauer pro Bild in Millisekunden (24 fps). Eine Datei sollte unter 2 MB bleiben,
sonst wird die App groß und lädt langsam. Hilft `-q 70` oder 768 x 768 Pixel nicht, schick mir die
Dateien, dann schauen wir gemeinsam.

## Klang und Musik

- `app/src/main/assets/audio/meow.ogg` ersetzt das synthetische Miauen. Eine echte Katze klingt besser.
- `app/src/main/assets/audio/music.ogg` ersetzt die Spieluhr. Sie sollte nahtlos loopen.
- Erlaubt sind ogg, mp3 und wav. Bei fremden Dateien die Lizenz notieren (zum Beispiel Freesound
  mit CC0-Filter).
