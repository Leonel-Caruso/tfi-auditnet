package ar.edu.uai.tfi.auditcore.application.config;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.stream.Collectors;

@ApplicationScoped
public class NormalizacionConfiguracionService {

    public String normalizar(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("La configuración no puede estar vacía.");
        }

        String lineasUnix = contenido
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        String normalizado = Arrays.stream(lineasUnix.split("\n", -1))
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                .strip();

        if (normalizado.isBlank()) {
            throw new IllegalArgumentException("La configuración no puede estar vacía.");
        }

        return normalizado;
    }
}
