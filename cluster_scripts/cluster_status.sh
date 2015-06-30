#!/bin/bash

service hadoop-hdfs-namenode status
service hadoop-0.20-mapreduce-jobtracker status
service hbase-master status

for node in `cat slaves`
do
  echo $node:
  ssh $node service hadoop-hdfs-datanode status
  ssh $node service hadoop-0.20-mapreduce-tasktracker status
  ssh $node service hbase-regionserver status
  ssh $node /srv/lily-2.4/service/lily-service status
done

