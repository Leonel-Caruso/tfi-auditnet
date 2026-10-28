# Bloque 16 — Cierre

Decisiones: D-69 y D-70 en `docs/entrega-2/REGISTRO-DECISIONES.md`. Sin migraciones ni cambios en la API.

## Ramas

| Rama o tag | Uso |
|---|---|
| `main` | Versión vigente. Cada push corre las pruebas, publica las imágenes y despliega en Azure con aprobación |
| `test` | Cambios en prueba. Corre las mismas pruebas y publica imágenes con la etiqueta `test`; no despliega |
| `entrega-2` | Historial de las entregas 2 y 3; no se borra |
| `entrega-1-stable`, `entrega-2-stable` | Tags de las entregas anteriores |
| `entrega-3-stable` | Tag del cierre, sobre `main` |

Para un cambio nuevo: trabajarlo en `test`, esperar las pruebas en verde y después unirlo a `main`.

```powershell
git checkout test
git pull
# cambios, commit
git push origin test
git checkout main
git pull
git merge --ff-only test
git push origin main
```

## Cambios

| Cambio | Dónde |
|---|---|
| El workflow corre en `main` y `test` y solo despliega desde `main`; también corre si cambian los scripts de Azure | `.github/workflows/imagenes.yml` |
| Etiqueta por defecto `main` en los scripts de creación y actualización | `infra/azure/02-crear-apps.sh`, `03-actualizar-imagenes.sh` |
| Restituir el ingreso de una app si Azure lo deshabilita al actualizar la imagen | `infra/azure/03-actualizar-imagenes.sh` |
| Referencias a la rama en README, despliegue y manual de instalación y desarrollo (3 idiomas, PDF regenerados) | `README.md`, `docs/deploy/AZURE.md`, `docs/manuales/` |

## Configuración en GitHub

*Settings → Environments → azure → Deployment branches and tags*: reemplazar `entrega-2` por `main`. Sin ese cambio,
GitHub rechaza el despliegue desde `main`. La credencial de Azure (OIDC) está atada al entorno, no a la rama, así que
no hay que tocar nada en Azure.

## Casos de prueba manuales

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B16-01 | Push a `test` | Corren las pruebas y las imágenes; el trabajo *Desplegar en Azure* aparece omitido |
| CP-B16-02 | Push a `main` | Corren las pruebas y el despliegue queda esperando aprobación |
| CP-B16-03 | Aprobar el despliegue | Las tres apps con la imagen del commit, con ingreso, y la prueba de humo en verde |
| CP-B16-04 | Abrir el sitio después del despliegue | Ingreso, tablero y descarga del manual sin cambios |
| CP-B16-05 | `git tag -l` | Están `entrega-1-stable`, `entrega-2-stable` y `entrega-3-stable` |
