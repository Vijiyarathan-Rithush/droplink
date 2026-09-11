package service;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AppVersion(int major, int minor, int patch, int beta) implements Comparable<AppVersion>
{
    private static final Pattern VERSION = Pattern.compile(
            "^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-beta\\.(\\d+))?$");

    public static AppVersion parse(String value)
    {
        Objects.requireNonNull(value, "value cannot be null");
        Matcher matcher = VERSION.matcher(value.trim());
        if (!matcher.matches()) throw new IllegalArgumentException("Unsupported version: " + value);
        int beta = matcher.group(4) == null ? Integer.MAX_VALUE : Integer.parseInt(matcher.group(4));
        return new AppVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                beta);
    }

    public boolean isBeta()
    {
        return beta != Integer.MAX_VALUE;
    }

    @Override
    public int compareTo(AppVersion other)
    {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        if (result == 0) result = Integer.compare(beta, other.beta);
        return result;
    }
}
