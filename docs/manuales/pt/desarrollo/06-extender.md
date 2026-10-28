## Como estender a plataforma

### Um endpoint novo

1. **Domínio:** se necessário, um record em `domain/model` e os métodos na interface de `domain/repository`.
2. **Aplicação:** o caso de uso em um serviço de `application/<modulo>`. Ele valida, aplica o escopo da organização e
   registra a operação no registro de transações (`TrazabilidadPort`, com o valor anterior e o novo).
3. **Infraestrutura:** a implementação do repositório com Panache em `infrastructure/persistence`.
4. **API:** o recurso em `api/rest` com `@Path`, os DTO como records em `api/rest/dto` e **`@RolesAllowed`** com os
   perfis que podem usá-lo.
5. **Permissões:** se for uma ação nova, adicioná-la a `MatrizPermisos` (management) e a `RolServiceTest`; a matriz é
   mostrada na tela de Usuários.
6. **Gateway:** se o prefixo da rota for novo e pertencer ao management-service, adicioná-lo à expressão de
   `location ~ ^/api/(auth|users|...)` em `frontend/web-app/nginx/default.conf.template`. O resto vai para o
   audit-core.
7. **Mensagens:** cada nova mensagem de erro vista pelo usuário vai em `src/main/resources/i18n/mensajes.txt` do
   serviço, com as linhas `es:`, `en:` e `pt:` (se faltar, é mostrada em espanhol).
8. **Testes:** do serviço com os repositórios em memória e, se uma tela a usar, a resposta simulada em
   `tests/pantallas/servidor.py`.

### Uma migração

1. Criar `V<n>__<descricao>.sql` em `db/migration` do serviço dono da tabela, com o número seguinte ao último **desse**
   serviço.
2. Ajustar a entidade Panache e o repositório.
3. Adicionar a linha em `docs/database/MIGRACIONES.md`.
4. Ao iniciar (`mvn quarkus:dev`, ou a implantação no Azure), o Flyway a aplica sozinho. Nunca editar uma já aplicada.

### Um tipo de regra de auditoria

Os tipos são `DEBE_CONTENER`, `NO_DEBE_CONTENER` e `VALOR_ESPERADO`. Para adicionar um:

1. O valor em `TipoReglaConfiguracion` e a sua avaliação em `MotorAuditoriaService` (devolve se cumpre e a evidência).
2. Uma migração que amplie o `CHECK` de `reglas_baseline.tipo`.
3. No console, a opção no formulário de Regras, o seu nome no dicionário (`condicion.<TIPO>`) e, se a evidência tiver
   uma frase nova, a sua tradução em `lib/evidencia.ts`.
4. Testes do motor com configurações que cumprem e que não cumprem.

### Uma tela

1. `src/pages/<rota>.astro` com `MasterLayout` (o atributo `i18n` indica o prefixo das chaves do cabeçalho) e o seu
   script em `src/scripts/<rota>.ts`, carregado com `<script src="../scripts/<rota>.ts">`. Não se escrevem scripts
   dentro do HTML: a política de segurança não os permite.
2. O item do menu em `components/Sidebar.astro` (código curto e perfis) e a rota na lista de
   `components/Topbar.astro`; opcionalmente, palavras para *Ir para módulo* em `layouts/MasterLayout.astro`.
3. As chaves `menu.<rota>` e `ruta.<rota>` em `i18n/es.ts`, `en.ts` e `pt.ts`, e um dicionário
   `i18n/pantallas/<tela>.ts` com `es`, `en` e `pt`, adicionado em `i18n/index.ts`.
4. A rota em `tests/pantallas/test_pantallas.py` (`PANTALLAS` ou `PANTALLAS_POR_PERFIL`).

### Uma tradução

- No HTML: `data-i18n="chave"` para o texto e `data-i18n-placeholder`, `data-i18n-title`, `data-i18n-aria-label` ou
  `data-i18n-label` para atributos.
- Nos scripts: `t('chave', { dado })`, `plural(n, 'chave')` (chaves `chave.uno` e `chave.otros`) e
  `etiqueta('grupo', codigo)` para códigos como severidades ou estados.
- O tipo `Textos<typeof es>` obriga inglês e português a terem as mesmas chaves: `npm run check` falha se faltar uma.
  Os dados carregados pelos usuários (nomes de regras, sedes) não são traduzidos.

### Um e-mail

Os textos estão em `management-service/src/main/resources/correo/textos_<idioma>.properties` e a montagem em
`PlantillasCorreo`. Um tipo novo precisa do seu valor em `TipoCorreo`, dos seus textos nos três idiomas e de um teste
em `PlantillasCorreoTest`. Ele é enfileirado com `CorreoService.encolar(...)` (ou, a partir do audit-core, na caixa de
saída comum) e sai com as novas tentativas de sempre.

### Os manuais

As fontes estão em `docs/manuales/<idioma>/usuario` e `docs/manuales/<idioma>/desarrollo`, em Markdown. As capturas de
tela são feitas automaticamente a partir do site de testes com dados fictícios (`capturas.py`, `datos_manual.py`):

```powershell
cd .\frontend\web-app; npm run build:pruebas; cd ..\..
python -m pip install -r docs\manuales\requirements.txt
python docs\manuales\generar.py
```

Os manuais do usuário ficam em `frontend/web-app/public/manuales` (o console os oferece para download) e este em
`docs/manuales/pdf`.
