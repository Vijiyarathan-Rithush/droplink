package domain;

import utils.IValidateService;
import utils.ValidationService;

public record NetworkEndpoint(String ip, int port)
{
    private static final IValidateService VALIDATION_SERVICE =
            new ValidationService();

    public NetworkEndpoint
    {
        if (!VALIDATION_SERVICE.isValidIp(ip))
        {
            throw new IllegalArgumentException(
                    "Invalid IP address: " + ip);
        }

        if (!VALIDATION_SERVICE.isValidPort(port))
        {
            throw new IllegalArgumentException(
                    "Invalid port: " + port);
        }
    }
}