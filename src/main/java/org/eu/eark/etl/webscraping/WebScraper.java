package org.eu.eark.etl.webscraping;

import java.util.List;

import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * extract some specific information from html code
 */
public abstract class WebScraper {

	public static final String CATEGORY = "str_category";
	public static final String HEADLINE = "str_headline";
	public static final String AUTHOR = "str_author";
	public static final String DATE_PUBLISHED = "str_datePublished";
	public static final String ARTICLE_BODY = "str_articleBody";
	public static final String POSTINGS = "int_postings";
	public static final String DER_STANDARD = "derstandard.at";
	public static final String FAZ = "faz.net";

	public static WebScraper createInstance(String type, String site) {
		Document document = Jsoup.parse(site);
		switch (type) {
		case DER_STANDARD:
			return new DerStandardWebScraper(document);
		case FAZ:
			return new FazWebScraper(document);
		default:
			throw new IllegalArgumentException("Invalid type: " + type);
		}
	}

	public abstract Object getValue(String element);
	
	public abstract String getStringValue(String element);
	
	public abstract DateTime getDateValue(String element);

	public abstract List<String> getFieldNames();

}
