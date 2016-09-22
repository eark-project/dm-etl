#for dry-run > write into file dry.txt and look up morphed fields

hadoop --config /etc/hadoop/conf jar /usr/lib/hbase-solr/tools/hbase-indexer-mr-job.jar --conf /etc/hbase/conf/hbase-site.xml -D 'mapred.child.java.opts=-Xmx500m' --hbase-indexer-file /home/rainer/morphlines/morphlines-hbase-collection-news-mapper.xml --zk-host 127.0.0.1/solr --collection hbase-collection-news --go-live --dry-run --log4j /home/rainer/log4j.properties 
