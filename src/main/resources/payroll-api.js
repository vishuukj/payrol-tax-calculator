/* Same-origin HTTP access. Payroll records and sessions are never stored locally. */
const PayrollApi = (() => {
    let onUnauthorized = () => {};
    let csrfToken = null;
    async function requestJson(url, options = {}, protect = false) {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), 12000);
        try {
            const response = await fetch(url, {...options, credentials:'same-origin', cache:'no-store', signal:controller.signal});
            let payload;
            try { payload = await response.json(); }
            catch { throw new Error('The server returned an unexpected response. Please retry.'); }
            if (!response.ok) {
                if (response.status === 401 && protect) { csrfToken = null; onUnauthorized(); }
                const error = new Error(payload.error || 'The request failed. Please retry.');
                error.status = response.status;
                throw error;
            }
            if (payload.csrfToken) csrfToken = payload.csrfToken;
            return payload;
        } catch (error) {
            if (error.name === 'AbortError') throw new Error('The request timed out. Check the server and try again.');
            if (error instanceof TypeError) throw new Error('Cannot reach the payroll server. Check that it is running and retry.');
            throw error;
        } finally { clearTimeout(timer); }
    }
    function formOptions(method, fields) {
        return {method, headers:{'Content-Type':'application/x-www-form-urlencoded'}, body:new URLSearchParams(fields)};
    }
    async function mutation(url, options, protect = false) {
        if (!csrfToken) await requestJson('/api/auth/session');
        return requestJson(url, {...options, headers:{...options.headers, 'X-CSRF-Token':csrfToken}}, protect);
    }
    return {
        request: (path = '', options = {}) => ['GET','HEAD'].includes((options.method || 'GET').toUpperCase())
            ? requestJson('/api/payroll'+path, options, true) : mutation('/api/payroll'+path, options, true),
        session: () => requestJson('/api/auth/session'),
        signIn: async (fields, setup = false) => {
            await requestJson('/api/auth/session');
            return mutation('/api/auth/'+(setup?'setup':'login'), formOptions('POST',fields));
        },
        signOut: async () => {
            const response = await mutation('/api/auth/logout', {method:'POST'});
            csrfToken = null;
            return response;
        },
        onUnauthorized: handler => { onUnauthorized = handler; },
        formOptions
    };
})();
