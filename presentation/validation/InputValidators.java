package presentation.validation;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class InputValidators
{
    private static final Pattern IPV4 = Pattern.compile(
            "^(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");

    private InputValidators()
    {
    }

    public static ValidationResult ipv4(String value)
    {
        String host = value == null ? "" : value.trim();
        if (host.isEmpty()) return ValidationResult.invalid("Bitte eine Empfänger-IP eingeben.");
        if (!IPV4.matcher(host).matches())
            return ValidationResult.invalid("Bitte eine gültige IPv4-Adresse eingeben, z. B. 192.168.1.24.");
        return ValidationResult.ok();
    }

    public static ValidationResult port(String value)
    {
        try
        {
            int port = Integer.parseInt(value == null ? "" : value.trim());
            return port >= 1 && port <= 65_535
                    ? ValidationResult.ok()
                    : ValidationResult.invalid("Der Port muss zwischen 1 und 65535 liegen.");
        }
        catch (NumberFormatException e)
        {
            return ValidationResult.invalid("Bitte eine gültige Portnummer eingeben.");
        }
    }

    public static ValidationResult existingFile(String value)
    {
        if (value == null || value.isBlank())
            return ValidationResult.invalid("Bitte eine Datei auswählen.");
        try
        {
            Path path = Path.of(value);
            return Files.isRegularFile(path) && Files.isReadable(path)
                    ? ValidationResult.ok()
                    : ValidationResult.invalid("Die ausgewählte Datei ist nicht lesbar oder existiert nicht mehr.");
        }
        catch (InvalidPathException e)
        {
            return ValidationResult.invalid("Der Dateipfad ist ungültig.");
        }
    }

    public static ValidationResult outputDirectory(String value)
    {
        if (value == null || value.isBlank())
            return ValidationResult.invalid("Bitte einen Zielordner auswählen.");
        try
        {
            Path path = Path.of(value).toAbsolutePath().normalize();
            if (Files.exists(path))
                return Files.isDirectory(path) && Files.isWritable(path)
                        ? ValidationResult.ok()
                        : ValidationResult.invalid("Der Zielordner ist nicht beschreibbar.");

            Path parent = path.getParent();
            return parent != null && Files.isDirectory(parent) && Files.isWritable(parent)
                    ? ValidationResult.ok()
                    : ValidationResult.invalid("Der Zielordner kann hier nicht erstellt werden.");
        }
        catch (InvalidPathException e)
        {
            return ValidationResult.invalid("Der Ordnerpfad ist ungültig.");
        }
    }
}
