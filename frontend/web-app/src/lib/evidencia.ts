// La evidencia la escribe el motor de auditoría en español y se guarda así (es parte del registro de la auditoría).
// Al mostrarla en inglés o portugués se reconocen sus frases y se traducen; las líneas de configuración quedan igual.
import { idiomaActual, t, type Clave } from './i18n';

const SIN_VALOR = '(sin valor)';

const FRASES: [RegExp, Clave, string[]][] = [
  [/^Patrón requerido encontrado: (.*)$/s, 'evidencia.requeridoEncontrado', ['linea']],
  [/^Patrón no permitido detectado: (.*)$/s, 'evidencia.prohibidoDetectado', ['linea']],
  [/^No se encontró el patrón requerido: (.*)$/s, 'evidencia.requeridoAusente', ['patron']],
  [/^No se detectó el patrón no permitido: (.*)$/s, 'evidencia.prohibidoAusente', ['patron']],
  [/^No se encontró el parámetro (.+?) \(valor esperado: (.*)\)\.$/s, 'evidencia.parametroAusente', ['parametro', 'esperado']],
  [/^Parámetro (.+?) con el valor esperado: (.*)\.$/s, 'evidencia.parametroCorrecto', ['parametro', 'esperado']],
  [/^Parámetro (.+?) con valor (.+?)(?: y (\d+) más)?; se esperaba: (.*)\.$/s, 'evidencia.parametroDistinto',
    ['parametro', 'encontrados', 'resto', 'esperado']],
];

/** La evidencia en el idioma de la consola; si no se reconoce la frase, tal cual se guardó. */
export function evidencia(texto: string): string {
  if (idiomaActual() === 'es') return texto;
  for (const [patron, clave, nombres] of FRASES) {
    const partes = patron.exec(texto);
    if (!partes) continue;
    const datos: Record<string, string> = {};
    nombres.forEach((nombre, i) => { datos[nombre] = partes[i + 1] ?? ''; });
    datos.encontrados = (datos.encontrados ?? '').replaceAll(SIN_VALOR, t('evidencia.sinValor'));
    datos.resto = datos.resto ? ` ${t('evidencia.yMas', { n: datos.resto })}` : '';
    return t(clave, datos);
  }
  return texto;
}
