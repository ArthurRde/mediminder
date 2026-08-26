# STATUS.md – Umsetzungsstand MediMinder MVP

Stand: 26.08.2026. Abgleich des kompletten Repositories (Backend, Frontend, Konfiguration, Tests) gegen die MVP-Spezifikation aus der Kurzpräsentation. Vorgehen: vollständiges Code-Review aller Quelldateien plus Testlauf (`./mvnw test`, 7 von 7 grün) und Frontend-Build (`npm run build`, tsc und vite ohne Fehler). Ein kompletter manueller Durchlauf des Kern-Workflows mit laufendem System und zwei Browsern steht noch aus, siehe Abschnitt 4.

## 1. MVP-Kernfunktionen

| # | Kernfunktion | Status | Details |
|---|---|---|---|
| 1 | Pflegekreis-Verwaltung mit Einladung | fertig | Anlegen, Detail mit Mitgliederliste, Einladung nur als ADMIN (`CircleService.invite`), Beitritt per Token als MEMBER (idempotent), Mitglied entfernen nur als ADMIN mit sofortigem Zugriffsentzug. UI: `CirclePage`, `JoinPage`, `OnboardingPage`. |
| 2 | Medikationsplan | fertig | Anlegen, Bearbeiten und Deaktivieren nur als ADMIN (serverseitig via `AccessGuard.requireAdmin`), Lesen für alle Mitglieder. Schedules mit Uhrzeit und Wochentagen, Bestand wird mitgeführt. Schedules werden beim Bearbeiten deaktiviert statt gelöscht, alte Events bleiben referenzierbar. Das UI blendet die Bearbeitung für MEMBER aus. |
| 3 | Tagesansicht "Heute" mit Abhaken | fertig | `GET /circles/{id}/today` erzeugt fehlende Events idempotent (get-or-create über Unique-Constraint, `TodayService`) und liefert eine chronologische Timeline aus Einnahmen, Terminen und heutigen Aufgaben. Overdue-Flag ab 30 min über der Zeit, Frontend pollt alle 30 s. Große Abhak-Buttons (52 px, gefordert waren mindestens 44), erledigt wird mint mit Name und Uhrzeit angezeigt, überfällig amber. |
| 4 | Doppelgabe-Schutz | fertig | Doppelt abgesichert wie spezifiziert: Unique(schedule, event_date) auf `IntakeEvent` plus atomares `UPDATE ... WHERE status = OPEN` (`IntakeEventRepository.confirmIfOpen`). Der Verlierer erhält 409 mit `{confirmedBy, confirmedAt}`, das Frontend zeigt einen freundlichen Hinweis und lädt neu. Bestand geht nur beim Gewinner um 1 runter, nie unter 0. Durch 4 Integrationstests abgedeckt. |
| 5 | Termin- und Aufgabenkalender mit Übernahme | fertig | Anlegen für ADMIN und MEMBER, Übernahme atomar (`claimIfUnassigned`, bei besetzt 409 mit Name), Zuständigkeits-Avatar mit Initialen, Aufgaben abhaken. UI: `CalendarPage` plus Termine und Aufgaben in der Tagesansicht. |

Alle fünf Kernfunktionen sind funktional vollständig. Die offenen Punkte sind Robustheits- und Randfälle (Abschnitt 3), keine fehlenden Features.

## 2. Definition of Done, Punkt für Punkt

Definition of Done des Kern-Workflows laut Projektplanung. Belege aus dem Code-Review; "getestet" heißt durch die 7 Backend-Tests abgedeckt.

| DoD-Schritt | Bewertung | Beleg |
|---|---|---|
| `docker compose up -d` startet DB | erfüllt | `docker-compose.yml` mit PostgreSQL 16, Adminer und Healthcheck. Benötigt `.env` (Vorlage `.env.example` vorhanden). |
| Backend startet mit Seed | erfüllt | `DemoDataSeeder` legt bei leerer DB exakt die spezifizierten Daten an (Familie Rode, Werner 81, Ramipril 08:00/18:00 Bestand 12, Metformin 12:00 Bestand 25, Kardiologe Do 09:30 unbesetzt, "Rezept anfordern", Morgengabe von Sabine um 08:02 bestätigt). |
| Login als sabine@demo.de | erfüllt | `AuthService.login` (BCrypt und JWT), `LoginPage`. |
| Tagesansicht zeigt heutige Gaben | erfüllt | `TodayService.getToday` plus `TodayPage`, Sortierung chronologisch (getestet). |
| Abhaken funktioniert, Bestand geht um 1 runter | erfüllt, getestet | `IntakeService.confirm` plus `MedicationRepository.decrementStock`, Test `ersterConfirmBestaetigtUndReduziertBestand`. |
| Zweiter Browser sieht Bestätigung nach Polling | erfüllt | `TodayPage` pollt alle 30 s (`POLL_INTERVAL_MS = 30_000`), Status samt confirmedBy und confirmedAt im DTO. |
| Zweiter Confirm liefert 409 mit freundlichem Hinweis | erfüllt, getestet | `confirmIfOpen` atomar, 409 mit Name und Uhrzeit, das Frontend zeigt eine Notice statt eines Fehlers. Der Test `zweiterConfirmLiefertKonfliktMitBestaetiger` prüft auch, dass der Bestand nur einmal reduziert wird. |
| Übernehmen des Kardiologen-Termins | erfüllt | `PlannerService.claimAppointment` atomar mit 409-Konflikt, Button in `CalendarPage` und `TodayPage`. |
| Jonas kann Medikationsplan nicht bearbeiten (403, im UI ausgeblendet) | teilweise | Serverseitig korrekt (`AccessGuard.requireAdmin` liefert 403), das UI blendet über `isAdmin` aus. Aber: Für den 403-Fall existiert kein automatisierter Test, nur das curl-Beispiel in `DEMO.md`. |

Zusatzprüfungen aus der Spezifikation: Farbwelt exakt umgesetzt (Petrol, Mint, Amber und Eisblau als CSS-Variablen), Mobile-First mit maximal 480 px zentriert, Bottom-Tabs Heute, Plan, Kalender und Kreis, JWT im Authorization-Header, keine Diagnose-Felder im Datenmodell, `.env` korrekt gitignored.

Fazit zur DoD: Nach Code-Review und Testlage in allen Punkten erfüllbar. Formal "done" aber erst nach einem kompletten manuellen Durchlauf auf frischer DB (P1 in Abschnitt 4) und einem Test für den 403-Fall.

## 3. Bugs und Inkonsistenzen (nach Schwere sortiert)

1. Kein 401-Handling im Frontend, dadurch bricht der Kern-Workflow nach Token-Ablauf. Das JWT läuft nach 24 h ab, User und Token bleiben aber im localStorage. `CircleContext.refresh` (`frontend/src/state/CircleContext.tsx:23`) schluckt den Fehler, `circle` wird null und der User landet auf /onboarding statt /login. Dort könnte er einen zweiten, verwaisten Pflegekreis anlegen; `TodayPage` bliebe sonst dauerhaft auf "Lade Tagesplan...". Es fehlt: bei `ApiError.status === 401` Logout und Redirect auf `/login`.
2. Deaktivierte Medikamente und Schedules bleiben in der Tagesansicht abhakbar. `IntakeEventRepository.findByCircleIdAndDate` (`backend/.../repository/IntakeEventRepository.java:21`) filtert nicht auf `medication.active` bzw. `schedule.active`. Bereits generierte offene Events eines heute deaktivierten Medikaments bleiben sichtbar und bestätigbar. Fachlich heikel, ein abgesetztes Medikament soll nicht weiter als fällig erscheinen.
3. `ddl-auto: update` gefährdet die DB-Ebene des Doppelgabe-Schutzes. Ohne Migrationstool legt Hibernate den Unique-Constraint auf einer bestehenden Datenbank unter Umständen stillschweigend nicht nach, etwa nach Schema-Drift auf einem alten Volume. Das atomare UPDATE bleibt als zweite Ebene wirksam, aber die im README beworbene doppelte Absicherung wäre dann einlagig.
4. Tote Endpunkte ohne Rechte-Differenzierung. `DELETE /api/appointments/{id}` und `DELETE /api/tasks/{id}` werden vom UI nirgends aufgerufen und stehen nicht in der Spezifikation. Trotzdem darf jedes MEMBER darüber fremd angelegte Termine und Aufgaben löschen (nur `requireMember`). Ebenfalls ungenutzt: `MembershipRepository.existsByUserIdAndCareCircleId`.
5. Destruktive Aktionen ohne Fehlerbehandlung und Rückfrage. `MedicationsPage.deactivate` (`frontend/src/pages/MedicationsPage.tsx:44`) hat kein try/catch (Fehler laufen als unhandled rejection ins Leere, das UI reagiert nicht) und keine Bestätigungsabfrage. `CirclePage.removeMember` entfernt Mitglieder ebenfalls ohne Rückfrage.
6. Die 404/403-Reihenfolge leakt die Existenz von IDs. `IntakeService.confirm`, `claimAppointment`, `claimTask` und `delete*` laden die Entität vor der Mitgliedschaftsprüfung. Nicht-Mitglieder können über 404 vs. 403 gültige IDs erraten. Für den MVP tolerierbar, aber leicht zu drehen (erst Mitgliedschaft prüfen, dann 404).
7. Es wird nur der erste Pflegekreis angezeigt. `CircleContext` nimmt `circles[0]` (ältester Beitritt). Tritt ein User mit bestehendem Kreis einem zweiten per Einladungslink bei, gelingt der Beitritt, die App zeigt aber weiter den alten Kreis, ohne Hinweis. Die Spezifikation verlangt kein Multi-Circle-UI, aber Join-Flow und Anzeige sind inkonsistent.
8. Offene Aufgaben von gestern fehlen in "Heute". `TodayService.tasks` filtert exakt auf das heutige Fälligkeitsdatum. Das ist spec-konform, aber inkonsistent zur Überfällig-Logik der Einnahmen: Eine gestern fällige, offene Aufgabe taucht in der Tagesansicht gar nicht auf, nur im Kalender-Tab.
9. `completeTask` ist nicht konfliktbehandelt. Anders als confirm und claim kein atomares Update und kein 409. Doppeltes Abhaken ist idempotent und harmlos, fällt aber aus dem sonst konsequenten Muster.
10. Testlücken: kein Test für den 403-Fall (MEMBER-Schreibzugriff auf den Medikationsplan, ein expliziter DoD-Punkt), keine Tests der Controller- und Security-Schicht (alles ruft Services direkt), keine Frontend-Tests.
11. Meta und Prozess: `SCOPE.md` fehlt komplett, obwohl das Abweichungsprotokoll als lebendes Dokument geplant ist. `STATUS.md` existierte bis zu diesem Dokument nicht.

Positiv aufgefallen: Berechtigungen werden durchgängig serverseitig erzwungen (`AccessGuard` in jedem Service-Aufruf), die Konfliktbehandlung bei confirm und claim ist sauber atomar statt read-modify-write, der doppelte POST auf `/circles/join` durch React StrictMode wird backendseitig idempotent abgefangen, und Seed-Daten, Farbwelt und UI-Maße entsprechen exakt der Spezifikation.

## 4. Priorisierte Empfehlung: Weg zum stabilen Kern-Workflow

Kern-Workflow: Login, Pflegekreis, Medikationsplan, Tagesansicht, Abhaken samt 409-Konfliktfall. Gemäß Arbeitsregel "Stabilität schlägt Funktionsbreite" ausdrücklich keine neuen Features.

P1, blockiert die DoD:
1. 401-Handling im Frontend (Befund 1). In `api/client.ts` bzw. `AuthContext` bei Status 401 Token und User verwerfen und nach `/login` leiten. Ohne das bricht der Kern-Workflow für jeden wiederkehrenden Nutzer nach 24 h.
2. Die DoD einmal komplett manuell durchspielen (frische DB via `docker compose down -v`, zwei Browser, alle 9 DoD-Schritte inklusive gleichzeitigem Confirm) und das Ergebnis hier dokumentieren. Erst dann ist die DoD formal erfüllt.

P2, härtet den Kern fachlich ab:
3. Active-Filter für die Tagesansicht (Befund 2): `findByCircleIdAndDate` auf aktive Medikamente und Schedules einschränken. Offene Events deaktivierter Medikamente ausblenden, bestätigte historisch belassen.
4. Einen Test für den 403-Fall ergänzen (MEMBER versucht, ein Medikament anzulegen oder zu ändern). Das schließt die einzige Testlücke, die ein expliziter DoD-Punkt ist.

P3, Aufräumen und Prozess:
5. Die toten DELETE-Endpunkte (Befund 4) entweder entfernen oder absichern (ADMIN bzw. Ersteller) und im UI anbieten. Die Entscheidung in `SCOPE.md` festhalten.
6. `deactivate` und `removeMember` mit Fehlerbehandlung und Bestätigungsabfrage versehen (Befund 5).
7. `SCOPE.md` anlegen (Arbeitsregel) und dort auch die bewussten Entscheidungen dokumentieren: kein Flyway (`ddl-auto: update`, Befund 3), Single-Circle-UI (Befund 7), Aufgaben nur mit heutigem Fälligkeitsdatum in "Heute" (Befund 8).

Nicht priorisiert (bewusst offen lassen, nur dokumentieren): die 404/403-Reihenfolge (Befund 6), die `completeTask`-Idempotenz (Befund 9) und die Frontend-Tests (Befund 10, zweiter Teil).
