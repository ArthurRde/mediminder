export function fmtTime(time: string): string {
  return time.slice(0, 5)
}

export function fmtClock(isoDateTime: string): string {
  return new Date(isoDateTime).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })
}

export function fmtDateTime(isoDateTime: string): string {
  return new Date(isoDateTime).toLocaleString('de-DE', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function fmtDate(isoDate: string): string {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString('de-DE', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
  })
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}
