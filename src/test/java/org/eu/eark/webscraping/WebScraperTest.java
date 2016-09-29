package org.eu.eark.webscraping;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
		WebScraper derStandardScraper14 = getOldWebScraper(WebScraper.DER_STANDARD, "derStandard_201408.html");
		WebScraper derStandardScraper16 = getOldWebScraper(WebScraper.DER_STANDARD, "derStandard_201609.html");
		WebScraper fazScraper14 = getOldWebScraper(WebScraper.FAZ, "faz_201408.html");
		WebScraper fazScraper16 = getOldWebScraper(WebScraper.FAZ, "faz_201609.html");
		Document derStandardDoc = Jsoup.connect("http://derstandard.at/2000014628073/Kuenstler-als-Bodenbereiter-fuer-den-Fuehrerkult").get();
		Document fazDoc = Jsoup.connect("http://www.faz.net/aktuell/politik/wahl-in-amerika/nach-der-tv-debatte-trump-plant-strategiewechsel-gegenueber-clinton-14457160.html").get();
		WebScraper derStandardScraper = new DerStandardWebScraper(derStandardDoc);
		WebScraper fazScraper = new FazWebScraper(fazDoc);
		Article s1 = new Article(
			"Etat > TV > TV-Tagebuch",
			"Die \"ZiB\" zu Mittag schweigt: Hundepunks nicht im Bild",
			"Christian Schachinger",
			new DateTime(2014, 7, 28, 17, 24, 0, 0),
			"Facebook-Seite der \"ZiB\":",
			"23");
		Article s2 = new Article(
			"Kultur > Bildende Kunst",
			"Künstler als Bodenbereiter für den Führerkult",
			"Alexander Kluy aus Frankfurt",
			new DateTime(2015, 4, 21, 17, 53, 0, 0),
			"Unter dem Titel",
			"2");
		Article f1 = new Article(
			"Feuilleton > Kunstmarkt",
			"Raubgrabungen Die wandernden Helme aus Aragonien",
			"Clementine Kügler, Madrid",
			new DateTime(2014, 7, 11, 0, 0, 0, 0),
			"\u00a9 INTERFOTO Seit Jahren kommt es in Spanien zu Plünderungen archäologischer Funde.");
		Article f2 = new Article(
			"Politik > Wahl in Amerika",
			"Nach TV-Debatte mit Clinton Trump plant Strategiewechsel ",
			"Anna-Lena Ripperger",
			new DateTime(2016, 9, 28, 0, 0, 0, 0),
			"$stleKurz To view this video");
		List<Object[]> data = articleData(derStandardScraper14, s1);
		data.addAll(articleData(derStandardScraper16, s2));
		data.addAll(articleData(derStandardScraper, s2));
		data.addAll(articleData(fazScraper14, f1));
		data.addAll(articleData(fazScraper16, f2));
		data.addAll(articleData(fazScraper, f2));
		return data;
	}
	
	private static List<Object[]> articleData(WebScraper s, Article a) {
		List<Object[]> data = new ArrayList<>();
		data.add(new Object[]{ s, WebScraper.CATEGORY, a.category });
		data.add(new Object[]{ s, WebScraper.HEADLINE, a.headline });
		data.add(new Object[]{ s, WebScraper.AUTHOR, a.author });
		data.add(new Object[]{ s, WebScraper.DATE_PUBLISHED, a.datePublished });
		data.add(new Object[]{ s, WebScraper.ARTICLE_BODY, a.articleBody });
		if (a.postings != null) {
			Object[] pData = { s, WebScraper.POSTINGS, a.postings };
			data.add(pData);
		}
		return data;
	}

	public static class Article {
		public String category;
		public String headline;
		public String author;
		public DateTime datePublished;
		public String articleBody;
		public String postings;
		public Article(String category, String headline, String author, DateTime datePublished, String articleBody) {
			this.category = category;
			this.headline = headline;
			this.author = author;
			this.datePublished = datePublished;
			this.articleBody = articleBody;
		}
		public Article(String category, String headline, String author, DateTime datePublished, String articleBody, String postings) {
			this.category = category;
			this.headline = headline;
			this.author = author;
			this.datePublished = datePublished;
			this.articleBody = articleBody;
			this.postings = postings;
		}
		public Article withBody(String articleBody) {
			return new Article(category, headline, author, datePublished, articleBody, postings);
		}
		public Article withPostings(String postings) {
			return new Article(category, headline, author, datePublished, articleBody, postings);
		}
	}
	
	public static WebScraper getOldWebScraper(String type, String filename) throws IOException {
		String resourceFilename = WebScraperTest.class.getResource("/" + filename).getFile();
		return WebScraper.createInstance(type, new String(Files.readAllBytes(Paths.get(resourceFilename))));
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
