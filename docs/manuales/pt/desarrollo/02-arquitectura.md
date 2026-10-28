## Arquitetura

```text
Navegador ──HTTPS──► web (nginx: site Astro + gateway da API)
                      ├─ /api/auth, users, roles, client-organizations, sites,
                      │  plans, admin ──────────────────────────► management-service
                      └─ o resto de /api ───────────────────────► audit-core-service

management-service ──┬─► PostgreSQL (dados, registros, caixa de saída de e-mail)
audit-core-service ──┤
                     └─► MongoDB / Cosmos DB (registro de exceções)
audit-core-service ────► Blob Storage (configurações, relatórios e backups)
management-service ────► Azure Communication Services (e-mail; Mailpit local)
Logic App (6:00) ──────► POST /internal/review/run (revisão periódica)
```

### Componentes

| Componente | Tecnologia | Responsabilidade |
|---|---|---|
| web | nginx + Astro 5 (site estático) | Serve o console e encaminha `/api/...` para cada serviço na mesma origem: os serviços não são expostos e não é preciso CORS. Publica a política de segurança de conteúdo. |
| management-service (porta 8081) | Java 21, Quarkus 3 | Acesso e JWT, usuários e perfis, organizações e sedes, planos, e-mail, registro do sistema. |
| audit-core-service (porta 8082) | Java 21, Quarkus 3 | Dispositivos, configurações, baselines e regras, motor de auditoria, achados, alertas, painel, relatórios, revisão periódica, arquivos e backups. |
| PostgreSQL 17 | Flyway por serviço | Um banco compartilhado; cada serviço migra as suas tabelas com a sua própria tabela de histórico. |
| MongoDB / Cosmos DB | API do MongoDB 4.2 | Registro de exceções (a entidade NoSQL). |
| Blob Storage / Azurite | contêiner privado | Arquivos com o seu SHA-256. |

### Decisões-chave

- **Dois serviços e um banco.** Eles são separados por responsabilidade (acesso e administração de um lado, auditoria
  do outro) e compartilham o PostgreSQL para que as consultas de escopo (organização, plano) sejam simples. O
  audit-core lê o plano com uma consulta nativa; por isso o management migra primeiro.
- **Segurança.** O login devolve um JWT assinado (HS256) com o usuário, a sua organização e os seus perfis. Cada
  endpoint declara os perfis permitidos com `@RolesAllowed` e cada serviço aplica o escopo por organização. Mudar o
  perfil, a organização ou a senha invalida as sessões abertas.
- **Três registros.** Sistema (acessos e segurança) e transações (valor anterior e novo de cada operação de negócio)
  no PostgreSQL; exceções no MongoDB com nível configurável. Um código de rastreio (`X-Request-ID`, colocado pelo
  nginx) relaciona uma mesma operação nos três.
- **Evidência inalterável.** Configurações, auditorias, acompanhamentos de achados e relatórios não são modificados: há
  triggers somente de inserção e os relatórios são um retrato fixo.
- **E-mail com caixa de saída.** Os avisos são salvos em `correos_salientes` dentro da mesma transação do fato que os
  origina e são despachados depois, com novas tentativas. Assim, uma falha do provedor não perde avisos.
- **Multi-idioma.** O console traduz no navegador com dicionários; as mensagens de erro são traduzidas com um catálogo
  conforme o `Accept-Language`; os e-mails usam o idioma do destinatário.
