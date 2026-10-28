## Instalação local

### Requisitos

| Ferramenta | Versão |
|---|---|
| Docker Desktop | com Docker Compose |
| Java (JDK) | 21 |
| Maven | 3.9 ou superior |
| Node.js | 24 |
| Python | 3.11 ou superior (testes de tela, dados de demonstração e manuais) |
| PowerShell | 5 ou superior (Windows); no Linux ou macOS, os mesmos passos com `bash` |

### 1. Variáveis de ambiente

Copie `.env.example` para `.env` e preencha os valores locais: banco de dados, `JWT_SECRET`, o administrador inicial
(`BOOTSTRAP_ADMIN_*`) e a organização inicial (`BOOTSTRAP_ORG_*`). O `.env` não vai para o Git.

### 2. Bancos e serviços de apoio

```powershell
.\scripts\load-env.ps1
docker compose --env-file .env -f docker/compose.database.yaml up -d
docker ps
```

Sobe `tfi-postgres` (5432), `tfi-mongo` (27017), `tfi-azurite` (10000, armazenamento) e `tfi-mailpit` (os e-mails são
lidos em `http://localhost:8025`).

### 3. Serviços

Em um terminal, **primeiro o management-service** (aplica as suas migrações e cria os perfis, a organização e o
administrador iniciais):

```powershell
.\scripts\load-env.ps1
cd .\backend\management-service
mvn quarkus:dev
```

Quando aparecer `Listening on: http://localhost:8081`, em outro terminal:

```powershell
.\scripts\load-env.ps1
cd .\backend\audit-core-service
mvn quarkus:dev
```

### 4. Console

```powershell
cd .\frontend\web-app
npm install
npm run dev -- --host localhost
```

Abra `http://localhost:4321` e entre com o administrador do `.env`. Em desenvolvimento, o navegador chama os serviços
nas portas 8081 e 8082 (não passa pelo nginx).

### 5. Dados de demonstração (opcional)

`scripts/datos-demo` carrega 6 organizações fictícias com sedes, usuários, equipamentos, baselines, regras e cerca de
100 auditorias, usando a API como faria um administrador:

```powershell
python scripts\datos-demo\cargar.py --local --admin <usuario_admin> --correo <caixa>@gmail.com
docker cp .\scripts\datos-demo\historia.sql tfi-postgres:/tmp/historia.sql
docker exec tfi-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB -f /tmp/historia.sql'
python scripts\datos-demo\cargar.py --local --admin <usuario_admin> --correo <caixa>@gmail.com --reportes
```

- `cargar.py` pede a senha do administrador e a dos novos usuários; não as salva. Os e-mails dos usuários são aliases
  da caixa (`caixa+usuario@dominio`) e os seus avisos de auditoria ficam desligados.
- `historia.sql` distribui nas últimas 8 semanas as auditorias recém-carregadas com os seus achados e alertas.
- `--reportes` gera os relatórios depois de distribuídas as datas.
- Pode ser executado de novo: o que já existe é reutilizado.
