// Diccionarios de la consola: los textos comunes (es.ts, en.ts, pt.ts) y uno por pantalla en pantallas/, cada uno
// con el español, el inglés y el portugués. {nombre} se reemplaza por el dato al mostrarlo.
import comunEs from './es';
import comunEn from './en';
import comunPt from './pt';
import * as general from './pantallas/general';
import * as tablero from './pantallas/tablero';
import * as cliente from './pantallas/cliente';
import * as organizaciones from './pantallas/organizaciones';
import * as usuarios from './pantallas/usuarios';
import * as plan from './pantallas/plan';
import * as planes from './pantallas/planes';
import * as bitacoras from './pantallas/bitacoras';
import * as admin from './pantallas/admin';
import * as dispositivos from './pantallas/dispositivos';
import * as baselines from './pantallas/baselines';
import * as reglas from './pantallas/reglas';
import * as auditorias from './pantallas/auditorias';
import * as historial from './pantallas/historial';
import * as hallazgos from './pantallas/hallazgos';
import * as alertas from './pantallas/alertas';
import * as reportes from './pantallas/reportes';

export const es = {
  ...comunEs,
  ...general.es,
  ...tablero.es,
  ...cliente.es,
  ...organizaciones.es,
  ...usuarios.es,
  ...plan.es,
  ...planes.es,
  ...bitacoras.es,
  ...admin.es,
  ...dispositivos.es,
  ...baselines.es,
  ...reglas.es,
  ...auditorias.es,
  ...historial.es,
  ...hallazgos.es,
  ...alertas.es,
  ...reportes.es,
};

export type Clave = keyof typeof es;

export const en: Record<Clave, string> = {
  ...comunEn,
  ...general.en,
  ...tablero.en,
  ...cliente.en,
  ...organizaciones.en,
  ...usuarios.en,
  ...plan.en,
  ...planes.en,
  ...bitacoras.en,
  ...admin.en,
  ...dispositivos.en,
  ...baselines.en,
  ...reglas.en,
  ...auditorias.en,
  ...historial.en,
  ...hallazgos.en,
  ...alertas.en,
  ...reportes.en,
};

export const pt: Record<Clave, string> = {
  ...comunPt,
  ...general.pt,
  ...tablero.pt,
  ...cliente.pt,
  ...organizaciones.pt,
  ...usuarios.pt,
  ...plan.pt,
  ...planes.pt,
  ...bitacoras.pt,
  ...admin.pt,
  ...dispositivos.pt,
  ...baselines.pt,
  ...reglas.pt,
  ...auditorias.pt,
  ...historial.pt,
  ...hallazgos.pt,
  ...alertas.pt,
  ...reportes.pt,
};
