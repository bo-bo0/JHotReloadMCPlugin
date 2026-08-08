package net.jhotreloadplugin.runtime;

import com.google.gson.JsonParser;
import net.mcreator.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JHotReloadStateReader
{
    private static final String ACTIVE_PROPERTY =
            "isJHotReloadActive";

    private JHotReloadStateReader() {}

    public static boolean isActive(Workspace workspace)
    {
        var configPath = getConfigPath(workspace);

        if (!Files.exists(configPath))
        {
            return false;
        }

        try (var reader = Files.newBufferedReader(configPath))
        {
            var root = JsonParser
                    .parseReader(reader)
                    .getAsJsonObject();

            var activeElement = root.get(ACTIVE_PROPERTY);

            if (activeElement == null)
            {
                return false;
            }

            return activeElement.getAsBoolean();
        }
        catch (IOException | RuntimeException exception)
        {
            return false;
        }
    }

    private static Path getConfigPath(Workspace workspace)
    {
        return workspace
                .getWorkspaceFolder()
                .toPath()
                .resolve("run")
                .resolve("JHotReload")
                .resolve("@CONFIGJHotReload.json");
    }
}