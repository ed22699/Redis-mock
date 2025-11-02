package com.mycompany.app;

import java.util.concurrent.ConcurrentHashMap;

public class CommandExecutor {

    private final ConcurrentHashMap<String, Object> dataStore;
    private final AofPersistence aofPersistence;

    /**
     * Responsible for handling all the executable commands within the server and ensuring 
     * persistence 
     * @param aofPersistence This is the file which the data is saved to in order to maintain persistence
     */
    public CommandExecutor(ConcurrentHashMap<String, Object> dataStore, AofPersistence aofPersistence) {
        this.dataStore = dataStore;
        this.aofPersistence = aofPersistence;
    }

    /**
     * Executes a given decoded RESP command (choices or PING, SET and GET)
     * @param command The decoded instruction passed by the client
     * @return The servers reaction to the command, this could be validation or a value itself
     */
    public Object execute(Object[] command) {
        String commandName = (String) command[0];
        return switch (commandName.toUpperCase()) {
            case "PING" -> ping(command);
            case "SET" -> {
                if (command.length != 3) {
                    yield new Exception("ERR wrong number of arguments for 'SET' command");
                }
                yield set((String) command[1], (String) command[2], command);
            }
            case "GET" -> {
                if (command.length != 2) {
                    yield new Exception("ERR wrong number of arguments for 'GET' command");
                }
                yield get((String) command[1]);
            }
            default -> new Exception("ERR unknown command '" + commandName + "'");
        };
    }

    /**
     * Sets a value in the database
     * @param key A unique string to store the location of the data
     * @param value The data to be stored
     * @param command The command that was used to store the data
     * @return Validates that the data was stored correctly
     */
    private String set(String key, String value, Object[] command) {
        dataStore.put(key, value);
        aofPersistence.logCommand(command);
        return "OK";
    }

    /**
     * Gets a value corresponding to the given key
     * @param key A unique string giving the location of the data 
     * @return The data present at the keys position
     */
    private Object get(String key) {
        return dataStore.get(key);
    }

    /**
     * Ensures the redis server is working by returning a given response
     * @param parts The command ping and any values you want to repeat back
     * @return PONG if command has no extra arguments, otherwise it will return the extra argument as a string
     */
    private String ping(Object[] parts) {
        if (parts.length > 1) {
            return (String) parts[1];
        }
        return "PONG";
    }
}
