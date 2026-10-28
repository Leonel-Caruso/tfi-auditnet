## Operação

### Modo demo e custos

As Container Apps escalam para zero quando não há tráfego: a primeira requisição leva alguns segundos para acordá-las.
O PostgreSQL pode ficar parado para economizar crédito. Antes de uma apresentação:

```bash
cd ~/tfi-auditnet/infra/azure
bash 07-modo-demo.sh on       # 1 réplica mínima e PostgreSQL ligado
bash 07-modo-demo.sh estado
bash 07-modo-demo.sh off      # ao terminar: volta a escalar para zero
```

O modo `on` consome a cota mesmo sem tráfego. Os custos são acompanhados no *Cost Management* do portal.

### Backups

No **Painel de administração → Gerar backup**: um ZIP com os dados de todas as tabelas, a versão do esquema e o script
de restauração, salvo no repositório de arquivos. O Azure também mantém cópias automáticas do PostgreSQL por 7 dias.
Como restaurar: `docs/database/RESPALDOS.md`.

### Revisão periódica

Uma Logic App chama `POST /internal/review/run` todos os dias às 6:00 (horário da Argentina). Para cada organização
ativa: reaudita os equipamentos cuja última auditoria ultrapassou o prazo, avisa os achados vencidos e os equipamentos
sem configuração nova, e a primeira revisão de cada mês envia o resumo do mês anterior. Pode ser disparada
manualmente no painel de administração. Se o PostgreSQL estava parado, a execução desse dia falha e a seguinte coloca
tudo em dia.

### E-mail

Os avisos passam pela caixa de saída (`correos_salientes`); o painel de administração mostra os pendentes, enviados e
com falha, e **Enviar pendentes** força o envio. O domínio gerenciado do Azure permite poucos envios por hora: com
muitos cadastros seguidos, os e-mails saem aos poucos. Localmente, são lidos no Mailpit (`http://localhost:8025`).

### Diagnóstico

1. **Painel de administração:** estado de cada serviço e dos seus componentes.
2. **Registros:** buscar o código de rastreio da operação e filtrar os três registros com ele.
3. **Logs das apps:** `az containerapp logs show -g rg-auditnet -n ca-auditnet-management --tail 100`
   (ou `ca-auditnet-core`, `ca-auditnet-web`).

### Dados de demonstração no Azure

No Cloud Shell (o diretório não é mantido entre sessões: clonar ou atualizar o repositório a cada vez):

```bash
cd ~ && git clone -b main https://github.com/Leonel-Caruso/tfi-auditnet.git; cd ~/tfi-auditnet && git pull
SITIO=https://$(az containerapp show -g rg-auditnet -n ca-auditnet-web --query properties.configuration.ingress.fqdn -o tsv)
python3 scripts/datos-demo/cargar.py --url $SITIO --admin <usuario_admin> --correo <caixa>@gmail.com
export PGPASSWORD=$(az containerapp secret show -g rg-auditnet -n ca-auditnet-management --secret-name db-password --query value -o tsv)
psql "host=psql-auditnet-lcaruso.postgres.database.azure.com dbname=tfi_auditnet user=auditnetadmin sslmode=require" -f scripts/datos-demo/historia.sql
unset PGPASSWORD
python3 scripts/datos-demo/cargar.py --url $SITIO --admin <usuario_admin> --correo <caixa>@gmail.com --reportes
```
