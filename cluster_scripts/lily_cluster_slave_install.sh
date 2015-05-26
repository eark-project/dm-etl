#!/bin/bash

# install java
mkdir /usr/lib/jvm
cd /usr/lib/jvm
wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u67-b01/jdk-7u67-linux-x64.tar.gz
tar -xzvf jdk-7u67-linux-x64.tar.gz
rm jdk-7u67-linux-x64.tar.gz
ln -s /usr/lib/jvm/jdk1.7.0_67 /usr/lib/jvm/default-java
update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk1.7.0_67/jre/bin/java 1100
export JAVA_HOME=/usr/lib/jvm/default-java
echo JAVA_HOME=/usr/lib/jvm/default-java >> /etc/environment

# add cloudera repo to package sources
cd /etc/apt/sources.list.d
wget http://archive.cloudera.com/cdh4/one-click-install/squeeze/amd64/cdh4-repository_1.0_all.deb
dpkg -i cdh4-repository_1.0_all.deb
rm cdh4-repository_1.0_all.deb
echo deb http://archive.cloudera.com/cdh4/debian/squeeze/amd64/cdh squeeze-cdh4.2.2 contrib > cloudera-cdh4.list
echo deb-src http://archive.cloudera.com/cdh4/debian/squeeze/amd64/cdh squeeze-cdh4.2.2 contrib >> cloudera-cdh4.list
apt-get update

# install libssl0.9.8 (included in debian 6 package list, but not in debian 7)
wget http://snapshot.debian.org/archive/debian/20110406T213352Z/pool/main/o/openssl098/libssl0.9.8_0.9.8o-7_amd64.deb
dpkg -i libssl0.9.8_0.9.8o-7_amd64.deb
rm libssl0.9.8_0.9.8o-7_amd64.deb

# install TaskTracker and DataNode
apt-get -y install hadoop-0.20-mapreduce-tasktracker hadoop-hdfs-datanode

# set JAVA_HOME for bigtop-utils (is used as Java finder in some hadoop applications)
echo export JAVA_HOME=/usr/lib/jvm/default-java >> /etc/default/bigtop-utils

# prepare hadoop for configuration
mkdir -p /disk2/dfs/dn /disk3/dfs/dn /disk4/dfs/dn
chown -R hdfs:hdfs /disk2/dfs/dn /disk3/dfs/dn /disk4/dfs/dn
mkdir -p /disk2/mapred/local /disk3/mapred/local /disk4/mapred/local
chown -R mapred:hadoop /disk2/mapred/local /disk3/mapred/local /disk4/mapred/local
cd /etc/hadoop
cp -r conf.empty conf.my_cluster
update-alternatives --install /etc/hadoop/conf hadoop-conf /etc/hadoop/conf.my_cluster 50 

# install region server
apt-get -y install hbase-regionserver

# install lily
cd /srv
wget http://lilyproject.org/release/2.4/lily-2.4.tar.gz
tar -xzvf lily-2.4.tar.gz
rm lily-2.4.tar.gz
cd lily-2.4/lib
cp org/lilyproject/lily-repository-impl/2.4/lily-repository-impl-2.4.jar org/lilyproject/lily-bytes/2.4/lily-bytes-2.4.jar org/lilyproject/lily-util/2.4/lily-util-2.4.jar org/lilyproject/lily-repository-api/2.4/lily-repository-api-2.4.jar org/lilyproject/lily-repository-id-impl/2.4/lily-repository-id-impl-2.4.jar org/lilyproject/lily-hbaseindex-base/2.4/lily-hbaseindex-base-2.4.jar com/gotometrics/orderly/orderly/0.11/orderly-0.11.jar org/lilyproject/lily-indexer-derefmap-indexfilter/2.4/lily-indexer-derefmap-indexfilter-2.4.jar com/ngdata/hbase-sep-api/1.1/hbase-sep-api-1.1.jar com/ngdata/hbase-sep-impl/1.1/hbase-sep-impl-1.1.jar /usr/lib/hbase/lib

# configure lily
cd /srv/lily-2.4/conf
sed -i s/localhost/s3scape02/ general/hadoop.xml general/hbase.xml general/zookeeper.xml repository/repository.xml
sed -i s/localhost:9001/s3scape02:8021/ general/mapreduce.xml
export LILY_CLI_ZK=s3scape02
echo LILY_CLI_ZK=s3scape02 >> /etc/environment

# install solr
cd /srv
wget http://archive.apache.org/dist/lucene/solr/4.0.0/apache-solr-4.0.0.tgz
tar -xzvf apache-solr-4.0.0.tgz
rm apache-solr-4.0.0.tgz

# install tomcat
cd /srv
TOMCAT_VERSION=8.0.15
wget http://tweedo.com/mirror/apache/tomcat/tomcat-8/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz
tar -xzvf apache-tomcat-$TOMCAT_VERSION.tar.gz
rm apache-tomcat-$TOMCAT_VERSION.tar.gz
mv apache-tomcat-$TOMCAT_VERSION apache-tomcat-8
cp apache-solr-4.0.0/dist/apache-solr-4.0.0.war apache-tomcat-8/webapps/solr.war
cd /srv/apache-solr-4.0.0/example/
/srv/apache-tomcat-8/bin/startup.sh
sleep 10
/srv/apache-tomcat-8/bin/shutdown.sh
cd /srv/apache-tomcat-8
sed -i s/8080/8983/ conf/server.xml
sed -i '/# OS specific support/i export CATALINA_OPTS="-DzkHost=s3scape02/solr"' bin/catalina.sh
