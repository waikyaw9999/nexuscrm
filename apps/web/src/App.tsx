import type { ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Auth, Me } from './lib/api';
import { Shell } from './components/Shell';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { ContactsPage } from './pages/ContactsPage';
import { CompaniesPage } from './pages/CompaniesPage';
import { PipelinePage } from './pages/PipelinePage';
import { ActivitiesPage } from './pages/ActivitiesPage';
import { MailPage } from './pages/MailPage';
import { SettingsPage } from './pages/SettingsPage';

function Guard({ children }: { children: ReactNode }) {
  const me = useQuery({ queryKey: ['me'], queryFn: () => Auth.me() as Promise<Me> });
  if (me.isLoading) return <div className="empty">Loading workspace…</div>;
  if (me.isError || !me.data) return <Navigate to="/login" replace />;
  return <Shell me={me.data}>{children}</Shell>;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/app" element={<Guard><DashboardPage /></Guard>} />
      <Route path="/app/contacts" element={<Guard><ContactsPage /></Guard>} />
      <Route path="/app/companies" element={<Guard><CompaniesPage /></Guard>} />
      <Route path="/app/pipeline" element={<Guard><PipelinePage /></Guard>} />
      <Route path="/app/activities" element={<Guard><ActivitiesPage /></Guard>} />
      <Route path="/app/mail" element={<Guard><MailPage /></Guard>} />
      <Route path="/app/settings" element={<Guard><SettingsPage /></Guard>} />
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  );
}
