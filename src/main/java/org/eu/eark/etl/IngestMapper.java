package org.eu.eark.etl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.io.compress.Compression.Algorithm;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCRecord;
import org.archive.util.LaxHttpParser;
import org.eu.eark.etl.webscraping.WebScraper;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * read records from warc files, extract information and store it in lily
 * repository
 */
public class IngestMapper extends Mapper<Text, Text, Text, Text> {
	// private LilyClient lilyClient;
	// private LRepository repository;
	public static final String REPOSITORY_TABLE_NAME = "news_archive";
	public static final String CF_REPOSITORY = "doc_colf";
	public static final String WEBSITE_TYPE = "website";

	private final int NEW_ARTICLE_BODY = 0;
	private final int ARTICLE_BODY_ALREADY_EXISTING = -1;

	protected Table table = null;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private Logger logger = Logger.getLogger(IngestMapper.class);

	// contentType
	public enum rField {
		str_recordType, str_url, str_date, str_contentType, int_size, str_body
	}

	public enum cField {
		str_category, str_headline, str_author, str_datePublished, str_articleBody, int_postings
	}

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {
		super.setup(context);

		/*
		 * this.lilyClient =
		 * LilyMapReduceUtil.getLilyClient(context.getConfiguration()); try {
		 * this.repository = lilyClient.getDefaultRepository(); } catch
		 * (RepositoryException e) { throw new
		 * RuntimeException("Failed to get repository", e); }
		 */
		Configuration hBaseConfig = HBaseConfiguration.create();
		Connection connection = ConnectionFactory.createConnection(hBaseConfig);
		Admin admin = connection.getAdmin();
		HTableDescriptor tableDsc = new HTableDescriptor(
				TableName.valueOf(REPOSITORY_TABLE_NAME));
		if (!admin.tableExists(tableDsc.getTableName())) {
			tableDsc.addFamily(new HColumnDescriptor(CF_REPOSITORY)/*
																															 * .setCompressionType
																															 * (
																															 * Algorithm.SNAPPY
																															 * )
																															 */);
			admin.createTable(tableDsc);

			for (rField field : rField.values()) {
				createHBaseColumn(field.toString(), admin);
			}

			for (cField field : cField.values()) {
				createHBaseColumn(field.toString(), admin);
			}
		}

		table = connection.getTable(tableDsc.getTableName());
		// Put put = new Put(Bytes.toBytes("http://url.com"));
		// put.addColumn(Bytes.toBytes(CF_REPOSITORY),
		// Bytes.toBytes(rField.url.toString()), Bytes.toBytes("http://url.com"));
		// put.addColumn(Bytes.toBytes(CF_REPOSITORY),
		// Bytes.toBytes(rField.contentType.toString()),
		// Bytes.toBytes("text/html"));
		// table.put(put);
	}

	private void createHBaseColumn(String fieldName, Admin admin)
			throws IOException {
		HTableDescriptor tableDsc = table.getTableDescriptor();
		HColumnDescriptor colDsc = new HColumnDescriptor(fieldName);
		// newColumn.setCompactionCompressionType(Algorithm.GZ);
		colDsc.setMaxVersions(HConstants.ALL_VERSIONS);
		admin.addColumn(tableDsc.getTableName(), colDsc);
	}

	@Override
	protected void cleanup(Context context) throws IOException,
			InterruptedException {
		// Closer.close(lilyClient);
		super.cleanup(context);
	}

	private String toSolrDateFormat(DateTime dateTime) {
		Date date = dateTime.toDate();
		return sdf.format(date);
	}
	
	@Override
	protected void map(Text key, Text value, Context context) throws IOException,
			InterruptedException {

		String path = key.toString();
		logger.info("IngestMapper.map() received path: " + path);

		try (FSDataInputStream fis = FileSystem.get(new java.net.URI(path),
				context.getConfiguration()).open(new Path(path))) {
			// LTable table = repository.getDefaultTable();

			try (ArchiveReader reader = ArchiveReaderFactory.get(path, fis, true)) {
				for (ArchiveRecord archiveRecord : reader) {
					WARCRecord warc = (WARCRecord) archiveRecord;
					logger.info("Mimetype: " + warc.getHeader().getMimetype());
					if (warc.getHeader().getMimetype()
							.equals("application/http; msgtype=response")) {

						String url = warc.getHeader().getUrl();
						logger.info("url: " + url);

						// name: "p$Website", erl
						// fields: [
						// string {name: "p$url", mandatory: true}, erl
						// datetime {name: "p$date", mandatory: true}, erl
						// string {name: "p$contentType", mandatory: false}, erl
						// integer {name: "p$size", mandatory: true},
						// string {name: "p$category", mandatory: false},
						// string {name: "p$headline", mandatory: false},
						// string {name: "p$author", mandatory: false},
						// datetime {name: "p$datePublished", mandatory: false},
						// string {name: "p$articleBody", mandatory: false},
						// integer {name: "p$postings", mandatory: false},
						// blob {name: "p$body", mandatory: false}
						// ]

						/*
						 * RecordId id = repository.getIdGenerator().newRecordId(url);
						 * 
						 * Record existingRecord = null; try { existingRecord =
						 * table.read(id, q("date"), q("size"), q(WebScraper.ARTICLE_BODY));
						 * } catch (RecordNotFoundException e) { //lily has no
						 * doesRecordExist function logger.info("  Record doesn't exist!");
						 * }
						 */

						// FIXME create a smarter id
						String id = url;
						Get get = new Get(Bytes.toBytes(url));
						Result existingRecord = table.get(get);
						if (existingRecord.isEmpty()) {
							logger.info("no existing record for this url.");
						} else {
							logger.info("a record for this url already exists");
						}

						Put put = new Put(Bytes.toBytes(id));
						put.addColumn(Bytes.toBytes(CF_REPOSITORY),
								Bytes.toBytes(rField.str_recordType.toString()),
								Bytes.toBytes(WEBSITE_TYPE));
						put.addColumn(Bytes.toBytes(CF_REPOSITORY),
								Bytes.toBytes(rField.str_url.toString()), Bytes.toBytes(url));

						// Record record = table.newRecord(id);
						// record.setRecordType(q("Website"));
						// record.setField(q("url"), url);

						String dateString = warc.getHeader().getDate();
						DateTime dateTime = ISODateTimeFormat
								.localDateOptionalTimeParser()
								.parseDateTime(dateString.substring(0, dateString.length() - 1));
						logger.info("date: " + toSolrDateFormat(dateTime));

						// TODO use a smarter serialization format like AVRO
						// TODO use the Phoenix API
						put.addColumn(Bytes.toBytes(CF_REPOSITORY),
								Bytes.toBytes(rField.str_date.toString()),
								Bytes.toBytes(toSolrDateFormat(dateTime)));

						// Parsing Content-Type
						String contentType = "null";
						boolean createNewVersion = true;

						logger.info("determining ContentType...");

						String headerLine;
						do {
							headerLine = LaxHttpParser.readLine(warc, "UTF-8");
							logger.debug("headerLine: " + headerLine);
							if (headerLine.startsWith("Content-Type: ")) {
								contentType = headerLine.substring(headerLine.indexOf(" ") + 1);
							}
						} while (!headerLine.equals(""));

						logger.info("ContentType: " + contentType);

						put.addColumn(Bytes.toBytes(CF_REPOSITORY),
								Bytes.toBytes(rField.str_contentType.toString()),
								Bytes.toBytes(contentType));

						// Parsing size
						long length = warc.getHeader().getLength() - warc.getPosition();

						// Aim to have cells no larger than 10 MB, or 50 MB if you use mob
						// about 2G
						//int sizeLimit = Integer.MAX_VALUE - 1024;
						int sizeLimit = 1000 * 1000;
						byte[] body = readBytes(warc, length, sizeLimit);

						logger.info("size of current warc record is: " + body.length);
						// logger.info(" testing byte conversion "+Bytes.toString(Bytes.toBytes(body.length))
						// + " "+Bytes.toInt(Bytes.toBytes(body.length)));

						// size in bytes
						put.addColumn(Bytes.toBytes(CF_REPOSITORY),
								Bytes.toBytes(rField.int_size.toString()),
								Bytes.toBytes(body.length));

						// is this document already in the database?
						if (!existingRecord.isEmpty()) {
							byte[] dateVal = existingRecord.getValue(
									Bytes.toBytes(CF_REPOSITORY),
									Bytes.toBytes(rField.str_date.toString()));

							Date existingDate = sdf.parse(Bytes.toString(dateVal));
							logger.info("existing record crawled on date: " + existingDate);

							// rawled at the same date
							if (dateTime.toDate().compareTo(existingDate) == 0) {
								logger
										.info("existing record has been craweld on same day as the actual one");
								// createNewVersion = false;
							}

							byte[] sizeVal = existingRecord.getValue(
									Bytes.toBytes(CF_REPOSITORY),
									Bytes.toBytes(rField.int_size.toString()));
							int existingSize = Bytes.toInt(sizeVal);
							logger
									.info("Retrieving warc record size from existing record: val="
											+ existingSize);

							// same content
							if (body.length == existingSize) {
								logger
										.info("existing record was of the same size as the actual one");
								// createNewVersion = false;
							}
						}

						if (contentType != null)
							put.addColumn(Bytes.toBytes(CF_REPOSITORY),
									Bytes.toBytes(rField.str_contentType.toString()),
									Bytes.toBytes(contentType));

						// ContentType: text/html; charset=utf-8
						if (contentType.startsWith("text/html")
								&& contentType.contains("=")) {
							// extract charset
							logger
									.info("This is a text/html page. Extracting content from record...");
							String charset = contentType
									.substring(contentType.indexOf('=') + 1);
							String domainName = url.split("/")[2];
							String decodedBody = new String(body, charset);

							int status = extractContent(domainName, existingRecord, put,
									decodedBody);
							if (status == ARTICLE_BODY_ALREADY_EXISTING)
								createNewVersion = false;
							else
								createNewVersion = true;

						}

						logger.info("createNewVersion is " + createNewVersion);

						//if (createNewVersion && body.length == 115998) {
						if (createNewVersion) {
							// enhance with compression and/or streaming data
							put.addColumn(Bytes.toBytes(CF_REPOSITORY),
									Bytes.toBytes(rField.str_body.toString()), body);
							table.put(put);

							// Indexer indexer = lilyClient.getIndexer();
							// indexer.index(table.getTableName(), record.getId());
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * prepares new record (put) and evaluates if article is already available
	 * 
	 * @return 0 if no articleBody was found 1 iff articleBody is new 2 iff
	 *         articleBody isn't new
	 */

	private int extractContent(String domain, Result existingRecord, Put put,
			String decodedBody) {

		// LTable table = repository.getDefaultTable();
		int status = NEW_ARTICLE_BODY;

		// logger.info("  contentType: " + contentType);
		// logger.info("  charset: " + charset); WebScraper webScraper;

		WebScraper webScraper = null;

		if (domain.contains(WebScraper.FAZ))
			webScraper = WebScraper.createInstance(WebScraper.FAZ, decodedBody);
		else
			webScraper = WebScraper.createInstance(WebScraper.DER_STANDARD,
					decodedBody);

		List<String> fieldNames = webScraper.getFieldNames();

		for (String fieldName : fieldNames) {
			Object fieldValue = webScraper.getValue(fieldName);
			if (fieldValue != null) {
				logger.info("webScraper: " + fieldName + ": " + fieldValue);
				// TODO is there a special treatment for updated number of postings required?
				// check if this record already exists
				if (fieldName.equals(cField.str_articleBody.toString())
						&& !existingRecord.isEmpty()) {
					byte[] existingBodyVal = existingRecord.getValue(
							Bytes.toBytes(CF_REPOSITORY),
							Bytes.toBytes(cField.str_articleBody.toString()));
					if (existingBodyVal == null)
						logger.info("existingBody has length: " + existingBodyVal);
					else
						logger.info("existingBody has length: " + existingBodyVal.length);
					if (existingBodyVal != null && existingBodyVal.length > 0) {
						String existingArticleBody = Bytes.toString(existingBodyVal);
						if (fieldValue.equals(existingArticleBody)) {
							logger.info("Existing article body is equal to current one!");
							status = ARTICLE_BODY_ALREADY_EXISTING;
						}
					} else {
						logger.info("Existing article body is empty!");
					}
				//typeCheck for number of Postings
				} else if(fieldName.equals(cField.int_postings.toString())) {
					try {
						int nPostings = Integer.parseInt((String)fieldValue);
					} catch (NumberFormatException e) {
						logger.warn("Unable to convert number of postings to int. Received value: "+fieldValue+" Setting value to -1");
						fieldValue = "-1";
					}
				} else if(fieldName.equals(cField.str_datePublished.toString())) {
					fieldValue = toSolrDateFormat((DateTime)fieldValue);
					logger.info("date published: " + fieldValue);
				}
				put.addColumn(Bytes.toBytes(CF_REPOSITORY),
						Bytes.toBytes(cField.valueOf(fieldName).toString()),
						Bytes.toBytes((String)fieldValue));
			}
		}
		return status;
	}

	/*
	 * private int extractContent(String domain, RecordId id, Record
	 * existingRecord, Record record, String html)
	 * throws RecordException, RepositoryException, InterruptedException,
	 * IndexerException {
	 * LTable table = repository.getDefaultTable();
	 * int status = 0;
	 * //System.out.println("  contentType: " + contentType);
	 * 
	 * //System.out.println("  charset: " + charset);
	 * WebScraper webScraper;
	 * if (domain.contains(WebScraper.FAZ))
	 * webScraper = WebScraper.createInstance(WebScraper.FAZ, html);
	 * else
	 * webScraper = WebScraper.createInstance(WebScraper.DER_STANDARD, html);
	 * List<String> fieldNames = webScraper.getFieldNames();
	 * 
	 * for (String fieldName : fieldNames) {
	 * Object fieldValue = webScraper.getValue(fieldName);
	 * if (fieldValue != null) {
	 * //System.out.println("  " + fieldName + ": " + fieldValue);
	 * record.setField(q(fieldName), fieldValue);
	 * if (fieldName.equals(WebScraper.ARTICLE_BODY)) {
	 * if (existingRecord != null &&
	 * existingRecord.hasField(q(WebScraper.ARTICLE_BODY))) {
	 * String existingArticleBody = (String)
	 * existingRecord.getField(q(WebScraper.ARTICLE_BODY));
	 * if (fieldValue.equals(existingArticleBody)) {
	 * System.out.println("  Article body is equivalent!");
	 * if (webScraper.getFieldNames().contains(WebScraper.POSTINGS)) {
	 * Object postings = webScraper.getValue(WebScraper.POSTINGS);
	 * if (postings != null) {
	 * Record postingsRecord = table.newRecord(id);
	 * postingsRecord.setField(q(WebScraper.POSTINGS), postings);
	 * table.update(postingsRecord);
	 * Indexer indexer = lilyClient.getIndexer();
	 * indexer.index(table.getTableName(), postingsRecord.getId());
	 * }
	 * }
	 * return 2;
	 * }
	 * }
	 * status = 1;
	 * }
	 * }
	 * }
	 * 
	 * return status;
	 * }
	 */

	// private static QName q(String name) {
	// return new QName("at.ac.ait", name);
	// }

	/**
	 * Read the bytes of the record into a buffer and return the buffer. Give a
	 * size limit to the buffer to prevent from exploding memory, but still read
	 * all the bytes from the stream even if the buffer is full. This way, the
	 * file position will be advanced to the end of the record.
	 */
	private static byte[] readBytes(ArchiveRecord record, long contentLength,
			int sizeLimit) throws IOException {
		// Ensure the record does strict reading.
		record.setStrict(true);

		int actualSizeLimit = (int) Math.min(sizeLimit, contentLength);

		byte[] bytes = new byte[actualSizeLimit];

		if (actualSizeLimit == 0) {
			return bytes;
		}

		// NOTE: Do not use read(byte[]) because ArchiveRecord does NOT
		// over-ride
		// the implementation inherited from InputStream. And since it does
		// not over-ride it, it won't do the digesting on it. Must use either
		// read(byte[],offset,length) or read().
		int pos = 0;
		int c = 0;
		while (((c = record.read(bytes, pos, (bytes.length - pos))) != -1)
				&& pos < bytes.length) {
			pos += c;
		}

		// Now that the bytes[] buffer has been filled, read the remainder
		// of the record so that the digest is computed over the entire
		// content.
		byte[] buf = new byte[1024 * 1024];
		long count = 0;
		while (record.available() > 0) {
			count += record.read(buf, 0, Math.min(buf.length, record.available()));
		}

		// Sanity check. The number of bytes read into our bytes[]
		// buffer, plus the count of extra stuff read after it should
		// equal the contentLength passed into this function.
		if (pos + count != contentLength) {
			throw new IOException(
					"Incorrect number of bytes read from ArchiveRecord: expected="
							+ contentLength + " bytes.length=" + bytes.length + " pos=" + pos
							+ " count=" + count);
		}

		return bytes;
	}
}
