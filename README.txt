In the current version, to pass messages between two emulators;

Start two emulators, each running Rangzen
telnet localhost 5554
> redir add udp:51689:51689

In the emulator with port 5556, send a message using the app interface
