package net.jhotreloadplugin;

import net.jhotreloadplugin.installation.JHotReloadWorkspaceInstaller;
import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.SwingUtilities;


public class JHotReloadPlugin extends JavaPlugin
{
    private static final Logger LOG = LogManager.getLogger("JHotReload plugin");

    public JHotReloadPlugin(Plugin plugin)
    {
        super(plugin);

        addListener(MCreatorLoadedEvent.class, event ->
                SwingUtilities.invokeLater(() ->
                {
                    var workspace = event.getMCreator().getWorkspace();
                    var installedPath = JHotReloadWorkspaceInstaller.install(workspace);
                    LOG.info("JHotReload installed in workspace: {}", installedPath);
                })
        );

        LOG.info("JHotReload plugin was loaded");
    }
}