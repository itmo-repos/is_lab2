import { useEffect, useState } from 'react';
import { api } from '../api';

const STATUS_LABELS = {
  SUCCEEDED: 'Успешно',
  FAILED: 'Ошибка',
};

export default function ImportHistoryPage() {
  const [operations, setOperations] = useState(null);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => {
    let active = true;
    api.get('/imports').then((data) => {
      if (active) {
        setOperations(data);
        setError('');
      }
    }).catch((err) => {
      if (active) setError(err.message);
    });
    return () => { active = false; };
  }, [revision]);

  async function upload(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const file = form.elements.importFile.files[0];
    if (!file || uploading) return;

    setUploadError('');
    setResult(null);
    setUploading(true);
    try {
      setResult(await api.upload('/imports', file));
      form.reset();
    } catch (err) {
      setUploadError(err.message);
    } finally {
      setUploading(false);
      setRevision((value) => value + 1);
    }
  }

  return (
    <section>
      <h1>История импорта</h1>
      <form onSubmit={upload}>
        <p>
          <label>
            Файл .ser (до 1 МиБ){' '}
            <input type="file" name="importFile" accept=".ser" disabled={uploading} required />
          </label>
          <button className="primary" disabled={uploading}>
            {uploading ? 'Импорт выполняется…' : 'Импортировать'}
          </button>
        </p>
      </form>
      {uploadError && <p className="error" role="alert">{uploadError}</p>}
      {result && <p role="status">Операция №{result.id}. Добавлено лабораторных работ: {result.addedCount}.</p>}
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
