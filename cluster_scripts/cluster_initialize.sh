#!/bin/bash

./copy_configuration.sh

sudo -u hdfs hdfs namenode -format

./start_hdfs.sh

sudo -u hdfs hadoop fs -mkdir /tmp
sudo -u hdfs hadoop fs -chmod -R 1777 /tmp
sudo -u hdfs hadoop fs -mkdir -p /var/lib/hadoop-hdfs/cache/mapred/mapred/staging
sudo -u hdfs hadoop fs -chmod 1777 /var/lib/hadoop-hdfs/cache/mapred/mapred/staging
sudo -u hdfs hadoop fs -chown -R mapred /var/lib/hadoop-hdfs/cache/mapred
sudo -u hdfs hadoop fs -mkdir /tmp/mapred/system
sudo -u hdfs hadoop fs -chown mapred:hadoop /tmp/mapred/system

# create directories for hbase
sudo -u hdfs hadoop fs -mkdir /hbase
sudo -u hdfs hadoop fs -chown hbase /hbase

# create directories for lily
sudo -u hdfs hadoop fs -mkdir -p /lily/blobs
sudo -u hdfs hadoop fs -chmod 777 /lily/blobs

./start_mapreduce_and_hbase.sh

sudo -u hdfs hadoop fs -mkdir  /user/rk
sudo -u hdfs hadoop fs -chown rk /user/rk
