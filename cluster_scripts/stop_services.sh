#!/bin/bash

for node in `cat slaves`
do
  echo $node:
  ssh $node service hbase-regionserver stop
  ssh $node service hadoop-0.20-mapreduce-tasktracker stop
done
service hbase-master stop
service hadoop-0.20-mapreduce-jobtracker stop
for node in `cat slaves`
do
  echo $node:
  ssh $node service hadoop-hdfs-datanode stop
done
service hadoop-hdfs-namenode stop
