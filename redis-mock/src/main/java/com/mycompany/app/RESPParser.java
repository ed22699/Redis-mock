package com.mycompany.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RESPParser {

    /**
     * Takes an input stream from a client and decodes the RESP message into a Java object.
     * @param is This is the input stream from a client.
     * @return A Java object representing the client's command (e.g., a String, an array of Objects, or null).
     */
    public static Object decode(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        return decodeFromReader(reader);
    }

    /**
     * Reads from a BufferedReader and decodes the next RESP message into a Java object.
     * @param reader A BufferedReader containing the encoded message from the client.
     * @return A Java object representing the client's command (e.g., a String, an array of Objects, or null).
     * @throws IOException Throws if message does not start with +, $, or *
     */
    private static Object decodeFromReader(BufferedReader reader) throws IOException {
        int firstByte = reader.read();
        switch (firstByte) {
            case '+':
                return reader.readLine();
            case '$':
                int length = Integer.parseInt(reader.readLine());
                if (length == -1) {
                    return null;
                }
                char[] buffer = new char[length];
                reader.read(buffer, 0, length);
                reader.readLine(); // Consume trailing CRLF
                return new String(buffer);
            case '*':
                int numElements = Integer.parseInt(reader.readLine());
                Object[] array = new Object[numElements];
                for (int i = 0; i < numElements; i++) {
                    array[i] = decodeFromReader(reader);
                }
                return array;
            default:
                //This is a workaround for the fact that the inputstream is not being fully consumed
                if(firstByte == '\r' || firstByte == '\n' || firstByte == -1){
                    return decodeFromReader(reader);
                }
                throw new IOException("Unknown RESP type: " + (char) firstByte + " " + firstByte);
        }
    }

    /**
     * Takes a Java object (e.g., a String or an array of strings) and encodes it into the RESP format to be sent to the client.
     * @param command An instruction from the client.
     * @return A RESP formatted string representing the server's response.
     */
    public static String encode(Object command) {
        StringBuilder sb = new StringBuilder();
        if (command instanceof String[] cmdArray) {
            sb.append("*").append(cmdArray.length).append("\r\n");
            for (String s : cmdArray) {
                sb.append("$").append(s.length()).append("\r\n");
                sb.append(s).append("\r\n");
            }
        }
        return sb.toString();
    }
}
