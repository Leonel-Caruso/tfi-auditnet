## Pruebas

| Qué | Comando | Dónde |
|---|---|---|
| Unitarias e integración de cada servicio | `mvn test` | en `backend/<servicio>` |
| Tipos y diccionarios del frontend | `npm run check` | en `frontend/web-app` |
| Pruebas de pantalla | `npm run build:pruebas` y `python -m pytest tests\pantallas` | build en `frontend/web-app`, pytest desde la raíz |
| Datos de demostración | `python -m pytest tests\datos_demo` | raíz |
| Carga (k6) | ver `docs/entrega-2/CARGA.md` | raíz |

### Pruebas de los servicios

Los servicios de aplicación se prueban sin base de datos, con repositorios en memoria (`support/EnMemoria...`) y un
reloj fijo. Cada decisión del registro indica qué prueba la respalda. Antes de subir cambios, los dos servicios tienen
que pasar `mvn test` completo.

### Pruebas de pantalla

`tests/pantallas` abre el sitio compilado en Chromium (Playwright) contra una API simulada (`servidor.py`, datos en
`datos.py`) y recorre todas las pantallas con los cinco perfiles: que carguen sin errores, que no muestren textos
técnicos, que entren en un celular de 390 px, que estén traducidas en inglés y portugués y que el tema oscuro no deje
fondos claros.

```powershell
cd .\frontend\web-app
npm run build:pruebas
cd ..\..
python -m pip install -r tests\pantallas\requirements.txt
python -m playwright install chromium
python -m pytest tests\pantallas
```

- `build:pruebas` compila el sitio para llamar a la API en el mismo origen, como detrás de nginx.
- El servidor de pruebas envía la misma política de seguridad de contenido que nginx: un script escrito dentro del
  HTML hace fallar la prueba. Por eso, para esperar una condición se usa `pagina.esperar("<expresión>")` y no
  `wait_for_function`.
- Para una pantalla nueva: sumar la ruta en `test_pantallas.py` y, si llama a endpoints nuevos, su respuesta en
  `RUTAS` de `servidor.py`.

### En el CI

Cada push a `entrega-2` o `main` corre, en GitHub Actions, los tests de los dos servicios, el chequeo y build del
frontend, las pruebas de pantalla y las de datos de demostración. Si algo falla no se publican imágenes.
