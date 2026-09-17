import { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Crm } from '../lib/api';
import { ErrorBanner } from '../components/Shell';

type Profile = { id: string; email: string; fullName: string };
type Tenant = { id: string; name: string; slug: string; plan: string };
type Member = { membershipId: string; email: string; fullName: string; role: string };
type Plan = { current: string; available: string[]; stripeEnabled: boolean };

export function SettingsPage() {
  const qc = useQueryClient();
  const profile = useQuery({ queryKey: ['profile'], queryFn: () => Crm.profile() as Promise<Profile> });
  const tenant = useQuery({ queryKey: ['tenant'], queryFn: () => Crm.tenant() as Promise<Tenant> });
  const team = useQuery({ queryKey: ['team'], queryFn: () => Crm.team() as Promise<Member[]> });
  const plan = useQuery({ queryKey: ['plan'], queryFn: () => Crm.plan() as Promise<Plan> });
  const saveProfile = useMutation({
    mutationFn: (body: object) => Crm.updateProfile(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profile'] }),
  });
  const saveTenant = useMutation({
    mutationFn: (body: object) => Crm.updateTenant(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tenant'] });
      qc.invalidateQueries({ queryKey: ['me'] });
    },
  });
  const invite = useMutation({ mutationFn: (body: object) => Crm.invite(body) });
  const checkout = useMutation({
    mutationFn: (p: string) => Crm.checkout(p),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['plan'] }),
  });

  function onProfile(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    saveProfile.mutate(Object.fromEntries(new FormData(e.currentTarget).entries()));
  }
  function onTenant(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    saveTenant.mutate(Object.fromEntries(new FormData(e.currentTarget).entries()));
  }
  function onInvite(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    invite.mutate(Object.fromEntries(new FormData(e.currentTarget).entries()));
  }

  return (
    <>
      <div className="top">
        <div>
          <p className="muted">Workspace, team, plan</p>
          <h1>Settings</h1>
        </div>
      </div>
      {(profile.isError || tenant.isError) && <ErrorBanner error={profile.error || tenant.error} />}
      <div className="row" style={{ alignItems: 'stretch' }}>
        <form className="panel" style={{ flex: 1 }} onSubmit={onProfile}>
          <h3>Profile</h3>
          <label>Full name</label>
          <input name="fullName" defaultValue={profile.data?.fullName} />
          <p className="muted">{profile.data?.email}</p>
          <button className="btn btn-primary" style={{ marginTop: 12 }}>Save</button>
        </form>
        <form className="panel" style={{ flex: 1 }} onSubmit={onTenant}>
          <h3>Workspace</h3>
          <label>Name</label>
          <input name="name" defaultValue={tenant.data?.name} />
          <p className="muted">/{tenant.data?.slug} · {tenant.data?.plan}</p>
          <button className="btn btn-primary" style={{ marginTop: 12 }}>Save</button>
        </form>
      </div>
      <section className="panel" style={{ marginTop: 16 }}>
        <h3>Team</h3>
        <table className="table">
          <thead><tr><th>Name</th><th>Email</th><th>Role</th></tr></thead>
          <tbody>
            {(team.data || []).map((m) => (
              <tr key={m.membershipId}><td>{m.fullName}</td><td>{m.email}</td><td>{m.role}</td></tr>
            ))}
          </tbody>
        </table>
        <form className="row" onSubmit={onInvite} style={{ marginTop: 12 }}>
          <input name="email" type="email" placeholder="invite@company.com" required />
          <select name="role" defaultValue="MEMBER"><option>ADMIN</option><option>MEMBER</option><option>VIEWER</option></select>
          <button className="btn btn-ghost">Invite</button>
        </form>
        {invite.isSuccess && <p className="ok">Invite created. Share the token from the API response in v1; email delivery is not wired.</p>}
        {invite.isError && <ErrorBanner error={invite.error} />}
      </section>
      <section className="panel" style={{ marginTop: 16 }}>
        <h3>Plan</h3>
        <p className="muted">Current: {plan.data?.current}. Stripe checkout is stubbed for v1.</p>
        <div className="row">
          {(plan.data?.available || ['FREE', 'PRO', 'BUSINESS']).map((p) => (
            <button key={p} className="btn btn-ghost" onClick={() => checkout.mutate(p)}>{p}</button>
          ))}
        </div>
        {checkout.isSuccess ? <p className="ok">{(checkout.data as { message: string }).message}</p> : null}
      </section>
    </>
  );
}
