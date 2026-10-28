## Baselines y reglas

Una **baseline** es la configuración esperada para un tipo de dispositivo en una organización. Sus **reglas** son los
controles que el motor verifica en cada auditoría. Solo puede haber **una baseline activa por tipo de dispositivo y
organización**: es la que se usa al auditar.

### Baselines

Menú `BL`. Al elegir una baseline ves su organización, tipo, versión, cantidad de reglas y de auditorías, y sus
versiones anteriores.

![Baselines con el detalle de la versión vigente](baselines.png)

**Crear una baseline**

1. Tocá **+ nueva baseline**.
2. Completá **nombre**, **descripción**, **tipo de dispositivo** y **organización**.
3. Tocá **Crear baseline**. Queda como versión 1, activa.

**Cambios sobre una baseline**

| Acción | Qué hace |
|---|---|
| **Editar** | Cambia la descripción. No cambia los criterios, así que no genera versión. |
| **Nueva versión** | Copia las reglas activas en una versión nueva, que queda activa; la anterior pasa a inactiva y conserva sus auditorías. |
| **Desactivar / Activar** | Si desactivás la única activa de un tipo, las auditorías nuevas de ese tipo no tienen baseline aplicable. |

> **Reglas congeladas:** cuando una versión ya se usó en auditorías, sus reglas no se modifican, para conservar la
> evidencia. Para cambiarlas generá una nueva versión.

### Reglas

Menú `RUL`. Lista los controles con su baseline, condición, severidad y estado. Al elegir uno ves su patrón, impacto y
recomendación.

![Reglas de control con el detalle de una regla](reglas.png)

**Crear una regla**

1. Tocá **+ nueva regla**.
2. Elegí la **baseline** y completá **código** (letras, números, guiones, puntos o guion bajo, por ejemplo
   `SEC-SSH-01`), **nombre** y **severidad** (baja, media, alta o crítica).
3. Elegí la **condición**:

| Condición | Cumple cuando | Ejemplo |
|---|---|---|
| debe contener | el patrón aparece en la configuración | `service password-encryption` |
| no debe contener | el patrón no aparece | `transport input telnet` |
| valor esperado | alguna línea que empieza con el parámetro tiene exactamente ese valor | parámetro `ip ssh version`, valor `2` |

4. Completá **descripción**, **impacto** (qué riesgo implica) y **recomendación** (cómo corregirlo).
5. Tocá **Crear regla**.

![Alta de una regla](regla-alta.png)

La comparación no distingue mayúsculas y los espacios repetidos cuentan como uno. No se permiten dos reglas activas
que evalúen lo mismo en una baseline, ni una que exija lo contrario de otra.

**Editar** cambia todo menos el código y la baseline. **Desactivar** saca la regla de las auditorías nuevas; las
anteriores conservan su resultado.
