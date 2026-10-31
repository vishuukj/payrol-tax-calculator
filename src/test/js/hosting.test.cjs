const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
const net = require('node:net');

async function main() {
  const listener = net.createServer();
  await new Promise(resolve => listener.listen(0, '127.0.0.1', resolve));
  const port = listener.address().port;
  await new Promise(resolve => listener.close(resolve));
  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'payroll-hosting-'));
  const server = spawn('java', ['-cp', ['build/classes', 'lib/*'].join(path.delimiter), 'payroll.Main', '--dashboard'], {
    env: { ...process.env, BIND_HOST: '0.0.0.0', PORT: String(port), DATABASE_PATH: path.join(temp, 'payroll.db'),
      RENDER_EXTERNAL_HOSTNAME: 'payroll-test.onrender.com', PUBLIC_ORIGIN: 'https://payroll-test.vercel.app' },
    stdio: 'ignore'
  });
  let startupError;
  server.on('error', error => { startupError = error; });
  const request = (route, method = 'GET', headers = {}, body = '') => new Promise((resolve, reject) => {
    const req = http.request({ hostname: '127.0.0.1', port, path: route, method,
      headers: { Host: 'payroll-test.onrender.com', ...headers } }, res => {
      let data = ''; res.on('data', part => { data += part; });
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, data }));
    });
    req.on('error', reject); req.end(body);
  });
  try {
    let session;
    for (let attempt = 0; attempt < 100; attempt++) {
      if (startupError) throw startupError;
      try { session = await request('/api/auth/session'); break; }
      catch { await new Promise(resolve => setTimeout(resolve, 100)); }
    }
    assert.equal(session?.status, 200, 'server starts using hosted environment');
    assert.match(session.headers['set-cookie'][0], /; Secure/);
    assert.equal((await request('/', 'GET', { Host: 'attacker.example' })).status, 403);
    const headers = { Cookie: session.headers['set-cookie'][0].split(';')[0],
      'X-CSRF-Token': JSON.parse(session.data).csrfToken, 'Content-Type': 'application/x-www-form-urlencoded',
      Origin: 'https://attacker.example' };
    assert.equal((await request('/api/auth/setup', 'POST', headers, 'username=Owner&password=test-password-123')).status, 403);
    headers.Origin = 'https://payroll-test.vercel.app';
    const setup = await request('/api/auth/setup', 'POST', headers, 'username=Owner&password=test-password-123');
    assert.equal(setup.status, 201, setup.data);
    assert.equal(JSON.parse(setup.data).authenticated, true);
    const config = JSON.parse(fs.readFileSync('.vercel/output/config.json', 'utf8'));
    assert.equal(config.version, 3);
    assert.match(config.routes[0].dest, /^https:\/\/[^/]+\/api\/\$1$/);
    assert.ok(fs.existsSync('.vercel/output/static/index.html'));
    console.log('Hosted binding, host restrictions, Secure cookies, origin/CSRF, and Vercel output checks passed.');
  } finally {
    const exited = new Promise(resolve => server.once('exit', resolve));
    if (server.exitCode === null) { server.kill(); await exited; }
    fs.rmSync(temp, { recursive: true, force: true });
  }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
