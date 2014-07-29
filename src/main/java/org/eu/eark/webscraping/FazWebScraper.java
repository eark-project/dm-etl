package org.eu.eark.webscraping;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.jsoup.nodes.Document;

public class FazWebScraper extends WebScraper {

	private static final String CATEGORY_SELECTOR = "*[itemtype=http://data-vocabulary.org/Breadcrumb] *[itemprop=title]";
	private static final String HEADLINE_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=headline]";
	private static final String AUTHOR_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=author] .caps";
	private static final String DATE_PUBLISHED_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=datePublished]";
	private static final String ARTICLE_BODY_SELECTOR = "*[itemtype=http://schema.org/Article] *[itemprop=articleBody]";
	private static final String POSTINGS_SELECTOR = "#forumbarTop .info";
	private String category;
	private String headline;
	private String author;
	private DateTime datePublished;
	private String articleBody;
	private Integer postings;

	public FazWebScraper(Document document) {
		SiteElement siteElement = new SiteElement(document, CATEGORY_SELECTOR);
		if (!siteElement.getElements().isEmpty())
			siteElement.getElements().remove(0);
		category = siteElement.categoryText();
		headline = new SiteElement(document, HEADLINE_SELECTOR).text();
		author = new SiteElement(document, AUTHOR_SELECTOR).text();
		datePublished = new SiteElement(document, DATE_PUBLISHED_SELECTOR)
			.textAsDateTime(DateTimeFormat.forPattern("dd.MM.yyyy,"));
		articleBody = new SiteElement(document, ARTICLE_BODY_SELECTOR).text();
		String postingsString = new SiteElement(document, POSTINGS_SELECTOR).ownText();
		if (postingsString != null) {
			postings = 0;
			if (postingsString.contains(" Postings")) {
				postings = Integer.parseInt(postingsString.substring(0, postingsString.indexOf(' ')));
			} else if (postingsString.contains("von ")) {
				postings = Integer.parseInt(postingsString.substring(postingsString.indexOf("von ") + 4));
			}
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

}
