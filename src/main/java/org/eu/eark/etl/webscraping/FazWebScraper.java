package org.eu.eark.etl.webscraping;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.jsoup.nodes.Document;

/**
 * extract some specific information from web pages of the domain faz.net
 */
public class FazWebScraper extends WebScraper {

	private static final String CATEGORY_SELECTOR = "*[itemtype=http://data-vocabulary.org/Breadcrumb] *[itemprop=title]";
	private static final String HEADLINE_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=headline]";
	private static final String AUTHOR_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=author] .caps";
	private static final String DATE_PUBLISHED_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=datePublished]";
	private static final String ARTICLE_BODY_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=articleBody]";
	private String category;
	private String headline;
	private String author;
	private String datePublished;
	private String articleBody;
	
	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public FazWebScraper(Document document) {
		SiteElement siteElement = new SiteElement(document, CATEGORY_SELECTOR);
		if (!siteElement.getElements().isEmpty())
			siteElement.getElements().remove(0);
		category = siteElement.categoryText();
		headline = new SiteElement(document, HEADLINE_SELECTOR).text();
		author = new SiteElement(document, AUTHOR_SELECTOR).text();
		String dateTimeString = new SiteElement(document, DATE_PUBLISHED_SELECTOR).text();
		if (dateTimeString != null) {
			dateTimeString = dateTimeString.replace(",", "");
			try {
				DateTime dTdatePublished = DateTimeFormat.forPattern("dd.MM.yyyy").parseDateTime(dateTimeString);
				datePublished = sdf.format(dTdatePublished.toDate());
			} catch (IllegalArgumentException e) {
				System.err.println("Problem while parsing DateTime: " + dateTimeString);
			}
		}
		articleBody = new SiteElement(document, ARTICLE_BODY_SELECTOR).text();
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
		default:
			throw new IllegalArgumentException("Invalid element: " + element);
		}
	}

	@Override
	public List<String> getFieldNames() {
		return Arrays.asList(CATEGORY, HEADLINE, AUTHOR, DATE_PUBLISHED, ARTICLE_BODY);
	}

}
