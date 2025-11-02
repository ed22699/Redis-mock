package com.mycompany.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the RESPParser.
 * This class follows the Test-Driven Development (TDD) approach.
 * It will fail to compile until the RESPParser class is implemented.
 */
public class RESPParserTest {

    @Test
    @DisplayName("Should correctly decode a simple string")
    public void testDecodeSimpleString() throws Exception {
        String input = "+OK\r\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        Object result = RESPParser.decode(inputStream);
        assertEquals("OK", result);
    }

    @Test
    @DisplayName("Should correctly decode a bulk string")
    public void testDecodeBulkString() throws Exception {
        String input = "$5\r\nhello\r\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        Object result = RESPParser.decode(inputStream);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("Should correctly decode a null bulk string")
    public void testDecodeNullBulkString() throws Exception {
        String input = "$-1\r\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        Object result = RESPParser.decode(inputStream);
        assertNull(result);
    }

    @Test
    @DisplayName("Should correctly decode an array of bulk strings")
    public void testDecodeArrayOfBulkStrings() throws Exception {
        String input = "*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        Object[] result = (Object[]) RESPParser.decode(inputStream);
        assertArrayEquals(new String[]{"GET", "key"}, result);
    }

    @Test
    @DisplayName("Should correctly encode an array of strings")
    public void testEncodeArray() {
        String[] command = {"SET", "key", "value"};
        // Assuming encode returns a String for simplicity in testing.
        // A byte[] return type would also be acceptable.
        String result = RESPParser.encode(command);
        String expected = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n";
        assertEquals(expected, result);
    }
}