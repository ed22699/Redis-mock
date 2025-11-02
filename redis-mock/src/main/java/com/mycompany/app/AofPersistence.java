package com.mycompany.app;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AofPersistence {

    private final String aofFile;

    /**
     * Creates a log of all the set calls to the database
     * @param aofFile The file to be read and written to
     */
    public AofPersistence(String aofFile) {
        this.aofFile = aofFile;
    }

    /**
     * Writes the command to the log
     * @param command the set command that was called
     */
    public synchronized void logCommand(Object[] command) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(aofFile, true))) {
            writer.write(RESPParser.encode(command));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the data from the command log
     * @return A list of commands to load into the CommandExecutor
     */
    public List<Object[]> loadData() {
        List<Object[]> commands = new ArrayList<>();
        Path path = Paths.get(aofFile);
        if (!Files.exists(path)) {
            return commands;
        }

        try (InputStream is = new FileInputStream(aofFile)) {
            while (is.available() > 0) {
                Object decoded = RESPParser.decode(is);
                if (decoded instanceof Object[]) {
                    commands.add((Object[]) decoded);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return commands;
    }
}