package net.jhotreloadplugin.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.jhotreloadplugin.runtime.JHotReloadRuntimePaths;
import net.mcreator.ui.MCreator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public final class JHotReloadConfigWindow extends JDialog
{
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final int SAVE_DELAY_MILLISECONDS = 300;

    private final MCreator mcreator;
    private final JPanel fieldsPanel;
    private final JLabel statusLabel;

    private Path configPath;
    private JsonObject config;

    public JHotReloadConfigWindow(MCreator mcreator)
    {
        super(mcreator, "JHotReload Config", false);

        this.mcreator = mcreator;

        fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        12,
                        12,
                        12,
                        12
                )
        );

        statusLabel = new JLabel(" ");

        var refreshButton = new JButton("Refresh");

        refreshButton.addActionListener(event -> reloadFromDisk());

        var bottomPanel = new JPanel(new BorderLayout());

        var buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );

        buttonPanel.add(refreshButton);

        bottomPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        6,
                        10,
                        8,
                        10
                )
        );

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        setLayout(new BorderLayout());

        add(
                new JScrollPane(fieldsPanel),
                BorderLayout.CENTER
        );

        add(bottomPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(mcreator);
    }

    public void open()
    {
        reloadFromDisk();

        setVisible(true);
        toFront();
        requestFocus();
    }

    private void reloadFromDisk()
    {
        configPath = JHotReloadRuntimePaths.getConfigFile(
                mcreator.getWorkspace()
        );

        if (!Files.exists(configPath))
        {
            showMissingConfig();
            return;
        }

        try (var reader = Files.newBufferedReader(
                configPath,
                StandardCharsets.UTF_8
        ))
        {
            var rootElement = JsonParser.parseReader(reader);

            if (!rootElement.isJsonObject())
            {
                throw new IOException(
                        "The JHotReload config is not a JSON object."
                );
            }

            config = rootElement.getAsJsonObject();

            rebuildFields();

            statusLabel.setText(
                    "Loaded " + configPath
            );
        }
        catch (Exception exception)
        {
            config = null;

            showError(
                    "Failed to read the JHotReload config.",
                    exception
            );
        }
    }

    private void rebuildFields()
    {
        fieldsPanel.removeAll();

        int row = 0;

        for (var entry : config.entrySet())
        {
            var nameLabel = new JLabel(entry.getKey());

            var nameConstraints = createConstraints(row, 0);

            nameConstraints.weightx = 0;
            nameConstraints.fill = GridBagConstraints.NONE;
            nameConstraints.anchor = GridBagConstraints.WEST;

            fieldsPanel.add(nameLabel, nameConstraints);

            var editor = createEditor(
                    entry.getKey(),
                    entry.getValue()
            );

            var editorConstraints = createConstraints(row, 1);

            editorConstraints.weightx = 1;
            editorConstraints.fill = GridBagConstraints.HORIZONTAL;

            fieldsPanel.add(editor, editorConstraints);

            row++;
        }

        if (row == 0)
        {
            var emptyLabel = new JLabel(
                    "The config file contains no properties."
            );

            var constraints = createConstraints(0, 0);

            constraints.gridwidth = 2;
            constraints.weightx = 1;

            fieldsPanel.add(emptyLabel, constraints);
        }

        var fillerConstraints = createConstraints(row, 0);

        fillerConstraints.gridwidth = 2;
        fillerConstraints.weighty = 1;
        fillerConstraints.fill = GridBagConstraints.VERTICAL;

        fieldsPanel.add(new JPanel(), fillerConstraints);

        fieldsPanel.revalidate();
        fieldsPanel.repaint();
    }

    private Component createEditor(
            String propertyName,
            com.google.gson.JsonElement value
    )
    {
        if (!value.isJsonPrimitive())
        {
            var unsupportedLabel = new JLabel(
                    value + " (unsupported value type)"
            );

            unsupportedLabel.setEnabled(false);

            return unsupportedLabel;
        }

        var primitive = value.getAsJsonPrimitive();

        if (primitive.isBoolean())
        {
            return createBooleanEditor(
                    propertyName,
                    primitive.getAsBoolean()
            );
        }

        if (primitive.isNumber())
        {
            return createNumberEditor(
                    propertyName,
                    primitive.toString()
            );
        }

        return createStringEditor(
                propertyName,
                primitive.getAsString()
        );
    }

    private JCheckBox createBooleanEditor(
            String propertyName,
            boolean initialValue
    )
    {
        var checkBox = new JCheckBox();

        checkBox.setSelected(initialValue);

        checkBox.addActionListener(event ->
        {
            config.addProperty(
                    propertyName,
                    checkBox.isSelected()
            );

            saveConfig();
        });

        return checkBox;
    }

    private JTextField createNumberEditor(
            String propertyName,
            String initialValue
    )
    {
        var textField = new JTextField(initialValue);

        installDebouncedSaver(
                textField,
                text ->
                {
                    try
                    {
                        var number = new BigDecimal(text);

                        config.add(
                                propertyName,
                                new JsonPrimitive(number)
                        );

                        saveConfig();
                    }
                    catch (NumberFormatException exception)
                    {
                        statusLabel.setText(
                                "\"" + text
                                        + "\" is not a valid number; "
                                        + "the config was not changed."
                        );
                    }
                }
        );

        return textField;
    }

    private JTextField createStringEditor(
            String propertyName,
            String initialValue
    )
    {
        var textField = new JTextField(initialValue);

        installDebouncedSaver(
                textField,
                text ->
                {
                    config.addProperty(propertyName, text);
                    saveConfig();
                }
        );

        return textField;
    }

    private void installDebouncedSaver(
            JTextField textField,
            Consumer<String> saveAction
    )
    {
        var saveTimer = new Timer(
                SAVE_DELAY_MILLISECONDS,
                event -> saveAction.accept(textField.getText())
        );

        saveTimer.setRepeats(false);

        textField.getDocument().addDocumentListener(
                new DocumentListener()
                {
                    @Override
                    public void insertUpdate(DocumentEvent event)
                    {
                        saveTimer.restart();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent event)
                    {
                        saveTimer.restart();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent event)
                    {
                        saveTimer.restart();
                    }
                }
        );

        textField.addActionListener(event ->
        {
            saveTimer.stop();
            saveAction.accept(textField.getText());
        });

        textField.addFocusListener(
                new FocusAdapter()
                {
                    @Override
                    public void focusLost(FocusEvent event)
                    {
                        saveTimer.stop();
                        saveAction.accept(textField.getText());
                    }
                }
        );
    }

    private void saveConfig()
    {
        if (config == null || configPath == null)
        {
            return;
        }

        Path temporaryFile = null;

        try
        {
            var parent = configPath.getParent();

            if (parent == null)
            {
                throw new IOException(
                        "The config file has no parent directory."
                );
            }

            temporaryFile = Files.createTempFile(
                    parent,
                    configPath.getFileName().toString(),
                    ".tmp"
            );

            try (var writer = Files.newBufferedWriter(
                    temporaryFile,
                    StandardCharsets.UTF_8
            ))
            {
                GSON.toJson(config, writer);
            }

            try
            {
                Files.move(
                        temporaryFile,
                        configPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                Files.move(
                        temporaryFile,
                        configPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            statusLabel.setText("Config saved automatically.");
        }
        catch (Exception exception)
        {
            showError(
                    "Failed to save the JHotReload config.",
                    exception
            );
        }
        finally
        {
            if (temporaryFile != null)
            {
                try
                {
                    Files.deleteIfExists(temporaryFile);
                }
                catch (IOException ignored)
                {
                }
            }
        }
    }

    private void showMissingConfig()
    {
        config = null;

        fieldsPanel.removeAll();

        var label = new JLabel(
                "<html>"
                        + "The JHotReload config has not been generated yet."
                        + "<br>"
                        + "Start the Minecraft client at least once, "
                        + "then press Refresh."
                        + "</html>"
        );

        var constraints = createConstraints(0, 0);

        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.CENTER;

        fieldsPanel.add(label, constraints);

        fieldsPanel.revalidate();
        fieldsPanel.repaint();

        statusLabel.setText(
                "Config file not found: " + configPath
        );
    }

    private void showError(
            String message,
            Exception exception
    )
    {
        statusLabel.setText(message);

        JOptionPane.showMessageDialog(
                this,
                message + "\n" + exception.getMessage(),
                "JHotReload Config",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static GridBagConstraints createConstraints(
            int row,
            int column
    )
    {
        var constraints = new GridBagConstraints();

        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;

        return constraints;
    }
}