## Baselines e regras

Uma **baseline** é a configuração esperada para um tipo de dispositivo em uma organização. As suas **regras** são os
controles que o motor verifica em cada auditoria. Há uma única baseline ativa por tipo de dispositivo e organização: é
a usada ao auditar. Quem as cria e versiona é o auditor técnico.

### Baselines

Menu `BL`. Ao escolher uma, você vê a organização, o tipo, a versão, a quantidade de regras e de auditorias e as
versões.

![Baselines com o detalhe da versão vigente](baselines.png)

### Regras

Menu `RUL`. Cada regra tem um código, uma condição, uma severidade, o impacto e a recomendação para corrigi-la.

![Regras de controle com o detalhe de uma regra](reglas.png)

| Condição | Cumpre quando |
|---|---|
| deve conter | o padrão aparece na configuração |
| não deve conter | o padrão não aparece |
| valor esperado | alguma linha que começa com o parâmetro tem exatamente esse valor |
