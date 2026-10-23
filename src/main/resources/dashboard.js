"use strict";
const $ = id => document.getElementById(id), money = value => Number(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
let records = [], filtered = [], editId = null, deleteId = null, busy = false, loadFailed = false;
let setupRequired = false, sessionVersion = 0;
// Reusable presentation components. Monetary values retain the backend's units.
const themeIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M20 15A9 9 0 0 1 9 4a9 9 0 1 0 11 11Z"/></svg>';
const refreshMarkup = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M20 7v5h-5M4 17v-5h5M6 7a7 7 0 0 1 12-1l2 3M4 15l2 3a7 7 0 0 0 12-1"/></svg><span>Refresh</span>';
const systemTheme = matchMedia('(prefers-color-scheme: dark)');
function renderPayBreakdown(record, valid) {
    const gross = record.monthly_salary + record.bonus;
    const tax = gross * record.tax_rate;
    const net = gross - tax;
    if (!valid || gross <= 0) {
        $('pay-breakdown').innerHTML = '<p class="empty">Enter a salary to see the take-home pay and tax split.</p>';
        return;
    }
    const taxPercent = record.tax_rate * 100;
    const netPercent = 100 - taxPercent;
    $('pay-breakdown').innerHTML =
        '<div class="take-home"><small>Take-home pay</small><strong>' + money(net) + '</strong></div>' +
            '<div class="stacked-bar" role="img" aria-label="Take-home pay ' + money(netPercent) + ' percent, ' + money(net) + '; tax ' + money(taxPercent) + ' percent, ' + money(tax) + '">' +
            '<span class="take-segment" style="width:' + netPercent + '%"></span><span class="tax-segment" style="width:' + taxPercent + '%"></span></div>' +
            '<div class="breakdown-legend"><div class="legend-row"><span class="legend-dot"></span><span>Take-home pay (' + money(netPercent) + '%)</span><strong>' + money(net) + '</strong></div>' +
            '<div class="legend-row"><span class="legend-dot tax"></span><span>Tax deduction (' + money(taxPercent) + '%)</span><strong>' + money(tax) + '</strong></div></div>';
}
function populateMonths() {
    const selected = $('month').value;
    const months = [...new Set(records.map(record => record.created_at.slice(0, 7)))].sort().reverse();
    $('month').innerHTML = '<option value="">All months</option>' + months.map(month => {
        const date = new Date(Number(month.slice(0, 4)), Number(month.slice(5, 7)) - 1, 1);
        const label = date.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
        return '<option value="' + month + '">' + escapeHtml(label) + '</option>';
    }).join('');
    $('month').value = months.includes(selected) ? selected : '';
}
function setNavigationOpen(open, restoreFocus = false) {
    $('sidebar').classList.toggle('is-open', open);
    $('sidebar-backdrop').hidden = !open;
    $('menu-toggle').setAttribute('aria-expanded', String(open));
    $('menu-toggle').setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
    $('sidebar').inert = mobileNavigation.matches && !open;
    if (open)
        $('sidebar').querySelector('a').focus();
    else if (restoreFocus)
        $('menu-toggle').focus();
}
const mobileNavigation = matchMedia('(max-width: 1000px)');
$('sidebar').inert = mobileNavigation.matches;
$('menu-toggle').onclick = () => setNavigationOpen(!$('sidebar').classList.contains('is-open'));
$('sidebar-backdrop').onclick = () => setNavigationOpen(false, true);
mobileNavigation.addEventListener?.('change', () => setNavigationOpen(false));
document.addEventListener('keydown', event => {
    if (!$('sidebar').classList.contains('is-open'))
        return;
    if (event.key === 'Escape')
        setNavigationOpen(false, true);
    if (event.key === 'Tab' && mobileNavigation.matches) {
        const controls = [...$('sidebar').querySelectorAll('a,button:not([disabled])')];
        const first = controls[0], last = controls[controls.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        }
        else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }
});
document.querySelectorAll('.sidebar-nav a, .sidebar .brand').forEach(link => link.addEventListener('click', () => {
    document.querySelectorAll('.sidebar-nav a').forEach(item => {
        const active = item.getAttribute('href') === link.getAttribute('href');
        item.classList.toggle('active', active);
        if (active) item.setAttribute('aria-current', 'location');
        else item.removeAttribute('aria-current');
    });
    setNavigationOpen(false);
    $(link.getAttribute('href').slice(1)).focus({ preventScroll: true });
}));
$('settings-open').onclick = () => { setNavigationOpen(false); $('settings-dialog').showModal(); };
$('footer-year').textContent = new Date().getFullYear();
function notice(text, error = false) { $('notice').textContent = text; $('notice').dataset.error = error; $('notice').hidden = false; }
function makeFields(prefix) { $(prefix + '-fields').innerHTML = [['employee_name', 'Employee name', 'text', '', true], ['monthly_salary', 'Monthly salary', 'number', '', false], ['bonus', 'Bonus', 'number', '0', false], ['tax_rate', 'Tax rate (%)', 'number', '20', false]].map(([name, label, type, value, wide]) => `<div class="${wide ? 'wide' : ''}"><label for="${prefix}-${name}">${label}</label><input id="${prefix}-${name}" name="${name}" type="${type}" value="${value}" ${type === 'number' ? 'min="0" step="any"' : ''} ${name === 'tax_rate' ? 'max="100"' : ''} ${name === 'employee_name' ? 'maxlength="200" autocomplete="name"' : ''} required aria-describedby="${prefix}-${name}-error"><span class="error" id="${prefix}-${name}-error"></span></div>`).join(''); }
function values(prefix, showErrors = false) {
    const result = {};
    let valid = true;
    for (const name of ['employee_name', 'monthly_salary', 'bonus', 'tax_rate']) {
        const input = $(prefix + '-' + name), raw = input.value.trim();
        let error = '';
        if (name === 'employee_name') {
            result[name] = raw;
            if (!raw)
                error = 'Enter an employee name.';
            else if (raw.length > 200)
                error = 'Use at most 200 characters.';
        }
        else {
            result[name] = Number(raw);
            if (raw === '' || !Number.isFinite(result[name]) || result[name] < 0)
                error = 'Enter a valid non-negative amount.';
            if (name === 'tax_rate' && result[name] > 100)
                error = 'Tax rate must be from 0% to 100%.';
        }
        if (error)
            valid = false;
        if (showErrors || input.dataset.touched) {
            $(prefix + '-' + name + '-error').textContent = error;
            input.setAttribute('aria-invalid', String(Boolean(error)));
        }
    }
    if (!Number.isFinite(result.monthly_salary + result.bonus)) {
        valid = false;
        if (showErrors) {
            $(prefix + '-monthly_salary-error').textContent = 'Salary plus bonus is too large.';
            $(prefix + '-monthly_salary').setAttribute('aria-invalid', 'true');
        }
    }
    result.tax_rate /= 100;
    return { valid, result };
}
function preview(prefix) {
    const { result: r, valid } = values(prefix);
    const gross = r.monthly_salary + r.bonus, tax = gross * r.tax_rate;
    const numeric = [r.monthly_salary, r.bonus, r.tax_rate, gross].every(Number.isFinite) && r.monthly_salary >= 0 && r.bonus >= 0 && r.tax_rate >= 0 && r.tax_rate <= 1;
    $(prefix + '-preview').innerHTML = `<div><span>Gross pay</span><strong>${numeric ? money(gross) : '—'}</strong></div><div><span>Tax deduction</span><strong>${numeric ? money(tax) : '—'}</strong></div><div class="net"><strong>Net pay</strong><strong>${numeric ? money(gross - tax) : '—'}</strong></div>`;
    if (prefix === 'create')
        renderPayBreakdown(r, numeric);
    return valid;
}
for (const prefix of ['create', 'edit']) {
    makeFields(prefix);
    $(prefix + '-form').addEventListener('input', () => preview(prefix));
    $(prefix + '-form').addEventListener('focusout', event => {
        if (event.target.tagName === 'INPUT') {
            event.target.dataset.touched = 'true';
            preview(prefix);
        }
    });
    preview(prefix);
}
const api = PayrollApi.request;
const formOptions = PayrollApi.formOptions;
async function load({ notify = false } = {}) {
    const version = sessionVersion;
    $('records-section').setAttribute('aria-busy', 'true');
    $('connection-status').textContent = 'Connecting…';
    $('refresh').disabled = true;
    $('refresh').textContent = 'Loading…';
    try {
        const payload = await api('');
        if (version !== sessionVersion) return false;
        if (!Array.isArray(payload.records)) throw new Error('The server returned invalid payroll data. Please retry.');
        records = payload.records;
        loadFailed = false;
        populateMonths();
        $('connection-status').textContent = 'Connected to database';
        render();
        $('last-refreshed').textContent = 'Updated ' + new Date().toLocaleTimeString();
        if (notify) notice('Payroll refreshed. Showing the latest saved records.');
        return true;
    }
    catch (error) {
        loadFailed = true;
        $('connection-status').textContent = 'Connection unavailable';
        $('table-state').hidden = false;
        $('table-state').textContent = 'Could not load records. Use Refresh to retry.';
        notice(error.message, true);
        return false;
    }
    finally {
        $('records-section').setAttribute('aria-busy', 'false');
        $('refresh').disabled = false;
        $('refresh').innerHTML = refreshMarkup;
    }
}
async function save(prefix, event) {
    event.preventDefault();
    if (busy)
        return;
    const { valid, result } = values(prefix, true);
    preview(prefix);
    if (!valid) {
        $(prefix + '-form').querySelector('[aria-invalid=true]')?.focus();
        return;
    }
    busy = true;
    const button = $(prefix + '-form').querySelector('[type=submit]');
    button.disabled = true;
    button.textContent = 'Saving…';
    try {
        await api(prefix === 'edit' ? '/' + editId : '', formOptions(prefix === 'edit' ? 'PUT' : 'POST', result));
        if (prefix === 'edit')
            $('edit-dialog').close();
        else {
            $('create-form').reset();
            for (const input of $('create-form').querySelectorAll('input')) {
                delete input.dataset.touched;
                input.removeAttribute('aria-invalid');
            }
            preview('create');
        }
        notice(prefix === 'edit' ? 'Payroll record updated.' : 'Payroll record saved.');
        await load();
    }
    catch (error) {
        if (prefix === 'edit')
            $('edit-error').textContent = error.message;
        else
            notice(error.message, true);
    }
    finally {
        busy = false;
        button.disabled = false;
        button.textContent = prefix === 'edit' ? 'Save changes' : 'Save payroll record';
    }
}
$('create-form').addEventListener('submit', event => save('create', event));
$('edit-form').addEventListener('submit', event => save('edit', event));
function render() { const month = $('month').value, query = $('search').value.trim().toLocaleLowerCase(), from = $('from').value, to = $('to').value; const bad = from && to && from > to; $('filter-error').textContent = bad ? 'The end date must be on or after the start date.' : ''; filtered = bad ? [] : records.filter(r => (!month || r.created_at.slice(0, 7) === month) && r.employee_name.toLocaleLowerCase().includes(query) && (!from || r.created_at.slice(0, 10) >= from) && (!to || r.created_at.slice(0, 10) <= to)); filtered.sort((a, b) => ({ 'salary-desc': () => b.monthly_salary - a.monthly_salary, 'salary-asc': () => a.monthly_salary - b.monthly_salary, name: () => a.employee_name.localeCompare(b.employee_name), newest: () => b.id - a.id }[$('sort').value])()); const gross = filtered.reduce((sum, r) => sum + r.gross_pay, 0), tax = filtered.reduce((sum, r) => sum + r.tax_amount, 0), net = filtered.reduce((sum, r) => sum + r.net_pay, 0); $('count').textContent = filtered.length; $('gross').textContent = money(gross); $('tax').textContent = money(tax); $('net-total').textContent = money(net); $('records-badge').textContent = filtered.length; $('records').innerHTML = filtered.map(r => `<tr><td><strong>${escapeHtml(r.employee_name)}</strong><br><small>${escapeHtml(r.created_at.slice(0, 10))} · #${r.id}</small></td><td>${money(r.monthly_salary)}</td><td>${money(r.gross_pay)}</td><td>${money(r.tax_amount)}<br><small>${money(r.tax_rate * 100)}%</small></td><td style="color:var(--green);font-weight:700">${money(r.net_pay)}</td><td><button data-action="edit" data-id="${r.id}">Edit</button><button data-action="slip" data-id="${r.id}">Payslip</button><button class="danger" data-action="delete" data-id="${r.id}" aria-label="Delete ${escapeHtml(r.employee_name)}">Delete</button></td></tr>`).join(''); $('table-state').hidden = filtered.length > 0; $('table-state').textContent = loadFailed ? 'Could not load records. Use Refresh to retry.' : bad ? 'Choose a valid date range.' : records.length ? 'No matching records. Try changing your filters.' : 'No payroll records yet. Create your first record using the form.'; $('record-count').textContent = `Showing ${filtered.length} of ${records.length} records · Summary reflects these filters`; $('export').disabled = !filtered.length; renderChart(); }
function renderChart() {
    let data;
    if ($('chart-mode').value === 'trend') {
        const months = new Map();
        for (const r of filtered) {
            const key = r.created_at.slice(0, 7), item = months.get(key) || { label: key, gross: 0, net: 0 };
            item.gross += r.gross_pay;
            item.net += r.net_pay;
            months.set(key, item);
        }
        data = [...months.values()].sort((a, b) => a.label.localeCompare(b.label));
    }
    else
        data = filtered.map(r => ({ label: r.employee_name + ' #' + r.id, gross: r.gross_pay, net: r.net_pay }));
    const showGross = $('show-gross').checked, showNet = $('show-net').checked, max = Math.max(1, ...data.map(r => Math.max(showGross ? r.gross : 0, showNet ? r.net : 0)));
    $('chart').innerHTML = !data.length ? '<p class="empty">Add records or adjust filters to see insights.</p>' : !showGross && !showNet ? '<p class="empty">Select a pay series above.</p>' : data.map(r => `<div class="bar-row"><span class="bar-label" title="${escapeHtml(r.label)}">${escapeHtml(r.label)}</span><div class="bar-track">${showGross ? `<div class="bar" tabindex="0" role="img" aria-label="${escapeHtml(r.label)} gross: ${money(r.gross)}" title="Gross: ${money(r.gross)}" style="width:${r.gross / max * 100}%"></div>` : ''}${showNet ? `<div class="bar netbar" tabindex="0" role="img" aria-label="${escapeHtml(r.label)} net: ${money(r.net)}" title="Net: ${money(r.net)}" style="width:${r.net / max * 100}%"></div>` : ''}</div></div>`).join('');
}
for (const id of ['month', 'search', 'from', 'to', 'sort'])
    $(id).addEventListener('input', render);
for (const id of ['chart-mode', 'show-gross', 'show-net'])
    $(id).addEventListener('change', renderChart);
$('clear').onclick = () => {
    for (const id of ['month', 'search', 'from', 'to'])
        $(id).value = '';
    $('sort').value = 'newest';
    render();
};
$('refresh').onclick = () => load({ notify: true });
document.querySelectorAll('[data-close]').forEach(button => button.onclick = () => $(button.dataset.close).close());
$('records').onclick = event => {
    const button = event.target.closest('[data-action]');
    if (!button)
        return;
    const r = records.find(r => r.id === Number(button.dataset.id));
    if (!r)
        return;
    if (button.dataset.action === 'edit') {
        editId = r.id;
        $('edit-error').textContent = '';
        for (const name of ['employee_name', 'monthly_salary', 'bonus', 'tax_rate']) {
            const input = $('edit-' + name);
            input.value = name === 'tax_rate' ? r[name] * 100 : r[name];
            delete input.dataset.touched;
            input.removeAttribute('aria-invalid');
            $('edit-' + name + '-error').textContent = '';
        }
        preview('edit');
        $('edit-dialog').showModal();
    }
    else if (button.dataset.action === 'delete') {
        deleteId = r.id;
        $('delete-message').textContent = `Delete the record for ${r.employee_name} (${money(r.gross_pay)} gross pay)?`;
        $('delete-error').textContent = '';
        $('delete-dialog').showModal();
    }
    else
        showSlip(r);
};
$('confirm-delete').onclick = async () => {
    if (busy)
        return;
    busy = true;
    $('confirm-delete').disabled = true;
    try {
        await api('/' + deleteId, { method: 'DELETE' });
        $('delete-dialog').close();
        notice('Payroll record deleted.');
        await load();
    }
    catch (error) {
        $('delete-error').textContent = error.message;
    }
    finally {
        busy = false;
        $('confirm-delete').disabled = false;
    }
};
function showSlip(r) { $('slip-content').innerHTML = `<p><strong>${escapeHtml(r.employee_name)}</strong><br><span class="muted">Record #${r.id} · Created ${escapeHtml(r.created_at)}</span></p>` + [['Monthly salary', r.monthly_salary], ['Bonus', r.bonus], ['Gross pay', r.gross_pay], ['Tax (' + money(r.tax_rate * 100) + '%)', r.tax_amount], ['Net pay', r.net_pay]].map(([label, value]) => `<div class="slip-line"><span>${label}</span><strong>${money(value)}</strong></div>`).join('') + '<p class="muted">Payroll record summary · Flat-rate tax calculation</p>'; $('print-area').innerHTML = '<h1>Salary slip</h1>' + $('slip-content').innerHTML; $('slip-dialog').showModal(); }
$('print').onclick = () => { $('slip-dialog').close(); window.print(); };
$('export').onclick = () => {
    const columns = ['id', 'employee_name', 'created_at', 'monthly_salary', 'bonus', 'gross_pay', 'tax_rate', 'tax_amount', 'net_pay'];
    const cell = value => {
        let text = String(value ?? '');
        if (/^[=+\-@\t\r\n]/.test(text))
            text = "'" + text;
        return '"' + text.replace(/"/g, '""') + '"';
    };
    const csv = [columns.map(cell).join(','), ...filtered.map(r => columns.map(key => cell(r[key])).join(','))].join('\r\n');
    const url = URL.createObjectURL(new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'payroll-' + new Date().toISOString().slice(0, 10) + '.csv';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    notice(`Exported ${filtered.length} records.`);
};
function theme(value) {
    document.documentElement.dataset.theme = value;
    const label = value === 'dark' ? 'Light mode' : 'Dark mode';
    $('theme').innerHTML = themeIcon + '<span>' + label + '</span>';
    $('theme').setAttribute('aria-label', 'Switch to ' + label.toLowerCase());
    $('theme').setAttribute('aria-pressed', String(value === 'dark'));
}
function setAppearance(preference) {
    $('appearance').value = preference;
    theme(preference === 'system' ? (systemTheme.matches ? 'dark' : 'light') : preference);
    try {
        localStorage.setItem('payroll-theme', preference);
    }
    catch { }
}
let saved;
try {
    saved = localStorage.getItem('payroll-theme');
}
catch { }
setAppearance(['light', 'dark', 'system'].includes(saved) ? saved : 'system');
$('appearance').addEventListener('change', () => setAppearance($('appearance').value));
$('theme').onclick = () => setAppearance(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark');
systemTheme.addEventListener?.('change', () => {
    if ($('appearance').value === 'system')
        setAppearance('system');
});

// Authentication is backed by the Java server and an HttpOnly session cookie.
function showSignedOut(setup = false, message = '') {
    sessionVersion++;
    setupRequired = setup;
    document.querySelectorAll('dialog[open]').forEach(dialog => dialog.close());
    setNavigationOpen(false);
    $('workspace-content').hidden = true;
    $('auth-screen').hidden = false;
    $('auth-title').textContent = setup ? 'Create your workspace account' : 'Welcome back';
    $('auth-description').textContent = setup ? 'Choose a username and password to protect this payroll database.' : 'Sign in to your payroll workspace.';
    $('auth-submit').textContent = setup ? 'Create account & continue' : 'Sign in';
    $('auth-submit').disabled = false;
    $('auth-password').value = '';
    $('auth-password').type = 'password';
    $('password-toggle').textContent = 'Show';
    $('password-toggle').setAttribute('aria-pressed','false');
    $('password-toggle').setAttribute('aria-label','Show password');
    $('auth-password').minLength = setup ? 8 : 1;
    $('auth-password').autocomplete = setup ? 'new-password' : 'current-password';
    $('password-help').textContent = setup ? 'Use at least 8 characters. There is no default password.' : '';
    $('auth-message').textContent = message;
    $('auth-message').hidden = !message;
    $('auth-retry').hidden = true;
    records = []; filtered = [];
    render();
    $('print-area').innerHTML = '';
    $('slip-content').innerHTML = '';
}
function showWorkspace(session) {
    sessionVersion++;
    $('auth-screen').hidden = true;
    $('workspace-content').hidden = false;
    $('sidebar-username').textContent = session.username;
    $('account-username').textContent = session.username;
    $('account-open').textContent = session.username.slice(0,2).toUpperCase();
    $('auth-password').value = '';
    $('auth-message').hidden = true;
    $('notice').hidden = true;
}
async function initializeSession() {
    $('auth-submit').disabled = true;
    $('auth-retry').disabled = true;
    $('auth-description').textContent = 'Connecting to your workspace…';
    try {
        const session = await PayrollApi.session();
        if (session.authenticated) { showWorkspace(session); await load(); }
        else showSignedOut(session.setupRequired);
    } catch (error) {
        $('auth-screen').hidden = false;
        $('workspace-content').hidden = true;
        $('auth-description').textContent = 'The workspace is unavailable.';
        $('auth-message').textContent = error.message;
        $('auth-message').hidden = false;
        $('auth-retry').hidden = false;
    } finally { $('auth-retry').disabled = false; }
}
PayrollApi.onUnauthorized(() => showSignedOut(false, 'Your session has expired. Please sign in again.'));
$('auth-retry').onclick = initializeSession;
$('password-toggle').onclick = () => {
    const visible = $('auth-password').type === 'password';
    $('auth-password').type = visible ? 'text' : 'password';
    $('password-toggle').textContent = visible ? 'Hide' : 'Show';
    $('password-toggle').setAttribute('aria-label', visible ? 'Hide password' : 'Show password');
    $('password-toggle').setAttribute('aria-pressed', String(visible));
};
$('auth-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (!$('auth-form').reportValidity()) return;
    $('auth-submit').disabled = true;
    $('auth-submit').textContent = setupRequired ? 'Creating account…' : 'Signing in…';
    $('auth-message').hidden = true;
    try {
        const session = await PayrollApi.signIn({username:$('auth-username').value.trim(), password:$('auth-password').value},setupRequired);
        showWorkspace(session);
        await load();
    } catch (error) {
        if (error.status === 409) { await initializeSession(); }
        $('auth-message').textContent = error.message;
        $('auth-message').hidden = false;
    } finally {
        $('auth-submit').disabled = false;
        $('auth-submit').textContent = setupRequired ? 'Create account & continue' : 'Sign in';
    }
});
function openAccount() {
    setNavigationOpen(false);
    $('account-error').textContent = '';
    $('account-dialog').showModal();
}
$('account-open').onclick = openAccount;
$('sidebar-account').onclick = openAccount;
$('sign-out').onclick = async () => {
    $('sign-out').disabled = true;
    try {
        await PayrollApi.signOut();
        showSignedOut(false, 'You have signed out.');
    } catch (error) { $('account-error').textContent = error.message; }
    finally { $('sign-out').disabled = false; }
};

initializeSession();
