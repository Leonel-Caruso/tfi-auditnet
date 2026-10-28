## Repositório e convenções

```text
backend/management-service/   Quarkus: acesso, usuários, organizações, planos, e-mail
backend/audit-core-service/   Quarkus: inventário, baselines, auditoria, achados, relatórios
frontend/web-app/             Astro: páginas, scripts, dicionários, estilos e nginx
docker/                       PostgreSQL, MongoDB, Azurite e Mailpit para desenvolvimento local
infra/azure/                  Scripts de criação e operação no Azure (01 a 09)
scripts/                      load-env.ps1 e datos-demo/ (carga de demonstração)
tests/pantallas/              Testes de tela (Playwright e API simulada)
tests/datos_demo/             Testes dos dados de demonstração
tests/carga/                  Teste de carga com k6
docs/                         Decisões, migrações, implantação e manuais
.github/workflows/            CI, imagens e implantação
```

### Camadas de cada serviço

```text
ar/edu/uai/tfi/<servico>/
  api/rest/         recursos JAX-RS e DTO: validam a entrada, @RolesAllowed e escopo
  application/      casos de uso e portas (application/port), sem infraestrutura
  domain/           modelo (records e enums) e repositórios como interfaces
  infrastructure/   adaptadores: Panache, segurança, e-mail, registros, arquivos, i18n
```

As dependências apontam para dentro: `api → application → domain`, e `infrastructure` implementa as portas do domínio
e da aplicação. Por isso os serviços de aplicação são testados com implementações em memória (`src/test/.../support`).

### Convenções

- **Nomes em espanhol** no domínio, nos serviços e nos textos; as rotas da API em inglês (`/api/devices`,
  `/api/findings`).
- **Código simples e defensável:** sem comentários extensos; só os que explicam um porquê.
- **Migrações Flyway** em `src/main/resources/db/migration` de cada serviço: `V<n>__<descricao>.sql`. Uma migração já
  aplicada nunca é modificada; as mudanças vão em uma nova. Histórico: `flyway_historial_management` e
  `flyway_historial_audit_core`.
- **Decisões** em `REGISTRO-DECISIONES.md` com número `D-xx`: o que foi decidido, por quê, alternativas e teste.
- **Textos da interface** nunca escritos no HTML nem nos scripts: sempre a partir dos dicionários (veja *Como estender
  a plataforma*).
- **Sem segredos no Git:** `.env` é ignorado; `.env.example` lista as variáveis sem valores.
