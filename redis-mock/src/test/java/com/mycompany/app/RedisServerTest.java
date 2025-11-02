package com.mycompany.app;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for the Redis mock server based on the plan in GEMINI.md.
 *
 * Note: This test requires the RedisServer to be running. It is designed
 * to fail until the server implementation is complete.
 */
public class RedisServerTest {

    private static final String HOST = "localhost";
    private static final int PORT = 6379;

    @BeforeAll
    public static void setup() {
        // TODO: Start the RedisServer in a background thread.
        // For now, this test assumes the server is started externally.
        System.out.println("Starting tests... Assumes RedisServer is running on " + HOST + ":" + PORT);
    }

    @AfterAll
    public static void tearDown() {
        // TODO: Stop the server thread.
        System.out.println("Tests finished.");
    }

    /**
     * Simulates a client sending a single command and receiving a multi-line RESP response.
     * This opens and closes a new connection for each command, simulating separate clients.
     *
     * @param command The raw RESP command to send.
     * @return A list of strings representing the lines of the server's response.
     */
    private List<String> executeCommand(String command) {
        List<String> responseLines = new ArrayList<>();
        try (Socket socket = new Socket(HOST, PORT); 
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            socket.setSoTimeout(1000); // Set a timeout to avoid tests hanging forever
            out.write(command.getBytes());
            out.flush();

            String line = in.readLine();
            if (line == null) {
                fail("Server did not respond to command: " + command.trim());
            }

            responseLines.add(line);

            // If it's a bulk string, read the second line
            if (line.startsWith("$")) {
                int length = Integer.parseInt(line.substring(1));
                if (length > 0) {
                    responseLines.add(in.readLine());
                }
            }
            return responseLines;

        } catch (Exception e) {
            fail("Test failed due to an exception while communicating with the server: " + e.getMessage());
            return null; // Unreachable
        }
    }

    @Test
    @DisplayName("Test 1: Basic SET/GET and Data Integrity Across Connections")
    public void testSetAndGetAcrossConnections() {
        // This test follows the "Basic SET/GET and Data Integrity" case from GEMINI.md.

        // 1. A client sends SET, expects "+OK"
        List<String> setResponse = executeCommand("*3 $3 SET $3 key $5 hello ");
        assertEquals(1, setResponse.size(), "SET command should return a single line response.");
        assertEquals("+OK", setResponse.get(0), "Response from first SET should be OK.");

        // 2. A different client sends GET, expects "$5" and "hello"
        List<String> getResponse1 = executeCommand("*2 $3 GET $3 key ");
        assertEquals(2, getResponse1.size(), "GET for 'key' should return a two-line bulk string response.");
        assertEquals("$5", getResponse1.get(0), "First line of GET response should be the length prefix '$5'.");
        assertEquals("hello", getResponse1.get(1), "Second line of GET response should be the value 'hello'.");

        // 3. The first client (or another new client) updates the key
        List<String> setResponse2 = executeCommand("*3 $3 SET $3 key $5 world ");
        assertEquals(1, setResponse2.size(), "Second SET command should also return a single line response.");
        assertEquals("+OK", setResponse2.get(0), "Response from second SET should be OK.");

        // 4. Another client fetches the updated value, expects "$5" and "world"
        List<String> getResponse2 = executeCommand("*2 $3 GET $3 key ");
        assertEquals(2, getResponse2.size(), "Second GET for 'key' should return a two-line bulk string response.");
        assertEquals("$5", getResponse2.get(0), "First line of second GET response should be the length prefix '$5'.");
        assertEquals("world", getResponse2.get(1), "Second line of second GET response should be the updated value 'world'.");
    }
}
