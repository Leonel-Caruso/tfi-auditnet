## Real problems and how they were solved

| Symptom | Cause | Solution |
|---|---|---|
| `No plugin found for prefix 'quarkus'` | `mvn quarkus:dev` run from the repository root | Run it inside `backend/<service>`. |
| audit-core does not start the first time | Its migrations reference management tables | Start management-service first. |
| The MongoDB driver rejects Cosmos DB | Azure created the account with API 3.6 and the driver requires 4.2 | Upgrade it from the portal (*Upgrade MongoDB server version*); `04-crear-cosmos.sh` checks it. |
| `SubscriptionNotFound` when creating a resource | The provider for that resource type was not registered in the subscription | `variables.sh` registers them first (`registrar_proveedores`). |
| `ExpressEnvironmentResourceNotSupported` | The Container Apps environment was created in *express* mode, which does not support scheduled jobs | The periodic review is scheduled with a Logic App (`09-crear-revision.sh`). |
| The smoke test gets `HTTP 000` and the web app has no address | In the Container Apps express environment, updating the image left the web app without ingress | `03-actualizar-imagenes.sh` checks the ingress of each app and enables it again. By hand: `az containerapp ingress enable -n ca-auditnet-web -g rg-auditnet --type external --target-port 8080`. |
| The first screen takes several seconds | The apps scaled to zero or PostgreSQL is stopped | `07-modo-demo.sh on` before presenting. |
| Cloud Shell does not find the repository or the variables | The Cloud Shell session is ephemeral | Clone or `git pull` and define the variables again in every session. |
| `unrecognized arguments` in `az postgres flexible-server firewall-rule` | The CLI changed its syntax | Use `--server-name <server> --name <rule>`. From Cloud Shell no rule is needed: it gets in through the Azure services rule. |
| Welcome emails take a while to arrive | The Azure managed domain is limited to 10 emails per hour | Wait; the outbox retries them. For more volume, a verified custom domain. |
| A site script does not run on Azure but does in development | The security policy blocks scripts written inside the HTML | Scripts in separate files; Astro configured with `assetsInlineLimit: 0`. |
| `wait_for_function` fails in the screen tests | It evaluates text as code and the policy blocks it | Use `pagina.esperar(...)`. |
| Playwright does not find the browser | The installed version does not match the package | `python -m playwright install chromium`. |
| *The account is locked* while testing a script | 5 wrong passwords in a row | Wait 15 minutes or unlock it from Users. |
| After deploying, the previous version shows | The browser cached the page | Reload with Ctrl+F5 (files in `/_astro/` are hashed; the HTML is always revalidated). |
| *Your session ended* without signing out | The profile, organization or password changed, or the token expired | Sign in again: this is the expected behavior. |
