# http-fileserver

Java based HTTP fileserver

## Installing

1. Open the project in IntelliJ Idea
2. Build > Build Artifacts > http-fileserver.jar > Build
3. Copy the project directory to your server if it isn't already there.
4. Run `chmod +x setup.sh` on your server.
5. Run `./setup.sh` on your server.
6. Configure your server.

## Configuring

The default configuration file, as used by the included `systemd` service is
`/etc/megatech/http-fileserver/default.conf`. For an example configuration please read
`example.conf`.
