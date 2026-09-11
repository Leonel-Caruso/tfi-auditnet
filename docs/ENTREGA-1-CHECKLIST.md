# Checklist — Cierre de la Primera Entrega

## Código y compilación

- [ ] `management-service`: `mvn test` → verde.
- [ ] `audit-core-service`: `mvn test` → verde.
- [ ] frontend: `npm run build` → verde.
- [ ] no hay `.env` real dentro del ZIP.
- [ ] no hay `target`, `node_modules`, `dist` ni `.astro` dentro del ZIP.

## Infraestructura

- [ ] PostgreSQL aparece `healthy`.
- [ ] `management-service` escucha en `8081`.
- [ ] `audit-core-service` escucha en `8082`.
- [ ] Astro escucha en `4321`.

## Seguridad

- [ ] login admin funciona.
- [ ] login `analista_prueba` funciona.
- [ ] analista consulta solo su organización.
- [ ] analista no puede crear dispositivos.
- [ ] analista no puede crear baseline/regla mediante POST directo.
- [ ] JWT emitido en `8081` es aceptado por `8082`.
- [ ] sesión vencida vuelve al login con mensaje claro.

## Flujo fuerte

- [ ] dispositivo `RTR-CORE-01` disponible.
- [ ] baseline `Hardening Cisco IOS-XE` disponible.
- [ ] reglas `SEC-SSH-01` y `SEC-TELNET-01` disponibles.
- [ ] configuración con Telnet genera 1 hallazgo ALTA.
- [ ] configuración corregida genera 0 hallazgos.
- [ ] las dos versiones quedan visibles.
- [ ] las dos auditorías quedan visibles.
- [ ] el hallazgo histórico no se elimina.
- [ ] datos persisten al reiniciar los backends.

## Presentación

- [ ] demo ensayada con `docs/demo/GUIA-DEMO-ENTREGA-1.md`.
- [ ] se puede explicar cada capa: API, aplicación, dominio, infraestructura.
- [ ] se puede explicar qué se dejó fuera y por qué.
- [ ] se puede explicar la relación entre dispositivo, baseline, regla, configuración, auditoría y hallazgo.
