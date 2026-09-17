import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Auth } from '../lib/api';

export function LoginPage() {
  const nav = useNavigate();
  const qc = useQueryClient();
  const [email, setEmail] = useState('oscar.d@example.net');
  const [password, setPassword] = useState('Demo1234!');
  const [error, setError] = useState('');

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await Auth.login(email, password);
      await qc.invalidateQueries({ queryKey: ['me'] });
      nav('/app');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to sign in');
    }
  }

  return (
    <div className="auth-shell">
      <section className="auth-brand">
        <div>
          <div className="mark"><span>N</span> NexusCRM</div>
          <h1>The operating system for customer relationships.</h1>
          <p>Multi-tenant pipeline, activities, and mailbox in one workspace — built for teams that sell.</p>
        </div>
        <p className="muted">Demo workspace: Acme Robotics</p>
      </section>
      <section className="auth-card">
        <form className="card" onSubmit={onSubmit}>
          <h2>Sign in</h2>
          <p className="muted">Use the seeded Acme Robotics owner account or your own workspace.</p>
          {error && <div className="error">{error}</div>}
          <label>Email</label>
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
          <label>Password</label>
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required />
          <div className="row" style={{ marginTop: 18 }}>
            <button className="btn btn-primary" type="submit">Enter workspace</button>
            <Link to="/register">Create a workspace</Link>
          </div>
        </form>
      </section>
    </div>
  );
}
