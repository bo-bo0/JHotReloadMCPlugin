package net.jhotreloadplugin.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mcreator.workspace.Workspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JHotReloadPluginSettings
{
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String FILE_NAME =
            ".jhotreload-plugin.json";

    private boolean keepActiveInExportedMods;

    private JHotReloadPluginSettings() {}

    public static JHotReloadPluginSettings load(
            Workspace workspace
    )
    {
        var path = getPath(workspace);

        if (!Files.exists(path))
        {
            return new JHotReloadPluginSettings();
        }

        try (var reader = Files.newBufferedReader(path))
        {
            var settings = GSON.fromJson(
                    reader,
                    JHotReloadPluginSettings.class
            );

            return settings != null
                    ? settings
                    : new JHotReloadPluginSettings();
        }
        catch (IOException exception)
        {
            throw new UncheckedIOException(
                    "Failed to read JHotReload plugin settings",
                    exception
            );
        }
    }

    public void save(Workspace workspace)
    {
        var path = getPath(workspace);

        try (var writer = Files.newBufferedWriter(path))
        {
            GSON.toJson(this, writer);
        }
        catch (IOException exception)
        {
            throw new UncheckedIOException(
                    "Failed to save JHotReload plugin settings",
                    exception
            );
        }
    }

    public boolean keepActiveInExportedMods()
    {
        return keepActiveInExportedMods;
    }

    public void setKeepActiveInExportedMods(boolean value)
    {
        keepActiveInExportedMods = value;
    }

    private static Path getPath(Workspace workspace)
    {
        return workspace
                .getWorkspaceFolder()
                .toPath()
                .resolve(FILE_NAME);
    }
}