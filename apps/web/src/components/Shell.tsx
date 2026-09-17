import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Auth, Me } from '../lib/api';

const links = [
  ['/app', 'Dashboard'],
  ['/app/contacts', 'Contacts'],
  ['/app/companies', 'Companies'],
  ['/app/pipeline', 'Pipeline'],
  ['/app/activities', 'Activities'],
  ['/app/mail', 'Mail'],
  ['/app/settings', 'Settings'],
];

export function Shell({ me, children }: { me: Me; children: ReactNode }) {
  const nav = useNavigate();
  const qc = useQueryClient();
  return (
    <div className="app">
      <aside className="sidebar">
        <div className="mark" style={{ marginBottom: 18 }}>
          <span>N</span> NexusCRM
        </div>
        {links.map(([to, label]) => (
          <NavLink key={to} to={to} end={to === '/app'} className={({ isActive }) => (isActive ? 'active' : '')}>
            {label}
          </NavLink>
        ))}
        <div style={{ marginTop: 'auto', padding: 12 }} className="muted">
          <div style={{ color: '#fff', fontWeight: 600 }}>{me.tenantName}</div>
          <div>{me.fullName} · {me.role}</div>
          <button
            className="btn btn-ghost"
            style={{ marginTop: 12, color: '#fff', borderColor: '#355' }}
            onClick={async () => {
              await Auth.logout();
              qc.clear();
              nav('/login');
            }}
          >
            Sign out
          </button>
        </div>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}

export function ErrorBanner({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : 'Something went wrong';
  return <div className="error">{message}</div>;
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <h2>{title}</h2>
          <button className="btn btn-ghost" onClick={onClose}>Close</button>
        </div>
        {children}
      </div>
    </div>
  );
}
