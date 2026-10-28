## Problemas reales y cómo se resolvieron

| Síntoma | Causa | Solución |
|---|---|---|
| `No plugin found for prefix 'quarkus'` | `mvn quarkus:dev` corrido desde la raíz del repositorio | Correrlo dentro de `backend/<servicio>`. |
| audit-core no arranca la primera vez | Sus migraciones referencian tablas de management | Levantar primero management-service. |
| El driver de MongoDB rechaza Cosmos DB | Azure creó la cuenta con la API 3.6 y el driver exige 4.2 | Subirla desde el portal (*Upgrade MongoDB server version*); `04-crear-cosmos.sh` lo verifica. |
| `SubscriptionNotFound` al crear un recurso | El proveedor de ese tipo de recurso no estaba registrado en la suscripción | `variables.sh` los registra antes (`registrar_proveedores`). |
| `ExpressEnvironmentResourceNotSupported` | El entorno de Container Apps quedó en modo *express*, que no admite trabajos programados | La revisión periódica se programa con una Logic App (`09-crear-revision.sh`). |
| La primera pantalla tarda varios segundos | Las apps escalaron a cero o PostgreSQL está detenido | `07-modo-demo.sh on` antes de presentar. |
| Cloud Shell no encuentra el repositorio ni las variables | La sesión de Cloud Shell es efímera | Clonar o `git pull` y volver a definir las variables en cada sesión. |
| `unrecognized arguments` en `az postgres flexible-server firewall-rule` | La CLI cambió la sintaxis | Usar `--server-name <servidor> --name <regla>`. Desde Cloud Shell no hace falta regla: entra por la de servicios de Azure. |
| Los correos de bienvenida tardan en llegar | El dominio administrado de Azure limita a 10 envíos por hora | Esperar; la bandeja de salida los reintenta. Para más volumen, un dominio propio verificado. |
| Un script del sitio no corre en Azure y sí en desarrollo | La política de seguridad bloquea scripts escritos dentro del HTML | Scripts en archivos aparte; Astro configurado con `assetsInlineLimit: 0`. |
| `wait_for_function` falla en las pruebas de pantalla | Evalúa texto como código y la política lo bloquea | Usar `pagina.esperar(...)`. |
| Playwright no encuentra el navegador | La versión instalada no coincide con la del paquete | `python -m playwright install chromium`. |
| *La cuenta está bloqueada* al probar un script | 5 contraseñas equivocadas seguidas | Esperar 15 minutos o desbloquear desde Usuarios. |
| Después de desplegar se ve la versión anterior | El navegador guardó la página | Recargar con Ctrl+F5 (los archivos de `/_astro/` llevan hash; el HTML se revalida siempre). |
| *Tu sesión terminó* sin haber salido | Se cambió el perfil, la organización o la contraseña, o venció el token | Volver a ingresar: es el comportamiento esperado. |
