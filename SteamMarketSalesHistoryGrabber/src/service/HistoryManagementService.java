package service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebEngine;
import model.ConsoleLineSeparator;
import model.ConvertedResponse;
import model.FileOperations;
import model.HoverItem;
import model.JSONValidator;
import model.LogFlow;
import model.MarketListing;

public class HistoryManagementService implements FileOperations, JSONValidator, ConsoleLineSeparator {
	
	private final static String STEAM_MARKET_HISTORY_URL, FILE_SAVE_PATH;	
	private static final int NUMBER_OF_THREADS, BREAK_BETWEEN_MARKET_PARSING, MAX_REDOWNLOAD_ATTEMPTS;
	
	static {
		STEAM_MARKET_HISTORY_URL = "https://steamcommunity.com/market/myhistory/render/?query=&start=";
		FILE_SAVE_PATH = "data/market_history_";
		// NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors();
		NUMBER_OF_THREADS = 1;
		BREAK_BETWEEN_MARKET_PARSING = 15;
		MAX_REDOWNLOAD_ATTEMPTS = 5;
	}
	
	private final int marketListingsAmountPerRequest;
	private ScheduledExecutorService threadPool = null;
	private AtomicInteger marketTransactionsParsedCounter, failedAttemptsCounter;
	private AtomicReference<String> grabbedMarketHistoryData;
	private final SimpleDateFormat dateFormat;
	private CountDownLatch countDownLatch;
	private DecimalFormatSymbols symbols;
	private DecimalFormat df;
	private final String lineSeparator;
	
	public HistoryManagementService() {
		marketListingsAmountPerRequest = 100;
		dateFormat = new SimpleDateFormat("MM-dd-YYYY_HH-mm-ss");
		countDownLatch = null;
		lineSeparator = produceSeparator("\u2500", 100);
		
		// set number formatter
		symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator(' ');
		df = new DecimalFormat();
		df.setDecimalFormatSymbols(symbols);
		df.setGroupingSize(3);
		df.setMaximumFractionDigits(0);
	}
	
	public AtomicReference<String> getGrabbedMarketHistoryData() {
		return grabbedMarketHistoryData;
	}
	
	public CountDownLatch getCountDownLatch() {
		if(countDownLatch != null) {
			return countDownLatch;
		} else {
			throw new IllegalStateException("CountDownLatch is not initialized!");
		}
	}

	public void marketHistoryParser(WebEngine signInThroughSteamWebEngine, final int marketTransactionsNumber, final int marketTransactionsOffset, Button stopMarketHistoryParserButton, TextFlow logTextFlow) {
		if(marketTransactionsNumber > marketTransactionsOffset) {
			threadPool = Executors.newScheduledThreadPool(NUMBER_OF_THREADS);
			marketTransactionsParsedCounter = new AtomicInteger(marketTransactionsOffset);
			failedAttemptsCounter = new AtomicInteger(0);
			grabbedMarketHistoryData = new AtomicReference<>();
			
			LogFlow.LOG.addTextToTextFlow(lineSeparator, 1, logTextFlow);
			
			threadPool.scheduleAtFixedRate(() -> {
				countDownLatch = new CountDownLatch(NUMBER_OF_THREADS);
				String connectionURL = STEAM_MARKET_HISTORY_URL + marketTransactionsParsedCounter.get() + "&count=" + marketListingsAmountPerRequest;
				LogFlow.LOG.addTextToTextFlow("Parsing data: " + df.format(marketTransactionsParsedCounter.get() + marketListingsAmountPerRequest) + " out of: " + df.format(marketTransactionsNumber) + "\n", 1, logTextFlow);
				LogFlow.LOG.addTextToTextFlow("Connecting to remote source: " + connectionURL + "\n", 1, logTextFlow);
				
				Platform.runLater(() -> {
					signInThroughSteamWebEngine.load(connectionURL);
				});
				
				try {
					countDownLatch.await();
					ConvertedResponse convertedData = convertHTMLToJSON(grabbedMarketHistoryData.get(), logTextFlow);
					
					if(convertedData.getConvertedResponse() != null) {
						String filePath = FILE_SAVE_PATH + marketTransactionsParsedCounter.get() + "_" + dateFormat.format(Calendar.getInstance().getTime()) + ".json";
						writeFileWithLock(filePath, convertedData.getConvertedResponse());
						String[] splittedFilePath = filePath.split("/");
						LogFlow.LOG.addTextToTextFlow("Market transactions history in a number of " + convertedData.getTransactionsListSize() + " was saved successfully to file: " + splittedFilePath[splittedFilePath.length - 1] + "\n", 2, logTextFlow);
						marketTransactionsParsedCounter.set(marketTransactionsParsedCounter.get() + marketListingsAmountPerRequest);
						
						if(marketTransactionsParsedCounter.get() >= marketTransactionsNumber) {
							LogFlow.LOG.addTextToTextFlow(lineSeparator, 1, logTextFlow);
							LogFlow.LOG.addTextToTextFlow("Data parsing has been successfully completed.\n", 2, logTextFlow);
							
							Platform.runLater(() -> {
								stopMarketHistoryParserButton.fire();
							});
						} else {					
							failedAttemptsCounter.set(0);
						}
					} else {
						LogFlow.LOG.addTextToTextFlow("There were some errors while downloading market transaction history. Program will try to download data again.\n", 0, logTextFlow);
					}
					
					LogFlow.LOG.addTextToTextFlow(lineSeparator, 1, logTextFlow);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}					
			}, 0, BREAK_BETWEEN_MARKET_PARSING, TimeUnit.SECONDS);
		} else {
			LogFlow.LOG.addTextToTextFlow("Offset value is equal or greater than the number of Steam Marketplace transactions. Parser data processing has been aborted.\n", 0, logTextFlow);
			
			Platform.runLater(() -> {
				stopMarketHistoryParserButton.fire();
			});
		}
	}
	
	public void stopMarketHistoryParser() {
		if(threadPool != null) {
			threadPool.shutdown();
			
			try {
				if(!threadPool.awaitTermination(1, TimeUnit.NANOSECONDS)) {
					threadPool.shutdownNow();
				}
			} catch(InterruptedException e) {
				threadPool.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}		
	}

	private ConvertedResponse convertHTMLToJSON(String htmlToConvert, TextFlow logTextFlow) {
		if(!isJSON(htmlToConvert)) {
			return null;
		}
		
		if(!isValidJSON(htmlToConvert)) {
			return null;
		}
		
		JSONObject jsonData = new JSONObject(htmlToConvert);
		
		if(jsonData.isNull("total_count")) {
			if(failedAttemptsCounter.get() <= MAX_REDOWNLOAD_ATTEMPTS) {
				failedAttemptsCounter.incrementAndGet();
				return null;
			} else {
				stopMarketHistoryParser();
				LogFlow.LOG.addTextToTextFlow("Cannot connect to remote source. Maximum number of reconnections (" + MAX_REDOWNLOAD_ATTEMPTS + ") has been reached. Parser has been stopped.\n", 0, logTextFlow);
				throw new IllegalArgumentException("Cannot connect to remote source.");
			}
		}
		
		String htmlContent = removeSpecialCharactersFromString(jsonData.getString("results_html"));
		Document doc = Jsoup.parse(htmlContent);
		Elements marketListingElements = doc.getElementsByClass("market_listing_row market_recent_listing_row");
		List<MarketListing> marketListingsList = new ArrayList<>();
		
		for(Element el : marketListingElements) {
			if(el.select("div.market_listing_right_cell.market_listing_whoactedwith > div.market_listing_whoactedwith_name_block").first() != null) {
				String[] listedDateCombined = el.getElementsByClass("market_listing_listed_date_combined").get(0).text().split(":");
				String[] whoactedwithNameBlock = el.getElementsByClass("market_listing_whoactedwith_name_block").get(0).text().split(":");
				String gameListingName = "";
				
				if(whoactedwithNameBlock.length > 1) {
					gameListingName = whoactedwithNameBlock[1].substring(1);
				}
				
				marketListingsList.add(new MarketListing(el.attr("id"), 
					el.getElementsByClass("market_listing_item_name").get(0).text(), 
					el.getElementsByClass("market_listing_game_name").get(0).text(), 
					el.getElementsByClass("market_listing_price").get(0).text(), 
					listedDateCombined[0], 
					gameListingName, 
					el.getElementsByClass("playerAvatar").get(0).select("a").first().attr("href"), 
					listedDateCombined[1].substring(1)));
			}
		}
		
		if(!marketListingsList.isEmpty()) {
			String[] hoversContent = removeSpecialCharactersFromString(jsonData.getString("hovers")).split(";");
			Set<HoverItem> matchedHoverItemsSet = new LinkedHashSet<>();
			
			for(String s : hoversContent) {
				String[] itemHoverContent = s.substring(s.indexOf("(") + 1, s.indexOf(")")).split(",");
				
				try {
					if(Integer.parseInt(itemHoverContent[2].trim()) == 730) {
						String historyRowId = itemHoverContent[1].replace("'", "").trim();
						matchedHoverItemsSet.add(new HoverItem(historyRowId.substring(0, historyRowId.lastIndexOf("_")), itemHoverContent[4].replace("'", "").trim()));
					}
				} catch(NumberFormatException e) {
					e.printStackTrace();
				}
			}
			
			if(!matchedHoverItemsSet.isEmpty()) {
				for(MarketListing ml : marketListingsList) {
					for(HoverItem hi : matchedHoverItemsSet) {
						if(ml.getHistory_row_id().equals(hi.getHistory_row_id())) {
							ml.setAsset_id(hi.getAsset_id());
							break;
						}
					}
				}
				
				JSONObject assetsObject = jsonData.getJSONObject("assets").getJSONObject("730").getJSONObject("2");			
				Iterator<String> assetsIterator = assetsObject.keys();
				
				while(assetsIterator.hasNext()) {
					String assetsObjectKey = assetsIterator.next();
					
					for(MarketListing ml : marketListingsList) {
						if(ml.getAsset_id() != null && ml.getAsset_id().equals(assetsObjectKey)) {
							JSONObject assetInnerObject = assetsObject.getJSONObject(assetsObjectKey);
							
							if(assetInnerObject.has("actions")) {
								ml.setInspect_ingame(assetInnerObject.getJSONArray("actions").getJSONObject(0).getString("link").replace("%assetid%", assetsObjectKey));
								break;
							}
						}
					}
				}
			}
		}
		
		return new ConvertedResponse(new Gson().toJson(marketListingsList), marketListingsList.size());
	}
	
	private String removeSpecialCharactersFromString(String data) {
		return data.replace("\n", "").replace("\r", "").replace("\t", "").replace("&lt;", "<").replace("&gt;", ">");
	}
	
	@Override
	public boolean isValidJSON(String jsonString) {
		try {
			JSONObject jsonObjectFromString = new JSONObject(jsonString);
			
			if(jsonObjectFromString.has("success")) {
				if(!jsonObjectFromString.getBoolean("success")) {
					return false;
				}
			} else {
				return false;
			}
		} catch (JSONException ex) {
			return false;
		}

		return true;
	}
	
	@Override
	public boolean isJSON(String jsonString) {
		try {
			new JSONObject(jsonString);
		} catch (JSONException ex) {
			try {
				new JSONArray(jsonString);
			} catch (JSONException e) {
				return false;
			}
		}

		return true;
	}
	
}
