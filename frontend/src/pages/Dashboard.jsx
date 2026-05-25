import { useState, useEffect } from 'react';
import { studentAPI, courseAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

export function Dashboard() {
  const { isAdmin, user } = useAuth();
  const [stats, setStats] = useState({ students: 0, courses: 0 });

  useEffect(() => {
    if (isAdmin()) {
      Promise.all([studentAPI.getAll(), courseAPI.getAll()])
        .then(([s, c]) => setStats({ students: s.data.data.length, courses: c.data.data.length }))
        .catch(() => {});
    } else {
      courseAPI.getAll()
        .then(c => setStats(p => ({ ...p, courses: c.data.data.length })))
        .catch(() => {});
    }
  }, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p>Welcome back, {user?.username}</p>
        </div>
      </div>

      <div className="stats-row">
        {isAdmin() && (
          <div className="stat-card">
            <div className="stat-label">Total Students</div>
            <div className="stat-value">{stats.students}</div>
          </div>
        )}
        <div className="stat-card">
          <div className="stat-label">Total Courses</div>
          <div className="stat-value">{stats.courses}</div>
        </div>
      </div>

      <div className="info-banner">
        {isAdmin()
          ? 'You are logged in as Admin. Use the sidebar to manage students and courses.'
          : 'You are logged in as a Student. Browse courses and view your profile.'}
      </div>
    </div>
  );
}

export function StudentsPage() {
  const [students, setStudents] = useState([]);
  const [courses, setCourses]   = useState([]);
  const [loading, setLoading]   = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing]   = useState(null);
  const [search, setSearch]     = useState('');
  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', phone: '', dateOfBirth: '', courseId: ''
  });

  const load = async () => {
    try {
      const [s, c] = await Promise.all([studentAPI.getAll(), courseAPI.getAll()]);
      setStudents(s.data.data);
      setCourses(c.data.data);
    } catch { toast.error('Failed to load data'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditing(null);
    setForm({ firstName: '', lastName: '', email: '', phone: '', dateOfBirth: '', courseId: '' });
    setShowModal(true);
  };

  const openEdit = (s) => {
    setEditing(s);
    setForm({
      firstName: s.firstName, lastName: s.lastName, email: s.email,
      phone: s.phone || '', dateOfBirth: s.dateOfBirth || '',
      courseId: s.course?.courseId || ''
    });
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      ...form,
      courseId: form.courseId ? Number(form.courseId) : null,
      dateOfBirth: form.dateOfBirth || null
    };
    try {
      if (editing) {
        await studentAPI.update(editing.studentId, payload);
        toast.success('Student updated!');
      } else {
        await studentAPI.create(payload);
        toast.success('Student created!');
      }
      setShowModal(false);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Delete student "${name}"?`)) return;
    try {
      await studentAPI.delete(id, false);
      toast.success('Student deleted');
      load();
    } catch (err) { toast.error(err.response?.data?.message || 'Delete failed'); }
  };

  const filtered = students.filter(s =>
    `${s.fullName} ${s.email}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Students</h1>
          <p>{students.length} total students</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Student</button>
      </div>

      <div className="search-bar">
        <input
          type="text"
          placeholder="Search by name or email..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
      </div>

      {loading ? <p className="text-muted">Loading...</p> : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Course</th>
                <th>Enrolled</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(s => (
                <tr key={s.studentId}>
                  <td><strong>{s.fullName}</strong></td>
                  <td>{s.email}</td>
                  <td>{s.phone || '—'}</td>
                  <td>
                    {s.course
                      ? <span className="badge">{s.course.courseCode}</span>
                      : <span className="text-muted">None</span>}
                  </td>
                  <td className="text-muted">
                    {s.enrolledAt ? new Date(s.enrolledAt).toLocaleDateString() : '—'}
                  </td>
                  <td>
                    <button className="btn btn-edit" onClick={() => openEdit(s)} style={{ marginRight: 6 }}>Edit</button>
                    <button className="btn btn-danger" onClick={() => handleDelete(s.studentId, s.fullName)}>Delete</button>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr className="empty-row"><td colSpan={6}>No students found</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h2>{editing ? 'Edit Student' : 'Add Student'}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>&times;</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label>First Name *</label>
                  <input value={form.firstName} required
                    onChange={e => setForm(p => ({ ...p, firstName: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label>Last Name *</label>
                  <input value={form.lastName} required
                    onChange={e => setForm(p => ({ ...p, lastName: e.target.value }))} />
                </div>
              </div>
              <div className="form-group">
                <label>Email *</label>
                <input type="email" value={form.email} required
                  onChange={e => setForm(p => ({ ...p, email: e.target.value }))} />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Phone</label>
                  <input value={form.phone}
                    onChange={e => setForm(p => ({ ...p, phone: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label>Date of Birth</label>
                  <input type="date" value={form.dateOfBirth}
                    onChange={e => setForm(p => ({ ...p, dateOfBirth: e.target.value }))} />
                </div>
              </div>
              <div className="form-group">
                <label>Course</label>
                <select value={form.courseId}
                  onChange={e => setForm(p => ({ ...p, courseId: e.target.value }))}>
                  <option value="">— No course —</option>
                  {courses.map(c => (
                    <option key={c.courseId} value={c.courseId}>
                      {c.courseName} ({c.courseCode})
                    </option>
                  ))}
                </select>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-edit" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editing ? 'Update' : 'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export function CoursesPage() {
  const { isAdmin } = useAuth();
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing]   = useState(null);
  const [form, setForm] = useState({ courseName: '', courseCode: '', courseDuration: '', description: '' });

  const load = async () => {
    try { const r = await courseAPI.getAll(); setCourses(r.data.data); }
    catch { toast.error('Failed to load courses'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditing(null);
    setForm({ courseName: '', courseCode: '', courseDuration: '', description: '' });
    setShowModal(true);
  };

  const openEdit = (c) => {
    setEditing(c);
    setForm({ courseName: c.courseName, courseCode: c.courseCode, courseDuration: c.courseDuration, description: c.description || '' });
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = { ...form, courseDuration: Number(form.courseDuration) };
    try {
      if (editing) { await courseAPI.update(editing.courseId, payload); toast.success('Course updated!'); }
      else         { await courseAPI.create(payload); toast.success('Course created!'); }
      setShowModal(false); load();
    } catch (err) { toast.error(err.response?.data?.message || 'Operation failed'); }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Delete course "${name}"?`)) return;
    try { await courseAPI.delete(id); toast.success('Course deleted'); load(); }
    catch (err) { toast.error(err.response?.data?.message || 'Delete failed'); }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Courses</h1>
          <p>{courses.length} available</p>
        </div>
        {isAdmin() && (
          <button className="btn btn-primary" onClick={openCreate}>+ Add Course</button>
        )}
      </div>

      {loading ? <p className="text-muted">Loading...</p> : (
        <div className="card-grid">
          {courses.map(c => (
            <div key={c.courseId} className="card">
              <span className="badge">{c.courseCode}</span>
              <h3>{c.courseName}</h3>
              <p>{c.description || 'No description.'}</p>
              <div className="card-footer">
                <span>{c.enrolledCount || 0} students &bull; {c.courseDuration} weeks</span>
                {isAdmin() && (
                  <div>
                    <button className="btn btn-edit" onClick={() => openEdit(c)} style={{ marginRight: 6 }}>Edit</button>
                    <button className="btn btn-danger" onClick={() => handleDelete(c.courseId, c.courseName)}>Delete</button>
                  </div>
                )}
              </div>
            </div>
          ))}
          {courses.length === 0 && <p className="text-muted">No courses yet.</p>}
        </div>
      )}

      {showModal && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h2>{editing ? 'Edit Course' : 'Add Course'}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>&times;</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Course Name *</label>
                <input value={form.courseName} required
                  onChange={e => setForm(p => ({ ...p, courseName: e.target.value }))} />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Course Code *</label>
                  <input value={form.courseCode} required
                    onChange={e => setForm(p => ({ ...p, courseCode: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label>Duration (weeks) *</label>
                  <input type="number" min={1} value={form.courseDuration} required
                    onChange={e => setForm(p => ({ ...p, courseDuration: e.target.value }))} />
                </div>
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea rows={3} value={form.description}
                  onChange={e => setForm(p => ({ ...p, description: e.target.value }))} />
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-edit" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editing ? 'Update' : 'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export function ProfilePage() {
  const { user, isAdmin } = useAuth();
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    if (!isAdmin()) {
      studentAPI.getMe().then(r => setProfile(r.data.data)).catch(() => {});
    }
  }, []);

  return (
    <div>
      <div className="page-header">
        <h1>My Profile</h1>
      </div>

      <div className="profile-section">
        <h2>Account Info</h2>
        <div className="profile-grid">
          <div className="profile-field">
            <label>Username</label>
            <span>{user?.username}</span>
          </div>
          <div className="profile-field">
            <label>Email</label>
            <span>{user?.email}</span>
          </div>
          <div className="profile-field">
            <label>Role</label>
            <span>{isAdmin() ? 'Administrator' : 'Student'}</span>
          </div>
        </div>
      </div>

      {!isAdmin() && profile && (
        <div className="profile-section">
          <h2>Student Details</h2>
          <div className="profile-grid">
            <div className="profile-field">
              <label>Full Name</label>
              <span>{profile.fullName}</span>
            </div>
            <div className="profile-field">
              <label>Phone</label>
              <span>{profile.phone || '—'}</span>
            </div>
            <div className="profile-field">
              <label>Date of Birth</label>
              <span>{profile.dateOfBirth || '—'}</span>
            </div>
            <div className="profile-field">
              <label>Enrolled At</label>
              <span>{profile.enrolledAt ? new Date(profile.enrolledAt).toLocaleDateString() : '—'}</span>
            </div>
          </div>
          {profile.course && (
            <div style={{ marginTop: 16, padding: '12px 14px', background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: 4 }}>
              <div style={{ fontSize: 12, color: '#666', marginBottom: 4 }}>Enrolled Course</div>
              <strong>{profile.course.courseName}</strong>
              <span style={{ color: '#666', fontSize: 13 }}> — {profile.course.courseCode} &bull; {profile.course.courseDuration} weeks</span>
            </div>
          )}
        </div>
      )}

      {!isAdmin() && !profile && (
        <div className="profile-section">
          <p className="text-muted">No student profile linked to this account yet.</p>
        </div>
      )}
    </div>
  );
}

export default Dashboard;
