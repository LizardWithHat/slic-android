# SLIC - Image Classification mit ML Modellen auf Android

## Übersicht

Dies ist ein Fork der [Beispiel-App](https://github.com/tensorflow/examples/blob/master/lite/examples/image_classification/android/README.md) von [TensorFlow Lite](https://tensorflow.org/lite) für Android.
Diese App benutzt ML Modelle im Tensorflow Lite Format um mit einer Auswahl von Modellen geschossene Fotos zu Klassifizieren.
Dabei können ebenfalls hinreichende Metadaten eingegeben werden, sollte das ML Modell dies unterstützen.
Die Modelle, die in dieser App inkludiert sind, klassifizieren auf unterschiedliche Weisen Hautkrebsarten.

## Unterschiede zur Ursprungsversion

*   Bereitstellung mehrerer Modelle

*   Erweiterbarkeit der Modell-Liste

*   Nutzung von hinreichenden Metadaten

*   Inferenzdaten Sammeln und Senden

*   Konfigurierbare Update Funktion der App und von ML Modellen

*   Einblendung eines Kamera-Finders für genauere Bilder

## Erweiterungen im Detail
In diesem Fork wurden als Teil eines größeren Systems zur Verwaltung von ML Lebenszyklen mehrere Erweiterungen vorgenommen.
Die meisten dieser Erweiterungen betreffen das Erweitern der App, damit mehrere ML Modelle über ein Interface benutzt werden können.

### Bereitstellung mehrerer Modelle und erweiterbare Modell-Liste
Dieser Fork erweitert die App um eine Möglichkeit mehrere Modelle einzulesen und über ein Interface anzubieten.
Dazu wird die Klasse "Kernels/Classifier.java" in ihrem Enum "Model" erweitert.
Soll ein weiterer Kernel benutzt werden, so muss eine neue Klasse erstellt werden, welche die Klasse Classifier erweitert.
Die Unterklassen sind ein Beispiel, wie diese zu implementieren sind.
Die TF-Lite Datei und deren Textdatei für Labels müssen dabei im Assets Ordner hinterlegt werden.
Sind weitere Klassen erstellt und entsprechende Daten im Assets Ordner hinterlegt, so werden diese in der Übersicht der Kernels angezeigt und können verwendet werden.

### Nutzung von hinreichenden Metadaten
Im Assets Ordner kann optional eine weitere JSON Datei hinterlegt werden, die die hinreichenden Metadaten beschreibt.
Werden hinreichende Metadaten benötigt, so können in der Datei sowohl Input Typ und Input-Möglichkeiten eingetragen werden.
Die von Classifier erweiterte Klasse muss dann noch implementieren, wie mit den extra Metadaten umgegangen werden soll.
Dazu müssen die Strategien aus Unterpaket "strategy" verwendet werden.
Die Metadaten können dann vom Nutzer in der Übersicht der ML Modelle eingetragen werden.

### Inferenzdaten Sammeln und Senden
Der Fork kann nach gesetzter Einstellung die Daten der Inferenz speichern. Dazu werden die eingegeben Metadaten in einer CSV und die geschossenen Bilder in einem Archiv hinterlegt, welches dann per WIFI an einem Konfigurierbaren Server gesendet werden.

### Konfigurierbare Update Funktion der App und von ML Modellen
Über die Einstellungen der App kann gesteuert werden von welchem Ort und wie oft die App nach neuen Modell und App Updates suchen soll.
Wird eine Aktualisierung gefunden, so wird eine Notifikation gesendet. Mit einem Klick auf die Notifikation wird dann das Update installiert.

### Einblendung eines Kamera-Finders für genauere Bilder
Die ursprüngliche App hatte keinen Kamera-Finder angezeigt. Da die Bilder für das ML Modell aus der Mitte herrausgeschnitten wurden, statt das ganze angezeigte Bild zu benutzen, kommt es zu einem Missverständnis, was genau das ML Modell auswertet.
Um dem entgegen zu wirken wird in diesem Fork ein Rechteck angezeigt, welches den Bereich markiert, den das ML Modell zur Auswertung übergeben wird.

## Vorraussetzungen zum Kompilieren

*   Android Studio 3.2 (installed on a Linux, Mac or Windows machine)

*   Über USB Verbundenes Android Gerät im
    [Entwickler Modus](https://developer.android.com/studio/debug/dev-options)
    mit eingeschalteten USB Debugging.

Das Projekt kann mit Git geklont und in Android Studio gebaut werden.
Über Android Studio kann die Anwendung dann auch auf Android Smartphones installiert werden.