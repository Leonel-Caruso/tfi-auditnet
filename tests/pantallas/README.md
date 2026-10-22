# Pruebas de pantalla

Abren el sitio compilado en Chromium (Playwright) contra una API simulada (`servidor.py`, datos en `datos.py`) y
verifican, con cada perfil: que todas las pantallas cargan sin errores, que no muestran textos técnicos, que entran
en un celular de 390 px, el paginador, el escapado de HTML, la pantalla de acceso y la app instalable.

Corren en GitHub Actions en cada push. En local (PowerShell, desde la raíz del repo):

```powershell
cd .\frontend\web-app
npm run build:pruebas
cd ..\..
python -m pip install -r tests\pantallas\requirements.txt
python -m playwright install chromium
python -m pytest tests\pantallas
```

Para agregar una pantalla nueva: sumar su ruta en `test_pantallas.py` y, si llama a endpoints nuevos, su respuesta
en `RUTAS` de `servidor.py`.
