# STATUS.md – Umsetzungsstand MediMinder MVP

Stand: 26.08.2026, nach der Stabilisierungs-Runde für den Kern-Workflow und der Testabdeckungs-Runde. Grundlage: Code-Review, Backend-Tests (`./mvnw test`, 27 von 27 grün), Frontend-Build (`npm run build` ohne Fehler) und ein kompletter manueller Durchlauf des Abnahmetests per curl gegen das laufende System auf frischer Datenbank, mit zwei User-Sessions.

## 1. MVP-Kernfunktionen

| # | Kernfunktion | Status | Details |
|---|---|---|---|
| 1 | Pflegekreis-Verwaltung mit Einladung | fertig, verifiziert | Anlegen, Mitgliederliste, Einladung nur als ADMIN (MEMBER bekommt 403), Beitritt per Token als MEMBER (idempotent). Entfernen nur als ADMIN, mit sofortigem Zugriffsentzug und Rückfrage im UI. |
| 2 | Medikationsplan | fertig, verifiziert | Ändern darf nur der ADMIN, serverseitig erzwungen (403, inzwischen auch getestet) und im UI ausgeblendet. Schedules werden beim Bearbeiten deaktiviert statt gelöscht. Deaktivieren fragt nach und behandelt Fehler. |
| 3 | Tagesansicht "Heute" mit Abhaken | fertig, verifiziert | Idempotente Event-Generierung, chronologische Timeline, overdue ab 30 min Verspätung, Polling alle 30 s. Erledigt wird mint mit Name und Uhrzeit angezeigt, überfällig amber. Offene Einnahmen deaktivierter Medikamente oder Schedules werden ausgeblendet, bestätigte bleiben als Historie sichtbar. |
| 4 | Doppelgabe-Schutz | fertig, verifiziert | Unique(schedule, event_date) plus atomares `UPDATE ... WHERE status = OPEN`. Der Verlierer erhält 409 mit `{confirmedBy, confirmedAt}`, das Frontend zeigt einen freundlichen Hinweis und lädt neu. Bestand geht nur beim Gewinner um 1 runter. Live per curl nachgestellt. |
| 5 | Termin- und Aufgabenkalender mit Übernahme | fertig, verifiziert | Übernahme atomar, bei besetzt 409 mit Name (live nachgestellt, zweite Übernahme liefert "Schon von Jonas übernommen."). Ungenutzte DELETE-Endpunkte wurden entfernt, siehe SCOPE.md. |

## 2. Abnahmetest, manueller Durchlauf am 26.08.2026 (frische DB)

| Schritt | Ergebnis |
|---|---|
| Registrierung neuer User | ok, 200 mit JWT. Doppelte E-Mail liefert 409 "Diese E-Mail-Adresse ist bereits registriert." |
| Login sabine@demo.de, falsches Passwort | ok. Falsches Passwort liefert 401 "E-Mail oder Passwort ist falsch." |
| Ungültiges oder abgelaufenes Token | ok, 401. Das Frontend verwirft die Session und leitet zum Login. |
| Tagesansicht als Sabine | ok, chronologisch (08:00, 12:00, 18:00). Morgengabe CONFIRMED durch Sabine um 08:02, overdue-Flag passt zur Tageszeit (12:00-Gabe um ca. 18 Uhr überfällig, 18:00-Gabe nicht). |
| Abhaken einer offenen Einnahme | ok, 200. Metformin-Bestand ging von 25 auf 24. |
| Zweiter Confirm als Jonas | ok, 409 mit "Bereits von Sabine um 17:57 bestätigt." und `{confirmedBy, confirmedAt}`. Bestand blieb bei 24. |
| Jonas (MEMBER) ändert Medikationsplan | ok, 403 serverseitig (auch per Integrationstest abgedeckt). Das UI blendet die Bearbeitung aus. |
| Einladungslink | ok. ADMIN erhält Token, MEMBER bekommt beim Einladen 403. Neuer User tritt bei und erscheint als MEMBER in der Mitgliederliste. |
| Übernahme Kardiologen-Termin | ok durch Jonas. Zweite Übernahme liefert 409 mit Name. |

Farbwelt, Mobile-First (max. 480 px), Bottom-Tabs, JWT im Authorization-Header, keine Diagnose-Felder und gitignorte `.env` sind unverändert erfüllt.

Hinweis: Der Testdurchlauf hat die Seed-Daten verändert (Bestand, Termin-Zuweisung, dritter User). Für eine frische Demo `docker compose down -v`, dann neu starten. Der Seed läuft bei leerer DB automatisch.

## 3. In den Stabilisierungs-Runden behoben

1. 401-Handling im Frontend. Eine abgelaufene oder ungültige Session wird zentral im API-Client erkannt, Token und User werden verworfen, es geht zurück zum Login. Vorher landete ein abgelaufener User im Onboarding und konnte dort verwaiste Zweit-Kreise anlegen.
2. Active-Filter für die Tagesansicht. Offene Events deaktivierter Medikamente oder Schedules erscheinen nicht mehr als fällig, was bei abgesetzten Medikamenten fachlich heikel war. Bestätigte bleiben sichtbar. Zwei neue Integrationstests.
3. Die 403-Testlücke ist geschlossen. `MedicationAccessTest` deckt create, update und deactivate als MEMBER sowie den Zugriff als Nicht-Mitglied ab.
4. Tote DELETE-Endpunkte für Termine und Aufgaben entfernt, inklusive einer ungenutzten Repository-Methode. Begründung in SCOPE.md.
5. Destruktive Aktionen abgesichert: Medikament deaktivieren und Mitglied entfernen haben eine zweistufige Rückfrage im Button und Fehlerbehandlung.
6. Kritische Logiken testabgedeckt, insgesamt 27 Tests. Tagesplan-Generierung (Idempotenz, Chronologie, Wochentags- und Active-Filter für Medikamente und Schedules), overdue-Flag deterministisch als Unit-Test mit Grenzfall bei 30 Minuten, Confirm-Konfliktfall (200 mit Bestand minus 1, 409 mit confirmedBy und confirmedAt, keine doppelte Reduktion, nie unter 0). Bei den Berechtigungen bekommt der MEMBER am Medikationsplan 403, ein Nicht-Mitglied bei jedem Zugriff auf fremde Kreise: Tagesplan, Kreis-Detail, Einladung, Termine und Aufgaben lesen, anlegen, übernehmen, abhaken, Medikation ändern.
7. Frontend defensiv gehärtet. Netzwerkfehler erscheinen als verständliche Meldung ("Server nicht erreichbar...") statt als roher Fetch-Fehler. Schlägt das Laden des Pflegekreises fehl, gibt es eine Fehlermeldung mit "Erneut versuchen" statt eines falschen Onboarding-Redirects. Die Tagesansicht zeigt bei einem Erstladefehler einen Retry an statt dauerhaft "Lade...". Ein abgelaufenes Token führt weiterhin zentral zum Login-Redirect.

## 4. Bekannte offene Punkte (bewusst zurückgestellt, siehe auch SCOPE.md)

- Die 404/403-Reihenfolge erlaubt Nicht-Mitgliedern das Erraten gültiger IDs. Für den MVP tolerierbar.
- `completeTask` hat keine Konfliktbehandlung. Doppeltes Abhaken ist idempotent und harmlos.
- Es wird nur der erste Pflegekreis angezeigt (Single-Circle-UI, dokumentiert).
- Offene Aufgaben von gestern erscheinen nur im Kalender-Tab, nicht in "Heute" (dokumentiert).
- Kein Migrationstool, `ddl-auto: update` (dokumentiert). Keine Frontend-Tests und keine Tests der Controller-/Security-Schicht, die Services werden direkt getestet.

Keiner dieser Punkte blockiert den Kern-Workflow. Sie sind Kandidaten für eine Ausbaustufe.
