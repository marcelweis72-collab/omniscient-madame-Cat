# Herkunft der Klänge

| Datei | Quelle | Lizenz |
|---|---|---|
| `app/src/main/assets/audio/meow_1.mp3` | Pixabay, Nutzer „sound_garage“, „cat meow 8 fx“ (ID 306184) | Pixabay Content License: Nutzung in Apps erlaubt, auch kommerziell, ohne Namensnennung. Nicht als einzelne Sounddatei weiterverkaufen. |
| `app/src/main/assets/audio/meow_2.mp3` | Pixabay, Nutzer „dragon-studio“, „cute cat meow“ (ID 472372) | Pixabay Content License, wie oben. |
| `app/src/main/assets/audio/meow_3.mp3` | Pixabay, Nutzer „freesound_community“, „cat meow“ (ID 85175) | Pixabay Content License, wie oben. Ursprünglich von Freesound übernommen. |

Weitere Miauer einfach als `meow_4.mp3`, `meow_5.mp3` … in denselben Ordner legen. Die App spielt sie zufällig, nie zweimal hintereinander denselben.

Die Lautstärke der Miauer ist angeglichen (mittlere Lautstärke -17 dB, Spitzen höchstens -1 dB). Neue Dateien vorher genauso anpassen, zum Beispiel mit `ffmpeg -i neu.mp3 -af volumedetect -f null -` messen und mit `-af volume=XdB` angleichen.
Schnurren (`purr.*`) und Musik (`music.*`) sind noch synthetische Platzhalter aus `Synth.kt`.
