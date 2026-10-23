// Descarga de archivos que llegan por fetch (con el token), con el nombre que indica el servidor.
import { localeActual } from './i18n';

/** Toma el nombre de Content-Disposition o usa el indicado. */
export function fileName(response: Response, fallback: string): string {
  const header = response.headers.get('Content-Disposition') ?? '';
  const match = /filename="?([^";]+)"?/i.exec(header);
  return match?.[1] ?? fallback;
}

export async function saveResponse(response: Response, fallback: string): Promise<void> {
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName(response, fallback);
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export const formatBytes = (bytes: number) => {
  const decimal = (valor: number) =>
    new Intl.NumberFormat(localeActual(), { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valor);
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${decimal(bytes / 1024)} KB`;
  return `${decimal(bytes / (1024 * 1024))} MB`;
};
