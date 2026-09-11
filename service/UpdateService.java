package service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateService
{
    private static final String RELEASES_API =
            "https://api.github.com/repos/Vijiyarathan-Rithush/droplink/releases";
    private static final String RELEASE_URL =
            "https://github.com/Vijiyarathan-Rithush/droplink/releases/tag/";
    private static final Pattern TAG_PATTERN = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"(v[^\\\"]+)\\\"");
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public Optional<UpdateInfo> findUpdate() throws IOException, InterruptedException
    {
        String currentText = System.getProperty("droplink.version", "dev");
        if ("dev".equalsIgnoreCase(currentText)) return Optional.empty();

        AppVersion current;
        try
        {
            current = AppVersion.parse(currentText);
        }
        catch (IllegalArgumentException ignored)
        {
            return Optional.empty();
        }

        boolean betaChannel = "beta".equalsIgnoreCase(
                System.getProperty("droplink.updateChannel", "stable"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        betaChannel ? RELEASES_API + "?per_page=20" : RELEASES_API + "/latest"))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DropLink-Update-Check")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return Optional.empty();

        Matcher tags = TAG_PATTERN.matcher(response.body());
        AppVersion best = current;
        String bestTag = null;
        while (tags.find())
        {
            String tag = tags.group(1);
            try
            {
                AppVersion candidate = AppVersion.parse(tag);
                if ((!candidate.isBeta() || betaChannel) && candidate.compareTo(best) > 0)
                {
                    best = candidate;
                    bestTag = tag;
                }
            }
            catch (IllegalArgumentException ignored)
            {
                // Ignore nightly and unrelated tags.
            }
        }
        return bestTag == null
                ? Optional.empty()
                : Optional.of(new UpdateInfo(bestTag.substring(1), RELEASE_URL + bestTag));
    }

    public record UpdateInfo(String version, String releaseUrl) {}
}
