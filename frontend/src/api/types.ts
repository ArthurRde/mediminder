export interface UserDto {
  id: number
  name: string
  email: string
}

export interface AuthResponse {
  token: string
  user: UserDto
}

export type Role = 'ADMIN' | 'MEMBER'

export interface CircleSummary {
  id: number
  name: string
  role: Role
  patientName: string | null
}

export interface Member {
  userId: number
  name: string
  email: string
  role: Role
  joinedAt: string
}

export interface Patient {
  id: number
  name: string
  birthYear: number | null
  note: string | null
}

export interface CircleDetail {
  id: number
  name: string
  myRole: Role
  inviteToken: string | null
  patient: Patient | null
  members: Member[]
}

export interface Schedule {
  id: number
  timeOfDay: string
  daysOfWeek: string[]
  active: boolean
}

export interface Medication {
  id: number
  name: string
  dosage: string
  stockCount: number
  active: boolean
  schedules: Schedule[]
}

export interface Intake {
  id: number
  time: string
  medicationName: string
  dosage: string
  status: 'OPEN' | 'CONFIRMED'
  confirmedBy: string | null
  confirmedAt: string | null
  overdue: boolean
}

export interface Appointment {
  id: number
  title: string
  dateTime: string
  location: string | null
  assignedToId: number | null
  assignedToName: string | null
}

export interface Task {
  id: number
  title: string
  dueDate: string
  status: 'OPEN' | 'DONE'
  assignedToId: number | null
  assignedToName: string | null
}

export interface TodayResponse {
  date: string
  intakes: Intake[]
  appointments: Appointment[]
  tasks: Task[]
}

export interface InviteDto {
  inviteToken: string
  joinPath: string
}
