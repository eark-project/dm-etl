#!/bin/bash

#test installation
#hadoop jar /usr/lib/hadoop-mapreduce/hadoop-mapreduce-examples.jar pi 10 100

#find a library 
#grep -r "Gauge" /usr/lib/hbase

#print job logs 
#yarn logs -applicationId application_1471451406971_0003

export HADOOP_CLASSPATH=/usr/lib/hbase/hbase-client.jar:/usr/lib/hbase/lib/hbase-common-1.2.0-cdh5.8.0.jar:/usr/lib/hbase/lib/hbase-protocol-1.2.0-cdh5.8.0.jar:/usr/lib/hbase/lib/htrace-core4-4.0.1-incubating.jar:/usr/lib/hbase/lib/htrace-core-3.2.0-incubating.jar:/usr/lib/hbase/lib/netty-all-4.0.23.Final.jar:/usr/lib/hbase/lib/metrics-core-2.2.0.jar

export LIBJARS=`echo ${HADOOP_CLASSPATH} | sed s/:/,/g`

#hadoop jar dm-etl-1.0.0-SNAPSHOT-mapreduce-job.jar -libjars ${LIBJARS} -z 127.0.0.1/solr

#hadoop jar dm-etl-1.0.0-SNAPSHOT-mapreduce-job.jar -libjars ${LIBJARS} -Dyarn.app.mapreduce.am.log.level=DEBUG -Dmapreduce.map.log.level=DEBUG -z 127.0.0.1/solr

hadoop jar dm-etl-1.0.0-SNAPSHOT-mapreduce-job.jar -libjars ${LIBJARS} -Dyarn.app.mapreduce.am.log.level=INFO,console -Dmapreduce.map.log.level=INFO,console -Dmapreduce.reduce.log.level=INFO,console -z 127.0.0.1/solr

