package net.jhotreloadplugin.ui;

import net.jhotreloadplugin.json.HotVariablesJsonDocument;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;

public final class HotVariablesEditorPanel extends JPanel
{
    private final JLabel fileLabel;
    private final JTable table;
    private final JButton resetButton;

    private Path selectedFile;
    private HotVariablesJsonDocument document;

    public HotVariablesEditorPanel()
    {
        super(new BorderLayout(8, 8));

        setBorder(
                BorderFactory.createEmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        fileLabel = new JLabel(
                "Select a mod element that contains Hot Variables"
        );

        table = new JTable();
        table.setFillsViewportHeight(true);

        table.putClientProperty(
                "terminateEditOnFocusLost",
                Boolean.TRUE
        );

        resetButton = new JButton("Reset");
        resetButton.setEnabled(false);
        resetButton.addActionListener(event -> reset());

        var statusLabel = new JLabel(
                "Changes are saved automatically"
        );

        var buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );

        buttonPanel.add(resetButton);

        var bottomPanel = new JPanel(
                new BorderLayout()
        );

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(fileLabel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void load(Path path)
    {
        selectedFile = path;

        try
        {
            document = HotVariablesJsonDocument.load(path);

            rebuildTable();

            fileLabel.setText(path.toString());
            resetButton.setEnabled(true);
        }
        catch (IOException exception)
        {
            document = null;

            resetButton.setEnabled(false);

            JOptionPane.showMessageDialog(
                    this,
                    exception.getMessage(),
                    "Failed to load JHotReload file",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void clear()
    {
        if (table.isEditing())
        {
            table.getCellEditor().cancelCellEditing();
        }

        selectedFile = null;
        document = null;

        table.setModel(new DefaultTableModel());

        fileLabel.setText(
                "Select a mod element that contains Hot Variables"
        );

        resetButton.setEnabled(false);
    }

    private void rebuildTable()
    {
        var model = new HotVariablesTableModel(
                document,
                this::showSaveError
        );

        table.setModel(model);

        table.getColumnModel()
                .getColumn(1)
                .setCellEditor(
                        new AutoSaveCellEditor(table)
                );
    }

    private void reset()
    {
        if (document == null)
        {
            return;
        }

        if (table.isEditing())
        {
            table.getCellEditor().cancelCellEditing();
        }

        try
        {
            document.reset();
            document.save();

            rebuildTable();
        }
        catch (IOException exception)
        {
            showSaveError(exception);
        }
    }

    private void showSaveError(Exception exception)
    {
        JOptionPane.showMessageDialog(
                this,
                exception.getMessage(),
                "Failed to update JHotReload file",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static final class AutoSaveCellEditor
            extends DefaultCellEditor
    {
        private final JTable table;
        private final JTextField textField;

        private int modelRow = -1;
        private boolean initializing;

        private AutoSaveCellEditor(JTable table)
        {
            super(new JTextField());

            this.table = table;
            textField = (JTextField)getComponent();

            textField.getDocument()
                    .addDocumentListener(
                            new DocumentListener()
                            {
                                @Override
                                public void insertUpdate(
                                        DocumentEvent event
                                )
                                {
                                    saveCurrentValue();
                                }

                                @Override
                                public void removeUpdate(
                                        DocumentEvent event
                                )
                                {
                                    saveCurrentValue();
                                }

                                @Override
                                public void changedUpdate(
                                        DocumentEvent event
                                )
                                {
                                    saveCurrentValue();
                                }
                            }
                    );
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column
        )
        {
            initializing = true;

            var component =
                    super.getTableCellEditorComponent(
                            table,
                            value,
                            isSelected,
                            row,
                            column
                    );

            modelRow = table.convertRowIndexToModel(row);
            initializing = false;

            return component;
        }

        @Override
        public boolean stopCellEditing()
        {
            var stopped = super.stopCellEditing();

            modelRow = -1;

            return stopped;
        }

        @Override
        public void cancelCellEditing()
        {
            super.cancelCellEditing();

            modelRow = -1;
        }

        private void saveCurrentValue()
        {
            if (initializing || modelRow < 0)
            {
                return;
            }

            if (!(table.getModel()
                    instanceof HotVariablesTableModel model))
            {
                return;
            }

            model.updateValueAt(
                    modelRow,
                    textField.getText()
            );
        }
    }
}