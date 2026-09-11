# Optionale Codesignatur

Der GitHub-Workflow signiert EXE, MSI und die portable Programmdatei automatisch,
sobald diese Repository-Secrets vorhanden sind:

- `WINDOWS_CERTIFICATE_BASE64`: Base64-kodierte PFX-Datei
- `WINDOWS_CERTIFICATE_PASSWORD`: Kennwort der PFX-Datei

Ohne diese Secrets bleibt der Build funktionsfähig, wird aber als unsigniert
gekennzeichnet. Ein öffentlich vertrauenswürdiges Code-Signing-Zertifikat muss von
einer Zertifizierungsstelle bezogen werden und kann nicht vom Projekt erzeugt werden.
