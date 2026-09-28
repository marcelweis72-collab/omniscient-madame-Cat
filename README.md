# Omniscient Madame Cat

Eine Wahrsager-Katze im Jahrmarktzelt. Tippe auf die Katze, sie miaut, schließt die Augen, denkt nach
und legt dir einen Spruch auf den Tisch, halb Glückskeks, halb Wahrsagerin.

- Ein Spruch pro Tag gratis, danach schläft die Katze.
- Mit einem kurzen Werbevideo lässt sie sich beliebig oft wecken und verrät einen weiteren Spruch.
- 600 Sprüche: 300 mystische (Glückskeks, Wahrsager, freche Katze) und 300 bodenständige Alltagsweisheiten, gezogen wie aus einem gemischten Kartenstapel.
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

1. Im Handy-Browser (bei GitHub angemeldet, das Repo ist privat) diesen Link öffnen:
   https://github.com/marcelweis72-collab/omniscient-madame-Cat/releases/download/neuester-build/omniscient-madame-cat.apk
2. Die heruntergeladene APK antippen.
3. Android fragt, ob du Apps aus dieser Quelle erlauben willst. Einmal erlauben.

Jeder Push auf `main` ersetzt die APK hinter diesem Link durch die neueste Version.

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
