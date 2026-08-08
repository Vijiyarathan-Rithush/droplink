package domain;

import java.util.regex.Pattern;

public record NetworkEndpoint(String ip, int port)
{
    private static final Pattern IP_PATTERN = Pattern.compile("^(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    public NetworkEndpoint
    {
        if ( ip != null && !IP_PATTERN.matcher(ip).matches())
        {
           throw new IllegalArgumentException("Invalid IP address: " + ip);
        }

        if (port < 1 && port > 65535)
        {
            throw new IllegalArgumentException("Invalid port number: " + port);
        }
    }
}