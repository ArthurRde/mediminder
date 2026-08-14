# MediMinder

Webanwendung, mit der Familien die häusliche Pflege eines Angehörigen gemeinsam koordinieren: digitaler Medikationsplan mit Einnahme-Bestätigung und Doppelgabe-Schutz, geteilter Termin- und Aufgabenkalender mit Übernahme-Funktion und eine Tagesansicht "Heute" mit geteiltem Echtzeit-Status für alle Mitglieder eines Pflegekreises.

> Hochschulprojekt (Modul "Projekt Softwareentwicklung & UX"), Einzelabgabe von Arthur Rode. Es werden ausschließlich fiktive Testdaten verwendet.

## Architektur

Das React-Frontend (Vite, TypeScript) spricht über eine REST-API mit JWT-Authentifizierung das Spring-Boot-Backend an, das alle Geschäftsregeln (Rollen, Doppelgabe-Schutz, Übernahme-Konflikte) serverseitig durchsetzt. Die Daten liegen in PostgreSQL; der Doppelgabe-Schutz ist doppelt abgesichert: ein Unique-Constraint auf `(schedule, date)` verhindert doppelte Tages-Events, und die Bestätigung selbst ist ein atomares `UPDATE ... WHERE status = 'OPEN'`, sodass bei gleichzeitigen Bestätigungen genau eine gewinnt und alle anderen ein 409 mit Name und Uhrzeit des Bestätigers erhalten. Das Frontend pollt die Tagesansicht alle 30 Sekunden und erzeugt so den geteilten "Echtzeit"-Eindruck ohne WebSockets.

```
mediminder/
├── backend/           Spring Boot 3 (Java 21, Maven-Wrapper), REST-API + JWT
├── frontend/          React 18 + TypeScript (Vite), Mobile-First
├── docker-compose.yml PostgreSQL 16 + Adminer
└── README.md
```

## Setup (unter 5 Minuten)

Voraussetzungen: Docker, Java 21+, Node 20+.

```bash
# 1. Datenbank starten (PostgreSQL auf :5432, Adminer auf http://localhost:8081)
docker compose up -d

# 2. Backend starten (http://localhost:8080) - legt beim ersten Start Demo-Daten an
cd backend
./mvnw spring-boot:run

# 3. Frontend starten (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Danach http://localhost:5173 öffnen und mit einem Demo-Login anmelden.

### Demo-Logins

| E-Mail          | Passwort   | Rolle im Pflegekreis "Familie Rode" |
|-----------------|------------|-------------------------------------|
| sabine@demo.de  | `demo1234` | ADMIN                               |
| jonas@demo.de   | `demo1234` | MEMBER                              |

Die Demo enthält Patient Werner (81), die Medikamente Ramipril 5 mg (08:00 & 18:00) und Metformin 500 mg (12:00), einen unbesetzten Kardiologen-Termin und die Aufgabe "Rezept anfordern". Die Morgengabe ist bereits von Sabine bestätigt, damit der geteilte Status sofort sichtbar ist.

**Den geteilten Status ausprobieren:** In einem zweiten Browser(-profil) als jonas@demo.de anmelden. Hakt Sabine eine Einnahme ab, sieht Jonas sie nach spätestens 30 Sekunden (Polling) als erledigt. Bestätigen beide gleichzeitig, gewinnt genau eine Bestätigung - die andere Person bekommt freundlich "Bereits von Sabine um 08:02 bestätigt" angezeigt. Jonas kann als MEMBER den Medikationsplan nicht bearbeiten (im UI ausgeblendet, serverseitig 403).

### Tests

```bash
cd backend
./mvnw test
```

Abgedeckt sind die zwei kritischen Logiken: die idempotente Tagesplan-Generierung (Mehrfachaufrufe erzeugen keine doppelten Events) und der Confirm-Konfliktfall (zweite Bestätigung -> 409 mit Bestätiger und Uhrzeit, Bestand wird nur einmal und nie unter 0 reduziert).

## Was ist bewusst nicht drin (Ausbaustufe)

- Vereinfachte "Mein Tag"-Ansicht für die Betroffenen-Rolle
- Automatische Reichweiten-/Nachbestell-Erinnerung (der Bestand wird nur mitgeführt)
- Push-Notifications/WebSockets (stattdessen 30-Sekunden-Polling)
- Pflegedienst-Anbindung
- Mehrsprachigkeit
- Passwort-Reset per E-Mail

Außerdem werden aus Datenminimierungsgründen bewusst keine Diagnose-Felder modelliert (es handelt sich um Gesundheitsdaten i. S. v. Art. 9 DSGVO).
