# Pruebas de pantalla

Abren el sitio compilado en Chromium (Playwright) contra una API simulada (`servidor.py`, datos en `datos.py`) y
verifican, con cada uno de los cinco perfiles: que todas las pantallas cargan sin errores, que el menú es el que le
corresponde, que no muestran textos técnicos y que entran en un celular de 390 px. También el paginador, el escapado de
HTML, la pantalla de acceso, la app instalable y los circuitos del administrador de organización y de los planes
(`test_perfiles.py`).

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

El servidor envía la misma política de seguridad de contenido que nginx: si una pantalla usa un script escrito dentro
del HTML, la prueba falla. Por esa política, para esperar una condición se usa `pagina.esperar("<expresión>")` en lugar
de `wait_for_function`. Con `abrir(..., sistema_oscuro=True)` la página se abre con el sistema en modo oscuro.
