# Marketplace frontend

React, TypeScript, Vite, Tailwind CSS, React Router, and Axios frontend for the Spring Boot marketplace backend.

## Run locally

```powershell
npm install
Copy-Item .env.example .env
npm run dev
```

The Vite development server proxies `/api` to `http://localhost:8080` by default. Set `VITE_PROXY_TARGET` in `.env` to use a different backend origin. Set `VITE_API_BASE_URL` to an absolute API URL when the frontend is deployed separately.

## Checks

```powershell
npm run lint
npm run build
```

The Axios client and typed endpoint methods are in `src/api/`. Browser routes are configured in `src/app/router.tsx`.
