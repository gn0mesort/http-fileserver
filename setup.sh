#!/usr/bin/env bash
# Don't invoke this as root it will ask permission as needed.
sudo mkdir -p /var/www/ /usr/local/bin
sudo cp -r fileserver-root /var/www/
sudo mv /var/www/fileserver-root /var/www/files
sudo cp -r etc /
sudo cp out/artifacts/http_fileserver_jar/http-fileserver.jar /usr/local/bin/http-fileserver.jar
sudo groupadd --system http-fileserver
sudo useradd --system --gid=http-fileserver http-fileserver
sudo chown http-fileserver:http-fileserver /var/www/files