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
	
	private static final String ARTICLE_SELECTOR_1 = "*[itemtype=http://schema.org/Article]";
	private static final String ARTICLE_SELECTOR_2 = "*[itemtype=http://schema.org/NewsArticle]";
	private static final String CATEGORY_SELECTOR = "*[typeof=v:Breadcrumb] a[property=v:title]";
	private static final String[] HEADLINE_SELECTORS = {
		ARTICLE_SELECTOR_1 + " *[itemprop=headline]",
		ARTICLE_SELECTOR_2 + " *[itemprop=headline]"};
	private static final String[] AUTHOR_SELECTORS = {
		ARTICLE_SELECTOR_1 + " *[itemprop=author]",
		ARTICLE_SELECTOR_2 + " *[itemprop=author]"};
	private static final String[] DATE_PUBLISHED_SELECTORS = {
		ARTICLE_SELECTOR_1 + " .date",
		ARTICLE_SELECTOR_2 + " .date"};
	private static final String[] ARTICLE_BODY_SELECTORS = {
		ARTICLE_SELECTOR_1 + " *[itemprop=articleBody]",
		ARTICLE_SELECTOR_2 + " *[itemprop=articleBody]"};
	private static final String POSTINGS_SELECTOR_1 = "#forumbarTop .info";
	private static final String POSTINGS_SELECTOR_2 = "#weiterLesenScroll .active.light small";
	private static final String POSTINGS_SELECTOR_3 = ".postings strong";
	
	private String category;
	private String headline;
	private String author;
	private DateTime datePublished;
	private String articleBody;
	private String postings;
	
	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Logger logger = Logger.getLogger(DerStandardWebScraper.class);

	public DerStandardWebScraper(Document document) {
		category = new SiteElement(document, CATEGORY_SELECTOR).categoryText();
		headline = new SiteElement(document, HEADLINE_SELECTORS).text();
		author = new SiteElement(document, AUTHOR_SELECTORS).text();
		datePublished = new SiteElement(document, DATE_PUBLISHED_SELECTORS).textAsDateTime(DateTimeFormat.forPattern(
				"dd. MMMM yyyy, HH:mm").withLocale(Locale.forLanguageTag("de-AT")));		
		articleBody = new SiteElement(document, ARTICLE_BODY_SELECTORS).text();
		String postingsStringOld = new SiteElement(document, POSTINGS_SELECTOR_1).ownText();
		if (postingsStringOld != null) {
			postings = "";
			if (postingsStringOld.contains(" Postings")) {
				postings = ""+Integer.parseInt(postingsStringOld.substring(0, postingsStringOld.indexOf(' ')));
			} else if (postingsStringOld.contains("von ")) {
				postings = ""+Integer.parseInt(postingsStringOld.substring(postingsStringOld.indexOf("von ") + 4));
			}
		} else {
			String postingsString = new SiteElement(document, POSTINGS_SELECTOR_2).ownText();
			if (postingsString != null)
				postings = ""+Integer.parseInt(postingsString.substring(1, postingsString.length()-1));
			else postings = new SiteElement(document, POSTINGS_SELECTOR_3).ownText();
		}
	}

	@Override
	public Object getValue(String element) {
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
	
	@Override
	public String getStringValue(String element) {
		switch (element) {
		case CATEGORY:
			return category;
		case HEADLINE:
			return headline;
		case AUTHOR:
			return author;
		case ARTICLE_BODY:
			return articleBody;
		case POSTINGS:
			return postings;
		default:
			throw new IllegalArgumentException("Invalid element: " + element);
		}
	}

	@Override
	public DateTime getDateValue(String element) {
		switch (element) {
		case DATE_PUBLISHED:
			return datePublished;
		default:
			throw new IllegalArgumentException("Invalid element: " + element);
		}
	}

}
