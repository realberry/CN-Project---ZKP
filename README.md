````md
# CN-Project---ZKP

## Project Structure
- `src/` contains all the source code.
- `bin/` contains compiled `.class` files.

## Compilation

If the `bin` directory does not exist, create it and compile the code using:

```bash
mkdir bin
javac -cp "lib/*;src" -d bin src/client/*.java src/server/*.java src/common/*.java
````

## Running the Project

### Start the Server

Run the server before testing any client:

```bash
java -cp "lib/*;bin" server.ZKPServer
```

### Run Clients

#### Valid Client

```bash
java -cp "lib/*;bin" client.ZKPClient
```

#### Invalid Client 1

Has an edge with the same colours:

```bash
java -cp "lib/*;bin" client.ZKPClientFailure1
```

#### Invalid Client 2

Uses more than 4 colours:

```bash
java -cp "lib/*;bin" client.ZKPClientFailure2
```

```
```
