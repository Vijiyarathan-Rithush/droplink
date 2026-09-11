package domain;

import java.util.Objects;

public record DiscoveredDevice(String name, NetworkEndpoint endpoint)
{
    public DiscoveredDevice
    {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Device name cannot be blank");
        name = name.trim();
        endpoint = Objects.requireNonNull(endpoint, "endpoint cannot be null");
    }

    @Override
    public String toString()
    {
        return name;
    }
}
