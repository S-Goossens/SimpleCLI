# CLI Module
Originally made for the SIMPA project. This module allows you to make a simple CLI for your application by using annotations to mark methods as commands.

## Getting started
### Creating the CLI
To create the CLI create an instance of the CommandInterpreter by calling the create() method of the CommandInterpreter class. 
This will require you to supply the package that you want to be searched for commands.
```java
CommandInterpreter ci = CommandInterpreter.create(new Class[]{AccCli.class});
```

It is also possible to add a prefix to the shell by giving a second argument with the desired prefix,
This prefix will then show up at every line where the user is prompted for input.
```java
CommandInterpreter ci = CommandInterpreter.create(new Class[]{AccCli.class}, "AccCli=> ");
```

### Running the CLI
To run the CLI run the start() or startFromFile(String filename, boolean debugMode) method of the CommandInterpreter instance.
If debug is set to true the shell will print every command it is executing.
```java
ci.start();
ci.startFromFile("script.txt", true);
```
Starting from a file requires a script that is set up in a correct way so that the application can read it.
Please read the section about writing a script for information on how to create a CLI script.

### Creating commands
#### @Command()
A command can be created by supplying a method in the scanned package with the @Command annotation.
This annotation requires a key and a description to be set, the key will be used to call the method
and the description will be printed when the user requests help with the -h or --help flag after the command.
It is possible to return objects in a method annotated with the @Command annotation, this allows users to store
the returned object in a variable which is then saved in the state of the CLI, look at the Syntax section for an example
on how to save variables.
#### @CommandParameter()
To define parameters for a command a parameter should be given the @CommandParameter annotation. This annotation
requires a set of keys (can be a single key) to be set, these keys are used to identify the parameter when executing the
command with the parameter. Further you can say that the parameters is required, this is false by default, and it is possible
to supply help which will be printed when the help flag (-h or --help) is added to the command.
```java
@Command(
        key = "print-text",
        description = "Prints the text from a file."
)
public void printText(@CommandParameter(keys = {"-f", "--file"}, required = true, help = "File to read from.") String fileName) {
    // do something
}
```

When the CLI is started the application will collect all methods annotated with @Command and make them available as commands.

Above command could be run with:
```text
print-text -f "filename"
```
### Executing commands
#### Syntax
The syntax for the CLI is fairly basic, this will go over some key concepts:

Comments
```text
# This is a comment
! This is also a comment
```
Variables
```text
# Storing an integer.
a = 1

# Storing a double.
b = 2.53434
c = 1E-4

# Storing a string. Strings should be written with double quotes.
d = "This is a string"

# Storing a boolean.
e = true

# Storing the returned object of a command.
f = some-command -t "Some parameter"
```

Running a command can be done by writing the key in the @Command annotation and adding the required flags.
In below example the print-text is the key of the Command and the -f is flag for a filename and the -o is a flag for the output file. 
flags should be added as <command-key> [flag-key] [flag-value] ...
```text
print-text -f "<<filename>>" -o "<<output-filename>>"
```

If a command returns an object it can be stored in the state by adding an assignment to a variable
with:
```text
<<variable name>> = <<command>>
```

##### IMPORTANT! The CLI syntax does not allow commands to be written over multiple lines.

#### Getting help
To display usage info and descriptions for every command the user can run the help command:
```text
help
```
Getting usage info and help for a specific command can be done by appending the command with the help flag -h or --help:
```text
print-text --help
print-text -h
```

#### Interactive shell
When running the start() method of the CommandInterpreter an interactive shell will be started
requesting the user for input, the input can be written with the syntax above. The interactive shell
is a good way to debug commands before adding them to a script.

##### Running files from the interactive shell
Files can be run from the interactive shell with the call command.
```text
call -f "<<filename>>"
```

##### Saving command history to a file
The command history from the interactive shell can be saved to a file by using:
```text
write-script -f "<<filename>>"
```

#### Writing a script
A script can be written that is supplied to the startFromFile(String filename) method or the ```text call -f "<<filename>>"``` command 
of the CommandInterpreter using the syntax explained in the section syntax, this syntax is the exact same as writing the commands in the interactive shell
so the interactive shell allows for simple debugging.


