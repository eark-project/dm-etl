package org.eu.eark.etl;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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
import org.lilyproject.client.LilyClient;
import org.lilyproject.indexer.Indexer;
import org.lilyproject.mapreduce.LilyMapReduceUtil;
import org.lilyproject.repository.api.*;
import org.lilyproject.util.io.Closer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * read records from warc files, extract information and store it in lily repository
 */
public class IngestMapper extends Mapper<Text, Text, Text, Text> {
	private LilyClient lilyClient;
	private LRepository repository;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		this.lilyClient = LilyMapReduceUtil.getLilyClient(context.getConfiguration());
		try {
			this.repository = lilyClient.getDefaultRepository();
		} catch (RepositoryException e) {
			throw new RuntimeException("Failed to get repository", e);
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		Closer.close(lilyClient);
		super.cleanup(context);
	}

	@Override
	protected void map(Text key, Text value, Context context) throws IOException, InterruptedException {

		String path = key.toString();

		try (FSDataInputStream fis = FileSystem.get(new java.net.URI(path), context.getConfiguration()).open(
				new Path(path))) {
			LTable table = repository.getDefaultTable();
			
			try (ArchiveReader reader = ArchiveReaderFactory.get(path, fis, true)) {
				for (ArchiveRecord archiveRecord : reader) {
					WARCRecord warc = (WARCRecord) archiveRecord;
					//System.out.println("Mimetype: " + warc.getHeader().getMimetype());
					if (warc.getHeader().getMimetype().equals("application/http; msgtype=response")) {
	
						String url = warc.getHeader().getUrl();
						System.out.println("url: " + url);
						RecordId id = repository.getIdGenerator().newRecordId(url);
						
						Record existingRecord = null;
						try {
							existingRecord = table.read(id, q("date"), q("size"), q(WebScraper.ARTICLE_BODY));
						} catch(RecordNotFoundException e) {
							/*lily has no doesRecordExist function*/
							System.out.println("  Record doesn't exist!");
						}
						
						Record record = table.newRecord(id);
						record.setRecordType(q("Website"));
						record.setField(q("url"), url);
	
						String dateString = warc.getHeader().getDate();
						DateTime date = ISODateTimeFormat.localDateOptionalTimeParser().parseDateTime(
								dateString.substring(0, dateString.length() - 1));
						//System.out.println("  date: " + date);
						record.setField(q("date"), date);
	
						String contentType = null;
						boolean createNewVersion = true;
						
						String headerLine;
						do {
							headerLine = LaxHttpParser.readLine(warc, "UTF-8");
							if (headerLine.startsWith("Content-Type: ")) {
								contentType = headerLine.substring(headerLine.indexOf(" ") + 1);
							}
						} while (!headerLine.equals(""));
	
						long length = warc.getHeader().getLength() - warc.getPosition();
						int sizeLimit = Integer.MAX_VALUE - 1024;
						byte[] body = readBytes(warc, length, sizeLimit);
						//System.out.println("  size: " + body.length);
						record.setField(q("size"), body.length);
						
						if (existingRecord != null) {
							DateTime existingDate = (DateTime)existingRecord.getField(q("date"));
							if (existingDate.equals(date))
								createNewVersion = false;
							if (existingRecord.hasField(q("size"))) {
								int existingSize = (int)existingRecord.getField(q("size"));
								if (body.length == existingSize)
									createNewVersion = false;
							}
						}
						
						if (contentType != null) {
							//System.out.println("  contentType: " + contentType);
							record.setField(q("contentType"), contentType);
							if (contentType.startsWith("text/html") && contentType.indexOf('=') != -1) {
								String charset = contentType.substring(contentType.indexOf('=') + 1);
								//System.out.println("  charset: " + charset);
								WebScraper webScraper;
								String domain = url.split("/")[2];
								if (domain.contains(WebScraper.FAZ))
									webScraper = WebScraper.createInstance(WebScraper.FAZ, new String(body, charset));
								else
									webScraper = WebScraper.createInstance(WebScraper.DER_STANDARD, new String(body,
											charset));
								List<String> fieldNames = webScraper.getFieldNames();
	
								for (String fieldName : fieldNames) {
									Object fieldValue = webScraper.getValue(fieldName);
									if (fieldValue != null) {
										//System.out.println("  " + fieldName + ": " + fieldValue);
										record.setField(q(fieldName), fieldValue);
										if (fieldName.equals(WebScraper.ARTICLE_BODY)) {
											if (existingRecord != null && existingRecord.hasField(q(WebScraper.ARTICLE_BODY))) {
												String existingArticleBody = (String)existingRecord.getField(q(WebScraper.ARTICLE_BODY));
												if (fieldValue.equals(existingArticleBody)) {
													System.out.println("  Article body is equivalent!");
													if (webScraper.getFieldNames().contains(WebScraper.POSTINGS)) {
														Record postingsRecord = table.newRecord(id);
														postingsRecord.setField(q(WebScraper.POSTINGS), webScraper.getValue(WebScraper.POSTINGS));
														table.update(postingsRecord);
														Indexer indexer = lilyClient.getIndexer();
														indexer.index(table.getTableName(), postingsRecord.getId());
													}
													createNewVersion = false;
												}
											}
										}
									}
								}
							}
						}
						
						if (createNewVersion) {
							if (body.length > 0) {
								Blob blob = new Blob(contentType, (long) body.length, url);
								try (OutputStream os = table.getOutputStream(blob)) {
									os.write(body);
								}
								record.setField(q("body"), blob);
							}
						
							table.createOrUpdate(record);
							//PrintUtil.print(record, repository);
							Indexer indexer = lilyClient.getIndexer();
							indexer.index(table.getTableName(), record.getId());
						}
	
					}
				}
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static QName q(String name) {
		return new QName("at.ac.ait", name);
	}

	/**
	 * Read the bytes of the record into a buffer and return the buffer. Give a size limit to the buffer to prevent from
	 * exploding memory, but still read all the bytes from the stream even if the buffer is full. This way, the file
	 * position will be advanced to the end of the record.
	 */
	private static byte[] readBytes(ArchiveRecord record, long contentLength, int sizeLimit) throws IOException {
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
		while (((c = record.read(bytes, pos, (bytes.length - pos))) != -1) && pos < bytes.length) {
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
			throw new IOException("Incorrect number of bytes read from ArchiveRecord: expected=" + contentLength
					+ " bytes.length=" + bytes.length + " pos=" + pos + " count=" + count);
		}

		return bytes;
	}
}
