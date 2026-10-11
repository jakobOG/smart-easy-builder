# Smart Easy Builder

Clientseitige Fabric-Mod (Java 21, Ziel: Minecraft **1.21.10**) zum bequemen Nachbauen von
**Litematica**-Schematics im Survival. Eigene Implementierung, orientiert an der Funktionsweise
von Litematicas Easy Place / Hold Easy Place, ohne übernommenen Fremdcode.

## Ehrlicher Projektstand (bitte zuerst lesen)

Dieses Projekt wurde in einer Umgebung **ohne Zugriff auf Fabric-/Minecraft-/Gradle-Server** geschrieben.

| Punkt | Stand |
|---|---|
| Kompiliert mit Gradle | **NEIN, nie gebaut.** Es gibt keine fertige JAR. |
| Im Spiel getestet | **NEIN.** |
| Syntax der Java-Dateien | Mit einem Parser geprüft (alle Dateien parsen). Das ist **kein Compile**: Typen und Methodennamen sind nicht gegen Minecraft-Mappings geprüft. |
| Unit-Tests | `ModConfigTest` (reines Java) ist geschrieben, aber **nicht ausgeführt**. |
| Gradle Wrapper | `gradle-wrapper.properties` liegt bei. `gradlew`, `gradlew.bat` und `gradle-wrapper.jar` fehlen (Download nicht möglich). Erzeugen: siehe unten. |
| Versionen in `gradle.properties` | **Ungeprüft** (Loom, Yarn, Loader, Fabric API). Auf <https://fabricmc.net/develop/> für 1.21.10 abgleichen. |

Rechne beim ersten Build mit Compilerfehlern an versionsabhängigen Stellen. Sie sind im Code mit
`VERSIONSABHAENGIG` bzw. `UNGEPRUEFT` markiert:

- `input/Keys.java`: `KeyBinding.Category` (Änderung ab 1.21.9).
- `mixin/BlockItemAccessor.java`, `mixin/MinecraftClientMixin.java`: Yarn-Methodennamen `getPlacementState`, `doItemUse`.
- `core/InventoryManager.java`: `getSelectedSlot()/setSelectedSlot()` (ab 1.21.5), `clickSlot`.
- `gui/Hud.java`: `HudRenderCallback` (in neuer Fabric API als veraltet markiert).
- `schematic/LitematicaSource.java`: Litematica-Klassen per Reflection (Namen aus Erinnerung, nicht gegen die Version geprüft).

## Bauen

Voraussetzungen: JDK 21, Internet.

```
# Wrapper einmalig erzeugen (benötigt ein installiertes Gradle ODER kopiere gradlew, gradlew.bat und
# gradle/wrapper/gradle-wrapper.jar aus der offiziellen Fabric-Example-Mod):
gradle wrapper --gradle-version 8.14

./gradlew build          # Windows: gradlew.bat build
```
Ergebnis: `build/libs/smart-easy-builder-0.1.0.jar` (die `-sources.jar` nicht installieren).
Prüfe danach, dass die JAR existiert und nicht leer ist.

Wenn Gradle Artefakte nicht findet: `gradle.properties` anpassen (siehe oben). Passt eine Methode
nicht, nennt der Compiler die Stelle; die Kommentare im Code sagen, was dort Version-abhängig ist.

## Installation

1. Fabric Loader + Fabric API für Minecraft 1.21.10 installieren.
2. **Litematica** (und MaLiLib) in der zu 1.21.10 passenden Version installieren. Ohne Litematica startet die
   Mod trotzdem und zeigt nur die Meldung "Litematica ist nicht installiert".
3. `smart-easy-builder-0.1.0.jar` in den `mods`-Ordner legen. Auf dem Server ist nichts nötig.

## Bedienung

Standardtasten (im Steuerungsmenü unter "Smart Easy Builder" änderbar):

| Taste | Funktion |
|---|---|
| `B` | Easy Place ein/aus |
| `N` | Einstellungsmenü |
| (unbelegt) | Automatisches Platzieren ein/aus |

Ablauf: Schematic in Litematica laden/anzeigen, Mod mit `B` einschalten, auf einen fehlenden Block zielen
und **Benutzen-Taste (Rechtsklick) halten**. Die Mod wählt das passende Item, wechselt/holt es in die Hotbar
und platziert, aber nur wenn der Vanilla-Klick den geforderten Blockzustand erzeugt.

Modi:
- **Nur bei gehaltener Taste** (Standard): platziert nur, solange Rechtsklick gehalten wird.
- **Automatisch** ("Nur bei gehaltener Taste" aus + "Automatisch platzieren" an): platziert beim Zielen ohne Taste.
  Sofort abschaltbar mit `B`. Pausiert bei offenem Menü/Chat, beim Abbauen (Linksklick), beim Essen/Bogenspannen, bei Tod, Dimensionswechsel und Verbindungsabbruch.

Menü (`N`): Easy Place, Auto, Nur-bei-Taste, Intervall (0/20/50/100/250/500/1000 ms), max. ausstehende Versuche,
Hotbar-Slot-Schutz (rot = geschützt), Hauptinventar-Schutz, Custom-Items, Vorrang für Litematica Easy Place,
Vanilla-Rechtsklick-Sperre, HUD, Reset. Unten: Modus, benötigtes Item, letzter Fehler. Alles wirkt sofort und
wird in `config/smart_easy_builder.json` gespeichert.

## Verhalten und Grenzen (so ist es gemeint)

- **Geschwindigkeit:** Maximal **ein** Platzierungsversuch pro Client-Tick (50 ms). Intervalle unter 50 ms (0, 20) wirken
  daher wie 50 ms. Zusätzlich begrenzen Ping, Serverantwort und serverseitige Limits die reale Rate. Es werden nie
  mehr Versuche ausstehend gehalten als "Max. ausstehend" (Standard 1, höchstens 4).
- **Bestätigung/Ablehnung:** heuristisch. Der Client sagt Platzierungen voraus. Ist der Block nach einer vom Ping
  abhängigen Wartezeit wieder weg, gilt das als Ablehnung; nach mehreren Fehlschlägen wird die Position zeitweise
  gesperrt (mit zunehmender Wartezeit statt Wiederholungsspam).
- **Rotation/Hälften:** Die Mod simuliert lokal den Vanilla-Platzierungsvorgang für mehrere zulässige Klickpositionen
  und platziert nur bei exakt passendem Ergebnis. Hängt die Ausrichtung von deiner Blickrichtung ab (Treppen, Öfen ...)
  und passt sie nicht, bekommst du eine Warnung ("drehe dich passend") und es wird **nichts** Falsches gesetzt.
  Es gibt keinen Easy-Place-Protokoll-Modus für Spezial-Server; nichts wird "erzwungen".
- **Reichweite/Wände:** Nur innerhalb deiner normalen Reichweite und nur mit freier Sichtlinie zum Stützblock.
- **Items:** Es werden nur Items ohne Komponenten-Änderungen (Name, Verzauberung, Custom-Daten ...) verwendet, außer
  "Custom-Items erlauben" ist an. Geschützte Slots werden nie verändert; nichts wird gedroppt. Das Holen aus dem
  Hauptinventar ist ein normaler Slot-Tausch.
- **Nicht automatisch platziert:** obere Türhälften und Bettköpfe (entstehen mit dem Hauptteil); Blöcke ohne Item (Wasser, Feuer ...).
- **Nachbarschaftsabhängige Zustände** (Zäune, Mauern, Treppenecken `shape`, `waterlogged`, `powered` ...) werden beim Vergleich ignoriert
  (`ignoredProperties` in der Config), weil Minecraft sie aus der Umgebung berechnet.
- **Interaktive Stützblöcke** (Truhen, Türen, Hebel, Betten ...) werden nicht angeklickt, um keine GUIs/Aktionen auszulösen.

## Server-Regeln

Diese Mod umgeht keine Anti-Cheat-Systeme oder Serverregeln. Automatisches Platzieren ist auf vielen Servern
untersagt oder wird als Cheat erkannt, auch bei kurzen Intervallen. Lies vor dem Einsatz die Regeln deines Servers.
Die Standardwerte sind bewusst konservativ (aus, nur bei gehaltener Taste, 100 ms).

## Projektstruktur

```
src/main/java/dev/smarteasybuilder/
  SmartEasyBuilder.java          Entrypoint
  config/ModConfig.java          Config laden/speichern/validieren
  core/PlacementController.java  Zustandsmaschine (Tick)
  core/Planner.java              Zielbestimmung + Klickplanung (Simulation)
  core/InventoryManager.java     Item-Suche, Hotbar, Schutz
  core/StateMatcher.java, Status.java
  schematic/*                    Litematica-Anbindung (Reflection) + Fallback
  gui/ConfigScreen.java, Hud.java
  input/Keys.java
  mixin/*                        Accessor + Rechtsklick-Sperre
src/main/resources/              fabric.mod.json, Mixin-Config, Sprachdateien (de/en)
src/test/java/...                ModConfigTest
```
