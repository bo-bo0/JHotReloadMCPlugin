package net.jhotreloadplugin.runtime;

import net.mcreator.workspace.Workspace;

import java.nio.file.Path;

public final class JHotReloadRuntimePaths
{
    public static final String ROOT_FOLDER_NAME = "JHotReload";
    public static final String CONFIG_FILE_NAME = "@CONFIGJHotReload.json";

    private JHotReloadRuntimePaths()
    {
    }

    public static Path getConfigFile(Workspace workspace)
    {
        return getRoot(workspace).resolve(CONFIG_FILE_NAME);
    }

    public static Path getRoot(Workspace workspace)
    {
        return workspace
                .getWorkspaceFolder()
                .toPath()
                .resolve("run")
                .resolve(ROOT_FOLDER_NAME);
    }

    public static boolean isEditableJson(Path path)
    {
        if (!path.getFileName().toString().endsWith(".json"))
        {
            return false;
        }

        return !path.getFileName()
                .toString()
                .equals(CONFIG_FILE_NAME);
    }
}