import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const authAPI = {
  login:    (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
};

export const courseAPI = {
  getAll:   ()         => api.get('/courses'),
  getById:  (id)       => api.get(`/courses/${id}`),
  create:   (data)     => api.post('/courses', data),
  update:   (id, data) => api.put(`/courses/${id}`, data),
  delete:   (id)       => api.delete(`/courses/${id}`),
};

export const studentAPI = {
  getAll:        ()             => api.get('/students'),
  getById:       (id)           => api.get(`/students/${id}`),
  getMe:         ()             => api.get('/students/me'),
  getByCourse:   (courseId)     => api.get(`/students/course/${courseId}`),
  create:        (data)         => api.post('/students', data),
  update:        (id, data)     => api.put(`/students/${id}`, data),
  delete:        (id, force)    => api.delete(`/students/${id}?force=${force}`),
};

export default api;
