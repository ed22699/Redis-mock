package com.mycompany.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {

    private final RedisCommandExecutor commandExecutor;

    public RedisServer() {
        this.commandExecutor = new RedisCommandExecutor();
    }

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
                new Thread(new ClientHandler(clientSocket.getInputStream(), clientSocket.getOutputStream(), commandExecutor)).start();
            } 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
