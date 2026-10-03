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
  /** Copia del impacto de la regla; null en hallazgos anteriores a la Entrega 2. */
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


// Bloque 2.3d: registros de las tres bitácoras (GET /api/admin/bitacoras/...).
export type SystemLogRecord = {
  id: number;
  fecha: string;
  evento: 'LOGIN_EXITOSO' | 'LOGIN_RECHAZADO' | 'USUARIO_CREADO' | 'ROLES_ASIGNADOS' | 'ROL_CREADO';
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
