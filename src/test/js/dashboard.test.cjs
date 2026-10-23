const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const html = fs.readFileSync('src/main/resources/dashboard.html', 'utf8');
const script = fs.readFileSync('src/main/resources/dashboard.js', 'utf8');
const apiScript = fs.readFileSync('src/main/resources/payroll-api.js', 'utf8');
const elements = new Map();
function element(id) {
    if (!elements.has(id))
        elements.set(id, { value: id.endsWith('tax_rate') ? '20' : id.endsWith('bonus') ? '0' : id === 'sort' ? 'newest' : id === 'chart-mode' ? 'employees' : '', checked: true, dataset: {}, innerHTML: '', textContent: '', hidden: false,
            attributes: {},
            classList: {
                values: new Set(),
                toggle(name, enabled) { if (enabled) this.values.add(name); else this.values.delete(name); },
                contains(name) { return this.values.has(name); },
                add(name) { this.values.add(name); }, remove(name) { this.values.delete(name); }
            },
            addEventListener() { }, setAttribute(name, value) { this.attributes[name] = value; }, removeAttribute(name) { delete this.attributes[name]; }, querySelectorAll() { return []; }, querySelector() { return { focus() { } }; }, focus() { this.focused = true; }, click() { }, showModal() { this.open = true; }, close() { this.open = false; } });
    return elements.get(id);
}
let download;
const sandbox = { console, Number, String, Map, Date, URLSearchParams, Blob, AbortController, clearTimeout() {}, setTimeout: fn => fn(), matchMedia: () => ({ matches: false }), localStorage: { getItem() { }, setItem() { } },
    URL: { createObjectURL(blob) { download = blob; return 'blob:test'; }, revokeObjectURL() { } },
    document: { addEventListener() { }, getElementById: element, querySelectorAll: () => [], createElement: () => element('download'), documentElement: { dataset: {} } },
    fetch: async () => ({ ok: true, json: async () => ({ records: [] }) }), window: { print() { } } };
vm.createContext(sandbox);
vm.runInContext(apiScript, sandbox);
vm.runInContext(script, sandbox);
const run = code => vm.runInContext(code, sandbox);
(async () => {
    await new Promise(resolve => setImmediate(resolve));
    element('create-employee_name').value = 'Alice';
    element('create-monthly_salary').value = '5000';
    element('create-bonus').value = '250';
    run("preview('create')");
    assert.match(element('create-preview').innerHTML, /4,200\.00/);
    assert.match(element('pay-breakdown').innerHTML, /Take-home pay \(80\.00%\)/);
    assert.match(element('pay-breakdown').innerHTML, /Tax deduction \(20\.00%\)/);
    assert.match(element('pay-breakdown').innerHTML, /width:80%/);
    element('create-tax_rate').value = '101';
    assert.equal(run("values('create',true).valid"), false);
    assert.match(element('create-tax_rate-error').textContent, /0% to 100%/);
    run(`records = [
    {id:1,employee_name:'Alice',monthly_salary:5000,bonus:250,gross_pay:5250,tax_rate:.2,tax_amount:1050,net_pay:4200,created_at:'2026-01-01 12:00:00'},
    {id:2,employee_name:'Bob <script>',monthly_salary:6500,bonus:300,gross_pay:6800,tax_rate:.2,tax_amount:1360,net_pay:5440,created_at:'2026-02-02 12:00:00'}
  ]; render();`);
    assert.equal(element('count').textContent, 2);
    assert.equal(element('net-total').textContent, '9,640.00');
    element('month').value = '2026-02';
    run('render()');
    assert.equal(element('count').textContent, 1);
    assert.equal(element('net-total').textContent, '5,440.00');
    element('month').value = '';
    run('render()');
    assert.match(element('records').innerHTML, /Bob &lt;script&gt;/);
    assert.doesNotMatch(element('records').innerHTML, /Bob <script>/);
    element('search').value = 'ALICE';
    run('render()');
    assert.equal(element('count').textContent, 1);
    assert.equal(element('gross').textContent, '5,250.00');
    element('search').value = '';
    element('from').value = '2026-02-02';
    element('to').value = '2026-02-02';
    run('render()');
    assert.equal(run('filtered[0].id'), 2);
    element('from').value = '2026-03-01';
    run('render()');
    assert.equal(element('count').textContent, 0);
    assert.match(element('filter-error').textContent, /end date/);
    element('from').value = '';
    element('to').value = '';
    element('sort').value = 'salary-asc';
    run('render()');
    assert.equal(run('filtered[0].employee_name'), 'Alice');
    element('sort').value = 'salary-desc';
    run('render()');
    assert.equal(run('filtered[0].id'), 2);
    element('chart-mode').value = 'trend';
    run('renderChart()');
    assert.match(element('chart').innerHTML, /2026-01/);
    assert.match(element('chart').innerHTML, /2026-02/);
    run("showSlip(records[0])");
    assert.match(element('print-area').innerHTML, /Salary slip/);
    assert.match(element('print-area').innerHTML, /4,200\.00/);
    run("records[0].employee_name = '=SUM(1,2)'; render(); $('export').onclick();");
    const csv = await download.text();
    assert.match(csv, /"'=SUM\(1,2\)"/);
    assert.match(csv, /monthly_salary/);
    run("setAppearance('dark')");
    assert.equal(sandbox.document.documentElement.dataset.theme, 'dark');
    run("setAppearance('light')");
    assert.equal(sandbox.document.documentElement.dataset.theme, 'light');
    element('create-monthly_salary').value = '0';
    element('create-bonus').value = '0';
    element('create-tax_rate').value = '20';
    run("preview('create')");
    assert.match(element('pay-breakdown').innerHTML, /Enter a salary/);
    run('mobileNavigation.matches = true; setNavigationOpen(true)');
    assert.equal(element('sidebar').inert, false);
    assert.equal(element('sidebar-backdrop').hidden, false);
    assert.equal(element('menu-toggle').attributes['aria-expanded'], 'true');
    run('setNavigationOpen(false, true)');
    assert.equal(element('sidebar').inert, true);
    assert.equal(element('menu-toggle').focused, true);
    run("$('settings-open').onclick()");
    assert.equal(element('settings-dialog').open, true);
    const outgoing = run("PayrollApi.formOptions('POST', {employee_name:'Alice',monthly_salary:5000,bonus:250,tax_rate:0.2})");
    assert.equal(outgoing.headers['Content-Type'], 'application/x-www-form-urlencoded');
    assert.equal(outgoing.body.get('tax_rate'), '0.2');
    sandbox.fetch = async () => ({ok:false, json:async () => ({error:'Database unavailable'})});
    await assert.rejects(run("PayrollApi.request('', {method:'POST'})"), /Database unavailable/);
    await run('load()');
    assert.equal(element('connection-status').textContent, 'Connection unavailable');
    assert.match(element('notice').textContent, /Database unavailable/);
    assert.equal(element('notice').dataset.error, true);
    assert.equal(element('refresh').disabled, false);
    assert.equal(run('records.length'), 2);
    console.log('Frontend checks passed: live preview, stacked breakdown, validation, month/date filters, net totals, search, sorting, escaping, charts, payslips, CSV safety, appearance, mobile navigation, Settings, API encoding, and backend errors.');
})().catch(error => { console.error(error); process.exitCode = 1; });
