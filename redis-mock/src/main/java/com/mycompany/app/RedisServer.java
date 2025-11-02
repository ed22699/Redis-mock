package com.mycompany.app;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {
    private final CommandExecutor commandExecutor;

    public RedisServer() {
        AofPersistence aofPersistence = new AofPersistence("redis.aof");
        this.commandExecutor = new CommandExecutor(new ConcurrentHashMap<>(), aofPersistence);
        loadDataFromFile(aofPersistence);
    }

    private void loadDataFromFile(AofPersistence aofPersistence) {
        List<Object[]> commands = aofPersistence.loadData();
        for (Object[] command : commands) {
            commandExecutor.execute(command);
        }
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
