import { useQuery } from '@tanstack/react-query';
import { Crm } from '../lib/api';
import { ErrorBanner } from '../components/Shell';

type Dash = {
  contacts: number;
  deals: number;
  openActivities: number;
  pipelineValue: number;
  wonValue: number;
  stages: { stage: string; count: number }[];
  recentActivities: { id: string; type: string; subject: string; dueAt?: string }[];
};

const money = (n: number) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(n || 0);

export function DashboardPage() {
  const q = useQuery({ queryKey: ['dashboard'], queryFn: () => Crm.dashboard() as Promise<Dash> });
  if (q.isError) return <ErrorBanner error={q.error} />;
  const d = q.data;
  return (
    <>
      <div className="top">
        <div>
          <p className="muted">Workspace overview</p>
          <h1>Dashboard</h1>
        </div>
      </div>
      <div className="kpis">
        <div className="kpi"><span>Contacts</span><b>{d?.contacts ?? '—'}</b></div>
        <div className="kpi"><span>Open pipeline</span><b>{d ? money(d.pipelineValue) : '—'}</b></div>
        <div className="kpi"><span>Won</span><b>{d ? money(d.wonValue) : '—'}</b></div>
        <div className="kpi"><span>Open activities</span><b>{d?.openActivities ?? '—'}</b></div>
      </div>
      <div className="row" style={{ marginTop: 18, alignItems: 'stretch' }}>
        <section className="panel" style={{ flex: 1 }}>
          <h3>Pipeline mix</h3>
          {(d?.stages || []).map((s) => (
            <div key={s.stage} className="row" style={{ justifyContent: 'space-between', padding: '8px 0' }}>
              <span>{s.stage}</span><b>{s.count}</b>
            </div>
          ))}
        </section>
        <section className="panel" style={{ flex: 1.4 }}>
          <h3>Upcoming work</h3>
          {(d?.recentActivities || []).length === 0 && <p className="muted">No activities yet.</p>}
          {(d?.recentActivities || []).map((a) => (
            <div key={a.id} style={{ padding: '10px 0', borderBottom: '1px solid #eee6d8' }}>
              <span className="pill">{a.type}</span> {a.subject}
              <div className="muted">{a.dueAt ? new Date(a.dueAt).toLocaleString() : 'No due date'}</div>
            </div>
          ))}
        </section>
      </div>
    </>
  );
}
