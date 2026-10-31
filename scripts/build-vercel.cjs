const fs = require('node:fs');
const path = require('node:path');
const backend = new URL(process.env.RENDER_BACKEND_URL || '');
if (backend.protocol !== 'https:' || backend.username || backend.password || backend.pathname !== '/' || backend.search || backend.hash) {
  throw new Error('RENDER_BACKEND_URL must be an HTTPS origin, such as https://your-service.onrender.com');
}
const output = path.join(__dirname, '..', '.vercel', 'output');
fs.mkdirSync(path.join(output, 'static'), { recursive: true });
for (const name of ['dashboard.html', 'dashboard.css', 'dashboard.js', 'payroll-api.js']) {
  fs.copyFileSync(path.join(__dirname, '..', 'src', 'main', 'resources', name),
    path.join(output, 'static', name === 'dashboard.html' ? 'index.html' : name));
}
fs.writeFileSync(path.join(output, 'config.json'), JSON.stringify({
  version: 3,
  routes: [
    { src: '/api/(.*)', dest: `${backend.origin}/api/$1`, headers: { 'Cache-Control': 'no-store' } },
    { handle: 'filesystem' },
    { src: '/', dest: '/index.html' }
  ]
}, null, 2));
