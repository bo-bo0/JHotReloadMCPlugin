package net.jhotreloadplugin.ui;

import net.jhotreloadplugin.runtime.JHotReloadRuntimePaths;
import net.mcreator.ui.MCreator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class HotVariablesWindow extends JDialog
{
    private final MCreator mcreator;
    private final JTree fileTree;
    private final HotVariablesEditorPanel editorPanel;

    public HotVariablesWindow(MCreator mcreator)
    {
        super(mcreator, "JHotReload Variables", false);

        this.mcreator = mcreator;

        fileTree = new JTree();
        editorPanel = new HotVariablesEditorPanel();

        fileTree.addTreeSelectionListener(event ->
        {
            var selectedNode =
                    (DefaultMutableTreeNode)fileTree
                            .getLastSelectedPathComponent();

            if (selectedNode == null)
            {
                return;
            }

            if (!(selectedNode.getUserObject()
                    instanceof RuntimePathNode runtimePathNode))
            {
                return;
            }

            var path = runtimePathNode.path();

            if (Files.isRegularFile(path)
                    && JHotReloadRuntimePaths.isEditableJson(path))
            {
                editorPanel.load(path);
            }
        });

        var splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(fileTree),
                editorPanel
        );

        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.3);

        var refreshButton = new JButton("Refresh");

        refreshButton.addActionListener(event -> refresh());

        var topPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        topPanel.add(refreshButton);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(mcreator);

        refresh();
    }

    public void open()
    {
        refresh();

        setVisible(true);
        toFront();
        requestFocus();
    }

    public void refresh()
    {
        var rootPath = JHotReloadRuntimePaths.getRoot(
                mcreator.getWorkspace()
        );

        try
        {
            var rootNode = buildTree(rootPath);

            fileTree.setModel(
                    new DefaultTreeModel(rootNode)
            );

            fileTree.setRootVisible(true);

            expandRoot();
        }
        catch (IOException exception)
        {
            JOptionPane.showMessageDialog(
                    this,
                    exception.getMessage(),
                    "Failed to scan JHotReload files",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private DefaultMutableTreeNode buildTree(Path root)
            throws IOException
    {
        var rootNode = new DefaultMutableTreeNode(
                new RuntimePathNode(root, "JHotReload")
        );

        if (!Files.exists(root))
        {
            return rootNode;
        }

        addChildren(rootNode, root);

        return rootNode;
    }

    private void addChildren(
            DefaultMutableTreeNode parentNode,
            Path directory
    ) throws IOException
    {
        try (var children = Files.list(directory))
        {
            var sortedChildren = children
                    .filter(path ->
                            Files.isDirectory(path)
                                    || JHotReloadRuntimePaths
                                    .isEditableJson(path)
                    )
                    .sorted(
                            Comparator
                                    .comparing(
                                            (Path path) ->
                                                    !Files.isDirectory(path)
                                    )
                                    .thenComparing(
                                            path -> path
                                                    .getFileName()
                                                    .toString()
                                                    .toLowerCase()
                                    )
                    )
                    .toList();

            for (var child : sortedChildren)
            {
                var node = new DefaultMutableTreeNode(
                        new RuntimePathNode(
                                child,
                                getDisplayName(child)
                        )
                );

                parentNode.add(node);

                if (Files.isDirectory(child))
                {
                    addChildren(node, child);
                }
            }
        }
    }

    private static String getDisplayName(Path path)
    {
        var name = path.getFileName().toString();

        if (name.endsWith(".json"))
        {
            return name.substring(
                    0,
                    name.length() - ".json".length()
            );
        }

        return name;
    }

    private void expandRoot()
    {
        SwingUtilities.invokeLater(() ->
        {
            var root = fileTree.getModel().getRoot();

            if (root != null)
            {
                fileTree.expandPath(
                        new TreePath(root)
                );
            }
        });
    }

    private record RuntimePathNode(
            Path path,
            String displayName
    )
    {
        @Override
        public String toString()
        {
            return displayName;
        }
    }
}