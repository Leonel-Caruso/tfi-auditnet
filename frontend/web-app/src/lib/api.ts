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
};

export type BaselineRule = {
  id: number;
  baselineId: number;
  codigo: string;
  nombre: string;
  descripcion: string;
  tipo: 'DEBE_CONTENER' | 'NO_DEBE_CONTENER';
  patron: string;
  severidad: 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
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
  tipo: 'DEBE_CONTENER' | 'NO_DEBE_CONTENER';
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
  tipoRegla: 'DEBE_CONTENER' | 'NO_DEBE_CONTENER';
  patron: string;
  severidad: 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  evidencia: string;
  recomendacion: string;
  estado: 'ABIERTO';
  fechaDeteccion: string;
};

export type AuditDetail = {
  auditoria: AuditSummary;
  evaluaciones: RuleEvaluation[];
  hallazgos: AuditFinding[];
};

