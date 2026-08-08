package net.jhotreloadplugin.installation;

import net.mcreator.workspace.Workspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JHotReloadWorkspaceInstaller
{
    private static final String RESOURCE_PATH =
            "/bundled-libs/JHotReload.jar";

    private static final String PLUGIN_DIRECTORY_NAME =
            ".jhotreload";

    private static final String LIBRARY_FILE_NAME =
            "JHotReload.jar";

    private JHotReloadWorkspaceInstaller() {}

    public static Path install(Workspace workspace)
    {
        var targetDirectory = workspace
                .getWorkspaceFolder()
                .toPath()
                .resolve(PLUGIN_DIRECTORY_NAME);

        var targetPath =
                targetDirectory.resolve(LIBRARY_FILE_NAME);

        try
        {
            Files.createDirectories(targetDirectory);

            try (var inputStream =
                         JHotReloadWorkspaceInstaller.class
                                 .getResourceAsStream(RESOURCE_PATH))
            {
                if (inputStream == null)
                {
                    throw new IllegalStateException(
                            "Bundled JHotReload library not found"
                    );
                }

                Files.copy(
                        inputStream,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return targetPath;
        }
        catch (IOException exception)
        {
            throw new UncheckedIOException(
                    "Failed to install JHotReload into workspace",
                    exception
            );
        }
    }
}