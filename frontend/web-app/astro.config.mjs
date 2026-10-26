import { defineConfig } from 'astro/config';

export default defineConfig({
  // Los scripts de las páginas van siempre en archivos aparte: la política de seguridad (CSP) no permite scripts
  // escritos dentro del HTML.
  vite: {
    build: { assetsInlineLimit: 0 },
  },
  server: {
    host: 'localhost',
    port: 4321,
  },
});
