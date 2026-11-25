import { useEffect, useState } from 'react';
import { api } from '../api';

const STATUS_LABELS = {
  SUCCEEDED: 'Успешно',
  FAILED: 'Ошибка',
};

export default function ImportHistoryPage() {
  const [operations, setOperations] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api.get('/imports').then((data) => {
      if (active) setOperations(data);
    }).catch((err) => {
      if (active) setError(err.message);
    });
    return () => { active = false; };
  }, []);

  return (
    <section>
      <h1>История импорта</h1>
      {error && <p className="error" role="alert">{error}</p>}
      {!error && operations === null && <p role="status">Загрузка…</p>}
      {operations?.length === 0 && <p>Операций импорта пока нет.</p>}
      {operations?.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">ID операции</th>
                <th scope="col">Статус</th>
                <th scope="col">Пользователь</th>
                <th scope="col">Добавлено объектов</th>
              </tr>
            </thead>
            <tbody>
              {operations.map((operation) => (
                <tr key={operation.id}>
                  <td>{operation.id}</td>
                  <td>{STATUS_LABELS[operation.status]}</td>
                  <td>{operation.username}</td>
                  <td>{operation.status === 'SUCCEEDED' ? operation.addedCount : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
