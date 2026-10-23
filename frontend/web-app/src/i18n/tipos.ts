/** Traducción de un diccionario: exactamente las mismas claves que el español. */
export type Textos<T> = { [K in keyof T]: string };
