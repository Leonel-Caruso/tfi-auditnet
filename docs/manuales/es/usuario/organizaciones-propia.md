## Tu organización y sus sedes

Menú `ORG`. Ves solo tu organización: a la derecha sus sedes y debajo el plan y los plazos de revisión.

![Tu organización con sus sedes, el plan y su uso](organizacion-propia.png)

### Sedes

1. Tocá **+ sede**.
2. Escribí el **nombre** (único dentro de la organización) y la **ubicación**.
3. Tocá **Registrar sede**.

**Editar** cambia esos datos. **Deshabilitar** una sede impide asignarle dispositivos nuevos; los que ya tiene
conservan su historial. **Habilitar** la vuelve a dejar disponible.

### Plan y uso

Muestra el plan vigente y cuántos dispositivos y usuarios activos usan del total permitido. **ver Mi plan →** abre el
detalle y el pedido de cambio (capítulo *Mi plan*).

### Plazos de revisión

![Plazos de revisión de la organización](plazos-revision.png)

| Campo | Qué define | Por defecto |
|---|---|---|
| crítica / alta / media / baja · días | En cuántos días vence un hallazgo pendiente de esa severidad, contados desde que se detectó | 7 / 15 / 30 / 60 |
| re-auditar cada · días | Cada cuánto se vuelve a auditar la última configuración de un equipo | 30 |
| configuración vieja a los · días | Desde cuándo un equipo sin configuración nueva cuenta como desactualizado | 30 |
| Re-auditar automáticamente | Si la revisión diaria vuelve a auditar sola los equipos cuyo plazo se cumplió | activado |

Los plazos de severidad tienen que ir de menor a mayor (crítica ≤ alta ≤ media ≤ baja) y cada valor va de 1 a 365
días. Tocá **Guardar plazos**: el cambio corrige al instante los vencimientos de todos los hallazgos.

> La revisión periódica se ejecuta todos los días a las 6:00 (hora de Argentina): re-audita lo que corresponde y avisa
> por correo los hallazgos vencidos y los equipos desactualizados. AuditNet no se conecta a los equipos: re-auditar es
> evaluar la última configuración cargada contra la baseline vigente.
