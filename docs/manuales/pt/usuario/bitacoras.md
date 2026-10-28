## Registros

Menu `BIT`. Três registros somente leitura, cada um na sua aba:

| Registro | O que registra | Onde fica salvo |
|---|---|---|
| Auditoria do sistema | acessos e segurança: entradas, recusas, bloqueios, mudanças de contas | PostgreSQL |
| Transações | operações de negócio com o valor anterior e o novo (cadastros, mudanças, auditorias, planos) | PostgreSQL |
| Exceções | erros inesperados dos serviços, com o seu rastreamento | MongoDB |

![Registro de auditoria do sistema](bitacoras.png)

Filtre por evento, resultado, usuário, entidade ou serviço e por datas, e clique em **Consultar**. Ao escolher um
registro, você vê todos os seus campos. O **código de rastreio** identifica uma mesma operação nos três registros: no
detalhe, *Filtrar por este código nos 3 registros* mostra tudo o que aconteceu nessa operação.
