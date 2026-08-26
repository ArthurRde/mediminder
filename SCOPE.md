# SCOPE.md – Abweichungsprotokoll MediMinder

Abgleich zwischen dem MVP, wie er in der Kurzpräsentation kommuniziert wurde (verbindliche Baseline), und der tatsächlichen Umsetzung bei Abgabe. Lebendes Dokument: Jede Scope-Entscheidung (weglassen, vereinfachen, verschieben) wird hier zeitnah nachgetragen. Der detaillierte Ist-Stand steht in `STATUS.md`.

Stand: 26.08.2026

## MVP-Kernfunktionen und Stretch Goal

| Funktion (geplant laut Präsentation) | Status bei Abgabe | Begründung der Priorisierung |
|---|---|---|
| 1. Pflegekreis anlegen & Mitglieder per Einladungslink (Rollen: Verwalter/Mitglied) | umgesetzt | Grundlage für alles Weitere. Ohne Kreis, Rollen und serverseitige Mitgliedschaftsprüfung sind weder Berechtigungen noch der geteilte Status demonstrierbar. Die Rollen sind bewusst auf ADMIN und MEMBER beschränkt, das reicht für alle geforderten Berechtigungsfälle. |
| 2. Digitaler Medikationsplan mit Dosierung & Einnahmezeiten | umgesetzt | Fachlicher Kern der App und Datengrundlage der Tagesansicht. Die Bearbeitung ist serverseitig auf ADMIN beschränkt, nicht nur im UI, weil falsche Medikationsdaten das größte fachliche Risiko sind. Eine Vereinfachung gibt es: Schedules werden beim Bearbeiten deaktiviert statt gelöscht, damit historische Einnahme-Events referenzierbar bleiben. |
| 3. Tagesansicht "Heute" mit Abhak-Funktion (Erledigt mit Name & Uhrzeit, Ampellogik) | umgesetzt | Wichtigster Screen und Einstieg in den Kern-Workflow, daher zuerst und am gründlichsten gebaut (idempotente Event-Generierung, Overdue-Logik, Polling alle 30 s). Der geteilte Status läuft über Polling statt Push oder WebSockets, die einfachste tragfähige Lösung, siehe unten. |
| 4. Doppelgabe-Schutz (sofortige Sichtbarkeit, Konfliktbehandlung bei gleichzeitiger Bestätigung) | umgesetzt | Höchste Priorität, weil Alleinstellungsmerkmal. Deshalb doppelt abgesichert (Unique-Constraint auf DB-Ebene plus atomares Update) und als einziger Bereich mit dedizierten Integrationstests für den Konfliktfall: 409 mit Name und Uhrzeit, Bestand wird nur einmal reduziert. |
| 5. Termin- & Aufgabenkalender mit Zuweisung und Übernahme | umgesetzt | Komplettiert den Anspruch der Familien-Koordination. Die Übernahme nutzt dasselbe atomare Konfliktmuster wie der Doppelgabe-Schutz (409 mit Name bei besetzt). Wenig Zusatzaufwand, konsistentes Verhalten. |
| Stretch: Bestands- & Reichweitenberechnung mit Rezept-Erinnerung | teilweise, bewusst reduziert | Der Bestand (`stockCount`) wird mitgeführt und beim Bestätigen atomar dekrementiert, nie unter 0. Das Datenfundament steht also. Reichweitenberechnung und Rezept-Erinnerung wurden verschoben: Sie waren als Stretch kommuniziert, und die Zeit floss stattdessen in die Härtung des Kern-Workflows ("Stabilität schlägt Funktionsbreite"). |

## Von Beginn an ausgeklammert (Ausbaustufe, so kommuniziert)

Diese Punkte waren nie Teil des MVP-Versprechens und wurden erwartungsgemäß nicht gebaut:

- Vereinfachte "Mein Tag"-Ansicht für die pflegebedürftige Person (Rolle "Betroffener")
- Anbindung ambulanter Pflegedienste
- Native Push-Benachrichtigungen (geteilter Status stattdessen über Polling alle 30 s), Mehrsprachigkeit

## Bewusste Vereinfachungen innerhalb des MVP

Entscheidungen unterhalb der Funktionsebene, die den Scope real geprägt haben:

| Entscheidung | Status | Begründung |
|---|---|---|
| Kein Migrationstool (Flyway/Liquibase), stattdessen `ddl-auto: update` | bewusst weggelassen | Für einen Prototyp mit Seed-Daten und `docker compose down -v` als Reset ausreichend. Das Risiko ist bekannt: Bei Schema-Drift auf einem alten Volume könnte der Unique-Constraint fehlen. Das atomare Update bleibt als zweite Schutzebene wirksam. |
| Ein Pflegekreis pro Nutzer im UI (Backend erlaubt mehrere Mitgliedschaften) | bewusst vereinfacht | Das Szenario (eine Familie, ein Pflegekreis) braucht kein Multi-Circle-UI. Bekannte Inkonsistenz: Der Beitritt zu einem zweiten Kreis gelingt, angezeigt wird weiter der erste. Dokumentiert statt gebaut. |
| Tagesansicht zeigt nur Aufgaben mit Fälligkeit = heute (keine überfälligen von gestern) | bewusst vereinfacht | Spec-konform umgesetzt. Die Überfällig-Logik gilt nur für Einnahmen, wo sie fachlich kritisch ist (Doppelgabe, vergessene Gabe). Überfällige Aufgaben bleiben im Kalender-Tab sichtbar. |
| DELETE-Endpunkte für Termine/Aufgaben entfernt (26.08.2026) | bewusst entfernt | Standen nicht in der Spezifikation, wurden vom UI nie aufgerufen und erlaubten jedem MEMBER das Löschen fremder Einträge. Entfernt statt abgesichert: kein UI-Bedarf, und ein toter, falsch berechtigter Endpunkt ist ein Risiko ohne Nutzen. Löschen von Terminen und Aufgaben wäre bei Bedarf eine Ausbaustufe. |

## Leitprinzip der Priorisierung

Priorisiert wurde durchgehend der Kern-Workflow von Login über Tagesansicht und Abhaken bis zum Konfliktfall und zur Termin-Übernahme, weil er das Alleinstellungsmerkmal Doppelgabe-Schutz trägt und ein stabiler, nachvollziehbarer Ablauf laut Bewertungskriterien wichtiger ist als Funktionsbreite. Konkret hieß das, Berechtigungen und Konfliktbehandlung serverseitig und atomar zu bauen statt weitere Features anzufangen, und den Testaufwand dort zu konzentrieren, wo das fachliche Risiko am größten ist. Was diesem Kern nicht diente, wurde verschoben (Reichweitenberechnung) oder einfach gehalten (Polling statt Push, Single-Circle-UI). Jede dieser Entscheidungen steht hier im Protokoll statt stillschweigend getroffen zu sein.
