package com.mycompany.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ClientHandler.
 * This class tests the client handler logic in isolation by mocking its dependencies.
 */
@ExtendWith(MockitoExtension.class)
public class ClientHandlerTest {

    private ByteArrayInputStream inputStream;
    private ByteArrayOutputStream outputStream;

    @Mock
    private CommandExecutor mockCommandExecutor;

    // This is the class we are testing
    private ClientHandler clientHandler;

    @BeforeEach
    void setUp() {
        // This output stream will capture what the ClientHandler writes to the socket
        outputStream = new ByteArrayOutputStream();
    }

    private void setupInputStream(String input) {
        inputStream = new ByteArrayInputStream(input.getBytes());
    }

    @Test
    @DisplayName("Should handle PING command")
    public void testHandlesPingCommand() throws Exception {
        // Arrange
        String command = "*1\r\n$4\r\nPING\r\n";
        setupInputStream(command);
        Object[] parsedCommand = {"PING"};
        when(mockCommandExecutor.execute(parsedCommand)).thenReturn("PONG");

        // Act
        clientHandler = new ClientHandler(inputStream, outputStream, mockCommandExecutor);
        clientHandler.run();

        // Assert
        verify(mockCommandExecutor, times(1)).execute(parsedCommand);
        assertEquals("+PONG\r\n", outputStream.toString());
    }

    @Test
    @DisplayName("Should handle SET command")
    public void testHandlesSetCommand() throws Exception {
        // Arrange
        String command = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n";
        setupInputStream(command);
        Object[] parsedCommand = {"SET", "key", "value"};
        when(mockCommandExecutor.execute(parsedCommand)).thenReturn("OK");

        // Act
        clientHandler = new ClientHandler(inputStream, outputStream, mockCommandExecutor);
        clientHandler.run();

        // Assert
        verify(mockCommandExecutor, times(1)).execute(parsedCommand);
        assertEquals("+OK\r\n", outputStream.toString());
    }

    @Test
    @DisplayName("Should handle GET command")
    public void testHandlesGetCommand() throws Exception {
        // Arrange
        String command = "*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n";
        setupInputStream(command);
        Object[] parsedCommand = {"GET", "key"};
        when(mockCommandExecutor.execute(parsedCommand)).thenReturn("value");

        // Act
        clientHandler = new ClientHandler(inputStream, outputStream, mockCommandExecutor);
        clientHandler.run();

        // Assert
        verify(mockCommandExecutor, times(1)).execute(parsedCommand);
        assertEquals("$5\r\nvalue\r\n", outputStream.toString());
    }

}