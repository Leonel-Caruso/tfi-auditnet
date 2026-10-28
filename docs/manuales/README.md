# Manuales de AuditNet

| Manual | Idiomas | PDF |
|---|---|---|
| Usuario · Administrador del sistema | es, en, pt | `frontend/web-app/public/manuales/manual-administrador-sistema-<idioma>.pdf` |
| Usuario · Administrador de organización | es, en, pt | `.../manual-administrador-organizacion-<idioma>.pdf` |
| Usuario · Analista de redes | es, en, pt | `.../manual-analista-redes-<idioma>.pdf` |
| Usuario · Auditor técnico | es, en, pt | `.../manual-auditor-tecnico-<idioma>.pdf` |
| Usuario · Responsable de gestión IT | es, en, pt | `.../manual-responsable-gestion-it-<idioma>.pdf` |
| Instalación y desarrollo | es, en, pt | `docs/manuales/pdf/manual-desarrollo-<idioma>.pdf` |

Los de usuario los publica el sitio: la consola los ofrece en el tablero y en el menú del usuario, en el idioma de la
cuenta.

## Fuentes

```text
docs/manuales/
  <idioma>/usuario/      un capítulo por archivo; generar.py arma cada manual con los que corresponden al perfil
  <idioma>/desarrollo/   capítulos numerados, en orden
  plantilla/             estilo del PDF y fuentes (Inter, licencia OFL)
  generar.py             Markdown → HTML → PDF con Chromium
  capturas.py            capturas del sitio de pruebas, en cada idioma
  datos_manual.py        datos ficticios que muestran las capturas
```

Los tres idiomas tienen los mismos archivos y las mismas capturas (`![epígrafe](archivo.png)`). Los rótulos de botones
y pantallas se escriben como aparecen en la consola en ese idioma.

## Regenerar

Desde la raíz del repositorio:

```powershell
cd .\frontend\web-app; npm run build:pruebas; cd ..\..
python -m pip install -r docs\manuales\requirements.txt
python -m playwright install chromium
python docs\manuales\generar.py
```

| Opción | Para qué |
|---|---|
| `--sin-capturas` | reutiliza las capturas de `docs/manuales/.capturas` (no se suben a Git) |
| `--solo usuario` / `--solo desarrollo` | genera solo esos manuales |
| `--idioma es` | genera solo ese idioma |

Después de regenerar los manuales de usuario, volver a compilar el sitio para que los publique.
