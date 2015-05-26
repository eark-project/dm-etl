#!/bin/bash

CURRENT_PWD=$PWD
cd /srv/apache-solr-4.0.0/example/ && /srv/apache-tomcat-8/bin/startup.sh
cd $CURRENT_PWD

for node in `cat slaves`
do
  echo $node:
  ssh $node /srv/lily-2.4/service/lily-service start
  ssh $node 'cd /srv/apache-solr-4.0.0/example/ && /srv/apache-tomcat-8/bin/startup.sh'
done
