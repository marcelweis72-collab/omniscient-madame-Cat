# Madame Minka

Eine Wahrsager-Katze im Jahrmarktzelt. Tippe auf die Katze, sie miaut, schließt die Augen, denkt nach
und legt dir einen Spruch auf den Tisch, halb Glückskeks, halb Wahrsagerin.

- Ein Spruch pro Tag gratis, ein zweiter gegen ein kurzes Werbevideo.
- Danach schläft die Katze bis Mitternacht, mit Countdown.
- 300 Sprüche in drei Arten, gezogen wie aus einem gemischten Kartenstapel.
- Samtrot, Tintenblau, Kerzenlicht, Papierkorn. Kein Material-Look, keine Glitzer-Effekte.

## Stand: Prototyp 0.1

| Teil | Stand |
|---|---|
| Ablauf, Tageslimit, Countdown | fertig |
| Zelt, Kerze, Kugel, Karten | fertig, gezeichnet im Code |
| Katze | Platzhalter, wird durch die Blender-Katze ersetzt (`docs/ASSETS.md`) |
| Miauen und Musik | synthetische Platzhalter, eigene Dateien möglich |
| Werbung | Platzhalter, zählt bis fünf. AdMob fehlt noch |
| Sprüche | 300 Entwürfe in `app/src/main/assets/sprueche.json`, bitte selbst überarbeiten |

## APK aufs Handy

1. Auf GitHub: Actions, letzter Lauf von "APK bauen", unten `madame-minka-debug` herunterladen.
2. ZIP entpacken, die APK aufs Handy kopieren und öffnen.
3. Android fragt, ob du Apps aus dieser Quelle erlauben willst. Einmal erlauben.

## Selbst bauen

JDK 17 und Gradle 8.11, dann:

```
bash tools/fetch-fonts.sh
gradle assembleDebug
```

Oder das Projekt in Android Studio öffnen.

## Vor dem Play Store noch offen

- AdMob einbinden, mit DSGVO-Einwilligungsdialog (UMP).
- Datenschutzerklärung und Datensicherheits-Formular im Play Store.
- `targetSdk` auf das Level anheben, das Google zum Zeitpunkt der Veröffentlichung verlangt.
- Release-Signatur einrichten.

## Schriften

Rye und IM Fell English, beide unter der SIL Open Font License, geladen über `tools/fetch-fonts.sh`.
