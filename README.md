dm-etl
======

eArk WP6 - reference implementation: ETL AIPs into Lily

TODOs
------
- extract content from AIP
- extract content from contzainers inside AIP)e.g. WACR)
- load content on "content level" into lily
- create SOLR and other configurations, commit into src/main/config/<system>
- CLI interface to load data into Lily
- experiment with full text index on high volumes (WACR data)
- maybe develop MR job to run this on all AIPs in HDFS

Future
-------
- create pig query files in src/main/resources
- understand metadaat (e.g. METS)
- understand content (e.g. text inside MS Office)
- experiment with big CSV from db in Solr
- integration of denoamlized data bases

Not Goal
--------
- no query, query is another project
