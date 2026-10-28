## Integração contínua e implantação no Azure

### GitHub Actions

`.github/workflows/imagenes.yml` roda em cada push para `main` ou `test` que altere código, testes ou o workflow:

| Job | O que faz |
|---|---|
| Tests | `mvn test` dos dois serviços |
| Frontend | `npm ci`, `npm run check` e build |
| Testes de tela | Playwright contra o site compilado, e os dos dados de demonstração |
| Imagens | Publica as 3 imagens no GitHub Container Registry com as tags `sha-<commit>` e a branch |
| Implantar no Azure | Só a partir de `main`: espera a **aprovação** do ambiente `azure`, entra no Azure por OIDC (sem senhas), atualiza as 3 Container Apps para `sha-<commit>` e faz um teste de fumaça |

Para aprovar: *Actions → a execução → Review deployments → azure → Approve and deploy*.

### Recursos no Azure

| Recurso | Nome | Para quê |
|---|---|---|
| Grupo de recursos | `rg-auditnet` (Brazil South) | Tudo da plataforma |
| PostgreSQL Flexible Server | `psql-auditnet-lcaruso`, banco `tfi_auditnet` | Dados e registros (B1ms, cópias de 7 dias) |
| Container Apps | `ca-auditnet-web`, `ca-auditnet-management`, `ca-auditnet-core` em `cae-auditnet` | O console com entrada pública e os dois serviços com entrada interna |
| Cosmos DB for MongoDB | `cosmos-auditnet-lcaruso` | Registro de exceções (plano gratuito) |
| Storage | `stauditnetlcaruso`, contêiner `auditnet-archivos` | Repositório de arquivos |
| Communication Services | domínio gerenciado pelo Azure | E-mail (5 por minuto e 10 por hora) |
| Logic App | revisão diária às 6:00 | Revisão periódica |

### Criar tudo do zero

No **Azure Cloud Shell (Bash)**, com o repositório clonado:

```bash
git clone -b main https://github.com/Leonel-Caruso/tfi-auditnet.git
cd tfi-auditnet/infra/azure
bash 01-crear-base.sh            # grupo, PostgreSQL, banco e ambiente de Container Apps
bash 02-crear-apps.sh            # as 3 Container Apps a partir das imagens publicadas
bash 04-crear-cosmos.sh          # registro de exceções
bash 05-crear-storage.sh         # repositório de arquivos
bash 06-configurar-github-oidc.sh  # implantação a partir do GitHub sem senhas
bash 08-crear-email.sh           # e-mail
bash 09-crear-revision.sh        # revisão periódica
```

Os scripts pedem as senhas ao serem executados e as salvam como *secrets* das Container Apps; não há segredos no
repositório. Podem ser executados de novo. `variables.sh` tem os nomes dos recursos e registra os provedores
necessários.

### Implantar uma versão manualmente

```bash
cd ~/tfi-auditnet/infra/azure && bash 03-actualizar-imagenes.sh sha-<commit>
```

Cada serviço aplica as suas migrações ao iniciar (o management primeiro). O painel de administração mostra a tag
implantada.

### Segurança da implantação

- HTTPS obrigatório com HSTS; o nginx adiciona `Content-Security-Policy`, `Permissions-Policy`, `X-Frame-Options` e
  `Referrer-Policy`.
- Os serviços não têm entrada pública: só se chega a eles pelo nginx. `/internal` não é publicado, exceto a revisão
  periódica, que exige um segredo de 256 bits.
- O PostgreSQL aceita conexões de serviços do Azure (incluindo o Cloud Shell). Para se conectar a partir de um PC, é
  preciso uma regra de firewall temporária com o seu IP.
- O GitHub Actions só pode atualizar as 3 apps, e cada implantação exige aprovação manual.
