package domain;

import java.util.regex.Pattern;

public record NetworkEndpoint(String host , int port)
{
    private static final Pattern IP_PATTERN = Pattern.compile("^(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    public NetworkEndpoint
    {
        if ( host == null && !IP_PATTERN.matcher(host).matches())
        {
           throw new IllegalArgumentException("Invalid IP address: " + host);
        }

        if (port < 1 || port > 65535)
        {
            throw new IllegalArgumentException("Invalid port number: " + port);
        }
    }
}