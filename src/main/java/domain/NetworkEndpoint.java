package domain;

import java.util.Objects;

public record NetworkEndpoint(String host, int port)
{
    private static final int MIN_PORT_NUMBER = 1;
    private static final int MAX_PORT_NUMBER = 65535;

    public NetworkEndpoint
    {
        Objects.requireNonNull(host, "host must not be null");
        if (host.isBlank()) throw new IllegalArgumentException("host must not be blank");
        if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) throw new IllegalArgumentException("port must not be negative");
    }
}
