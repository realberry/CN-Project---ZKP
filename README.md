# CN-Project---ZKP

## Project Structure
- `src/` contains the source code  
- `bin/` contains compiled `.class` files  

## Compilation
If the `bin` directory does not exist, create it and compile the code:

```bash
mkdir bin
javac -cp "lib/*;src" -d bin src/client/*.java src/server/*.java src/common/*.java
