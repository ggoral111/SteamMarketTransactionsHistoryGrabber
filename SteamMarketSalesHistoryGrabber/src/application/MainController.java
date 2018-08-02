package application;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import service.HistoryManagementService;
import service.MarketListingsSearcherService;

public class MainController {
	
	@FXML
	private AnchorPane signInThroughSteamAnchorPane;
	
	@FXML
	private Button startMarketHistoryParserButton;
	
	@FXML
	private Button stopMarketHistoryParserButton;
	
	@FXML
	private Button startSearchListingsButton;
	
	@FXML
	private Button stopSearchListingsButton;
	
	@FXML 
	private TextField marketTransactionsNumberTextField;
	
	@FXML 
	private TextField marketTransactionsOffsetTextField;
	
	@FXML
	private TextField searchListingsWordsTextField;

	private final static HistoryManagementService HISTORY_MANAGEMENT_SERVICE;
	private final static MarketListingsSearcherService MARKET_LISTINGS_SEARCHER_SERVICE;
	private final static String STEAM_LOGIN_URL, STEAM_COMMUNITY_COOKIE_DOMAIN, STEAM_STORE_COOKIE_DOMAIN, WEB_ENGINE_USER_AGENT;
	
	static {
		HISTORY_MANAGEMENT_SERVICE = new HistoryManagementService();
		MARKET_LISTINGS_SEARCHER_SERVICE = new MarketListingsSearcherService();
		STEAM_LOGIN_URL = "https://steamcommunity.com/login/home/";
		STEAM_COMMUNITY_COOKIE_DOMAIN = "steamcommunity.com";
		STEAM_STORE_COOKIE_DOMAIN = "store.steampowered.com";
		WEB_ENGINE_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0";
	}
	
	private WebView signInThroughSteamWebView;
	private WebEngine signInThroughSteamWebEngine;
	private CookieManager signInThroughSteamCookieManager;
	private boolean isSignedIntoSteam, isReadyToGrabData, isSearchingListings;
	
	public MainController() {
		this.signInThroughSteamWebView = null;
		this.signInThroughSteamWebEngine = null;
		this.signInThroughSteamCookieManager = null;
		this.isSignedIntoSteam = false;
	}
	
	@FXML
	private void initialize() {	
		// disable text fields and buttons at launch
		disableTextFieldsAndButtons();
		enableSearchTextFieldAndButtons();
		
		signInThroughSteamCookieManager = new CookieManager();
		CookieHandler.setDefault(signInThroughSteamCookieManager);	
		signInThroughSteamWebView = new WebView();
		signInThroughSteamWebView.setPrefWidth(1280);
		signInThroughSteamWebView.setPrefHeight(720);
		signInThroughSteamWebView.setContextMenuEnabled(false);
		signInThroughSteamWebView.setZoom(1);		
		signInThroughSteamWebEngine = signInThroughSteamWebView.getEngine();
		signInThroughSteamWebEngine.setUserAgent(WEB_ENGINE_USER_AGENT);
		
		signInThroughSteamWebEngine.getLoadWorker().stateProperty().addListener((ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) -> {
			if(newValue == Worker.State.SUCCEEDED) {
				if(!isSignedIntoSteam) {
					AtomicBoolean isSteamLoginSecureCommunityPresent = new AtomicBoolean(false);
					AtomicBoolean isSteamLoginSecureStorePresent = new AtomicBoolean(false);
					
					signInThroughSteamCookieManager.getCookieStore().getCookies().forEach(cookie -> {
						if(cookie.getName().equals("steamLoginSecure")) {
							if(cookie.getDomain().equals(STEAM_COMMUNITY_COOKIE_DOMAIN)) {
								isSteamLoginSecureCommunityPresent.set(true);
							} else if(cookie.getDomain().equals(STEAM_STORE_COOKIE_DOMAIN)) {
								isSteamLoginSecureStorePresent.set(true);
							}
						}					
					});

					if(isSteamLoginSecureCommunityPresent.get() && isSteamLoginSecureStorePresent.get()) {
						isSignedIntoSteam = true;
						enableTextFieldsAndButtons();
						System.out.println("Signed into Steam successfully!");
					} else {
						isSignedIntoSteam = false;
						disableTextFieldsAndButtons();
						System.out.println("Signing into Steam failed!");
					}					
				} else if(isSignedIntoSteam && isReadyToGrabData) {
					HISTORY_MANAGEMENT_SERVICE.getGrabbedMarketHistoryData().set(signInThroughSteamWebEngine.executeScript("document.getElementsByTagName('pre')[0].innerHTML").toString());					
					System.out.println("Data successfully grebbed from remote source.");
					HISTORY_MANAGEMENT_SERVICE.getCountDownLatch().countDown();
				}
			} else if(newValue == Worker.State.FAILED){
				if(isSignedIntoSteam && isReadyToGrabData) {
					HISTORY_MANAGEMENT_SERVICE.getGrabbedMarketHistoryData().set(null);
					System.out.println("Data was not loaded properly from remote source.");
					HISTORY_MANAGEMENT_SERVICE.getCountDownLatch().countDown();
				}
			}
		});
		
		signInThroughSteamWebEngine.load(STEAM_LOGIN_URL);
		signInThroughSteamAnchorPane.getChildren().add(signInThroughSteamWebView);	
	}
	
	@FXML
	private void startMarketHistoryParserOnClick() {
		String marketTransactionsNumberTextFieldValue = marketTransactionsNumberTextField.getText();
		String marketTransactionsOffsetTextFieldValue = marketTransactionsOffsetTextField.getText();
		
		if(!marketTransactionsNumberTextFieldValue.isEmpty() && marketTransactionsNumberTextFieldValue != null && !marketTransactionsOffsetTextFieldValue.isEmpty() && marketTransactionsOffsetTextFieldValue != null) {
			try {
				int marketTransactionsNumber = Integer.parseInt(marketTransactionsNumberTextFieldValue);
				int marketTransactionsOffset = Integer.parseInt(marketTransactionsOffsetTextFieldValue);				
				isReadyToGrabData = true;
				disableTextFieldsAndButtons();
				disableSearchTextFieldAndButtons();
				HISTORY_MANAGEMENT_SERVICE.marketHistoryParser(signInThroughSteamWebEngine, marketTransactionsNumber, marketTransactionsOffset, stopMarketHistoryParserButton);
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}
	}
	
	@FXML
	private void stopMarketHistoryParserOnClick() {
		if(isReadyToGrabData) {
			HISTORY_MANAGEMENT_SERVICE.stopMarketHistoryParser();			
			enableTextFieldsAndButtons();
			enableSearchTextFieldAndButtons();
			isReadyToGrabData = false;
		}
	}
	
	@FXML
	private void startSearchListingsOnClick() {
		String searchListingsWordsTextFieldValue = searchListingsWordsTextField.getText();
		
		if(!searchListingsWordsTextFieldValue.isEmpty() && searchListingsWordsTextFieldValue != null) {
			isSearchingListings = true;
			disableTextFieldsAndButtons();
			disableSearchTextFieldAndButtons();
			MARKET_LISTINGS_SEARCHER_SERVICE.searchListings(searchListingsWordsTextFieldValue, stopSearchListingsButton);
		}
	}
	
	@FXML
	private void stopSearchListingsOnClick() {
		if(isSearchingListings) {
			MARKET_LISTINGS_SEARCHER_SERVICE.stopSearchListings();		
			enableTextFieldsAndButtons();
			enableSearchTextFieldAndButtons();
			isSearchingListings = false;
		}
	}
	
	private void disableSearchTextFieldAndButtons() {
		searchListingsWordsTextField.setDisable(true);
		startSearchListingsButton.setDisable(true);
		
		if(isSignedIntoSteam && isReadyToGrabData) {
			stopSearchListingsButton.setDisable(true);
		} else {
			stopSearchListingsButton.setDisable(false);
		}
	}
	
	private void enableSearchTextFieldAndButtons() {
		searchListingsWordsTextField.setDisable(false);
		startSearchListingsButton.setDisable(false);
		stopSearchListingsButton.setDisable(true);
	}
	
	private void disableTextFieldsAndButtons() {
		marketTransactionsNumberTextField.setDisable(true);
		marketTransactionsOffsetTextField.setDisable(true);
		startMarketHistoryParserButton.setDisable(true);
		
		if(!isSignedIntoSteam) {
			stopMarketHistoryParserButton.setDisable(true);
		} else if(isSignedIntoSteam && isSearchingListings) {
			stopMarketHistoryParserButton.setDisable(true);
		} else {
			stopMarketHistoryParserButton.setDisable(false);
		}
	}
	
	private void enableTextFieldsAndButtons() {
		marketTransactionsNumberTextField.setDisable(false);
		marketTransactionsOffsetTextField.setDisable(false);
		startMarketHistoryParserButton.setDisable(false);
		stopMarketHistoryParserButton.setDisable(true);
	}
	
	public void stopParserOnWindowClose() {
		if(isReadyToGrabData) {
			HISTORY_MANAGEMENT_SERVICE.stopMarketHistoryParser();
		} else if(isSearchingListings) {
			MARKET_LISTINGS_SEARCHER_SERVICE.stopSearchListings();
		}
	}
}