import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          EduTrack
          <span>Student Manager</span>
        </div>

        <nav>
          <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>Dashboard</NavLink>
          {isAdmin() && (
            <NavLink to="/students" className={({ isActive }) => isActive ? 'active' : ''}>Students</NavLink>
          )}
          <NavLink to="/courses" className={({ isActive }) => isActive ? 'active' : ''}>Courses</NavLink>
          <NavLink to="/profile" className={({ isActive }) => isActive ? 'active' : ''}>My Profile</NavLink>
        </nav>

        <div className="sidebar-footer">
          <div className="username">{user?.username}</div>
          <div style={{ fontSize: 11, color: '#a0b4c8', marginBottom: 8 }}>
            {isAdmin() ? 'Admin' : 'Student'}
          </div>
          <button className="btn-logout" onClick={handleLogout}>Logout</button>
        </div>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
