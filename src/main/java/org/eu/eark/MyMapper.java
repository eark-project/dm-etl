package org.eu.eark;

import org.apache.commons.httpclient.HttpParser;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lilyproject.client.LilyClient;
import org.lilyproject.indexer.Indexer;
import org.lilyproject.mapreduce.LilyMapReduceUtil;
import org.lilyproject.repository.api.*;
import org.lilyproject.util.io.Closer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class MyMapper extends Mapper<Text, Text, Text, Text> {
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
    protected void map(Text key, Text value, Context context)
            throws IOException, InterruptedException {

    	String path = key.toString();
        
    	FSDataInputStream fis = null;
    	
    	try {
	        LTable table = repository.getDefaultTable();
	
	        fis = FileSystem.get( new java.net.URI( path ), context.getConfiguration() ).open( new Path( path ) );
			ArchiveReader reader = ArchiveReaderFactory.get(path, fis, true);
				
			for (ArchiveRecord archiveRecord : reader) {
				WARCRecord warc = (WARCRecord) archiveRecord;
				System.out.println("Mimetype: " + warc.getHeader().getMimetype());
				if (warc.getHeader().getMimetype().equals("application/http; msgtype=response")) {
					
					Record record = table.newRecord();
					record.setId(repository.getIdGenerator().newRecordId());
					record.setRecordType(q("Website"));
					
					String url = warc.getHeader().getUrl();
					System.out.println("  url: " + url);
					record.setField(q("url"), url);
					
					String dateString = warc.getHeader().getDate();
					DateTime date = ISODateTimeFormat.localDateOptionalTimeParser()
							.parseDateTime(dateString.substring(0, dateString.length()-1));
					System.out.println("  date: " + date);
					record.setField(q("date"), date);

					String contentType = null;
					String articleBody = null;
					String headerLine;
					do {
						headerLine = HttpParser.readLine(warc, "UTF-8");
						if (headerLine.startsWith("Content-Type: ")) {
							contentType = headerLine.substring(headerLine.indexOf(" ") + 1);
						}
					} while (!headerLine.equals(""));
					
					long length = warc.getHeader().getLength() - warc.getPosition();
					int sizeLimit = Integer.MAX_VALUE - 1024;
					byte[] body = readBytes(warc, length, sizeLimit);
					System.out.println("  size: " + body.length);
					record.setField(q("size"), body.length);
					
					if (contentType != null) {
						System.out.println("  contentType: " + contentType);
						record.setField(q("contentType"), contentType);
						if (contentType.startsWith("text/html") && contentType.indexOf('=') != -1) {
							String charset = contentType.substring(contentType.indexOf('=')+1);
							System.out.println("  charset: " + charset);
							Document document = Jsoup.parse(new String(body,charset));
							Elements categoryElements = document.select("*[typeof=v:Breadcrumb] a[property=v:title]");
							Elements headlineElements = document.select("*[itemtype=http://schema.org/Article] *[itemprop=headline]");
							Elements authorElements = document.select("*[itemtype=http://schema.org/Article] *[itemprop=author]");
							Elements datePublishedElements = document.select("*[itemtype=http://schema.org/Article] *[itemprop=datePublished]");
							Elements articleBodyElements = document.select("*[itemtype=http://schema.org/Article] *[itemprop=articleBody]");
							Elements postingsElements = document.select("#forumbarTop .info");
							
							if (!categoryElements.isEmpty()) {
								String category = "";
								for (Element element : categoryElements) {
									if (!category.isEmpty()) category += " > ";
									category += element.text();
								}
								System.out.println("  category: " + category);
								record.setField(q("category"), category);
							}
							if (!headlineElements.isEmpty()) {
								String headline = headlineElements.get(0).text();
								System.out.println("  headline: " + headline);
								record.setField(q("headline"), headline);
							}
							if (!authorElements.isEmpty()) {
								String author = authorElements.get(0).text();
								System.out.println("  author: " + author);
								record.setField(q("author"), author);
							}
							if (!datePublishedElements.isEmpty()) {
								String datePublishedString = datePublishedElements.get(0).text();
								DateTime datePublished = DateTimeFormat.forPattern("dd. MMMM yyyy, HH:mm")
										.withLocale(Locale.forLanguageTag("de-AT"))
										.parseDateTime(datePublishedString);
								System.out.println("  datePublished: " + datePublished);
								record.setField(q("datePublished"), datePublished);
							}
							if (!articleBodyElements.isEmpty()) {
								articleBody = articleBodyElements.get(0).text();
								System.out.println("  articleBody: " + articleBody);
								record.setField(q("articleBody"), articleBody);
							}
							if (!postingsElements.isEmpty()) {
								String postingsString = postingsElements.get(0).ownText();
								int postings = 0;
								if (postingsString.contains(" Postings")) {
									postings = Integer.parseInt(postingsString.substring(0, postingsString.indexOf(' ')));
								} else if (postingsString.contains("von ")) {
									postings = Integer.parseInt(postingsString.substring(postingsString.indexOf("von ") + 4));
								}
								System.out.println("  postings: " + postings);
								record.setField(q("postings"), postings);
							}
						}
					}
					
					if (articleBody == null) {
						Blob blob = new Blob(contentType, (long) body.length, url);
						OutputStream os = table.getOutputStream(blob);
						try {
							os.write(body);
						} finally {
							os.close();
						}
						record.setField(q("body"), blob);
					}
					
					table.createOrUpdate(record);
					Indexer indexer = lilyClient.getIndexer();
					indexer.index(table.getTableName(), record.getId());
					
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
	private byte[] readBytes(ArchiveRecord record, long contentLength, int sizeLimit) throws IOException {
		// Ensure the record does strict reading.
		record.setStrict(true);

		sizeLimit = (int) Math.min(sizeLimit, contentLength);

		byte[] bytes = new byte[sizeLimit];

		if (sizeLimit == 0) {
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
