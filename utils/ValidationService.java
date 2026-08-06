package utils;

import java.util.regex.Pattern;

public class ValidationService implements IValidateService
{
    private static final Pattern IP_PATTERN = Pattern.compile("^(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    @Override
    public boolean isValidIp(String ip)
    {
        if (IP_PATTERN.matcher(ip).matches())
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isValidPort(int port)
    {
        if (port >= 1 && port <= 65535)
        {
            return true;
        }
        return false;
    }
}
