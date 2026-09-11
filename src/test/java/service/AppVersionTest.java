package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppVersionTest
{
    @Test
    void stableVersionIsNewerThanItsBeta()
    {
        assertTrue(AppVersion.parse("v1.3.0").compareTo(AppVersion.parse("v1.3.0-beta.2")) > 0);
    }

    @Test
    void laterPatchIsNewer()
    {
        assertTrue(AppVersion.parse("1.2.4").compareTo(AppVersion.parse("1.2.3")) > 0);
    }

    @Test
    void detectsChannelsAndRejectsInvalidVersions()
    {
        assertTrue(AppVersion.parse("v2.0.0-beta.1").isBeta());
        assertFalse(AppVersion.parse("v2.0.0").isBeta());
        assertThrows(IllegalArgumentException.class, () -> AppVersion.parse("nightly"));
    }
}
