#!/bin/bash

# stop the crawl job
curl -v -d "action=teardown" -k -u admin:admin --anyauth --location https://localhost:8443/engine/job/newspaper

# clear or create 'processed_warcs' directory
hadoop fs -mkdir -p processed_warcs
hadoop fs -rm "processed_warcs/*"

# move old warc files to 'processed_warcs' directory
hadoop fs -mv "warcs/*" processed_warcs/

# copy warc files into hdfs
hadoop fs -put /srv/heritrix-3.2.0/jobs/newspaper/latest/warcs/*.warc.gz warcs/

# start job
hadoop jar /srv/dm-etl/dm-etl-1.0.0-SNAPSHOT-mapreduce-job.jar -z s3scape02
