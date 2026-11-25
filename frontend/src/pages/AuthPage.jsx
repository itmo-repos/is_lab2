import { useState } from 'react';
import { api } from '../api';
import AuthParticles from '../components/AuthParticles';

export default function AuthPage({ onLogin, sessionError }) {
  const [register, setRegister] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  async function submit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const fields = new FormData(form);
    const credentials = { username: fields.get('username'), password: fields.get('password') };
    setError('');
    setMessage('');
    setBusy(true);

    try {
      if (register) {
        await api.post('/auth/register', credentials);
        form.elements.password.value = '';
        setRegister(false);
        setMessage('Аккаунт создан. Теперь можно войти.');
      } else {
        onLogin(await api.post('/auth/login', credentials));
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <AuthParticles className="auth-particles" particleColors={['#000000']}
        particleBaseSize={300} moveParticlesOnHover />
      <header className="auth-header">Лабораторные работы</header>
      <div className="auth-card">
        <h1>{register ? 'Регистрация' : 'Вход в систему'}</h1>
        <form onSubmit={submit}>
          <label>
            Логин
            <input name="username" autoComplete="username" minLength={3} maxLength={32} required />
          </label>
          <label>
            Пароль
            <input name="password" type="password" autoComplete={register ? 'new-password' : 'current-password'}
              minLength={8} maxLength={128} required />
          </label>
          {(error || sessionError) && <p className="auth-feedback error" role="alert">{error || sessionError}</p>}
          {message && <p className="auth-feedback" role="status">{message}</p>}
          <div className="auth-buttons">
            <button className="auth-switch" type="button" disabled={busy} onClick={() => {
              setRegister(!register);
              setError('');
              setMessage('');
            }}>
              {register ? 'Ко входу' : 'Регистрация'}
            </button>
            <button className="primary" disabled={busy}>
              {register ? 'Создать аккаунт' : 'Войти'}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}
