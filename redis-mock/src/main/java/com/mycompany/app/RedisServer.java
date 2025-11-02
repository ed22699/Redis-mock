package com.mycompany.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {

    private final ConcurrentHashMap<String, String> dataStore = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        RedisServer server = new RedisServer();
        server.start();
    }

    /**
     * Starts the server on port 6379, creating a new thread as each new client connects
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(6379)) {
            System.out.println("Redis mock server started on port 6379");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            } 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Establishes a connection between an individual client and the server, upon this connection
     * the server listens for all incoming commands (currently GET, SET and PING), reacting appropriately
     * @param clientSocket The unique connection between a specific client and the server
     */
    private void handleClient(Socket clientSocket) {
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            Object decoded = RESPParser.decode(in);
            if (decoded instanceof Object[] decodedArray) {
                String command = (String) decodedArray[0];

                String key;
                String value;
                switch (command.toUpperCase()) {
                    case "SET":
                        key = (String) decodedArray[1];
                        value = (String) decodedArray[2];
                        set(key, value);
                        out.write("+OK\r\n".getBytes());
                        break;
                    case "GET":
                        key = (String) decodedArray[1];
                        value = get(key);
                        if (value != null) {
                            String response = "$" + value.length() + "\r\n" + value + "\r\n";
                            out.write(response.getBytes());
                        } else {
                            out.write("$-1\r\n".getBytes());
                        }
                        break;
                    case "PING":
                        out.write(ping(decodedArray).getBytes());
                        break;
                    default:
                        out.write("-ERR unknown command\r\n".getBytes());
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stores both the key and the corresponding value passed into the servers data
     * @param key A unique sequence used to locate the data saved 
     * @param value The data to be saved
     */
    private void set(String key, String value) {
        dataStore.put(key, value);
    }

    /**
     * Uses the unique key to locate the corresponding value data and returns it
     * @param key A unique sequence used to locate the data saved
     * @return The data corresponding to the keys defined location
     */
    private String get(String key) {
        return dataStore.get(key);
    }

    /**
     * Reacts to the ping command and outputs appropriately to validate the servers basic functionality
     * @param parts The array of objects passed as a ping command
     * @return PONG if only a PING was set, otherwise return the message passed after the PING
     */
    private String ping(Object[] parts) {
        if (parts.length > 1) {
            String message = (String) parts[1];
            return "$" + message.length() + "\r\n" + message + "\r\n";
        }
        return "+PONG\r\n";
    }
}
