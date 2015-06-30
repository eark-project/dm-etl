package org.eu.eark.webscraping;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eu.eark.etl.webscraping.DerStandardWebScraper;
import org.eu.eark.etl.webscraping.FazWebScraper;
import org.eu.eark.etl.webscraping.WebScraper;
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
		Document derStandardDoc = Jsoup.connect("http://derstandard.at/2000003637703/Die-ZiB-zu-Mittag-schweigtHundepunks-nicht-im-Bild").get();
		Document fazDoc = Jsoup.connect("http://www.faz.net/aktuell/feuilleton/kunstmarkt/raubgrabungen-die-wandernden-helme-aus-aragonien-13040696.html").get();
		WebScraper derStandardScraper = new DerStandardWebScraper(derStandardDoc);
		WebScraper fazScraper = new FazWebScraper(fazDoc);
		return Arrays.asList(new Object[][] { { derStandardScraper, WebScraper.CATEGORY, "Etat > TV > Fernsehkritik: TV-Tagebuch" },
				{ derStandardScraper, WebScraper.HEADLINE, "Die \"ZiB\" zu Mittag schweigt: Hundepunks nicht im Bild" },
				{ derStandardScraper, WebScraper.AUTHOR, "Christian Schachinger" },
				{ derStandardScraper, WebScraper.DATE_PUBLISHED, new DateTime(2014, 7, 28, 17, 24, 0, 0) },
				{ derStandardScraper, WebScraper.ARTICLE_BODY, "Facebook-Seite der \"ZiB\": " },
				{ derStandardScraper, WebScraper.POSTINGS, null },
				{ fazScraper, WebScraper.CATEGORY, "Feuilleton > Kunstmarkt" },
				{ fazScraper, WebScraper.HEADLINE, "Raubgrabungen Die wandernden Helme aus Aragonien" },
				{ fazScraper, WebScraper.AUTHOR, "Clementine Kügler, Madrid" },
				{ fazScraper, WebScraper.DATE_PUBLISHED, new DateTime(2014, 7, 11, 0, 0, 0, 0) },
				{ fazScraper, WebScraper.ARTICLE_BODY, "\u00a9 INTERFOTO Seit Jahren kommt es in Spanien zu Plünderungen archäologischer Funde." }});
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
