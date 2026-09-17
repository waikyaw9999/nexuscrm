import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Crm } from '../lib/api';
import { ErrorBanner, Modal } from '../components/Shell';

type Contact = {
  id: string;
  firstName: string;
  lastName: string;
  email?: string;
  title?: string;
  phone?: string;
  tags?: string;
};

export function ContactsPage() {
  const [q, setQ] = useState('');
  const [open, setOpen] = useState(false);
  const qc = useQueryClient();
  const list = useQuery({ queryKey: ['contacts', q], queryFn: () => Crm.contacts(q) as Promise<{ items: Contact[]; total: number }> });
  const create = useMutation({
    mutationFn: (body: object) => Crm.createContact(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['contacts'] });
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
          <p className="muted">{list.data?.total ?? 0} people</p>
          <h1>Contacts</h1>
        </div>
        <div className="row">
          <input placeholder="Search" value={q} onChange={(e) => setQ(e.target.value)} style={{ width: 220 }} />
          <button className="btn btn-primary" onClick={() => setOpen(true)}>New contact</button>
        </div>
      </div>
      {list.isError && <ErrorBanner error={list.error} />}
      <section className="panel">
        <table className="table">
          <thead><tr><th>Name</th><th>Email</th><th>Title</th><th>Tags</th></tr></thead>
          <tbody>
            {(list.data?.items || []).map((c) => (
              <tr key={c.id}>
                <td>{c.firstName} {c.lastName}</td>
                <td>{c.email}</td>
                <td>{c.title}</td>
                <td>{c.tags}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {list.isSuccess && list.data.items.length === 0 && <div className="empty">No contacts yet. Add your first buyer.</div>}
      </section>
      {open && (
        <Modal title="New contact" onClose={() => setOpen(false)}>
          <form onSubmit={onSubmit}>
            <label>First name</label><input name="firstName" required />
            <label>Last name</label><input name="lastName" required />
            <label>Email</label><input name="email" type="email" />
            <label>Title</label><input name="title" />
            <label>Tags</label><input name="tags" placeholder="champion, procurement" />
            {create.isError && <ErrorBanner error={create.error} />}
            <button className="btn btn-primary" style={{ marginTop: 16 }}>Save</button>
          </form>
        </Modal>
      )}
    </>
  );
}
