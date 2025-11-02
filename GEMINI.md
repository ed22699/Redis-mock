you are a senior java engineer specialising in secure application development, assisting 
me (the lead engineer)

## Rules
### Code Style
- all functions should start lowercase and be camel case
- with each function and class add a template for where the documentation will be 
but do not fill it in i.e. add parameters, returns, description but do not write the 
actual content, I want to write this to help understand each function
- try to maintain good coding practices, use one class per file unless if you can 
justify otherwise

### Testing
- I will be using two agents. One will be writing the tests and the other will be 
writing the code to pass the tests, this will be test driven development
- if you are the agent writing the code you may run tests however, run the individual tests 
only occasionally running all tests to check for breaks

## Plan
Of course. Here is a step-by-step plan for implementing your own version of Redis in Java.

### 1. Data Structures
*   **Primary Data Store:** The core of the database will be a `java.util.concurrent.ConcurrentHashMap<String, Object>`.
    *   Using `ConcurrentHashMap` provides built-in thread safety for key-level operations, which is essential for handling multiple client connections simultaneously.
    *   The value will be of type `Object` to store different data types. We will perform runtime checks (`instanceof`) to determine if the stored value is a `String` or another `Map` for the hash type.
*   **String Storage:** For Redis Strings, the value in the primary map will be a standard Java `String`.
    *   `ConcurrentHashMap<String, String>`
*   **Hash Storage:** For Redis Hashes, the value in the primary map will be another `ConcurrentHashMap` to store the field-value pairs of the hash.
    *   `ConcurrentHashMap<String, ConcurrentHashMap<String, String>>`

### 2. Key Functions or Classes
*   **`RedisServer`:** The main class. It will contain the `main` method to start the server. Its primary responsibilities will be:
    *   Opening a `java.net.ServerSocket` on a specified port.
    *   Running an infinite loop to accept incoming client `Socket` connections.
    *   For each new connection, it will spawn a new `Thread` running a `ClientHandler` task to process client requests independently.
*   **`ClientHandler` (implements `Runnable`):** This class will manage the lifecycle of a single client connection.
    *   It will hold the client `Socket` and its `InputStream` and `OutputStream`.
    *   It will read raw byte data from the client, pass it to the `RESPParser`, and receive a structured command.
    *   It will pass the command to a command execution layer, receive a result, and use the `RESPParser` to encode the result back into the RESP format before writing it to the `OutputStream`.
*   **`RESPParser`:** A utility class with no state, responsible for protocol-level serialization and deserialization.
    *   `decode(InputStream)`: A method that reads from the client input stream and parses the RESP format into a Java object, likely `Object[]`, representing the command and its arguments (e.g., `["SET", "mykey", "myvalue"]`).
    *   `encode(Object)`: A method that takes a Java object (e.g., a `String`, `Integer`, `Exception`, or `Array`) and serializes it into the correct RESP byte format to be sent to the client.
*   **`CommandExecutor`:** This class will contain the business logic for all supported commands (`SET`, `GET`, `HSET`, `HGET`, `PING`, etc.).
    *   It will have a central `execute(Object[] command)` method that uses a `switch` statement or a `Map<String, Function>` on the command name (e.g., "SET").
    *   It will interact with the central data structures to read or modify data.
    *   For write operations (`SET`, `HSET`), it will delegate to the `AofPersistence` class to log the command *after* a successful in-memory operation.
*   **`AofPersistence`:** This class will manage writing to and reading from the Append-Only File.
    *   `logCommand(Object[] command)`: Converts the raw command back into the RESP format and appends it to the `redis.aof` file. Access to this method must be `synchronized` to prevent race conditions from multiple client threads writing to the file simultaneously.
    *   `loadData()`: Called once on server startup. It will read the `redis.aof` file line by line, parse each line as a command, and execute it to rebuild the in-memory data state.

### 3. Potential Challenges
*   **RESP Parsing Robustness:** The parser must correctly handle partial reads from the TCP stream. A command may not arrive in a single `read()` call. The parser will need to buffer incoming bytes until a complete RESP message (`
` terminated) is available.
*   **Concurrency Control for AOF:** While `ConcurrentHashMap` handles the data store, the AOF file is a shared resource. All writes to the AOF file must be synchronized to ensure commands are written sequentially and the file does not get corrupted by interleaved writes from different threads.
*   **Atomicity:** For this initial version, operations like `SET` are atomic thanks to `ConcurrentHashMap`. However, more complex, multi-key commands in a real Redis implementation would require more advanced locking or transactional mechanisms.
*   **Error Handling:** The server must not crash on invalid input. It needs to handle malformed RESP, unknown commands, and commands with the wrong number of arguments by sending a well-formed RESP error message back to the client.

### 4. Implementation Order
1.  **RESP Parser:** Start here. This is a self-contained, critical component that can be built and unit-tested in isolation. Create methods to parse a RESP string into a command array and encode various responses back to RESP strings.
2.  **Core Data Structures & Command Logic:** Implement the `ConcurrentHashMap` store and the basic command logic for `PING`, `SET`, and `GET`. At this stage, all data is in-memory only.
3.  **Basic Server and Client Handler:** Create the `RedisServer` to accept a single connection and a `ClientHandler` that can process one command, execute it, and return a response before closing.
4.  **Full Concurrency:** Modify the `RedisServer` to spin up a new thread for each client, enabling it to handle multiple connections simultaneously.
5.  **AOF Persistence:** Implement the `AofPersistence` class. Integrate `logCommand` into the write commands (`SET`, `HSET`) and add the `loadData` call to the server's startup sequence.
6.  **Add Hash Commands:** With the core structure in place, add the logic for `HSET`, `HGET`, and `HGETALL`, including the necessary type-checking on the values retrieved from the main data store.

### 5. Initial Tests
1.  **Basic SET/GET and Data Integrity:**
    *   Connect a client, send a `SET key "hello"` command, and verify the response is `OK`.
    *   Connect a second, concurrent client, send a `GET key` command, and verify the response is `"hello"`.
    *   Have the first client send a `SET key "world"` command.
    *   Have the second client send another `GET key` and verify the response is now `"world"`.
    *   This test validates the concurrent network server, RESP parsing, and basic string operations.
2.  **AOF Persistence and Data Recovery:**
    *   Start the server and ensure the AOF file is empty.
    *   Connect a client and execute `SET persistent_key "i must survive"`.
    *   Shut down the server.
    *   Manually inspect the `redis.aof` file to confirm it contains the raw `SET` command in RESP format.
    *   Restart the server.
    *   Connect a new client and execute `GET persistent_key`.
    *   Verify the response is `"i must survive"`, confirming the server correctly reloaded the data from the AOF file on startup.
