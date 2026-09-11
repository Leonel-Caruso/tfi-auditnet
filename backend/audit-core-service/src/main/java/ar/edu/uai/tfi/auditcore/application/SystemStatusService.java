package ar.edu.uai.tfi.auditcore.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SystemStatusService {

    private static final String STATUS_UP = "UP";

    @ConfigProperty(name = "app.name")
    String appName;

    @ConfigProperty(name = "app.delivery")
    String delivery;

    public SystemStatus getStatus() {
        return new SystemStatus(appName, STATUS_UP, delivery);
    }
}
