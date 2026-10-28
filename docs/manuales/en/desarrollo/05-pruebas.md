## Testing

| What | Command | Where |
|---|---|---|
| Unit and integration tests of each service | `mvn test` | in `backend/<service>` |
| Frontend types and dictionaries | `npm run check` | in `frontend/web-app` |
| Screen tests | `npm run build:pruebas` and `python -m pytest tests\pantallas` | build in `frontend/web-app`, pytest from the root |
| Demo data | `python -m pytest tests\datos_demo` | root |
| Load (k6) | see `docs/entrega-2/CARGA.md` | root |

### Service tests

Application services are tested without a database, with in-memory repositories (`support/EnMemoria...`) and a fixed
clock. Each decision in the register states which test backs it. Before pushing changes, both services must pass the
full `mvn test`.

### Screen tests

`tests/pantallas` opens the compiled site in Chromium (Playwright) against a mock API (`servidor.py`, data in
`datos.py`) and goes through every screen with the five profiles: that they load without errors, that they do not show
technical texts, that they fit on a 390 px phone, that they are translated into English and Portuguese and that the
dark theme leaves no light backgrounds.

```powershell
cd .\frontend\web-app
npm run build:pruebas
cd ..\..
python -m pip install -r tests\pantallas\requirements.txt
python -m playwright install chromium
python -m pytest tests\pantallas
```

- `build:pruebas` compiles the site to call the API on the same origin, as behind nginx.
- The test server sends the same content security policy as nginx: a script written inside the HTML makes the test
  fail. That is why waiting for a condition uses `pagina.esperar("<expression>")` and not `wait_for_function`.
- For a new screen: add the route in `test_pantallas.py` and, if it calls new endpoints, their response in `RUTAS` of
  `servidor.py`.

### In CI

Every push to `entrega-2` or `main` runs, on GitHub Actions, the tests of both services, the frontend check and build,
the screen tests and the demo data tests. If anything fails, no images are published.
