import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Crm } from '../lib/api';
import { ErrorBanner, Modal } from '../components/Shell';

type Activity = {
  id: string;
  type: string;
  subject: string;
  dueAt?: string;
  completedAt?: string;
};

export function ActivitiesPage() {
  const [open, setOpen] = useState(false);
  const qc = useQueryClient();
  const list = useQuery({ queryKey: ['activities'], queryFn: () => Crm.activities() as Promise<Activity[]> });
  const create = useMutation({
    mutationFn: (body: object) => Crm.createActivity(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['activities'] });
      setOpen(false);
    },
  });
  const complete = useMutation({
    mutationFn: (id: string) => Crm.completeActivity(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['activities'] }),
  });

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.currentTarget).entries());
    create.mutate({
      type: data.type,
      subject: data.subject,
      body: data.body,
      dueAt: data.dueAt ? new Date(String(data.dueAt)).toISOString() : null,
    });
  }

  return (
    <>
      <div className="top">
        <div>
          <p className="muted">Calls, meetings, tasks</p>
          <h1>Activities</h1>
        </div>
        <button className="btn btn-primary" onClick={() => setOpen(true)}>Log activity</button>
      </div>
      {list.isError && <ErrorBanner error={list.error} />}
      <section className="panel">
        {(list.data || []).map((a) => (
          <div key={a.id} className="row" style={{ justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid #eee6d8' }}>
            <div>
              <span className="pill">{a.type}</span> {a.subject}
              <div className="muted">{a.dueAt ? new Date(a.dueAt).toLocaleString() : 'No due date'} {a.completedAt ? '· done' : ''}</div>
            </div>
            {!a.completedAt && <button className="btn btn-ghost" onClick={() => complete.mutate(a.id)}>Complete</button>}
          </div>
        ))}
        {list.isSuccess && list.data.length === 0 && <div className="empty">Nothing scheduled.</div>}
      </section>
      {open && (
        <Modal title="Log activity" onClose={() => setOpen(false)}>
          <form onSubmit={onSubmit}>
            <label>Type</label>
            <select name="type"><option>CALL</option><option>MEETING</option><option>TASK</option><option>NOTE</option></select>
            <label>Subject</label><input name="subject" required />
            <label>Due</label><input name="dueAt" type="datetime-local" />
            <label>Notes</label><textarea name="body" rows={3} />
            {create.isError && <ErrorBanner error={create.error} />}
            <button className="btn btn-primary" style={{ marginTop: 16 }}>Save</button>
          </form>
        </Modal>
      )}
    </>
  );
}
