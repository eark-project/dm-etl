package org.eu.eark.etl.webscraping;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * convenience wrapper for jsoup library
 */
public class SiteElement {

	private Elements elements;
	
	public SiteElement(Document document, String cssSelector) {
		elements = document.select(cssSelector);
	}

	public String text() {
		if (getElements().isEmpty()) return null;
		return getElements().get(0).text();
	}
	
	public String ownText() {
		if (getElements().isEmpty()) return null;
		return getElements().get(0).ownText();
	}
	
	public String attr(String key) {
		if (getElements().isEmpty()) return null;
		return getElements().get(0).attr(key);
	}

	public String categoryText() {
		if (getElements().isEmpty()) return null;
		String category = "";
		for (Element element : getElements()) {
			if (!category.isEmpty()) category += " > ";
			category += element.text();
		}
		return category;
	}

	public DateTime textAsDateTime(DateTimeFormatter dateTimeFormatter) {
		if (getElements().isEmpty()) return null;
		String dateTimeString = getElements().get(0).text();
		DateTime dateTime = null;
		try {
			dateTime = dateTimeFormatter.parseDateTime(dateTimeString);
		} catch (IllegalArgumentException e) {
			System.err.println("Problem while parsing DateTime: " + dateTimeString);
		}
		return dateTime;
	}

	public Elements getElements() {
		return elements;
	}

}
