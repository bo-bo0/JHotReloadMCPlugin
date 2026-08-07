package net.jhotreloadplugin.ui;

import net.jhotreloadplugin.json.HotVariablesJsonDocument;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class HotVariablesTableModel extends AbstractTableModel
{
    private static final int NAME_COLUMN = 0;
    private static final int VALUE_COLUMN = 1;

    private final HotVariablesJsonDocument document;
    private final List<String> variableNames;
    private final List<String> editedValues;
    private final Consumer<Exception> saveErrorHandler;

    public HotVariablesTableModel(
            HotVariablesJsonDocument document,
            Consumer<Exception> saveErrorHandler
    )
    {
        this.document = document;
        this.saveErrorHandler = saveErrorHandler;

        variableNames = document.getVariableNames();
        editedValues = new ArrayList<>();

        for (var variableName : variableNames)
        {
            editedValues.add(
                    document.getDisplayValue(variableName)
            );
        }
    }

    @Override
    public int getRowCount()
    {
        return variableNames.size();
    }

    @Override
    public int getColumnCount()
    {
        return 2;
    }

    @Override
    public String getColumnName(int column)
    {
        return switch (column)
        {
            case NAME_COLUMN -> "Variable";
            case VALUE_COLUMN -> "Value";
            default -> throw new IllegalArgumentException(
                    "Invalid column: " + column
            );
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return switch (columnIndex)
        {
            case NAME_COLUMN -> variableNames.get(rowIndex);
            case VALUE_COLUMN -> editedValues.get(rowIndex);
            default -> throw new IllegalArgumentException(
                    "Invalid column: " + columnIndex
            );
        };
    }

    @Override
    public boolean isCellEditable(
            int rowIndex,
            int columnIndex
    )
    {
        return columnIndex == VALUE_COLUMN;
    }

    @Override
    public void setValueAt(
            Object value,
            int rowIndex,
            int columnIndex
    )
    {
        if (columnIndex != VALUE_COLUMN)
        {
            return;
        }

        updateValueAt(
                rowIndex,
                value != null ? value.toString() : ""
        );
    }

    public void updateValueAt(
            int rowIndex,
            String newValue
    )
    {
        if (Objects.equals(
                editedValues.get(rowIndex),
                newValue
        ))
        {
            return;
        }

        editedValues.set(rowIndex, newValue);
        fireTableCellUpdated(rowIndex, VALUE_COLUMN);

        try
        {
            document.setValueFromText(
                    variableNames.get(rowIndex),
                    newValue
            );

            document.save();
        }
        catch (Exception exception)
        {
            saveErrorHandler.accept(exception);
        }
    }
}