package com.mycompany.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final InputStream in;
    private final OutputStream out;
    private final CommandExecutor commandExecutor;

    /**
     * Sets up a designated handler to deal with a single individual client
     * @param in The stream being passed by the client to the server
     * @param out The stream being outputted by the server to the client
     * @param commandExecutor The executor in charge of the commands and persistence of data within the server
     */
    public ClientHandler(InputStream in, OutputStream out, CommandExecutor commandExecutor) {
        this.in = in;
        this.out = out;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public void run() {
        try {
            Object decoded = RESPParser.decode(in);
            if (decoded instanceof Object[] decodedArray) {
                Object result = commandExecutor.execute(decodedArray);
                String encoded = RESPParser.encode(result);
                out.write(encoded.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
