package org.eu.eark.webscraping;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class WebScraperTest {
	
	private WebScraper webScraper;
	private String fieldName;
	private Object expectedResult;

	public WebScraperTest(WebScraper webScraper, String fieldName, Object expectedResult) {
		this.webScraper = webScraper;
		this.fieldName = fieldName;
		this.expectedResult = expectedResult;
	}
	
	@Parameters
	public static List<Object[]> data() throws IOException {
		Document doc = Jsoup.connect("http://derstandard.at/2000003637703/Die-ZiB-zu-Mittag-schweigtHundepunks-nicht-im-Bild").get();
		WebScraper derStandardScraper = new DerStandardWebScraper(doc);
		return Arrays.asList(new Object[][] { { derStandardScraper, WebScraper.CATEGORY, "Etat > TV > TV-Tagebuch" },
				{ derStandardScraper, WebScraper.HEADLINE, "Die \"ZiB\" zu Mittag schweigt: Hundepunks nicht im Bild" },
				{ derStandardScraper, WebScraper.AUTHOR, "Christian Schachinger" },
				{ derStandardScraper, WebScraper.DATE_PUBLISHED, new DateTime(2014, 7, 28, 17, 24, 0, 0) },
				{ derStandardScraper, WebScraper.ARTICLE_BODY, "Facebook-Seite der \"ZiB\": " },
				{ derStandardScraper, WebScraper.POSTINGS, 20 }});
	}
	
	@Test
	public void testNewspaperElement() {
		Object fieldValue = webScraper.getValue(fieldName);
		if (fieldName.equals(WebScraper.ARTICLE_BODY)) {
			assertTrue(((String)fieldValue).startsWith((String)expectedResult));
		} else
			assertEquals(expectedResult, fieldValue);
	}

}
