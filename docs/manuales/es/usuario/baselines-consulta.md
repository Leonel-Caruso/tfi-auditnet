## Baselines y reglas

Una **baseline** es la configuración esperada para un tipo de dispositivo en una organización. Sus **reglas** son los
controles que el motor verifica en cada auditoría. Hay una sola baseline activa por tipo de dispositivo y
organización: es la que se usa al auditar. Las crea y versiona el auditor técnico.

### Baselines

Menú `BL`. Al elegir una ves su organización, tipo, versión, cantidad de reglas y de auditorías, y sus versiones.

![Baselines con el detalle de la versión vigente](baselines.png)

### Reglas

Menú `RUL`. Cada regla tiene un código, una condición, una severidad, el impacto y la recomendación para corregirla.

![Reglas de control con el detalle de una regla](reglas.png)

| Condición | Cumple cuando |
|---|---|
| debe contener | el patrón aparece en la configuración |
| no debe contener | el patrón no aparece |
| valor esperado | alguna línea que empieza con el parámetro tiene exactamente ese valor |
