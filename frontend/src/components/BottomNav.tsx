import { NavLink } from 'react-router-dom'

const tabs = [
  { to: '/', label: 'Heute', icon: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zm-1.2 12.6-3.3-3.3 1.4-1.4 1.9 1.9 4.3-4.3 1.4 1.4-5.7 5.7z' },
  { to: '/plan', label: 'Plan', icon: 'M8.5 3A5.5 5.5 0 0 0 4.6 12.4l7 7a5.5 5.5 0 1 0 7.8-7.8l-7-7A5.46 5.46 0 0 0 8.5 3zm-2.5 8 5-5 5.6 5.6-5 5z' },
  { to: '/kalender', label: 'Kalender', icon: 'M7 2v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2H7zm-2 8h14v10H5V10z' },
  { to: '/kreis', label: 'Kreis', icon: 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-3.3 0-7 1.7-7 4v2h14v-2c0-2.3-3.7-4-7-4zm8.5-2.5a3 3 0 1 0-2-5.6 6 6 0 0 1 0 5.3c.6.2 1.3.3 2 .3zM17 13.2c1.8.8 3 2 3 3.8v2h2v-2c0-1.7-2.2-3.2-5-3.8z' },
]

export default function BottomNav() {
  return (
    <nav className="bottom-nav">
      {tabs.map((tab) => (
        <NavLink key={tab.to} to={tab.to} end={tab.to === '/'} className="nav-item">
          <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true">
            <path d={tab.icon} fill="currentColor" />
          </svg>
          <span>{tab.label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
