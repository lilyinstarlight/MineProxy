MineProxy
=========

MineProxy is a small program that will proxy Minecraft clients or Minecraft servers to an [MCAuth](https://github.com/fkmclane/MCAuth) authentication server.  The IP address for the authentication server is stored in a configuration file.  If the program is not given an argument, it will assume it is running a client and download the Minecraft launcher and the configuration file will be created in that same folder as auth.txt.  If auth.txt is not blank, it will proxy it to the specified authentication server.  If the program is given one or more arguments, it will assume it is proxying a server, start the JAR passed as the first argument and use auth.properties in the current working directory as the configuration.

Licensed under the MIT License; see `LICENSE` for more details.
