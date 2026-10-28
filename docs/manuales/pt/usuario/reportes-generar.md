## Relatórios de auditoria

Menu `REP`. Um relatório reúne auditorias com o resumo executivo, achados, severidade, impacto e evidência. Ele fica
salvo como **retrato fixo**: reflete o estado dos achados no momento da emissão e não muda, mesmo que os dados mudem
depois.

![Critérios de um novo relatório e relatórios gerados](reportes.png)

### Gerar um relatório

1. Em **Critérios**, informe o intervalo de datas (**desde** e **até**) ou os números de **auditorias específicas**
   separados por vírgula (por exemplo `12, 15`; nesse caso as datas são opcionais).
2. Se quiser, restrinja por **dispositivo**, **severidade** e **estado** dos achados. Sem marcar = todos.
3. Clique em **Gerar relatório**.

O administrador do sistema também escolhe a organização (ou todas). Um relatório inclui até 500 auditorias: se os
critérios abrangerem mais, restrinja as datas. O título é montado no idioma de quem o gera.

### Ver e baixar

![Visualização do documento do relatório](reporte-documento.png)

Ao escolher um relatório, o documento é mostrado: dados gerais, resumo executivo com a conformidade, escopo e
critérios, distribuição de achados, auditorias incluídas, detalhe de cada achado e observações.

- **Baixar PDF:** imprime o documento em A4 com número de página. Sai no idioma do console.
- **Baixar CSV:** para Excel, com uma linha por achado. Em espanhol e português usa `;` como separador e em inglês `,`.
