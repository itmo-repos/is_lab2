import { useEffect, useState } from 'react';
import LabWorksPage from './pages/LabWorksPage';
import VisualizationPage from './pages/VisualizationPage';
import ReferencesPage from './pages/ReferencesPage';
import SpecialPage from './pages/SpecialPage';
import AuthPage from './pages/AuthPage';
import { api } from './api';

const ROUTES = [
  { path: '#/labworks', title: 'Лабораторные', page: LabWorksPage },
  { path: '#/visualization', title: 'Визуализация', page: VisualizationPage },
  { path: '#/references', title: 'Справочники', page: ReferencesPage },
  { path: '#/special', title: 'Спецоперации', page: SpecialPage },
];

function useHash() {
  const [hash, setHash] = useState(window.location.hash);
  useEffect(() => {
    const onChange = () => setHash(window.location.hash);
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return hash;
}

export default function App() {
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);
  const [sessionError, setSessionError] = useState('');
  const [loggingOut, setLoggingOut] = useState(false);
  const hash = useHash();

  useEffect(() => {
    let active = true;
    const expire = () => {
      setAccount(null);
      setSessionError('');
    };
    window.addEventListener('lab-session-expired', expire);
    api.get('/auth/me').then((user) => {
      if (active) setAccount(user);
    }).catch((error) => {
      if (active && error.status !== 401) setSessionError(error.message);
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => {
      active = false;
      window.removeEventListener('lab-session-expired', expire);
    };
  }, []);

  async function logout() {
    setLoggingOut(true);
    setSessionError('');
    try {
      await api.post('/auth/logout');
      setAccount(null);
    } catch (error) {
      if (error.status === 401) setAccount(null);
      else setSessionError(error.message);
    } finally {
      setLoggingOut(false);
    }
  }

  if (loading) return <main>Загрузка…</main>;
  if (!account) {
    return (
      <AuthPage sessionError={sessionError} onLogin={(user) => {
        setSessionError('');
        setAccount(user);
      }} />
    );
  }

  const route = ROUTES.find((r) => r.path === hash) ?? ROUTES[0];
  const Page = route.page;

  return (
    <div className="layout">
      <nav className="menu">
        {ROUTES.map((r) => (
          <a key={r.path} href={r.path} className={r === route ? 'active' : ''}>
            {r.title}
          </a>
        ))}
        <div className="menu-account">
          <span>{account.username} · {account.role === 'ADMIN' ? 'Администратор' : 'Пользователь'}</span>
          <button disabled={loggingOut} onClick={logout}>Выйти</button>
        </div>
      </nav>
      <main>
        {sessionError && <p className="error" role="alert">{sessionError}</p>}
        <Page />
      </main>
    </div>
  );
}
