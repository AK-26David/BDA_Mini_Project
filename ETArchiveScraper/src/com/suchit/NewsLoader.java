package com.suchit;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;

import com.suchit.utils.GetThread;
import com.suchit.utils.HttpClientFactory;
import com.suchit.utils.PostURL;

public class NewsLoader {

	private static final String FWD_SLASH = "/";
	private static final String CMS = ".cms";
	private static final String STARTTIME = ",starttime-";
	private static final String MONTH = ",month-";
	private static final String SITEMAP_PATHS = "/industry,/markets,/news,/personal-finance,/tech,/opinion,/small-biz,/mutual-funds";
	private static final String ECONOMIC_TIMES_SEED = "https://economictimes.indiatimes.com/archivelist/year-";
	private static org.slf4j.Logger log = LoggerFactory.getLogger(NewsLoader.class);
	private static LocalDate etStartDate = LocalDate.of(1900, 1, 1);
	private static final String ECONOMIC_TIMES_HOST = "https://economictimes.indiatimes.com";
	private static final DateTimeFormatter etArticleDateFMT = DateTimeFormatter.ofPattern("MMM dd, yyyy",
			Locale.ENGLISH);
	public static final CloseableHttpClient etHttpClient = HttpClientFactory
			.getHttpClient("https://economictimes.indiatimes.com");
	static ExecutorService execService = Executors.newVirtualThreadPerTaskExecutor();
	private static final String loginURL = "https://jsso.indiatimes.com/sso/crossapp/identity/web/verifyLoginOtpPassword";
	private static final String logoutURL = "https://oauth2.economictimes.indiatimes.com/oauth/api/merchant/ET/token/logout";
	public static String ET_HOST = "https://economictimes.indiatimes.com";
	private static String ticketID;
	CopyOnWriteArrayList<Article> articles = new CopyOnWriteArrayList<Article>();

	public NewsLoader() {
	}

//	MAKE THE CHANGES HERE FOR THE DATE
	private void economicTimesLoader() {
		// Check if categories exists for ET
		var hrefsToCrawl = loadEconomicTimesCategories();
		LocalDate startDate = LocalDate.of(2024, 9, 01);
		long startDateInt;
		LocalDate endDate = LocalDate.of(2024, 9, 30);
		startDateInt = ChronoUnit.DAYS.between(etStartDate, startDate) + 1;
		while (startDate.isBefore(endDate)) {
			startDateInt += 1;
			log.error("Loading ET articles for date {}", startDate);
			List<Article> articleList = new ArrayList<>();
			String archiveListURL = ECONOMIC_TIMES_SEED + startDate.getYear() + MONTH + startDate.getMonth().getValue()
					+ STARTTIME + startDateInt + CMS;
			GetThread getURL = new GetThread(etHttpClient, archiveListURL);
			String response = getURL.executeGetString();
			if (response == null) {
				startDate = startDate.plusDays(1);
				continue;
			}
			Document doc = Jsoup.parse(response);
			Element content = doc.getElementById("pageContent");
			if (content == null) {
				startDate = startDate.plusDays(1);
				continue;
			}
			Elements articleUrlTable = content.getElementsByClass("content");
			if (articleUrlTable.isEmpty())
				continue;
			Elements articleUrls = articleUrlTable.get(0).select("a[href]");
			// Use for compile error in parallel stream
			final LocalDate startDateforStream = startDate;
			articleUrls.forEach(element -> {
				try {
					String href = element.attr("href");
					boolean matched = false;
					for (String urlToCrawl : hrefsToCrawl) {
						if (!href.contains(urlToCrawl)) {
							continue;
						}
						matched = true;
						break;
					}
					if (!matched)
						return;
//					log.debug("Loading arcticle {}" , href);
					String[] urlIdArr = StringUtils.split(href, FWD_SLASH);
					String urlId = urlIdArr[urlIdArr.length - 1];
					String urlHash;
					try {
						urlHash = urlId.substring(0, urlId.lastIndexOf("."));
					} catch (Exception e) { // If does not point to specific article then skip
//					log.error("Skiping for {}", element.attr("href"));
						return;
					}
					String[] urlCats = StringUtils
							.split(href.substring(ECONOMIC_TIMES_HOST.length() + 1, href.length()), FWD_SLASH);
					// Apply selective filter as needed
//					if (!canConsumeUrl(href)) {
//						return;
//					}
					Article article = new Article();
					article.setUrl(href);
					article.setId(urlHash);
					article.setDate(startDateforStream);
					articleList.add(article);
				} catch (Exception e) {
					log.error("Exception {}", e);
				}
			});
			articleList.parallelStream().forEach(article -> {
//			articleList.stream().forEach(article -> {
				GetThread getArticle = new GetThread(etHttpClient, article.getUrl());
				final String articleResponse = getArticle.executeGetString();
				parseETArticleInfo(article, articleResponse);
				if (article.getTitle() != null) {
					articles.add(article);
				}
			});
			startDate = startDate.plusDays(1);
		}
	}

	private List<String> loadEconomicTimesCategories() {
		List<String> hrefsToCrawl = new ArrayList<>();
		Arrays.asList(SITEMAP_PATHS.split(",")).forEach(path -> {
			try {
				GetThread getURL = new GetThread(etHttpClient, ECONOMIC_TIMES_HOST + path);
				String response = getURL.executeGetString();
				if (getURL.statusCode() != 200) {
					log.error("Categories could not be fetched for {}", path);
					return;
				}
				Document doc = Jsoup.parse(response);
				Element content = doc.getElementById("subnav");
				if (content == null) {
					log.error("Categories could not be fetched. Response is null");
					return;
				}
				Elements links = content.select("a[href]");
				links.forEach(link -> {
					String href = link.attr("href");
					if (href.length() <= ECONOMIC_TIMES_HOST.length())
						return;
					href = href.substring(ECONOMIC_TIMES_HOST.length(), href.length());
					// Hack to add interview categetory under opinion for older articles. speaking
					// tree is skipped anyways
					if (href.contains("opinion/speaking-tree")) {
						href = href.replace("speaking-tree", "interviews");
					}
					// Apply selective URL filter pattern
//					if (canConsumeUrl(href)) {
					hrefsToCrawl.add(href);
//					}
				});
			} catch (Exception e) {
				log.error("{}", e);
			}
		});
		return hrefsToCrawl;
	}

	private boolean canConsumeUrl(String href) {
		return (href.startsWith("/markets") || href.startsWith("/news") || href.startsWith("/industry")
				|| href.startsWith("/small-biz") || href.startsWith("/personal-finance")
				|| href.startsWith("/mutual-funds") || href.startsWith("/tech") || href.startsWith("/opinion")
//			|| href.startsWith("/nri")				
				|| href.startsWith("/defence")
//			|| href.startsWith("/wealth")			
				|| href.startsWith("/mf"))
				&& !(href.contains("marketstats") || href.contains("stock-quotes")
						|| href.contains("stock-market-holiday-calendar") || href.contains("live-coverage")
						|| href.contains("technical-charts") || href.contains("magazines") || href.contains("politics")
						|| href.contains("etmarkets-podcasts") || href.contains("elections")
						|| href.contains("latest-news") || href.contains("calculators")
						|| href.contains("ifsc-bank-code") || href.contains("interest-rates")
						|| href.contains("etwealth") || href.contains("nps-national-pension-scheme")
						|| href.contains("aadhaar-card") || href.contains("currency-converter")
						|| href.contains("newspaper-subscription") || href.contains("speaking-tree")
						|| href.contains("bliss-of") || href.contains("sports") || href.contains("/mf/learn")
						|| href.contains("aqi-delhi") || href.contains("mukesh-ambani") || href.contains("dons-of")
						|| href.contains("expert-bio") || href.contains("etmarkets-live")
						|| href.contains("hr-leadership") || href.contains("newsletters")
						|| href.contains("multimedialist") || href.contains("best-mutual-funds")
						|| href.contains("stock-screener") || href.contains("international")
						|| href.contains("candlestick-screener") || href.contains("cryptocurrency")
						|| href.contains("newslist"));
	}

	private void parseETArticleInfo(Article article, final String responseArticle) {
		try {
			if (responseArticle == null) {
				log.error("Response null for {}", article.getUrl());
				return;
			}
			Document doc = Jsoup.parse(responseArticle);
			Elements articleElementList = doc.getElementsByClass("article_wrap");
			if (articleElementList.isEmpty()) {
				log.error("No element list uner article_Wrap. No Article");
				return;
			}
			// No Date id etc return
			if (articleElementList.get(0).getAllElements().isEmpty()) {
				log.error("No id. No Article");
				return;
			}
			Element articleElement = articleElementList.get(0).getAllElements().get(0);
			Element articleBlock = articleElementList.get(0).getAllElements().get(1);
			// Match if date is correct or not
			if (!articleBlock.attr("data-artdate").isBlank()) {
				LocalDate articleDate = LocalDate.parse(articleBlock.attr("data-artdate").subSequence(0, 12),
						etArticleDateFMT);
				if (!article.getDate().isEqual(articleDate)) {
					log.error("Article Date does not matches for {} , {} , {}", articleDate, article.getDate(),
							article.getUrl());
					return; // Error in ET page listing . This article will get created on next date.
				}
			}
			// Match if article id is correct or not
			if (!articleBlock.attr("data-article_id").isBlank()) {
				if (!article.getId().equals(articleBlock.attr("data-article_id")))
					log.error("Article ID does not matche for {} ", article.getUrl());
			}
			String articleTitle = doc.head().getElementsByTag("title").getFirst().text();
			String articleDesc = doc.head().selectFirst("meta[name=description]").attr("content");
			String articleKeywords = doc.head().selectFirst("meta[name=keywords]").attr("content");
			if (articleTitle.isEmpty()) {
				articleTitle = articleBlock.attr("data-arttitle");
			}
			if (articleDesc.isEmpty()) {
				// Summary changes with articles
				if (!articleElementList.get(0).getElementsByClass("summary").isEmpty()) {
					articleDesc = articleElementList.get(0).getElementsByClass("summary").get(0).text();
				} else if (!articleElement.getElementsByTag("artsummary").isEmpty()) {
					articleDesc = articleElement.getElementsByTag("artsummary").get(0).text();
				} else {
					log.error("Summary not avaialble for {}", ECONOMIC_TIMES_HOST + article.getUrl());
					articleDesc = articleTitle;
				}
			}
			Elements artTexts = articleElement.getElementsByTag("article");
			if (!artTexts.isEmpty()) {
				String mainArticle = artTexts.get(0).getElementsByClass("artText").get(0).wholeText();
				// Body has lot of junk and not ready for consumption by Text analysis parsers.
				// Need to clean up so each sentence is in a new line which can be used for text
				// analysis per line
				String[] ss = mainArticle.split("\\r?\\n");
				StringBuffer buf = new StringBuffer();
				for (int i = 0; i < ss.length; i++) {
					String node = ss[i];
					node = node.trim();
					if (node.isBlank())
						continue;
					// skip junk lines. Add more string matching
					if (node.contains("The views and recommendations"))
						continue;
					if (i == ss.length - 1) {
						if (node.contains("(You can now "))
							continue;
					}
					buf.append(node);
					// Required to avoid OOM by OpenNLP due very large sentence.
					if (node.charAt(node.length() - 1) != '.' && node.charAt(node.length() - 1) != '?'
							&& node.charAt(node.length() - 1) != '!') {
						buf.append(".");
					}
					buf.append(System.lineSeparator());
				}
				if (buf.isEmpty()) {
					log.error("Body with no content for {}", article.getId());
					return;
				}
				// Need one object per thread
				article.setTitle(articleTitle);
				article.setDescription(articleDesc);
				article.setBody(buf.toString());
				article.setKeywords(articleKeywords);
			} else {
				log.error("Body is empty for {}", article.getId());
				return;
			}
		} catch (Exception e) {
			log.error("Article {} could not be parsed {} ", article.getUrl(), e);
		}
	}

	public static boolean login() {
		StringEntity requestEntity = new StringEntity("{\"email\":\"v_vivekg@yahoo.com\",\"password\":\"vivek@et24\"}",
				ContentType.APPLICATION_JSON);
		PostURL getEquitySymbols = new PostURL(etHttpClient, loginURL);
		getEquitySymbols.getHttpPost().addHeader("Channel", "et");
		getEquitySymbols.getHttpPost().addHeader("isjssocrosswalk", "true");
		getEquitySymbols.getHttpPost().addHeader("Platform", "WEB");
		getEquitySymbols.getHttpPost().addHeader("Origin", ET_HOST);
		getEquitySymbols.getHttpPost().addHeader("Referer", ET_HOST);
		getEquitySymbols.setEntity(requestEntity);
		byte[] response = getEquitySymbols.executePost();
		ticketID = new JSONObject(new String(response)).getJSONObject("data").getString("ticketId");
		if (getEquitySymbols.getStatusCode() != HttpStatus.SC_OK) {
			return false;
		}
		return true;
	}

	public static void logout() {
		StringEntity requestEntity = new StringEntity("{\"ticketId\":\"" + ticketID + "\"}",
				ContentType.APPLICATION_JSON);
		PostURL getEquitySymbols = new PostURL(etHttpClient, logoutURL);
		getEquitySymbols.getHttpPost().addHeader("Channel", "et");
		getEquitySymbols.getHttpPost().addHeader("isjssocrosswalk", "true");
		getEquitySymbols.getHttpPost().addHeader("Platform", "WEB");
		getEquitySymbols.getHttpPost().addHeader("Origin", ET_HOST);
		getEquitySymbols.getHttpPost().addHeader("Referer", ET_HOST);
		getEquitySymbols.setEntity(requestEntity);
		getEquitySymbols.executePost();
	}

	public static void main(String[] args) throws Exception {
		try {
			System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "6");
			NewsLoader crawl = new NewsLoader();
			if(login()){
				
				// MAKE THE CHANGES FOR DATE HERE
				
				
				crawl.economicTimesLoader();
				logout();
				execService.shutdown();
				try {
					execService.awaitTermination(1, TimeUnit.HOURS);
				} catch (InterruptedException e) {
				}
			}
			
			FileWriter outputfile = new FileWriter("news.csv");
	        CSVWriter writer = new CSVWriter(outputfile);
	        
	        String[] head = {"Date", "Body", "Title", "Description"};
	        writer.writeNext(head);
	        
			// do something useful with articles
	        crawl.articles.forEach(article -> {
	        	String[] row = {article.getDate().toString(), article.getBody(), article.getTitle(), article.getDescription()};
	        	writer.writeNext(row);  // Convert to String array for CSVWriter
		        }
			);
			
			writer.close();
			
		} catch (Exception e) {
			log.error("{}", e);
		}
	}
}
