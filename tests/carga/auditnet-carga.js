// Prueba de carga k6 de solo lectura sobre la consola (tablero, hallazgos, alertas, historial).
// Uso: k6 run -e BASE_URL=https://<sitio> -e USUARIO=<usuario> -e CLAVE=<VALOR_CONFIGURADO_EN_ENV> tests/carga/auditnet-carga.js
// Opcionales: -e VUS=10 -e DURACION=1m. Conviene un usuario de solo consulta creado para la prueba.
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:4321').replace(/\/+$/, '');
const VUS = Number(__ENV.VUS || 10);
const DURACION = __ENV.DURACION || '1m';

const tiempoTablero = new Trend('auditnet_tablero_ms', true);
const tiempoHallazgos = new Trend('auditnet_hallazgos_ms', true);
const tiempoAlertas = new Trend('auditnet_alertas_ms', true);
const tiempoHistorial = new Trend('auditnet_historial_ms', true);

export const options = {
  scenarios: {
    consola: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: VUS }, // subida gradual
        { duration: DURACION, target: VUS }, // meseta
        { duration: '15s', target: 0 }, // bajada
      ],
      gracefulRampDown: '10s',
    },
  },
  // Si algún umbral falla, k6 termina con código 99. El calentamiento y el login de setup() no cuentan.
  thresholds: {
    'http_req_failed{fase:medicion}': ['rate<0.01'], // menos del 1 % de errores
    'http_req_duration{fase:medicion}': ['p(95)<1500'], // 95 % de las respuestas en menos de 1,5 s
    'checks{fase:medicion}': ['rate>0.99'],
    'http_reqs{fase:medicion}': ['count>0'], // sin solicitudes medidas la prueba no puede darse por aprobada
    auditnet_tablero_ms: ['p(95)<2000'],
    auditnet_hallazgos_ms: ['p(95)<1500'],
  },
  setupTimeout: '4m',
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

// Un solo inicio de sesión para toda la prueba: así no se dispara el bloqueo por intentos (5 fallidos = 15 min).
export function setup() {
  // Las Container Apps pueden estar en cero réplicas: se espera a que respondan antes de medir.
  for (const ruta of ['/api/management/status', '/api/audit-core/status']) {
    let codigo = 0;
    for (let intento = 0; intento < 20 && codigo !== 200; intento += 1) {
      codigo = http.get(`${BASE_URL}${ruta}`, { timeout: '20s', tags: { name: 'calentamiento' } }).status;
      if (codigo !== 200) sleep(10);
    }
    if (codigo !== 200) throw new Error(`${ruta} no responde (HTTP ${codigo}). ¿Está encendido el entorno?`);
  }
  if (!__ENV.USUARIO || !__ENV.CLAVE) throw new Error('Faltan USUARIO y CLAVE (-e USUARIO=... -e CLAVE=...).');
  const respuesta = http.post(`${BASE_URL}/api/auth/login`,
    JSON.stringify({ identidad: __ENV.USUARIO, password: __ENV.CLAVE }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } });
  if (respuesta.status !== 200) throw new Error(`No se pudo iniciar sesión (HTTP ${respuesta.status}).`);
  return { token: respuesta.json('accessToken') };
}

export default function (datos) {
  const parametros = (nombre) => ({
    headers: { Authorization: `Bearer ${datos.token}` },
    tags: { name: nombre, fase: 'medicion' },
  });

  group('tablero', () => {
    const r = http.get(`${BASE_URL}/api/dashboard/summary`, parametros('tablero'));
    tiempoTablero.add(r.timings.duration);
    check(r, { 'tablero 200': (x) => x.status === 200 }, { fase: 'medicion' });
  });
  sleep(1 + Math.random());

  group('hallazgos', () => {
    const r = http.get(`${BASE_URL}/api/findings?estado=PENDIENTES&pagina=1&porPagina=25`, parametros('hallazgos'));
    tiempoHallazgos.add(r.timings.duration);
    check(r, {
      'hallazgos 200': (x) => x.status === 200,
      'hallazgos con total': (x) => x.headers['X-Total-Count'] !== undefined,
    }, { fase: 'medicion' });
  });
  sleep(1 + Math.random());

  group('alertas', () => {
    const r = http.get(`${BASE_URL}/api/alerts/critical`, parametros('alertas'));
    tiempoAlertas.add(r.timings.duration);
    check(r, { 'alertas 200': (x) => x.status === 200 }, { fase: 'medicion' });
  });
  sleep(1 + Math.random());

  group('historial', () => {
    const r = http.get(`${BASE_URL}/api/audits/history?pagina=1&porPagina=25`, parametros('historial'));
    tiempoHistorial.add(r.timings.duration);
    check(r, { 'historial 200': (x) => x.status === 200 }, { fase: 'medicion' });
  });
  sleep(2 + Math.random() * 2);
}

// Resumen legible por consola y detalle completo en JSON.
export function handleSummary(data) {
  const m = data.metrics;
  const medidas = m['http_reqs{fase:medicion}']?.values.count ?? 0;
  const ms = (valor) => (medidas === 0 || valor === undefined ? '—' : `${Math.round(valor)} ms`);
  const fila = (nombre, metrica) => `  ${nombre.padEnd(22)} p95 ${ms(m[metrica]?.values['p(95)']).padStart(8)}   promedio ${ms(m[metrica]?.values.avg).padStart(8)}   máx ${ms(m[metrica]?.values.max).padStart(8)}`;
  const estado = (r) => (medidas === 0 ? 'SIN MEDICIÓN' : r.ok ? 'OK' : 'FALLA');
  const umbrales = Object.entries(m)
    .filter(([, metrica]) => metrica.thresholds)
    .flatMap(([nombre, metrica]) => Object.entries(metrica.thresholds).map(([regla, r]) => `  ${estado(r).padEnd(12)} ${nombre} ${regla}`));
  const texto = [
    '',
    `AuditNet · prueba de carga contra ${BASE_URL}`,
    `  usuarios simultáneos: ${VUS} · solicitudes: ${m.http_reqs?.values.count ?? 0} (${(m.http_reqs?.values.rate ?? 0).toFixed(1)}/s, incluye calentamiento) · medidas: ${medidas}`,
    `  errores medidos: ${((m['http_req_failed{fase:medicion}']?.values.rate ?? 0) * 100).toFixed(2)} % · chequeos OK: ${((m['checks{fase:medicion}']?.values.rate ?? 0) * 100).toFixed(2)} %`,
    fila('todas las medidas', 'http_req_duration{fase:medicion}'),
    fila('tablero', 'auditnet_tablero_ms'),
    fila('hallazgos (pág. 1)', 'auditnet_hallazgos_ms'),
    fila('alertas', 'auditnet_alertas_ms'),
    fila('historial (pág. 1)', 'auditnet_historial_ms'),
    'Criterios:',
    ...umbrales,
    ...(medidas === 0 ? ['', 'No se midió ninguna solicitud: revisar el error de arriba (calentamiento o inicio de sesión).'] : []),
    '',
  ].join('\n');
  return { stdout: texto, [__ENV.SALIDA || 'resultado-carga.json']: JSON.stringify(data, null, 2) };
}
