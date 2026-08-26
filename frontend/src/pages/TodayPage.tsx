import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import type { Appointment, Intake, Task, TodayResponse } from '../api/types'
import { fmtClock, fmtTime } from '../format'
import { useCircle } from '../state/CircleContext'
import Avatar from '../components/Avatar'

const POLL_INTERVAL_MS = 30_000

type TimelineEntry =
  | { kind: 'intake'; time: string; intake: Intake }
  | { kind: 'appointment'; time: string; appointment: Appointment }

function buildTimeline(data: TodayResponse): TimelineEntry[] {
  const entries: TimelineEntry[] = [
    ...data.intakes.map((intake) => ({ kind: 'intake' as const, time: fmtTime(intake.time), intake })),
    ...data.appointments.map((appointment) => ({
      kind: 'appointment' as const,
      time: fmtClock(appointment.dateTime),
      appointment,
    })),
  ]
  return entries.sort((a, b) => a.time.localeCompare(b.time))
}

export default function TodayPage() {
  const { circle } = useCircle()
  const [data, setData] = useState<TodayResponse | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  // Polling-Fehler bei vorhandenen Daten ignorieren, der nächste Poll kommt eh
  const load = useCallback(async () => {
    try {
      setData(await api.get<TodayResponse>(`/circles/${circle!.id}/today`))
      setLoadError(null)
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : 'Tagesplan konnte nicht geladen werden.')
    }
  }, [circle])

  useEffect(() => {
    load()
    const id = setInterval(load, POLL_INTERVAL_MS)
    return () => clearInterval(id)
  }, [load])

  const handle = async (action: () => Promise<unknown>) => {
    try {
      await action()
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setNotice(err.message)
      } else if (err instanceof Error) {
        setNotice(err.message)
      }
    }
    await load()
  }

  const confirmIntake = (intake: Intake) => handle(() => api.post(`/intake-events/${intake.id}/confirm`))

  const claimAppointment = (appointment: Appointment) =>
    handle(() => api.post(`/appointments/${appointment.id}/claim`))

  const completeTask = (task: Task) => handle(() => api.post(`/tasks/${task.id}/done`))

  if (!data) {
    if (loadError) {
      return (
        <>
          <p className="error">{loadError}</p>
          <button className="btn btn-primary" onClick={() => load()}>
            Erneut versuchen
          </button>
        </>
      )
    }
    return <p>Lade Tagesplan…</p>
  }

  const timeline = buildTimeline(data)
  const openTasks = data.tasks.filter((task) => task.status === 'OPEN')
  const doneTasks = data.tasks.filter((task) => task.status === 'DONE')

  return (
    <>
      <header className="page-header">
        <h1>Heute</h1>
        <p className="muted">
          {new Date().toLocaleDateString('de-DE', { weekday: 'long', day: 'numeric', month: 'long' })}
          {circle?.patientName ? ` · für ${circle.patientName}` : ''}
        </p>
      </header>

      {notice && (
        <div className="notice" role="status" onClick={() => setNotice(null)}>
          {notice} <span className="muted">(Tippen zum Schließen)</span>
        </div>
      )}

      {timeline.length === 0 && <p className="muted">Heute stehen keine Einnahmen oder Termine an.</p>}

      {timeline.map((entry) =>
        entry.kind === 'intake' ? (
          <IntakeCard key={`i-${entry.intake.id}`} intake={entry.intake} onConfirm={confirmIntake} />
        ) : (
          <AppointmentCard
            key={`a-${entry.appointment.id}`}
            appointment={entry.appointment}
            onClaim={claimAppointment}
          />
        ),
      )}

      {(openTasks.length > 0 || doneTasks.length > 0) && <h2>Aufgaben heute</h2>}
      {openTasks.map((task) => (
        <div className="card row" key={task.id}>
          <div className="grow">
            <strong>{task.title}</strong>
            {task.assignedToName && <span className="muted"> · {task.assignedToName}</span>}
          </div>
          <button className="check-btn" onClick={() => completeTask(task)} aria-label="Aufgabe abhaken">
            ✓
          </button>
        </div>
      ))}
      {doneTasks.map((task) => (
        <div className="card row done" key={task.id}>
          <div className="grow">
            <strong>{task.title}</strong>
            <span className="confirmed-note">✓ {task.assignedToName ?? 'erledigt'}</span>
          </div>
        </div>
      ))}
    </>
  )
}

function IntakeCard({ intake, onConfirm }: { intake: Intake; onConfirm: (intake: Intake) => void }) {
  const confirmed = intake.status === 'CONFIRMED'
  const classes = ['card', 'row', confirmed ? 'confirmed' : '', intake.overdue ? 'overdue' : '']
  return (
    <div className={classes.join(' ').trim()}>
      <div className="time">{fmtTime(intake.time)}</div>
      <div className="grow">
        <strong>{intake.medicationName}</strong> <span className="muted">{intake.dosage}</span>
        {confirmed && intake.confirmedAt && (
          <span className="confirmed-note">
            ✓ {intake.confirmedBy} · {fmtClock(intake.confirmedAt)}
          </span>
        )}
        {intake.overdue && <span className="overdue-note">überfällig</span>}
      </div>
      {!confirmed && (
        <button className="check-btn" onClick={() => onConfirm(intake)} aria-label="Einnahme bestätigen">
          ✓
        </button>
      )}
    </div>
  )
}

function AppointmentCard({
  appointment,
  onClaim,
}: {
  appointment: Appointment
  onClaim: (appointment: Appointment) => void
}) {
  return (
    <div className="card row appointment">
      <div className="time">{fmtClock(appointment.dateTime)}</div>
      <div className="grow">
        <strong>{appointment.title}</strong>
        {appointment.location && <span className="muted"> · {appointment.location}</span>}
        <span className="muted small">Termin</span>
      </div>
      {appointment.assignedToName ? (
        <Avatar name={appointment.assignedToName} />
      ) : (
        <button className="btn btn-outline" onClick={() => onClaim(appointment)}>
          Übernehmen
        </button>
      )}
    </div>
  )
}
