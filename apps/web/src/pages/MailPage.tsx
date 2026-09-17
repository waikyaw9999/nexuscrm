import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Crm } from '../lib/api';
import { ErrorBanner, Modal } from '../components/Shell';

type Box = { id: string; provider: string; emailAddress: string; displayName?: string };
type Msg = {
  id: string;
  subject?: string;
  fromAddress?: string;
  toAddresses?: string;
  bodyText?: string;
  folder: string;
  sentAt?: string;
};

export function MailPage() {
  const qc = useQueryClient();
  const [folder, setFolder] = useState('INBOX');
  const [selected, setSelected] = useState<string | null>(null);
  const [compose, setCompose] = useState(false);
  const boxes = useQuery({ queryKey: ['mailboxes'], queryFn: () => Crm.mailboxes() as Promise<Box[]> });
  const box = boxes.data?.[0];
  const messages = useQuery({
    queryKey: ['messages', box?.id, folder],
    queryFn: () => Crm.messages(box!.id, folder) as Promise<Msg[]>,
    enabled: !!box,
  });
  const detail = useQuery({
    queryKey: ['message', box?.id, selected],
    queryFn: () => Crm.message(box!.id, selected!) as Promise<Msg>,
    enabled: !!box && !!selected,
  });
  const connect = useMutation({
    mutationFn: () => Crm.connectMock(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['mailboxes'] }),
  });
  const send = useMutation({
    mutationFn: (body: object) => Crm.send(box!.id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['messages'] });
      setCompose(false);
    },
  });

  function onSend(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    send.mutate(Object.fromEntries(new FormData(e.currentTarget).entries()));
  }

  return (
    <>
      <div className="top">
        <div>
          <p className="muted">{box ? `${box.provider} · ${box.emailAddress}` : 'No mailbox connected'}</p>
          <h1>Mail</h1>
        </div>
        <div className="row">
          {!box && <button className="btn btn-primary" onClick={() => connect.mutate()}>Connect demo mailbox</button>}
          {box && <button className="btn btn-primary" onClick={() => setCompose(true)}>Compose</button>}
          <a className="btn btn-ghost" href="/bff/oauth/gmail">Connect Gmail</a>
        </div>
      </div>
      {boxes.isError && <ErrorBanner error={boxes.error} />}
      {messages.isError && <ErrorBanner error={messages.error} />}
      {!box && <div className="panel empty">Connect the mock mailbox to browse inbox, sent, and drafts without OAuth.</div>}
      {box && (
        <div className="mail-layout">
          <aside className="panel">
            {['INBOX', 'SENT', 'DRAFTS'].map((f) => (
              <div key={f} className={`list-item ${folder === f ? 'active' : ''}`} onClick={() => { setFolder(f); setSelected(null); }}>{f}</div>
            ))}
          </aside>
          <section className="panel">
            {(messages.data || []).map((m) => (
              <div key={m.id} className={`list-item ${selected === m.id ? 'active' : ''}`} onClick={() => setSelected(m.id)}>
                <strong>{m.subject || '(no subject)'}</strong>
                <div className="muted">{m.fromAddress}</div>
              </div>
            ))}
            {messages.isSuccess && messages.data.length === 0 && <div className="empty">This folder is empty.</div>}
          </section>
          <article className="panel">
            {!selected && <div className="empty">Select a message</div>}
            {detail.data && (
              <>
                <h3>{detail.data.subject}</h3>
                <p className="muted">{detail.data.fromAddress} → {detail.data.toAddresses}</p>
                <p style={{ whiteSpace: 'pre-wrap' }}>{detail.data.bodyText}</p>
              </>
            )}
          </article>
        </div>
      )}
      {compose && box && (
        <Modal title="Compose" onClose={() => setCompose(false)}>
          <form onSubmit={onSend}>
            <label>To</label><input name="to" required />
            <label>Subject</label><input name="subject" required />
            <label>Body</label><textarea name="body" rows={6} />
            {send.isError && <ErrorBanner error={send.error} />}
            <button className="btn btn-primary" style={{ marginTop: 16 }}>Send</button>
          </form>
        </Modal>
      )}
    </>
  );
}
