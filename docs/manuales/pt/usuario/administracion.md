## Painel de administração

Menu `ADM`. Estado da plataforma, e-mail, revisão periódica, arquivos, backups e segurança das contas.

![Painel de administração](administracion.png)

| Painel | O que mostra ou permite |
|---|---|
| Estado da plataforma | se os serviços respondem, a versão, a memória e os componentes (PostgreSQL, MongoDB, armazenamento). **Atualizar** verifica novamente. |
| Planos | quantos pedidos de mudança de plano há para resolver, com link para *Planos e pedidos*. |
| E-mail | a caixa de saída: pendentes, enviados e com falha. Os avisos saem sozinhos e os que falham são reenviados; **Enviar pendentes** força o envio. |
| Revisão periódica | a última execução (reauditorias, achados vencidos, configurações desatualizadas, e-mails). Roda sozinha todos os dias às 6:00; **Executar revisão agora** a dispara na hora. |
| Repositório de arquivos | configurações importadas e relatórios com o seu hash SHA-256. **Enviar pendentes** envia os que ainda não têm o seu arquivo. |
| Backups | **Gerar backup** cria um ZIP com os dados de todas as tabelas e o script de restauração. Inclui dados pessoais: baixe-o somente para um local seguro. |
| Segurança | usuários ativos, contas bloqueadas, acessos e recusas das últimas 24 horas, com link para o registro do sistema. |
