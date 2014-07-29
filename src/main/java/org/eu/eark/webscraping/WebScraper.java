package org.eu.eark.webscraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public abstract class WebScraper {

	public static final String CATEGORY = "category";
	public static final String HEADLINE = "headline";
	public static final String AUTHOR = "author";
	public static final String DATE_PUBLISHED = "datePublished";
	public static final String ARTICLE_BODY = "articleBody";
	public static final String POSTINGS = "postings";
	public static final String DER_STANDARD = "derstandard.at";
	public static final String FAZ = "www.faz.net";

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

	public abstract Object getValue(String headline);

}
