package net.jhotreloadplugin;

import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.mcreator.ui.action.BasicAction;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.workspace.Workspace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

public class JHotReloadPlugin extends JavaPlugin
{
    private static final Logger LOG =
            LogManager.getLogger("JHotReload Plugin");

    public JHotReloadPlugin(Plugin plugin)
    {
        super(plugin);

        addListener(MCreatorLoadedEvent.class, event ->
                SwingUtilities.invokeLater(() ->
                {
                    var workspaceReportAction = new BasicAction(
                            event.getMCreator().getActionRegistry(),
                            L10N.t("plugin.jhotreloadplugin.menu.button"),
                            actionEvent -> showWorkspaceReport(
                                    event.getMCreator().getWorkspace(),
                                    event.getMCreator()
                            )
                    );

                    workspaceReportAction.setIcon(UIRES.get("16px.play"));

                    var menu = new JMenu(
                            L10N.t("plugin.jhotreloadplugin.menu.main")
                    );

                    menu.add(workspaceReportAction);

                    event.getMCreator()
                            .getMainMenuBar()
                            .add(menu);

                    event.getMCreator()
                            .getToolBar()
                            .addToRightToolbar(workspaceReportAction);
                })
        );

        LOG.info("JHotReload Plugin was loaded");
    }

    private static void showWorkspaceReport(
            Workspace workspace,
            java.awt.Component parent
    )
    {
        var report = new StringBuilder();

        report.append("MCreator workspace report")
                .append(System.lineSeparator())
                .append("=========================")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        report.append("MCreator version: ")
                .append(workspace.getMCreatorVersion())
                .append(System.lineSeparator());

        report.append("Mod elements: ")
                .append(workspace.getModElements().size())
                .append(System.lineSeparator());

        report.append("Variables: ")
                .append(workspace.getVariableElements().size())
                .append(System.lineSeparator());

        report.append("Sounds: ")
                .append(workspace.getSoundElements().size())
                .append(System.lineSeparator());

        report.append("Tags: ")
                .append(workspace.getTagElements().size())
                .append(System.lineSeparator());

        report.append(System.lineSeparator())
                .append("Mod elements")
                .append(System.lineSeparator())
                .append("------------")
                .append(System.lineSeparator());

        if (workspace.getModElements().isEmpty())
        {
            report.append("No mod elements found.")
                    .append(System.lineSeparator());
        }
        else
        {
            for (var modElement : workspace.getModElements())
            {
                report.append("- ")
                        .append(modElement.getName())
                        .append(" [")
                        .append(modElement.getTypeString())
                        .append(']')
                        .append(System.lineSeparator());
            }
        }

        var textArea = new JTextArea(report.toString());

        textArea.setEditable(false);
        textArea.setCaretPosition(0);

        var scrollPane = new JScrollPane(textArea);

        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(
                parent,
                scrollPane,
                "Workspace Report",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}