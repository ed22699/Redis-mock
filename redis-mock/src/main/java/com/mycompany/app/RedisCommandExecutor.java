package com.mycompany.app;

import java.util.concurrent.ConcurrentHashMap;

public class RedisCommandExecutor implements CommandExecutor {

    private final ConcurrentHashMap<String, String> dataStore = new ConcurrentHashMap<>();

    /**
     * Executes any command passed, saving or retrieving data
     * @param command The decoded command of the client to be run
     * @return Outputs either an acknowledgement of success or a value depending on the command passed
     */
    @Override
    public Object execute(Object[] command) {
        String commandName = (String) command[0];
        return switch (commandName.toUpperCase()) {
            case "PING" -> ping(command);
            case "SET" -> set((String) command[1], (String) command[2]);
            case "GET" -> get((String) command[1]);
            default -> "-ERR unknown command";
        };
    }

    /**
     * Stores the data as a key value pair
     * @param key A unique string used to find the data saved
     * @param value The data which is to be saved to the database
     * @return OK to validate that the data has been stored correctly
     */
    private String set(String key, String value) {
        dataStore.put(key, value);
        return "OK";
    }

    /**
     * Gets the data from a key value pair given the individual key
     * @param key A unique string used to retrieve the data saved
     * @return The data located at that given key
     */
    private String get(String key) {
        return dataStore.get(key);
    }

    /**
     * Used to acknowledge the Redis server is working
     * @param parts PING command and then any other strings you want the server to return back
     * @return PING if no more parts, otherwise it will return the other parts as a string
     */
    private String ping(Object[] parts) {
        if (parts.length > 1) {
            return (String) parts[1];
        }
        return "PONG";
    }
}
