#!/bin/bash

/srv/apache-tomcat-8/bin/shutdown.sh

for node in `cat slaves`
do
  echo $node:
  ssh $node /srv/lily-2.4/service/lily-service stop
  ssh $node /srv/apache-tomcat-8/bin/shutdown.sh
done
