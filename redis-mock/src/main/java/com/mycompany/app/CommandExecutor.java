package com.mycompany.app;

public interface CommandExecutor {
    /**
     * Executes any command passed, saving or retrieving data
     * @param command The decoded command of the client to be run
     * @return Outputs either an acknowledgement of success or a value depending on the command passed
     */
    Object execute(Object[] command);
}
