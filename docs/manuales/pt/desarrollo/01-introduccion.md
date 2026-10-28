## Introdução

O AuditNet é uma plataforma web B2B para auditar configurações de equipamentos de rede Cisco IOS/IOS-XE. Ela compara a
configuração real de cada equipamento (running-config) com uma configuração esperada (baseline), detecta os desvios,
prioriza-os por severidade e pela criticidade do equipamento, gera alertas e relatórios e preserva a evidência de cada
auditoria. É multiorganização: cada cliente vê apenas os seus dados.

Este manual é para quem precisa **instalar, operar ou modificar** a plataforma: desenvolvedores que entram no projeto e
quem a implanta no Azure. Ele cobre:

- a arquitetura e por que ela é montada assim;
- como o repositório está organizado e quais convenções segue;
- como executá-la em um computador local e carregar dados de demonstração;
- como rodar os testes;
- como adicionar um endpoint, uma migração, uma tela, uma tradução ou um e-mail;
- o fluxo de integração e implantação no Azure e a operação diária;
- os problemas reais que surgiram durante o desenvolvimento e como foram resolvidos;
- o que cada perfil pode fazer e onde isso é controlado no código.

| Dado | Valor |
|---|---|
| Repositório | `github.com/Leonel-Caruso/tfi-auditnet` |
| Branch de trabalho | `entrega-2` (é unida à `main` ao fechar a entrega) |
| Decisões de projeto | `docs/entrega-2/REGISTRO-DECISIONES.md` (D-01 em diante) |
| Migrações | `docs/database/MIGRACIONES.md` |
| Implantação | `docs/deploy/AZURE.md` |

> Nos comandos, o que está entre `<` e `>` é substituído pelo valor real (sem os sinais). Senhas e segredos nunca são
> escritos no repositório nem na linha de comando: os scripts os pedem ao serem executados.
