# Windows-Integration

Der Installer erstellt Startmenü- und Desktop-Verknüpfungen. DropLink akzeptiert
außerdem einen Dateipfad als Startargument, sodass eine spätere Shell-Integration
die Datei direkt in der Senden-Ansicht vorauswählen kann.

Für automatische Firewall- und Explorer-Kontextmenü-Einträge ist ein signiertes,
administratives WiX-Installationspaket nötig. Diese Eingriffe werden bewusst nicht
heimlich beim Programmstart ausgeführt. Bis ein Code-Signing-Zertifikat hinterlegt
ist, fragt Windows beim ersten eingehenden Netzwerkzugriff selbst nach der Freigabe.
