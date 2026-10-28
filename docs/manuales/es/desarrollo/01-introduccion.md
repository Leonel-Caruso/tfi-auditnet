## Introducción

AuditNet es una plataforma web B2B para auditar configuraciones de equipos de red Cisco IOS/IOS-XE. Compara la
configuración real de cada equipo (running-config) con una configuración esperada (baseline), detecta los desvíos, los
prioriza por severidad y por la criticidad del equipo, genera alertas y reportes y conserva la evidencia de cada
auditoría. Es multi organización: cada cliente ve solo sus datos.

Este manual es para quien tiene que **instalar, operar o modificar** la plataforma: desarrolladores que se suman al
proyecto y quien la despliega en Azure. Cubre:

- la arquitectura y por qué está armada así;
- cómo está organizado el repositorio y qué convenciones sigue;
- cómo levantarla en una computadora local y cargar datos de demostración;
- cómo correr las pruebas;
- cómo agregar un endpoint, una migración, una pantalla, una traducción o un correo;
- el circuito de integración y despliegue en Azure, y la operación diaria;
- los problemas reales que aparecieron durante el desarrollo y cómo se resolvieron;
- qué puede hacer cada perfil y dónde se controla en el código.

| Dato | Valor |
|---|---|
| Repositorio | `github.com/Leonel-Caruso/tfi-auditnet` |
| Ramas | `main` (versión vigente, se despliega en Azure) y `test` (pruebas, no se despliega); `entrega-2` queda como historial |
| Decisiones de diseño | `docs/entrega-2/REGISTRO-DECISIONES.md` (D-01 en adelante) |
| Migraciones | `docs/database/MIGRACIONES.md` |
| Despliegue | `docs/deploy/AZURE.md` |

> En los comandos, lo que va entre `<` y `>` se reemplaza por el valor real (sin los signos). Las contraseñas y
> secretos nunca se escriben en el repositorio ni en la línea de comandos: los scripts los piden al ejecutarse.
