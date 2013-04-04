MineProxy
======

This is a small program that will proxy Minecraft clients or Minecraft servers to an MCAuth authentication server.  It stores the authentication server address in a file.  If the program is not given an argument, it will assume it is running a client and will download the minecraft launcher jar and place it in the .minecraft folder along with auth.txt.  If auth.txt is not blank, it will proxy it to the specified authentication server.  If the program is given one or more arguments, it will assume it is proxying a server and start the jar given in the first argument with the following arguments then use auth.properties in the current working folder.  To make it work with the Minecraft launcher, net.minecraft.Util is patched to not login with SSL.

Credit goes to MineshafterSquared for the MineProxy class and for the rewritten net.minecraft.Util.

Licensed under the MIT License; see `LICENSE.txt` for more details.
