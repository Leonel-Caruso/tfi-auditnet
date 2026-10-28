## Testes

| O quê | Comando | Onde |
|---|---|---|
| Unitários e de integração de cada serviço | `mvn test` | em `backend/<servico>` |
| Tipos e dicionários do frontend | `npm run check` | em `frontend/web-app` |
| Testes de tela | `npm run build:pruebas` e `python -m pytest tests\pantallas` | build em `frontend/web-app`, pytest a partir da raiz |
| Dados de demonstração | `python -m pytest tests\datos_demo` | raiz |
| Carga (k6) | veja `docs/entrega-2/CARGA.md` | raiz |

### Testes dos serviços

Os serviços de aplicação são testados sem banco de dados, com repositórios em memória (`support/EnMemoria...`) e um
relógio fixo. Cada decisão do registro indica qual teste a respalda. Antes de enviar mudanças, os dois serviços devem
passar no `mvn test` completo.

### Testes de tela

`tests/pantallas` abre o site compilado no Chromium (Playwright) contra uma API simulada (`servidor.py`, dados em
`datos.py`) e percorre todas as telas com os cinco perfis: que carreguem sem erros, que não mostrem textos técnicos,
que caibam em um celular de 390 px, que estejam traduzidas para inglês e português e que o tema escuro não deixe fundos
claros.

```powershell
cd .\frontend\web-app
npm run build:pruebas
cd ..\..
python -m pip install -r tests\pantallas\requirements.txt
python -m playwright install chromium
python -m pytest tests\pantallas
```

- `build:pruebas` compila o site para chamar a API na mesma origem, como atrás do nginx.
- O servidor de testes envia a mesma política de segurança de conteúdo que o nginx: um script escrito dentro do HTML
  faz o teste falhar. Por isso, para esperar uma condição usa-se `pagina.esperar("<expressao>")` e não
  `wait_for_function`.
- Para uma tela nova: adicionar a rota em `test_pantallas.py` e, se ela chamar endpoints novos, a sua resposta em
  `RUTAS` de `servidor.py`.

### No CI

Cada push para `entrega-2` ou `main` executa, no GitHub Actions, os testes dos dois serviços, a verificação e o build
do frontend, os testes de tela e os dos dados de demonstração. Se algo falhar, as imagens não são publicadas.
