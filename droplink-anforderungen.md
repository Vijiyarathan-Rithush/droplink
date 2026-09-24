# DropLink - Anforderungen

## Zweck

DropLink ermöglicht es, Dateien schnell, direkt und zuverlässig zwischen zwei Geräten im gleichen Netzwerk zu übertragen.

## Kernanforderungen

- Dateien schnell und sauber senden
- einzelne oder mehrere Dateien auswählen
- Geräte im lokalen Netzwerk finden
- Geräte manuell über IP-Adresse verbinden
- Übertragungsanfrage anzeigen
- Anfrage akzeptieren oder ablehnen
- Übertragungsfortschritt anzeigen
- Übertragung abbrechen können
- Dateigrösse und Dateiintegrität prüfen
- Fehler verständlich anzeigen
- Verbindung nach der Übertragung sauber beenden
- Oberfläche bleibt während der Übertragung bedienbar

## Übertragung

- TCP für zuverlässige Dateiübertragung
- Dateien streamen und nicht vollständig in den Arbeitsspeicher laden
- Dateiname, Dateigrösse und Prüfsumme übertragen
- temporäre Datei während des Empfangs verwenden
- empfangene Datei erst nach erfolgreicher Prüfung endgültig speichern
- beschädigte oder unvollständige Dateien nicht als fertig anzeigen
- Timeouts für Verbindungen und Antworten
- sauberer Abbruch von Netzwerkverbindungen und Threads

## Sicherheit

- Empfänger muss jeder Übertragung zustimmen
- Dateien dürfen nur im ausgewählten Zielordner gespeichert werden
- Schutz gegen ungültige Pfade und Path Traversal
- Dateigrössen und Nachrichtenlängen validieren
- keine Passwörter oder privaten Daten in Logs
- spätere Erweiterung um verschlüsselte Verbindungen möglich

## Architektur

- klare Trennung zwischen Domain, Application, Infrastructure und Presentation
- keine JavaFX-Abhängigkeiten in der Domain
- Netzwerkcode nicht in JavaFX-Controllern
- Abhängigkeiten über Interfaces
- unveränderliche Datenobjekte, wo sinnvoll
- keine unnötigen Schichten oder künstlichen Klassen
- keine Klassen mit `UseCase` im Namen
- konkrete Datei- und Klassennamen werden während der Umsetzung gemeinsam entschieden

## Bewusste Einschränkungen

- keine Datenbank
- kein Cloud-Service
- kein Login
- keine Benutzerkonten
- kein Übertragungsverlauf
- keine zentrale Serveranwendung
- keine unnötigen Enterprise-Funktionen

## Technischer Rahmen

- Java
- JavaFX
- Maven
- Git und GitHub
- produktionsnaher, wartbarer und testbarer Code
- Installer erst nach der funktionierenden Kernfunktion

## Technische Vereinbarungen

- jedes Gerät ist gleichzeitig Sender und Empfänger
- jedes Gerät startet einen TCP-Server und kann ausgehende TCP-Verbindungen aufbauen
- TCP wird für Dateiübertragungen verwendet
- UDP oder mDNS wird nur für die Gerätesuche verwendet
- pro Übertragung wird zunächst eine eigene TCP-Verbindung verwendet
- eine spätere dauerhafte bidirektionale Verbindung bleibt möglich
- Dateien werden gestreamt und nicht vollständig in den Arbeitsspeicher geladen
- jede Übertragung besitzt einen eigenen Status und kann separat abgebrochen werden
- Netzwerkoperationen dürfen den JavaFX-Thread nicht blockieren
- alle Netzwerk- und Dateioperationen müssen abbrechbar sein
- Verbindungen, Streams und Threads werden zuverlässig geschlossen
- Eingaben, Dateigrössen und Nachrichtenlängen werden validiert
- empfangene Dateien werden zuerst temporär gespeichert
- eine Datei gilt erst nach erfolgreicher Integritätsprüfung als fertig
- keine Datenbank
- keine Cloud
- kein Login und keine Benutzerkonten
- keine REST-API
- keine unnötigen Frameworks
- keine Klassen mit `UseCase` im Namen
- keine `Impl`-Klassen
- keine globalen Zustände
- Abhängigkeiten werden über Konstruktoren übergeben
- JavaFX darf nicht in der Domain-Schicht verwendet werden
- Netzwerkcode darf nicht direkt in UI-Controllern liegen
- `var` wird nicht verwendet
- Records werden für passende unveränderliche Datenobjekte verwendet
- wichtige Geschäftslogik und technische Komponenten werden getestet

## Peer-to-Peer-Transceiver

DropLink ist eine bidirektionale Peer-to-Peer-Anwendung. Beide Geräte besitzen dieselben Fähigkeiten.

Jedes Gerät:

- wartet über einen TCP-Server auf eingehende Verbindungen
- kann über einen TCP-Client andere Geräte verbinden
- kann Dateien senden
- kann Dateien empfangen
- kann gleichzeitig senden und empfangen

Die Begriffe Sender und Empfänger gelten nur für eine einzelne Übertragung. Ein Gerät ist nicht dauerhaft nur Sender oder nur Empfänger.

Für die erste Version wird pro Übertragung eine eigene TCP-Verbindung verwendet. Dadurch bleiben Übertragungen voneinander getrennt und können einfacher abgebrochen, getestet und behandelt werden.

## Fertigstellung

DropLink ist für die erste Version fertig, wenn zwei Geräte Dateien zuverlässig übertragen können, der Fortschritt sichtbar ist, Abbruch und Fehlerbehandlung funktionieren und die Integrität der empfangenen Dateien geprüft wird.
