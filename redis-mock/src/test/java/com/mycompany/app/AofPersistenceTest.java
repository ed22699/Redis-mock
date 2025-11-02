package com.mycompany.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the AofPersistence class.
 * This class tests the file I/O logic for the Append-Only File persistence.
 */
public class AofPersistenceTest {

    @TempDir
    Path tempDir; // JUnit 5 will create and clean up this temporary directory

    private Path aofFile;
    private AofPersistence aofPersistence;

    @BeforeEach
    void setUp() throws IOException {
        aofFile = tempDir.resolve("test-redis.aof");
        // In a real scenario, the AofPersistence might need the RESPParser.
        // For this test, we assume the persistence class can encode/decode or uses a utility.
        aofPersistence = new AofPersistence(aofFile.toString());
    }

    @Test
    @DisplayName("logCommand should write a single command to the file")
    public void testLogSingleCommand() throws IOException {
        Object[] command = {"SET", "key1", "value1"};

        aofPersistence.logCommand(command);

        String fileContent = Files.readString(aofFile);
        String expectedContent = "*3\r\n$3\r\nSET\r\n$4\r\nkey1\r\n$6\r\nvalue1\r\n";
        assertEquals(expectedContent, fileContent);
    }

    @Test
    @DisplayName("logCommand should append multiple commands to the file")
    public void testLogMultipleCommands() throws IOException {
        Object[] command1 = {"SET", "key1", "value1"};
        Object[] command2 = {"GET", "key1"};

        aofPersistence.logCommand(command1);
        aofPersistence.logCommand(command2);

        String fileContent = Files.readString(aofFile);
        String expectedContent = "*3\r\n$3\r\nSET\r\n$4\r\nkey1\r\n$6\r\nvalue1\r\n" +
                               "*2\r\n$3\r\nGET\r\n$4\r\nkey1\r\n";
        assertEquals(expectedContent, fileContent);
    }

    @Test
    @DisplayName("loadData should read all commands from a file")
    public void testLoadData() throws IOException {
        String fileContent = "*3\r\n$3\r\nSET\r\n$4\r\nkey1\r\n$6\r\nvalue1\r\n" +
                             "*2\r\n$3\r\nGET\r\n$4\r\nkey1\r\n";
        Files.writeString(aofFile, fileContent);

        List<Object[]> commands = aofPersistence.loadData();

        assertNotNull(commands);
        assertEquals(2, commands.size());
        assertArrayEquals(new Object[]{"SET", "key1", "value1"}, commands.get(0));
        assertArrayEquals(new Object[]{"GET", "key1"}, commands.get(1));
    }

    @Test
    @DisplayName("loadData on an empty file should return an empty list")
    public void testLoadDataOnEmptyFile() throws IOException {
        // File is already empty, just call load
        List<Object[]> commands = aofPersistence.loadData();

        assertNotNull(commands);
        assertTrue(commands.isEmpty());
    }

    @Test
    @DisplayName("loadData on a non-existent file should return an empty list")
    public void testLoadDataOnNonExistentFile() throws IOException {
        Files.deleteIfExists(aofFile);

        List<Object[]> commands = aofPersistence.loadData();

        assertNotNull(commands);
        assertTrue(commands.isEmpty());
    }
}
