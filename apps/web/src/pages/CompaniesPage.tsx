import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Crm } from '../lib/api';
import { ErrorBanner, Modal } from '../components/Shell';

type Company = { id: string; name: string; domain?: string; industry?: string; website?: string };

export function CompaniesPage() {
  const [q, setQ] = useState('');
  const [open, setOpen] = useState(false);
  const qc = useQueryClient();
  const list = useQuery({ queryKey: ['companies', q], queryFn: () => Crm.companies(q) as Promise<{ items: Company[]; total: number }> });
  const create = useMutation({
    mutationFn: (body: object) => Crm.createCompany(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['companies'] });
      setOpen(false);
    },
  });

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    create.mutate(Object.fromEntries(new FormData(e.currentTarget).entries()));
  }

  return (
    <>
      <div className="top">
        <div>
          <p className="muted">{list.data?.total ?? 0} accounts</p>
          <h1>Companies</h1>
        </div>
        <div className="row">
          <input placeholder="Search" value={q} onChange={(e) => setQ(e.target.value)} style={{ width: 220 }} />
          <button className="btn btn-primary" onClick={() => setOpen(true)}>New company</button>
        </div>
      </div>
      {list.isError && <ErrorBanner error={list.error} />}
      <section className="panel">
        <table className="table">
          <thead><tr><th>Name</th><th>Domain</th><th>Industry</th></tr></thead>
          <tbody>
            {(list.data?.items || []).map((c) => (
              <tr key={c.id}><td>{c.name}</td><td>{c.domain}</td><td>{c.industry}</td></tr>
            ))}
          </tbody>
        </table>
        {list.isSuccess && list.data.items.length === 0 && <div className="empty">No companies yet.</div>}
      </section>
      {open && (
        <Modal title="New company" onClose={() => setOpen(false)}>
          <form onSubmit={onSubmit}>
            <label>Name</label><input name="name" required />
            <label>Domain</label><input name="domain" />
            <label>Industry</label><input name="industry" />
            <label>Website</label><input name="website" />
            {create.isError && <ErrorBanner error={create.error} />}
            <button className="btn btn-primary" style={{ marginTop: 16 }}>Save</button>
          </form>
        </Modal>
      )}
    </>
  );
}
