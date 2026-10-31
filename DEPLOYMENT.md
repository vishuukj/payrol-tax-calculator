# Deploy java-sqlite to Render and Vercel

This folder is a checkout of `vishuukj/payrol-tax-calculator`, branch
`java-sqlite`. Deployment changes must be pushed to that branch before importing
the repository into either provider.

1. In Render, create a Blueprint from this repository and select `java-sqlite`.
   The included `render.yaml` creates a Docker web service on the Starter plan
   with a 1 GB persistent disk. This is a paid configuration: review Render's
   price before creating it. SQLite is stored at `/var/data/payroll.db`.
2. Supply `PUBLIC_ORIGIN` as the intended Vercel production origin, for example
   `https://your-project.vercel.app`, without a trailing slash. You can update it
   when Vercel assigns the final domain. Do not use a wildcard.
3. Import the same repository and branch in Vercel. Use the repository root,
   framework **Other**, and the build command from `vercel.json`. Add
   `RENDER_BACKEND_URL=https://your-service.onrender.com` to the Vercel build
   environment, replacing the example with the actual Render service URL.
   The build generates Vercel Build Output API files including the API proxy.
4. Set Vercel's production branch to `java-sqlite`. Ensure Render's
   `PUBLIC_ORIGIN` exactly matches the production frontend URL, then redeploy
   Render if the value changed. Preview domains cannot perform mutations unless
   explicitly configured as the public origin instead.
5. Open the Vercel URL and immediately create the first workspace account.
   First-run setup is public until that account exists; keep the deployment
   behind provider access protection until the owner finishes setup. Do not
   upload existing payroll databases to Git or to the deployment build.
6. Verify sign-in, create/edit/delete using a disposable record, refresh, and
   sign-out. Restart Render and confirm the account and saved records remain.
   Sessions are held in memory, so a restart requires signing in again.

Vercel serves the static dashboard and proxies `/api/*` to Render. Cookies are
HttpOnly, SameSite=Strict, and Secure on Render. CSRF validation remains enabled;
the backend only accepts its Render hostname and local hosts, and accepts the
explicit frontend origin for browser mutations. The backend does not enable
cross-origin browser access. Use one Render instance with the attached SQLite
disk. Back up the disk before changing or deleting the service.

Local verification:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./build.ps1
$env:RENDER_BACKEND_URL='https://your-service.onrender.com'
node scripts/build-vercel.cjs
node src/test/js/hosting.test.cjs
```

Docker/Maven builds run the Java integration suite. The local PowerShell build
also runs the existing frontend and page integration suites. The hosting check
uses a temporary database and starts its own Java server.
