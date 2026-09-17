import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Auth } from '../lib/api';

export function RegisterPage() {
  const nav = useNavigate();
  const qc = useQueryClient();
  const [error, setError] = useState('');

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.currentTarget).entries());
    setError('');
    try {
      await Auth.register(data);
      await qc.invalidateQueries({ queryKey: ['me'] });
      nav('/app');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to register');
    }
  }

  return (
    <div className="auth-shell">
      <section className="auth-brand">
        <div>
          <div className="mark"><span>N</span> NexusCRM</div>
          <h1>Stand up a workspace in a minute.</h1>
          <p>Owner role, isolated tenant data, and a mock mailbox so you can demo the full product path.</p>
        </div>
      </section>
      <section className="auth-card">
        <form className="card" onSubmit={onSubmit}>
          <h2>Create workspace</h2>
          {error && <div className="error">{error}</div>}
          <label>Full name</label>
          <input name="fullName" required />
          <label>Work email</label>
          <input name="email" type="email" required />
          <label>Password</label>
          <input name="password" type="password" minLength={8} required />
          <label>Organization</label>
          <input name="organizationName" required />
          <div className="row" style={{ marginTop: 18 }}>
            <button className="btn btn-primary">Create</button>
            <Link to="/login">Back to sign in</Link>
          </div>
        </form>
      </section>
    </div>
  );
}
