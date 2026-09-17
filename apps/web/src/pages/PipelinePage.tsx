import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Crm } from '../lib/api';
import { ErrorBanner, Modal } from '../components/Shell';

const STAGES = ['LEAD', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'] as const;

type Deal = {
  id: string;
  title: string;
  stage: (typeof STAGES)[number];
  amount?: number;
  currency?: string;
};

const money = (n?: number) =>
  n == null ? '' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(n);

export function PipelinePage() {
  const [open, setOpen] = useState(false);
  const qc = useQueryClient();
  const deals = useQuery({ queryKey: ['deals'], queryFn: () => Crm.deals() as Promise<Deal[]> });
  const create = useMutation({
    mutationFn: (body: object) => Crm.createDeal(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['deals'] });
      qc.invalidateQueries({ queryKey: ['dashboard'] });
      setOpen(false);
    },
  });
  const move = useMutation({
    mutationFn: ({ id, stage }: { id: string; stage: string }) => Crm.moveDeal(id, stage),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['deals'] }),
  });

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.currentTarget).entries());
    create.mutate({ ...data, amount: data.amount ? Number(data.amount) : null, stage: data.stage });
  }

  return (
    <>
      <div className="top">
        <div>
          <p className="muted">Kanban by stage</p>
          <h1>Pipeline</h1>
        </div>
        <button className="btn btn-primary" onClick={() => setOpen(true)}>New deal</button>
      </div>
      {deals.isError && (
        <ErrorBanner error={deals.error} />
      )}
      {deals.isError && (
        <div className="panel">Could not load pipeline. Refresh or check the API — this is not an empty board.</div>
      )}
      {deals.isSuccess && (
        <div className="board">
          {STAGES.map((stage) => {
            const items = deals.data.filter((d) => d.stage === stage);
            return (
              <div className="col" key={stage}>
                <h3>{stage} · {items.length}</h3>
                {items.map((deal) => (
                  <article className="deal" key={deal.id}>
                    <strong>{deal.title}</strong>
                    <div className="muted">{money(deal.amount)}</div>
                    <select
                      value={deal.stage}
                      onChange={(e) => move.mutate({ id: deal.id, stage: e.target.value })}
                      style={{ marginTop: 8 }}
                    >
                      {STAGES.map((s) => <option key={s}>{s}</option>)}
                    </select>
                  </article>
                ))}
              </div>
            );
          })}
        </div>
      )}
      {open && (
        <Modal title="New deal" onClose={() => setOpen(false)}>
          <form onSubmit={onSubmit}>
            <label>Title</label><input name="title" required />
            <label>Amount</label><input name="amount" type="number" step="0.01" />
            <label>Stage</label>
            <select name="stage" defaultValue="LEAD">{STAGES.map((s) => <option key={s}>{s}</option>)}</select>
            {create.isError && <ErrorBanner error={create.error} />}
            <button className="btn btn-primary" style={{ marginTop: 16 }}>Save</button>
          </form>
        </Modal>
      )}
    </>
  );
}
