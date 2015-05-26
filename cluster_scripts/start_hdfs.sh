#!/bin/bash

service hadoop-hdfs-namenode start
for node in `cat slaves`
do
  echo $node:
  ssh $node service hadoop-hdfs-datanode start
done
