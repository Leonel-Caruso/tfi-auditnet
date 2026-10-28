## Problemas reais e como foram resolvidos

| Sintoma | Causa | Solução |
|---|---|---|
| `No plugin found for prefix 'quarkus'` | `mvn quarkus:dev` executado a partir da raiz do repositório | Executá-lo dentro de `backend/<servico>`. |
| O audit-core não inicia na primeira vez | As suas migrações referenciam tabelas do management | Subir primeiro o management-service. |
| O driver do MongoDB recusa o Cosmos DB | O Azure criou a conta com a API 3.6 e o driver exige 4.2 | Atualizá-la no portal (*Upgrade MongoDB server version*); `04-crear-cosmos.sh` verifica isso. |
| `SubscriptionNotFound` ao criar um recurso | O provedor desse tipo de recurso não estava registrado na assinatura | `variables.sh` os registra antes (`registrar_proveedores`). |
| `ExpressEnvironmentResourceNotSupported` | O ambiente de Container Apps ficou no modo *express*, que não aceita jobs agendados | A revisão periódica é agendada com uma Logic App (`09-crear-revision.sh`). |
| O teste de fumaça dá `HTTP 000` e a web não tem endereço | No ambiente express do Container Apps, atualizar a imagem deixou a app web sem entrada | `03-actualizar-imagenes.sh` verifica a entrada de cada app e a habilita de novo. Manualmente: `az containerapp ingress enable -n ca-auditnet-web -g rg-auditnet --type external --target-port 8080`. |
| A primeira tela demora vários segundos | As apps escalaram para zero ou o PostgreSQL está parado | `07-modo-demo.sh on` antes de apresentar. |
| O Cloud Shell não encontra o repositório nem as variáveis | A sessão do Cloud Shell é efêmera | Clonar ou `git pull` e definir as variáveis novamente em cada sessão. |
| `unrecognized arguments` em `az postgres flexible-server firewall-rule` | A CLI mudou a sintaxe | Usar `--server-name <servidor> --name <regra>`. No Cloud Shell não é preciso regra: ele entra pela de serviços do Azure. |
| Os e-mails de boas-vindas demoram a chegar | O domínio gerenciado do Azure limita a 10 envios por hora | Aguardar; a caixa de saída tenta de novo. Para mais volume, um domínio próprio verificado. |
| Um script do site não roda no Azure, mas roda em desenvolvimento | A política de segurança bloqueia scripts escritos dentro do HTML | Scripts em arquivos separados; Astro configurado com `assetsInlineLimit: 0`. |
| `wait_for_function` falha nos testes de tela | Ele avalia texto como código e a política o bloqueia | Usar `pagina.esperar(...)`. |
| O Playwright não encontra o navegador | A versão instalada não coincide com a do pacote | `python -m playwright install chromium`. |
| *A conta está bloqueada* ao testar um script | 5 senhas erradas seguidas | Aguardar 15 minutos ou desbloquear em Usuários. |
| Depois de implantar, aparece a versão anterior | O navegador guardou a página | Recarregar com Ctrl+F5 (os arquivos de `/_astro/` têm hash; o HTML é sempre revalidado). |
| *Sua sessão terminou* sem ter saído | O perfil, a organização ou a senha mudaram, ou o token venceu | Entrar novamente: é o comportamento esperado. |
