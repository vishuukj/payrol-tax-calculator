# Local workspace security

This document describes the Java/SQLite web application. The separate Swing/MySQL
application is unchanged.

## Access boundaries

- The HTTP server binds to `127.0.0.1`. Request hosts must be `127.0.0.1:PORT`
  or `localhost:PORT`; unexpected hosts are rejected to limit DNS rebinding.
- Every payroll read and write requires a valid, unexpired server-side session.
- The application has one workspace owner account. All records belong to that
  workspace. It does not implement multiple organizations, roles, or per-employee
  account access.
- Session cookies contain 256-bit random opaque tokens and use `HttpOnly`,
  `SameSite=Strict`, and an eight-hour lifetime. Invalid, unknown, duplicate,
  revoked, or expired cookies cannot authorize access. Server restart invalidates
  all sessions. At most 100 authenticated/anonymous sessions are retained.
- Passwords use PBKDF2-HMAC-SHA256 with 210,000 iterations and a random 16-byte
  salt. Only salts and hashes are stored, never plaintext passwords.
- Login attempts are throttled after five failures for 30 seconds.

## CSRF protection

1. Fetch `GET /api/auth/session` and retain the session cookie.
2. Read the `csrfToken` response field and send it in the `X-CSRF-Token` header
   for every state-changing request, including setup, login, and logout.
3. Setup/login rotate both the session and CSRF token. Use the new `csrfToken`
   in their responses for later requests.
4. A browser refresh can recover the current token through the session endpoint.
   Tokens stay in JavaScript memory, not local storage or URLs.

The frontend adapter performs this exchange automatically. Requests from foreign
origins or marked `Sec-Fetch-Site: cross-site` cannot mutate data. Non-browser
clients may omit Origin, but they still need a valid cookie and CSRF header.

## Responses and diagnostics

Responses disable caching and include a content security policy, frame denial,
MIME sniffing prevention, a no-referrer policy, and restricted browser feature
permissions. Scripts load from the same origin. Inline styles remain allowed
for generated chart widths; inline scripts are not allowed.

Unexpected server errors log only a random incident ID and exception class.
Request bodies, cookies, CSRF values, passwords, hashes, salaries, employee
names, and raw SQL exception messages are not written to server diagnostics.
Clients receive a generic error. CLI payroll output is intentional user output,
not a server diagnostic log.

## Limits

This is a local HTTP application. The cookie deliberately has no `Secure` flag
on loopback HTTP. Public hosting requires a reviewed HTTPS deployment and Secure
cookies; do not expose the current listener through a public proxy unchanged.
SQLite files are not encrypted by this application. Anyone who can read the
database file through the operating system can bypass web login; account/session
controls are not disk encryption. Backup and recovery improvements are a separate
planned stage.

## Verification

`SecurityTest.java` covers anonymous access, CSRF on authentication and CRUD,
cross-origin requests, session rotation, duplicate/forged/revoked/expired cookies,
host validation, response headers, stored password hashing, and redacted internal
errors. Expiry tests use a controllable clock, not eight-hour sleeps. All tests
use disposable databases; the page-event integration suite exercises the actual
HTTP adapter and event handlers against Java using a strict DOM test double.
