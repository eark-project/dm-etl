#!/bin/bash

HC=/etc/hadoop/conf.my_cluster
HBC=/etc/hbase/conf.dist
for node in `cat slaves`
do
  cd $HC
  scp core-site.xml hdfs-site.xml root@$node:$HC
  scp mapred-site_slave.xml root@$node:$HC/mapred-site.xml
  cd $HBC
  scp hbase-site.xml root@$node:$HBC
done
