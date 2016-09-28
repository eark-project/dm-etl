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

	private static final String ARTICLE_SELECTOR_1 = "*[itemtype=http://schema.org/Article]";
	private static final String ARTICLE_SELECTOR_2 = "*[itemtype=http://schema.org/NewsArticle]";
	private static final String[] CATEGORY_SELECTORS = {
		"*[itemtype=http://schema.org/BreadcrumbList] *[itemprop=title]",
		"*[itemtype=http://schema.org/BreadcrumbList] ul *[itemprop=name]"};
	private static final String[] HEADLINE_SELECTORS = {
		ARTICLE_SELECTOR_1 + " *[itemprop=headline]",
		ARTICLE_SELECTOR_2 + " *[itemprop=headline]"};
	private static final String[] AUTHOR_SELECTORS = {
		ARTICLE_SELECTOR_1 + " *[itemprop=author] .caps",
		ARTICLE_SELECTOR_2 + " .Autor .caps"};
	private static final String[] DATE_PUBLISHED_SELECTORS = {
		ARTICLE_SELECTOR_1 + " *[itemprop=datePublished]",
		ARTICLE_SELECTOR_2 + " *[itemprop=datePublished]"};
	private static final String[] ARTICLE_BODY_SELECTORS = {
		ARTICLE_SELECTOR_1 + " *[itemprop=articleBody]",
		ARTICLE_SELECTOR_2 + " *[itemprop=articleBody]"};
	private String category;
	private String headline;
	private String author;
	private DateTime datePublished;
	private String articleBody;
	
	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public FazWebScraper(Document document) {
		SiteElement siteElement = new SiteElement(document, CATEGORY_SELECTORS);
		if (!siteElement.getElements().isEmpty())
			siteElement.getElements().remove(0);
		category = siteElement.categoryText();
		headline = new SiteElement(document, HEADLINE_SELECTORS).text();
		author = new SiteElement(document, AUTHOR_SELECTORS).text();
		String dateTimeString = new SiteElement(document, DATE_PUBLISHED_SELECTORS).text();
		if (dateTimeString != null) {
			dateTimeString = dateTimeString.replace(",", "");
			try {
				datePublished = DateTimeFormat.forPattern("dd.MM.yyyy").parseDateTime(dateTimeString);
			} catch (IllegalArgumentException e) {
				System.err.println("Problem while parsing DateTime: " + dateTimeString);
			}
		}
		articleBody = new SiteElement(document, ARTICLE_BODY_SELECTORS).text();
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
		default:
			throw new IllegalArgumentException("Invalid element: " + element);
		}
	}

	@Override
	public List<String> getFieldNames() {
		return Arrays.asList(CATEGORY, HEADLINE, AUTHOR, DATE_PUBLISHED, ARTICLE_BODY);
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
