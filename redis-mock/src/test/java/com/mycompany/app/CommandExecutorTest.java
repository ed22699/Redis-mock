package com.mycompany.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CommandExecutor.
 * This class now uses a real ConcurrentHashMap to be more of an integration test,
 * while still mocking the persistence layer to avoid file I/O.
 */
@ExtendWith(MockitoExtension.class)
public class CommandExecutorTest {

    // Use a real data store object
    private ConcurrentHashMap<String, Object> realDataStore;

    @Mock
    private AofPersistence mockAofPersistence;

    private CommandExecutor commandExecutor;

    @BeforeEach
    void setUp() {
        // Instantiate the real map for each test and inject it with the mock persistence
        realDataStore = new ConcurrentHashMap<>();
        commandExecutor = new CommandExecutor(realDataStore, mockAofPersistence);
    }

    @Test
    @DisplayName("Execute SET should store value in the map and log command")
    public void testExecuteSetCommand() {
        Object[] command = {"SET", "key", "value"};

        Object result = commandExecutor.execute(command);

        // Assert the state of the real map
        assertEquals("value", realDataStore.get("key"));
        // Verify the interaction with the mock persistence layer
        verify(mockAofPersistence, times(1)).logCommand(command);
        assertEquals("OK", result);
    }

    @Test
    @DisplayName("Execute GET should retrieve value from the map and not log")
    public void testExecuteGetCommand() {
        // Arrange: Set up the real map directly
        realDataStore.put("key", "value");
        Object[] command = {"GET", "key"};

        Object result = commandExecutor.execute(command);

        // Assert the result and verify no interaction with persistence
        assertEquals("value", result);
        verifyNoInteractions(mockAofPersistence);
    }

    @Test
    @DisplayName("Execute GET for non-existent key should return null")
    public void testExecuteGetCommandOnNonExistentKey() {
        Object[] command = {"GET", "key"};
        // No setup needed, the real map is empty

        Object result = commandExecutor.execute(command);

        assertEquals(null, result);
        verifyNoInteractions(mockAofPersistence);
    }

    @Test
    @DisplayName("Execute PING should return PONG and not touch dependencies")
    public void testExecutePingCommand() {
        Object[] command = {"PING"};

        Object result = commandExecutor.execute(command);

        assertEquals("PONG", result);
        assertTrue(realDataStore.isEmpty(), "Data store should not be modified by PING");
        verifyNoInteractions(mockAofPersistence);
    }

    @Test
    @DisplayName("Execute PING with argument should return argument")
    public void testExecutePingWithArgument() {
        Object[] command = {"PING", "hello"};

        Object result = commandExecutor.execute(command);

        assertEquals("hello", result);
        assertTrue(realDataStore.isEmpty(), "Data store should not be modified by PING");
        verifyNoInteractions(mockAofPersistence);
    }

    @Test
    @DisplayName("Execute unknown command should return an error")
    public void testExecuteUnknownCommand() {
        Object[] command = {"UNKNOWN_COMMAND", "arg1"};

        Object result = commandExecutor.execute(command);

        assertTrue(result instanceof Exception, "Result should be an Exception for unknown commands");
        assertEquals("ERR unknown command 'UNKNOWN_COMMAND'", ((Exception) result).getMessage());
        assertTrue(realDataStore.isEmpty());
        verifyNoInteractions(mockAofPersistence);
    }

    @Test
    @DisplayName("Execute command with wrong number of arguments should return an error")
    public void testExecuteWrongNumberOfArguments() {
        Object[] command = {"GET"}; // GET requires one argument

        Object result = commandExecutor.execute(command);

        assertTrue(result instanceof Exception, "Result should be an Exception for wrong argument count");
        assertEquals("ERR wrong number of arguments for 'GET' command", ((Exception) result).getMessage());
        assertTrue(realDataStore.isEmpty());
        verifyNoInteractions(mockAofPersistence);
    }
}
