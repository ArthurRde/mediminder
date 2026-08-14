# MediMinder - Demo-Anleitung

Schritt-für-Schritt-Drehbuch für eine Live-Demo (ca. 5-10 Minuten), z. B. bei der Abgabe oder in Usability-Tests.

## Vorbereitung (einmalig, ~3 Minuten)

Voraussetzungen: Docker, Java 21+, Node 20+.

```bash
# Terminal 1: Datenbank
docker compose up -d

# Terminal 2: Backend (wartet kurz, bis die DB healthy ist)
cd backend
./mvnw spring-boot:run

# Terminal 3: Frontend
cd frontend
npm install   # nur beim ersten Mal
npm run dev
```

Fertig, wenn das Backend im Log `Demo-Daten angelegt: Pflegekreis 'Familie Rode'...` meldet und Vite `http://localhost:5173` anzeigt.

Frischer Zustand gewünscht? `docker compose down -v` löscht die Datenbank samt Volume. Beim nächsten Start werden die Demo-Daten neu angelegt, inklusive der bereits bestätigten Morgengabe.

## Demo-Zugänge

| Rolle   | E-Mail           | Passwort   |
|---------|------------------|------------|
| ADMIN   | `sabine@demo.de` | `demo1234` |
| MEMBER  | `jonas@demo.de`  | `demo1234` |

Beide gehören zum Pflegekreis "Familie Rode" mit Patient Werner (81).

Adminer für den DB-Einblick (optional): http://localhost:8081, System `PostgreSQL`, Server `db`, User, Passwort und DB jeweils `mediminder`.

## Demo-Drehbuch

Setup: Zwei Browserfenster nebeneinander (oder Browser plus Inkognito-Fenster).
Fenster A = Sabine, Fenster B = Jonas, jeweils http://localhost:5173.

### 1. Tagesansicht "Heute" (Startscreen)

Als Sabine anmelden. Zu sehen:
- Chronologische Liste der heutigen Gaben: Ramipril 08:00 (bereits mint mit "✓ Sabine · 08:02"), Metformin 12:00, Ramipril 18:00
- Überfällige Gaben (mehr als 30 Minuten über der Uhrzeit) haben einen Amber-Rahmen und den Hinweis "überfällig"
- Unten die Aufgabe "Rezept anfordern"

### 2. Geteilter Status

- In Fenster B als Jonas anmelden, gleiche Tagesansicht.
- Sabine (A) hakt die 18:00-Gabe ab. Die Karte wird mint mit Name und Uhrzeit.
- Jonas (B) sieht die Bestätigung nach spätestens 30 Sekunden automatisch (Polling), bei Reload sofort.

### 3. Doppelgabe-Schutz (das Kernfeature)

Frische DB verwenden (siehe oben), dann:
- In beiden Fenstern liegt dieselbe offene Gabe, z. B. Metformin 12:00.
- Jonas (B) hakt sie ab.
- Sabine (A) hakt dieselbe Gabe ab, bevor ihr Polling aktualisiert hat.
- Ergebnis ist kein Fehler, sondern der Hinweis "Bereits von Jonas um HH:MM bestätigt." Die Liste aktualisiert sich. Serverseitig war das ein HTTP 409, und das atomare Update garantiert, dass der Bestand nur einmal reduziert wird.

### 4. Übernahme-Funktion

- Tab Kalender: Der Termin "Kardiologe" (nächsten Donnerstag, 09:30) ist unbesetzt.
- Jonas klickt "Übernehmen", sein Initialen-Avatar erscheint.
- Klickt Sabine mit veralteter Ansicht ebenfalls auf Übernehmen, kommt "Schon von Jonas übernommen." (409).

### 5. Rollen und Berechtigungen

- Tab Plan als Sabine (ADMIN): Medikamente anlegen, bearbeiten und deaktivieren geht.
- Gleicher Tab als Jonas (MEMBER): Die Buttons sind ausgeblendet. Die API lehnt Schreibzugriffe zusätzlich serverseitig mit 403 ab, was sich per `curl` zeigen lässt:

```bash
# Als Jonas einloggen und Token holen
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"jonas@demo.de","password":"demo1234"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# Medikament anlegen als MEMBER, erwartet: 403
curl -i -X POST http://localhost:8080/api/circles/1/medications \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Test","dosage":"1 mg","stockCount":5,"schedules":[{"timeOfDay":"09:00","daysOfWeek":["MONDAY"]}]}'
```

### 6. Pflegekreis und Einladung

- Tab Kreis als Sabine: Mitgliederliste mit Rollen-Badges, "Einladungslink kopieren", Mitglied entfernen.
- Optional zeigen: Link in einem neuen Inkognito-Fenster öffnen und ein neues Konto registrieren. Der User tritt automatisch als MEMBER bei.

### 7. Onboarding (optional)

Ein neues Konto ohne Einladungslink registrieren. Es folgt ein geführter Assistent: Pflegekreis, Patient, erstes Medikament, Einladungslink.

## Alles wieder runterfahren

```bash
# Backend und Frontend: Ctrl+C in den jeweiligen Terminals
docker compose down        # DB stoppen, Daten bleiben erhalten
docker compose down -v     # DB stoppen UND Demo-Daten zurücksetzen
```
