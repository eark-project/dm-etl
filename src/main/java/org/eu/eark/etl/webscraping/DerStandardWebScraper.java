package org.eu.eark.etl.webscraping;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.jsoup.nodes.Document;

/**
 * extract some specific information from web pages of the domain derstandard.at
 */
public class DerStandardWebScraper extends WebScraper {

	private static final String CATEGORY_SELECTOR = "*[typeof=v:Breadcrumb] a[property=v:title]";
	private static final String HEADLINE_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=headline]";
	private static final String AUTHOR_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=author]";
	private static final String DATE_PUBLISHED_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=datePublished]";
	private static final String ARTICLE_BODY_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=articleBody]";
	private static final String POSTINGS_SELECTOR_OLD = "#forumbarTop .info";
	private static final String POSTINGS_SELECTOR = "#weiterLesenScroll .active.light small";
	private String category;
	private String headline;
	private String author;
	//private DateTime datePublished;
	private String datePublished;
	private String articleBody;
	private String postings;
	
	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Logger logger = Logger.getLogger(DerStandardWebScraper.class);

	public DerStandardWebScraper(Document document) {
		category = new SiteElement(document, CATEGORY_SELECTOR).categoryText();
		headline = new SiteElement(document, HEADLINE_SELECTOR).text();
		author = new SiteElement(document, AUTHOR_SELECTOR).text();
		DateTime dTdatePublished = new SiteElement(document, DATE_PUBLISHED_SELECTOR).textAsDateTime(DateTimeFormat.forPattern(
				"dd. MMMM yyyy, HH:mm").withLocale(Locale.forLanguageTag("de-AT")));
		logger.info("WebScraper retrieved dtdatePlublished: "+dTdatePublished);
		String datePublished = dTdatePublished == null ? "" : sdf.format(dTdatePublished.toDate());
		
		articleBody = new SiteElement(document, ARTICLE_BODY_SELECTOR).text();
		String postingsStringOld = new SiteElement(document, POSTINGS_SELECTOR_OLD).ownText();
		if (postingsStringOld != null) {
			postings = "";
			if (postingsStringOld.contains(" Postings")) {
				postings = ""+Integer.parseInt(postingsStringOld.substring(0, postingsStringOld.indexOf(' ')));
			} else if (postingsStringOld.contains("von ")) {
				postings = ""+Integer.parseInt(postingsStringOld.substring(postingsStringOld.indexOf("von ") + 4));
			}
		} else {
			String postingsString = new SiteElement(document, POSTINGS_SELECTOR).ownText();
			if (postingsString != null)
				postings = ""+Integer.parseInt(postingsString.substring(1, postingsString.length()-1));
		}
	}

	@Override
	public String getValue(String element) {
		switch (element) {
		case CATEGORY:
			return category;
		case HEADLINE:
			return headline;
		case AUTHOR:
			return author;
		case DATE_PUBLISHED:
			return datePublished;
		case ARTICLE_BODY:
			return articleBody;
		case POSTINGS:
			return postings;
		default:
			throw new IllegalArgumentException("Invalid element: " + element);
		}
	}

	@Override
	public List<String> getFieldNames() {
		return Arrays.asList(CATEGORY, HEADLINE, AUTHOR, DATE_PUBLISHED, ARTICLE_BODY, POSTINGS);
	}

}
