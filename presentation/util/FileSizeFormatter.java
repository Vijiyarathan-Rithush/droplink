package presentation.util;

public final class FileSizeFormatter
{
    private FileSizeFormatter()
    {
    }

    public static String format(long bytes)
    {
        if (bytes < 1_024) return bytes + " B";
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1_024 && unit < units.length - 1)
        {
            value /= 1_024;
            unit++;
        }
        return String.format("%.1f %s", value, units[unit]);
    }
}
