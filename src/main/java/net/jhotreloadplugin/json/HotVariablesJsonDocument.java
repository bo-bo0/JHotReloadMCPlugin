package net.jhotreloadplugin.json;

import com.google.gson.*;

import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HotVariablesJsonDocument
{
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Path path;
    private final JsonObject resetSnapshot;

    private JsonObject root;

    private HotVariablesJsonDocument(Path path, JsonObject root)
    {
        this.path = path;
        this.root = root;
        this.resetSnapshot = root.deepCopy();
    }

    public static HotVariablesJsonDocument load(Path path) throws IOException
    {
        try (var reader = Files.newBufferedReader(
                path,
                StandardCharsets.UTF_8
        ))
        {
            var jsonElement = JsonParser.parseReader(reader);

            if (!jsonElement.isJsonObject())
            {
                throw new IOException(
                        "The JHotReload file does not contain a JSON object: "
                                + path
                );
            }

            var root = jsonElement.getAsJsonObject();

            for (var entry : root.entrySet())
            {
                if (!entry.getValue().isJsonPrimitive())
                {
                    throw new IOException(
                            "Variable \"" + entry.getKey()
                                    + "\" does not contain a primitive value"
                    );
                }
            }

            return new HotVariablesJsonDocument(path, root);
        }
    }

    public Path getPath()
    {
        return path;
    }

    public List<String> getVariableNames()
    {
        return Collections.unmodifiableList(
                new ArrayList<>(root.keySet())
        );
    }

    public String getDisplayValue(String variableName)
    {
        var value = getPrimitive(variableName);

        if (value.isString())
        {
            return value.getAsString();
        }

        return value.toString();
    }

    public void setValueFromText(
            String variableName,
            String newValue
    )
    {
        if (!root.has(variableName))
        {
            throw new IllegalArgumentException(
                    "Unknown JHotReload variable: " + variableName
            );
        }

        try
        {
            var parsedValue = JsonParser.parseString(newValue);

            if (parsedValue.isJsonPrimitive())
            {
                root.add(variableName, parsedValue);
            }
            else
            {
                root.addProperty(variableName, newValue);
            }
        }
        catch (JsonParseException exception)
        {
            root.addProperty(variableName, newValue);
        }
    }

    public void reset()
    {
        root = resetSnapshot.deepCopy();
    }

    public void save() throws IOException
    {
        var parent = path.getParent();

        if (parent == null)
        {
            throw new IOException(
                    "The JSON file has no parent directory: " + path
            );
        }

        var temporaryFile = Files.createTempFile(
                parent,
                path.getFileName().toString(),
                ".tmp"
        );

        try
        {
            try (var writer = Files.newBufferedWriter(
                    temporaryFile,
                    StandardCharsets.UTF_8
            ))
            {
                GSON.toJson(root, writer);
            }

            try
            {
                Files.move(
                        temporaryFile,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                Files.move(
                        temporaryFile,
                        path,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
        finally
        {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private JsonPrimitive getPrimitive(String variableName)
    {
        var value = root.get(variableName);

        if (value == null || !value.isJsonPrimitive())
        {
            throw new IllegalArgumentException(
                    "Unknown JHotReload variable: " + variableName
            );
        }

        return value.getAsJsonPrimitive();
    }
}