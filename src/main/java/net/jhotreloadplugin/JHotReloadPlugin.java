package net.jhotreloadplugin;

import net.jhotreloadplugin.ui.JHotReloadConfigWindow;
import net.jhotreloadplugin.installation.JHotReloadWorkspaceInstaller;
import net.jhotreloadplugin.ui.HotVariablesWindow;

import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.action.BasicAction;
import net.mcreator.ui.init.L10N;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class JHotReloadPlugin extends JavaPlugin
{
    private static final Logger LOG =
            LogManager.getLogger("JHotReload plugin");

    public JHotReloadPlugin(Plugin plugin)
    {
        super(plugin);

        System.out.println("[JHotReload] Plugin constructor executed");

        LOG.info("JHotReload plugin was loaded");

        addListener(MCreatorLoadedEvent.class, event ->
        {
            System.out.println("[JHotReload] MCreatorLoadedEvent received");

            LOG.info("MCreatorLoadedEvent received");

            SwingUtilities.invokeLater(() -> initializeWorkspaceWindow(event.getMCreator()));
        });
    }

    private void initializeWorkspaceWindow(MCreator mcreator)
    {
        registerUserInterface(mcreator);
        installLibrary(mcreator);
    }

    private void registerUserInterface(MCreator mcreator)
    {
        try
        {
            System.out.println("[JHotReload] Registering user interface");

            var openHotVariablesAction = new BasicAction
            (
                    mcreator.getActionRegistry(),
                    L10N.t("plugin.jhotreload.open_variables"),
                    actionEvent -> openHotVariablesWindow(mcreator)
            );

            var openConfigAction = new BasicAction(
                    mcreator.getActionRegistry(),
                    L10N.t("plugin.jhotreload.open_config"),
                    actionEvent -> openConfigWindow(mcreator)
            );

            var menu = new JMenu(L10N.t("plugin.jhotreload.menu"));

            menu.add(openHotVariablesAction);
            menu.addSeparator();
            menu.add(openConfigAction);

            var menuBar = mcreator.getMainMenuBar();

            menuBar.add(menu);
            menuBar.revalidate();
            menuBar.repaint();

            var toolbar = mcreator.getToolBar();

            var toolbarButton = toolbar.addToRightToolbar(openHotVariablesAction);

            var iconUrl = JHotReloadPlugin.class.getResource("/icons/jhotreload_variables.png");

            if (iconUrl == null)
            { throw new IllegalStateException("JHotReload toolbar icon was not found"); }

            toolbarButton.setIcon(new ImageIcon(iconUrl));
            toolbarButton.setText(null);

            toolbarButton.setToolTipText(L10N.t("plugin.jhotreload.open_variables"));

            toolbarButton.setToolTipText(L10N.t("plugin.jhotreload.open_variables"));

            toolbar.revalidate();
            toolbar.repaint();

            System.out.println("[JHotReload] User interface registered successfully");

            LOG.info
            (
                    "JHotReload UI registered. Button visible: {}, size: {}",
                    toolbarButton.isVisible(),
                    toolbarButton.getSize()
            );
        }
        catch (Throwable exception)
        {
            LOG.error(
                    "Failed to register JHotReload user interface",
                    exception
            );

            exception.printStackTrace();

            JOptionPane.showMessageDialog(
                    mcreator,
                    "Failed to register the JHotReload interface:\n"
                            + exception,
                    "JHotReload plugin error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void openHotVariablesWindow(MCreator mcreator)
    {
        try
        { new HotVariablesWindow(mcreator).open(); }

        catch (Throwable exception)
        {
            LOG.error
            (
                    "Failed to open the hot variables window",
                    exception
            );

            exception.printStackTrace();

            JOptionPane.showMessageDialog
            (
                    mcreator,
                    "Failed to open the hot variables window:\n"
                            + exception,
                    "JHotReload plugin error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void openConfigWindow(MCreator mcreator)
    {
        try
        {
            new JHotReloadConfigWindow(mcreator).open();
        }
        catch (Throwable exception)
        {
            LOG.error(
                    "Failed to open the JHotReload config window",
                    exception
            );

            JOptionPane.showMessageDialog(
                    mcreator,
                    "Failed to open the JHotReload config window:\n"
                            + exception,
                    "JHotReload plugin error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void installLibrary(MCreator mcreator)
    {
        try
        {
            var installedPath = JHotReloadWorkspaceInstaller.install(mcreator.getWorkspace());

            LOG.info
            (
                    "JHotReload installed in workspace: {}",
                    installedPath
            );

            System.out.println("[JHotReload] Library installed at: " + installedPath);
        }
        catch (Throwable exception)
        {
            LOG.error
            (
                    "Failed to install JHotReload into the workspace",
                    exception
            );

            exception.printStackTrace();

            JOptionPane.showMessageDialog
            (
                    mcreator,
                    "The interface was loaded, but the library "
                            + "could not be installed:\n"
                            + exception,
                    "JHotReload installation error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}