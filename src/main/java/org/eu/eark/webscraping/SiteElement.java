package org.eu.eark.webscraping;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SiteElement {

	private Elements elements;
	
	public SiteElement(Document document, String cssSelector) {
		elements = document.select(cssSelector);
	}

	public String text() {
		if (elements.isEmpty()) return null;
		return elements.get(0).text();
	}
	
	public String ownText() {
		if (elements.isEmpty()) return null;
		return elements.get(0).ownText();
	}

	public String categoryText() {
		if (elements.isEmpty()) return null;
		String category = "";
		for (Element element : elements) {
			if (!category.isEmpty()) category += " > ";
			category += element.text();
		}
		return category;
	}

	public DateTime textAsDateTime(DateTimeFormatter dateTimeFormatter) {
		if (elements.isEmpty()) return null;
		String dateTimeString = elements.get(0).text();
		DateTime dateTime = null;
		try {
			dateTime = dateTimeFormatter.parseDateTime(dateTimeString);
		} catch (IllegalArgumentException e) {
			System.err.println("Problem while parsing DateTime: " + dateTimeString);
		}
		return dateTime;
	}

}
