## Baselines e regras

Uma **baseline** é a configuração esperada para um tipo de dispositivo em uma organização. As suas **regras** são os
controles que o motor verifica em cada auditoria. Só pode haver **uma baseline ativa por tipo de dispositivo e
organização**: é a usada ao auditar.

### Baselines

Menu `BL`. Ao escolher uma baseline você vê a organização, o tipo, a versão, a quantidade de regras e de auditorias e
as versões anteriores.

![Baselines com o detalhe da versão vigente](baselines.png)

**Criar uma baseline**

1. Clique em **+ nova baseline**.
2. Preencha **nome**, **descrição**, **tipo de dispositivo** e **organização**.
3. Clique em **Criar baseline**. Ela fica como versão 1, ativa.

**Mudanças em uma baseline**

| Ação | O que faz |
|---|---|
| **Editar** | Muda a descrição. Não muda os critérios, por isso não gera versão. |
| **Nova versão** | Copia as regras ativas para uma nova versão, que fica ativa; a anterior passa a inativa e mantém as suas auditorias. |
| **Desativar / Ativar** | Se você desativar a única ativa de um tipo, as novas auditorias desse tipo não têm baseline aplicável. |

> **Regras congeladas:** quando uma versão já foi usada em auditorias, as suas regras não são alteradas, para
> preservar a evidência. Para mudá-las, gere uma nova versão.

### Regras

Menu `RUL`. Lista os controles com a sua baseline, condição, severidade e estado. Ao escolher um, você vê o padrão, o
impacto e a recomendação.

![Regras de controle com o detalhe de uma regra](reglas.png)

**Criar uma regra**

1. Clique em **+ nova regra**.
2. Escolha a **baseline** e preencha **código** (letras, números, hífens, pontos ou sublinhado, por exemplo
   `SEC-SSH-01`), **nome** e **severidade** (baixa, média, alta ou crítica).
3. Escolha a **condição**:

| Condição | Cumpre quando | Exemplo |
|---|---|---|
| deve conter | o padrão aparece na configuração | `service password-encryption` |
| não deve conter | o padrão não aparece | `transport input telnet` |
| valor esperado | alguma linha que começa com o parâmetro tem exatamente esse valor | parâmetro `ip ssh version`, valor `2` |

4. Preencha **descrição**, **impacto** (que risco implica) e **recomendação** (como corrigi-lo).
5. Clique em **Criar regra**.

![Cadastro de uma regra](regla-alta.png)

A comparação não diferencia maiúsculas e espaços repetidos contam como um. Não são permitidas duas regras ativas que
avaliem a mesma coisa em uma baseline, nem uma que exija o contrário de outra.

**Editar** muda tudo, menos o código e a baseline. **Desativar** tira a regra das novas auditorias; as anteriores
mantêm o seu resultado.
