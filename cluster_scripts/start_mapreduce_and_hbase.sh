#!/bin/bash

service hadoop-0.20-mapreduce-jobtracker start
service hbase-master start
for node in `cat slaves`
do
  echo $node:
  ssh $node service hadoop-0.20-mapreduce-tasktracker start
  ssh $node service hbase-regionserver start
done
