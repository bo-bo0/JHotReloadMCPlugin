package net.jhotreloadplugin;

import net.jhotreloadplugin.installation.JHotReloadWorkspaceInstaller;
import net.jhotreloadplugin.runtime.JHotReloadStateReader;
import net.jhotreloadplugin.ui.HotVariablesWindow;
import net.jhotreloadplugin.ui.JHotReloadConfigWindow;

import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.action.BasicAction;
import net.mcreator.ui.init.L10N;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;

public class JHotReloadPlugin extends JavaPlugin
{
    private static final Logger LOG =
            LogManager.getLogger("JHotReload plugin");

    private static final String TOOLBAR_ICON_PATH =
            "/icons/jhotreload_variables.png";

    private static final int ACTIVE_STATE_CHECK_INTERVAL = 1000;

    public JHotReloadPlugin(Plugin plugin)
    {
        super(plugin);

        addListener(
                MCreatorLoadedEvent.class,
                event -> SwingUtilities.invokeLater(
                        () -> initializeWorkspace(event.getMCreator())
                )
        );

        LOG.info("JHotReload plugin was loaded");
    }

    private void initializeWorkspace(MCreator mcreator)
    {
        installLibrary(mcreator);
        registerUserInterface(mcreator);
    }

    private void registerUserInterface(MCreator mcreator)
    {
        try
        {
            var openHotVariablesAction =
                    createOpenHotVariablesAction(mcreator);

            var openConfigAction =
                    createOpenConfigAction(mcreator);

            registerMenu(
                    mcreator,
                    openHotVariablesAction,
                    openConfigAction
            );

            registerToolbar(
                    mcreator,
                    openHotVariablesAction
            );

            LOG.info("JHotReload user interface registered");
        }
        catch (Exception exception)
        {
            showError(
                    mcreator,
                    "Failed to register the JHotReload interface",
                    "JHotReload plugin error",
                    exception
            );
        }
    }

    private BasicAction createOpenHotVariablesAction(MCreator mcreator)
    {
        return new BasicAction(
                mcreator.getActionRegistry(),
                L10N.t("plugin.jhotreload.open_variables"),
                event -> openHotVariablesWindow(mcreator)
        );
    }

    private BasicAction createOpenConfigAction(MCreator mcreator)
    {
        return new BasicAction(
                mcreator.getActionRegistry(),
                L10N.t("plugin.jhotreload.open_config"),
                event -> openConfigWindow(mcreator)
        );
    }

    private void registerMenu(
            MCreator mcreator,
            BasicAction openHotVariablesAction,
            BasicAction openConfigAction
    )
    {
        var menu = new JMenu(
                L10N.t("plugin.jhotreload.menu")
        );

        menu.add(openHotVariablesAction);
        menu.addSeparator();
        menu.add(openConfigAction);

        var menuBar = mcreator.getMainMenuBar();

        menuBar.add(menu);
        menuBar.revalidate();
        menuBar.repaint();
    }

    private void registerToolbar(
            MCreator mcreator,
            BasicAction openHotVariablesAction
    )
    {
        var toolbar = mcreator.getToolBar();

        var hotVariablesButton =
                toolbar.addToRightToolbar(
                        openHotVariablesAction
                );

        configureHotVariablesButton(
                hotVariablesButton
        );

        registerActiveIndicator(
                mcreator,
                hotVariablesButton.getParent()
        );

        toolbar.revalidate();
        toolbar.repaint();
    }

    private void configureHotVariablesButton(
            javax.swing.JButton button
    )
    {
        button.setIcon(loadToolbarIcon());
        button.setText(null);

        button.setToolTipText(
                L10N.t("plugin.jhotreload.open_variables")
        );
    }

    private ImageIcon loadToolbarIcon()
    {
        var iconUrl = JHotReloadPlugin.class.getResource(
                TOOLBAR_ICON_PATH
        );

        if (iconUrl == null)
        {
            throw new IllegalStateException(
                    "JHotReload toolbar icon was not found: "
                            + TOOLBAR_ICON_PATH
            );
        }

        return new ImageIcon(iconUrl);
    }

    private void registerActiveIndicator(
            MCreator mcreator,
            java.awt.Container rightToolbar
    )
    {
        var activeLabel = createActiveLabel();

        var activeIndicator = Box.createHorizontalBox();

        activeIndicator.add(activeLabel);
        activeIndicator.add(
                Box.createHorizontalStrut(10)
        );

        rightToolbar.add(activeIndicator, 0);

        updateActiveIndicator(
                mcreator,
                activeIndicator
        );

        startActiveStateTimer(
                mcreator,
                activeIndicator
        );

        rightToolbar.revalidate();
        rightToolbar.repaint();
    }

    private JLabel createActiveLabel()
    {
        var label = new JLabel(
                "⚠ JHotReload ACTIVE"
        );

        label.setForeground(
                new Color(255, 140, 0)
        );

        label.setFont(
                label.getFont().deriveFont(Font.BOLD)
        );

        label.setToolTipText(
                "JHotReload is currently active. "
                        + "Disable it when exporting the mod or "
                        + "to prevent value sharing between "
                        + "different instances."
        );

        return label;
    }

    private void startActiveStateTimer(
            MCreator mcreator,
            Box activeIndicator
    )
    {
        var timer = new Timer(
                ACTIVE_STATE_CHECK_INTERVAL,
                event -> updateActiveIndicator(
                        mcreator,
                        activeIndicator
                )
        );

        timer.start();
    }

    private void updateActiveIndicator(
            MCreator mcreator,
            Box activeIndicator
    )
    {
        boolean active = JHotReloadStateReader.isActive(
                mcreator.getWorkspace()
        );

        if (activeIndicator.isVisible() == active)
        {
            return;
        }

        activeIndicator.setVisible(active);

        var parent = activeIndicator.getParent();

        if (parent != null)
        {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void openHotVariablesWindow(MCreator mcreator)
    {
        try
        {
            new HotVariablesWindow(mcreator).open();
        }
        catch (Exception exception)
        {
            showError(
                    mcreator,
                    "Failed to open the hot variables window",
                    "JHotReload plugin error",
                    exception
            );
        }
    }

    private void openConfigWindow(MCreator mcreator)
    {
        try
        {
            new JHotReloadConfigWindow(mcreator).open();
        }
        catch (Exception exception)
        {
            showError(
                    mcreator,
                    "Failed to open the JHotReload config window",
                    "JHotReload plugin error",
                    exception
            );
        }
    }

    private void installLibrary(MCreator mcreator)
    {
        try
        {
            var installedPath =
                    JHotReloadWorkspaceInstaller.install(
                            mcreator.getWorkspace()
                    );

            LOG.info(
                    "JHotReload installed in workspace: {}",
                    installedPath
            );
        }
        catch (Exception exception)
        {
            showError(
                    mcreator,
                    "The interface was loaded, but the library "
                            + "could not be installed",
                    "JHotReload installation error",
                    exception
            );
        }
    }

    private void showError(
            MCreator mcreator,
            String message,
            String title,
            Exception exception
    )
    {
        LOG.error(message, exception);

        JOptionPane.showMessageDialog(
                mcreator,
                message + ":\n" + exception,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }
}