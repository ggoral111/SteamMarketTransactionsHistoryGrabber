package service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.scene.control.Button;
import model.FileOperations;
import model.JSONValidator;
import model.MarketListing;

public class MarketListingsSearcherService implements FileOperations, JSONValidator {
	
	private final static int NUMBER_OF_THREADS;
	private final static String DATA_FOLDER_PATH, SEARCH_RESULT_FILE_PATH;
	
	static {
		NUMBER_OF_THREADS = 16;
		DATA_FOLDER_PATH = "data/";
		SEARCH_RESULT_FILE_PATH = "data/results/search_result_";
		// NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors();
	}
	
	private ExecutorService threadPool = null;
	private final SimpleDateFormat dateFormat;
	
	public MarketListingsSearcherService() {
		dateFormat = new SimpleDateFormat("MM-dd-YYYY_HH-mm-ss");
	}
	
	public void searchListings(String wordsToSearch, Button stopSearchListingsButton) {
		List<String> filesPathsList = getFilePaths();
		String[] splittedWordsToSearch = splitSearchWords(wordsToSearch);
		List<MarketListing> resultsList = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger threadCounter = new AtomicInteger(0);
		threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		
		for(String path : filesPathsList) {
			threadPool.execute(() -> {
				int threadNumber = threadCounter.getAndIncrement();
				String jsonListings = readFileWithLock(path);
				String[] splittedPath = path.split("\\\\");
				
				if(jsonListings != null && isJSON(jsonListings)) {
					System.out.println("Thread[" + threadNumber+ "] operations started. File: " + splittedPath[splittedPath.length - 1] + " was successfully loaded.");
					JSONArray listingsArray = new JSONArray(jsonListings);
					
					if(listingsArray.length() > 0) {
						for(int i=0; i<listingsArray.length(); i++) {
							JSONObject listingObject = listingsArray.getJSONObject(i);
							
							for(String s : splittedWordsToSearch) {
								if(listingObject.getString("item_name").toLowerCase().contains(s)) {
									MarketListing ml = new MarketListing(listingObject.getString("history_row_id"), listingObject.getString("item_name"), listingObject.getString("item_game_listing_name"), listingObject.getString("item_price"), listingObject.getString("item_action_type"), listingObject.getString("item_whoactedwith_profile_name"), listingObject.getString("item_whoactedwith_profile_url"), listingObject.getString("item_date"));						
									
									if(listingObject.has("asset_id")) {
										ml.setAsset_id(listingObject.getString("asset_id"));
									}
									
									if(listingObject.has("inspect_ingame")) {
										ml.setInspect_ingame(listingObject.getString("inspect_ingame"));
									}								
								
									resultsList.add(ml);
									break;
								}
							}
						}
					}
				}
				
				System.out.println("Thread[" + threadNumber + "] operations ended for file: " + splittedPath[splittedPath.length - 1]);
			});
		}
		
		stopSearchListings();
		System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");		
		
		if(!resultsList.isEmpty()) {
			String filePath = SEARCH_RESULT_FILE_PATH + dateFormat.format(Calendar.getInstance().getTime()) + ".json";
			writeFile(filePath, new Gson().toJson(resultsList));
			String[] splittedFilePath = filePath.split("/");
			System.out.println("Matched history transactions was saved successfully to file: " + splittedFilePath[splittedFilePath.length - 1]);
		} else {
			System.out.println("There were no matches corresponding to your search criteria.");
		}
		
		Platform.runLater(() -> {
			stopSearchListingsButton.fire();
		});		
	}
	
	public void stopSearchListings() {
		if(threadPool != null) {
			threadPool.shutdown();
			
			try {
				if(!threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
					threadPool.shutdownNow();
				}
			} catch(InterruptedException e) {
				threadPool.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}		
	}
	
	private List<String> getFilePaths() {
		List<String> filesPathsList = new ArrayList<>();
		
		for (File currentFile : new File(DATA_FOLDER_PATH).listFiles()) {
			if(currentFile.isFile()) {
				filesPathsList.add(currentFile.getAbsolutePath());
			}
		}
		
		return filesPathsList;
	}
	
	private String[] splitSearchWords(String lineToSplit) {
		String[] separatorsArray = { ",", ";" };
		StringBuilder regExpBuilder = new StringBuilder();	
		regExpBuilder.append("[");
		
		for(String s : separatorsArray) {
			regExpBuilder.append(Pattern.quote(s));
		}
		
		regExpBuilder.append("]");
		String[] splittedWords = lineToSplit.split(regExpBuilder.toString());

		for(int i=0; i<splittedWords.length; i++) {
			splittedWords[i] = splittedWords[i].trim().toLowerCase();
		}
		
		return splittedWords;
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
