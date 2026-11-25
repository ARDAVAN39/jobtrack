import type { Stats } from '../api/types'

export function StatsBar({ stats }: { stats: Stats }) {
  return (
    <section className="stats" aria-label="Summary">
      <Stat label="Total" value={stats.total} />
      <Stat label="In progress" value={stats.active} />
      <Stat label="Interviews" value={stats.byStatus.INTERVIEWING} />
      <Stat label="Offers" value={stats.byStatus.OFFER} />
      <Stat
        label="Interview rate"
        value={stats.interviewRate === null ? '–' : `${stats.interviewRate}%`}
        hint="Share of sent applications that led to an interview or offer"
      />
    </section>
  )
}

function Stat({ label, value, hint }: { label: string; value: number | string; hint?: string }) {
  return (
    <div className="stat" title={hint}>
      <span className="stat-value">{value}</span>
      <span className="stat-label">{label}</span>
    </div>
  )
}
