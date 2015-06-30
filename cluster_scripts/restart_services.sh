#!/bin/bash

./copy_configuration.sh
./stop_services.sh
./start_hdfs.sh
./start_mapreduce_and_hbase.sh
