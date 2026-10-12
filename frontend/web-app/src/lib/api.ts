export const MANAGEMENT_API_URL =
  import.meta.env.PUBLIC_MANAGEMENT_API_URL ?? 'http://localhost:8081';

export const AUDIT_CORE_API_URL =
  import.meta.env.PUBLIC_AUDIT_CORE_API_URL ?? 'http://localhost:8082';

export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  organizationId: number;
  nombre: string;
  username: string;
  email: string;
  roles: string[];
};

export type CurrentUser = {
  username: string;
  subject: string;
  organizationId: number | null;
  roles: string[];
};

export type Organization = {
  id: number;
  identificador: string;
  razonSocial: string;
  estado: string;
};

export type Site = {
  id: number;
  organizationId: number;
  nombre: string;
  ubicacion: string;
  estado: string;
};

export type User = {
  id: number;
  organizationId: number;
  nombre: string;
  email: string;
  nombreUsuario: string;
  estado: string;
  roles: string[];
  /** Fin del bloqueo por intentos fallidos; null si la cuenta no está bloqueada. */
  bloqueadoHasta: string | null;
  /** Último inicio de sesión exitoso; null si nunca ingresó. */
  ultimoAcceso: string | null;
};

/** Perfil de acceso fijo: solo se edita la descripción. */
export type Role = {
  id: number;
  nombre: string;
  descripcion: string;
  estado: string;
};

export type Permission = {
  modulo: string;
  accion: string;
  perfiles: string[];
};

export type HealthComponent = {
  nombre: string;
  estado: 'UP' | 'DOWN';
  latenciaMs: number;
  detalle: string;
};

export type ServiceHealth = {
  servicio: string;
  estado: 'UP' | 'DEGRADADO';
  version: string;
  inicio: string;
  segundosEnLinea: number;
  memoriaUsadaMb: number;
  memoriaMaximaMb: number;
  componentes: HealthComponent[];
  verificado: string;
};

export type SecuritySummary = {
  usuariosActivos: number;
  usuariosDeshabilitados: number;
  cuentasBloqueadas: number;
  eventosUltimas24h: Record<string, number>;
  ultimosRechazos: SystemLogRecord[];
  desde: string;
};

// Repositorio de archivos y respaldos (audit-core-service, /api/system).
export type StoredFile = {
  id: number;
  tipo: 'CONFIGURACION' | 'REPORTE' | 'RESPALDO';
  entidadId: number | null;
  organizacionId: number | null;
  nombre: string;
  tipoContenido: string;
  tamanioBytes: number;
  sha256: string;
  fecha: string;
  usuario: string;
  detalle: string | null;
};

export type StorageStatus = {
  configurado: boolean;
  contenedor: string;
  archivos: Record<'CONFIGURACION' | 'REPORTE' | 'RESPALDO', { cantidad: number; bytes: number }>;
  configuracionesPendientes: number;
  reportesPendientes: number;
};

export type StorageSyncResult = { configuraciones: number; reportes: number; pendientes: number };

export const ROLE_LABEL: Record<string, string> = {
  ADMINISTRADOR_SISTEMA: 'Administrador del sistema',
  ANALISTA_RED: 'Analista de redes',
  AUDITOR_TECNICO: 'Auditor técnico',
  RESPONSABLE_GESTION_IT: 'Responsable de gestión IT',
};

/** Opciones de fetch para enviar JSON (POST, PUT, PATCH). */
export function jsonRequest(method: 'POST' | 'PUT' | 'PATCH', payload: unknown): RequestInit {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  };
}

export async function readApiError(response: Response): Promise<string> {
  try {
    const payload = await response.json();
    return payload?.message ?? payload?.error ?? 'La operación no pudo completarse.';
  } catch {
    if (response.status === 401) return 'Credenciales inválidas o sesión vencida.';
    if (response.status === 403) return 'No tenés permisos para realizar esta operación.';
    return `Error HTTP ${response.status}.`;
  }
}

export type DeviceType = {
  id: number;
  nombre: string;
  fabricante: string;
  familia: string;
  activo: boolean;
};

export type NetworkDevice = {
  id: number;
  nombre: string;
  identificador: string;
  tipoDispositivoId: number;
  fabricante: string;
  organizacionId: number;
  sedeId: number | null;
  criticidad: 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  estado: 'ACTIVO' | 'INACTIVO';
};

export type ConfigurationBaseline = {
  id: number;
  nombre: string;
  descripcion: string;
  version: number;
  tipoDispositivoId: number;
  organizacionId: number;
  estado: 'ACTIVO' | 'INACTIVO';
  /** Auditorías que usaron esta versión; si es mayor a 0 sus reglas están congeladas. */
  auditorias: number;
};

export type RuleType = 'DEBE_CONTENER' | 'NO_DEBE_CONTENER' | 'VALOR_ESPERADO';

export type BaselineRule = {
  id: number;
  baselineId: number;
  codigo: string;
  nombre: string;
  descripcion: string;
  tipo: RuleType;
  /** Texto buscado, o parámetro evaluado en reglas VALOR_ESPERADO. */
  patron: string;
  valorEsperado: string | null;
  severidad: 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  impacto: string;
  recomendacion: string;
  estado: 'ACTIVA' | 'INACTIVA';
};

export type DeviceConfiguration = {
  id: number;
  dispositivoId: number;
  organizacionId: number;
  version: number;
  formato: 'TEXTO' | 'TXT' | 'CFG';
  nombreFuente: string;
  contenidoOriginal: string;
  contenidoNormalizado: string;
  fechaImportacion: string;
  usuarioResponsable: string;
};

export type AuditSummary = {
  id: number;
  configuracionId: number;
  dispositivoId: number;
  baselineId: number;
  organizacionId: number;
  fechaEjecucion: string;
  ejecutadoPor: string;
  totalReglas: number;
  reglasCumplidas: number;
  totalHallazgos: number;
  severidadMaxima: 'NINGUNA' | 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  resultado: 'CUMPLE' | 'CON_HALLAZGOS';
  estado: 'FINALIZADA';
};

export type RuleEvaluation = {
  id: number;
  auditoriaId: number;
  reglaId: number;
  codigoRegla: string;
  nombreRegla: string;
  tipo: RuleType;
  patron: string;
  severidad: 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  cumple: boolean;
  evidencia: string;
};

export type AuditFinding = {
  id: number;
  auditoriaId: number;
  reglaId: number;
  dispositivoId: number;
  baselineId: number;
  organizacionId: number;
  codigoRegla: string;
  nombreRegla: string;
  tipoRegla: RuleType;
  patron: string;
  severidad: 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  evidencia: string;
  recomendacion: string;
  /** Copia del impacto de la regla; null en hallazgos creados antes de existir el campo. */
  impacto: string | null;
  estado: FindingStatus;
  fechaDeteccion: string;
  /** Último cambio de estado de seguimiento (null si nunca cambió). */
  fechaEstado: string | null;
  usuarioEstado: string | null;
  /** Criticidad actual del dispositivo, usada para priorizar (null en el detalle de auditoría). */
  criticidadDispositivo: NetworkDevice['criticidad'] | null;
};

export type FindingStatus = 'ABIERTO' | 'EN_REVISION' | 'RESUELTO' | 'ACEPTADO';

/** Transiciones permitidas (igual que EstadoHallazgo en audit-core-service). */
export const FINDING_TRANSITIONS: Record<FindingStatus, FindingStatus[]> = {
  ABIERTO: ['EN_REVISION', 'RESUELTO', 'ACEPTADO'],
  EN_REVISION: ['ABIERTO', 'RESUELTO', 'ACEPTADO'],
  RESUELTO: ['ABIERTO'],
  ACEPTADO: ['ABIERTO'],
};

/** Cerrar (RESUELTO, ACEPTADO) o reabrir (ABIERTO) exige comentario. */
export const findingStatusNeedsComment = (status: FindingStatus) => status !== 'EN_REVISION';

export type FindingTracking = {
  id: number;
  hallazgoId: number;
  estadoAnterior: FindingStatus;
  estadoNuevo: FindingStatus;
  comentario: string | null;
  usuario: string;
  fecha: string;
};

export type AuditDetail = {
  auditoria: AuditSummary;
  evaluaciones: RuleEvaluation[];
  hallazgos: AuditFinding[];
  /** Configuración evaluada y baseline usada (null solo ante datos incompletos por un error previo). */
  configuracion: {
    id: number;
    version: number;
    formato: string;
    nombreFuente: string;
    fechaImportacion: string;
    usuarioResponsable: string;
  } | null;
  baseline: { id: number; nombre: string; version: number; estado: string } | null;
};

export type RuleChangeCategory =
  | 'NUEVO_HALLAZGO' | 'CORREGIDO' | 'PERSISTENTE' | 'SIN_CAMBIO' | 'REGLA_NUEVA' | 'REGLA_RETIRADA';

export type AuditComparison = {
  actual: AuditSummary;
  /** null si es la primera auditoría del dispositivo. */
  anterior: AuditSummary | null;
  cambios: {
    codigoRegla: string;
    nombreRegla: string;
    severidad: string | null;
    cumpleAntes: boolean | null;
    cumpleAhora: boolean | null;
    categoria: RuleChangeCategory;
  }[];
  resumen: Record<RuleChangeCategory, number>;
};


// Registros de las tres bitácoras (GET /api/admin/bitacoras/...).
export const SYSTEM_EVENTS = [
  'LOGIN_EXITOSO', 'LOGIN_RECHAZADO', 'CUENTA_BLOQUEADA', 'CUENTA_DESBLOQUEADA',
  'USUARIO_CREADO', 'USUARIO_MODIFICADO', 'USUARIO_HABILITADO', 'USUARIO_DESHABILITADO', 'ROLES_ASIGNADOS',
  'CONTRASENA_CAMBIADA', 'CONTRASENA_RESTABLECIDA', 'ROL_CREADO', 'ROL_MODIFICADO',
] as const;
export type SystemEvent = typeof SYSTEM_EVENTS[number];

export type SystemLogRecord = {
  id: number;
  fecha: string;
  evento: SystemEvent;
  resultado: 'EXITO' | 'FALLO';
  actor: string | null;
  usuarioAfectadoId: number | null;
  organizacionId: number | null;
  origenIp: string | null;
  userAgent: string | null;
  correlacion: string | null;
  detalle: string | null;
};

export type TransactionLogRecord = {
  id: number;
  fecha: string;
  servicio: string;
  entidad: string;
  entidadId: number | null;
  operacion: 'ALTA' | 'MODIFICACION' | 'CAMBIO_ESTADO' | 'BAJA_LOGICA' | 'IMPORTACION' | 'EJECUCION';
  actor: string;
  organizacionId: number | null;
  valorAnterior: string | null;
  valorNuevo: string | null;
  correlacion: string | null;
  detalle: string | null;
};

export type ExceptionLogRecord = {
  fecha: string;
  servicio: string;
  nivel: 'ERROR' | 'WARN';
  tipo: string;
  mensaje: string | null;
  traza: string | null;
  metodoHttp: string | null;
  ruta: string | null;
  estadoHttp: number;
  usuario: string | null;
  correlacion: string | null;
};

// Alertas críticas, tablero de control y estado general del cliente.
export type Severity = AuditFinding['severidad'];
export type DeviceCriticality = NetworkDevice['criticidad'];
export type AlertStatus = 'ACTIVA' | 'EN_ATENCION' | 'CERRADA';
export type TechnicalProfile = 'ANALISTA_RED' | 'AUDITOR_TECNICO';
export type RiskLevel = 'SIN_DATOS' | 'BAJO' | 'MEDIO' | 'ALTO';

export type CriticalAlert = {
  id: number;
  hallazgoId: number;
  auditoriaId: number;
  dispositivoId: number;
  organizacionId: number;
  severidad: 'ALTA' | 'CRITICA';
  estado: AlertStatus;
  fechaGeneracion: string;
  /** Perfil técnico al que se derivó (null si no se derivó). */
  perfilAsignado: TechnicalProfile | null;
  comentario: string | null;
  fechaEstado: string | null;
  usuarioEstado: string | null;
  dispositivoIdentificador: string | null;
  dispositivoNombre: string | null;
  sedeId: number | null;
  criticidadDispositivo: DeviceCriticality | null;
  codigoRegla: string | null;
  nombreRegla: string | null;
  estadoHallazgo: FindingStatus | null;
};

export type CriticalAlertDetail = {
  alerta: CriticalAlert;
  /** null si el hallazgo de origen no pudo obtenerse. */
  hallazgo: AuditFinding | null;
};

export type DeviceRisk = {
  dispositivoId: number;
  identificador: string;
  nombre: string;
  sedeId: number | null;
  criticidad: DeviceCriticality;
  nivel: RiskLevel;
  pendientes: number;
  criticosPendientes: number;
  altosPendientes: number;
  alertasVigentes: number;
  ultimaAuditoria: string | null;
};

export type WeeklyEvolution = {
  /** Lunes de la semana (yyyy-mm-dd). */
  semana: string;
  auditorias: number;
  hallazgosDetectados: number;
  hallazgosCerrados: number;
};

export type DashboardSummary = {
  generado: string;
  nivelRiesgo: RiskLevel;
  dispositivos: {
    registrados: number;
    activos: number;
    auditados: number;
    activosSinAuditar: number;
    porCriticidad: Record<DeviceCriticality, number>;
  };
  hallazgos: {
    pendientes: number;
    enRevision: number;
    cerrados: number;
    pendientesPorSeveridad: Record<Severity, number>;
    cerradosPorSeveridad: Record<Severity, number>;
  };
  alertas: { vigentes: number; activas: number; enAtencion: number; criticasVigentes: number };
  auditorias: { total: number; ultimos30Dias: number; cumplimientoActual: number | null };
  auditoriasRecientes: AuditSummary[];
  dispositivosEnRiesgo: DeviceRisk[];
  evolucion: WeeklyEvolution[];
};

export type SiteRisk = {
  /** null agrupa los dispositivos sin sede. */
  sedeId: number | null;
  nivel: RiskLevel;
  dispositivos: number;
  auditados: number;
  pendientes: number;
  criticosPendientes: number;
  alertasVigentes: number;
};

export type ClientAuditStatus = {
  organizacionId: number;
  resumen: DashboardSummary;
  sedes: SiteRisk[];
  dispositivos: DeviceRisk[];
  /** Últimos reportes de la organización (sin contenido). */
  reportes: ReportSummary[];
};

// Reportes de auditoría (foto fija guardada al generarlos).
export type ReportCriteria = {
  organizacionId: number | null;
  desde: string | null;
  hasta: string | null;
  dispositivoIds: number[];
  severidades: Severity[];
  estados: FindingStatus[];
  auditoriaIds: number[];
};

export type ReportSummary = {
  id: number;
  organizacionId: number | null;
  titulo: string;
  totalAuditorias: number;
  totalHallazgos: number;
  generadoPor: string;
  fechaGeneracion: string;
};

export type ReportContent = {
  resumen: {
    auditorias: number;
    dispositivos: number;
    reglasEvaluadas: number;
    reglasCumplidas: number;
    cumplimiento: number | null;
    hallazgos: number;
    hallazgosPendientes: number;
    hallazgosPorSeveridad: Record<string, number>;
    hallazgosPorEstado: Record<string, number>;
    severidadMaxima: string;
    resultadoGeneral: 'SIN_DESVIOS' | 'CON_DESVIOS';
  };
  auditorias: {
    id: number;
    fechaEjecucion: string;
    ejecutadoPor: string;
    dispositivoId: number;
    dispositivoIdentificador: string | null;
    dispositivoNombre: string | null;
    criticidadDispositivo: string | null;
    sedeId: number | null;
    configuracionId: number;
    configuracionVersion: number | null;
    baselineId: number;
    baselineNombre: string | null;
    baselineVersion: number | null;
    totalReglas: number;
    reglasCumplidas: number;
    totalHallazgos: number;
    severidadMaxima: string;
    resultado: string;
  }[];
  hallazgos: {
    id: number;
    auditoriaId: number;
    dispositivoId: number;
    dispositivoIdentificador: string | null;
    reglaId: number;
    codigoRegla: string;
    nombreRegla: string;
    severidad: Severity;
    impacto: string | null;
    evidencia: string;
    recomendacion: string;
    estado: FindingStatus;
    fechaDeteccion: string;
  }[];
  inconsistencias: string[];
};

export type AuditReport = ReportSummary & {
  criterios: ReportCriteria;
  contenido: ReportContent;
};
