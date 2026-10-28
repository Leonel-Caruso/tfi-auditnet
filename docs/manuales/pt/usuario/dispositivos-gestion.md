## Dispositivos

Menu `DEV`. É o inventário de equipamentos de rede que podem ser auditados. No topo ficam a busca (identificador, nome
ou fabricante) e os filtros por criticidade e estado; ao escolher um equipamento, o detalhe aparece à direita.

![Inventário de dispositivos com o detalhe do equipamento escolhido](dispositivos.png)

### Registrar um dispositivo

1. Clique em **+ registrar ativo**.
2. Preencha:
   - **identificador:** código único dentro da organização, por exemplo `ROS-RTR-01`;
   - **nome** descritivo;
   - **tipo de dispositivo** (o fabricante é preenchido conforme o tipo);
   - **organização** (o administrador do sistema a escolhe; o da organização registra na sua);
   - **sede** (opcional);
   - **criticidade:** baixa, média, alta ou crítica. Pesa no nível de risco e nos alertas.
3. Clique em **Registrar dispositivo**. Ele fica no estado ativo.

![Cadastro de um dispositivo](dispositivo-alta.png)

O cadastro conta para o limite de dispositivos ativos do plano da organização.

### Editar, desativar ou reativar

No detalhe do equipamento: **Editar** muda os seus dados (a organização não muda). **Desativar** o deixa inativo:
mantém o histórico, mas não aceita novas configurações nem auditorias. **Reativar** o habilita novamente. As duas ações
pedem confirmação e todas as mudanças ficam no registro de transações com o valor anterior e o novo.

### Tipos de dispositivo

Os tipos (por exemplo *Router ISR 4331 · Cisco · IOS-XE*) são um catálogo comum a todas as organizações, e as
baselines são definidas por tipo. Só o administrador do sistema os registra, com **+ tipo**: nome, fabricante e
família ou sistema operacional.
