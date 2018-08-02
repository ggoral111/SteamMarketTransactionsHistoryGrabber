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

public class MainController {
	
	@FXML
	private AnchorPane signInThroughSteamAnchorPane;
	
	@FXML
	private Button startMarketHistoryParserButton;
	
	@FXML
	private Button stopMarketHistoryParserButton;
	
	@FXML 
	private TextField marketTransactionsNumberTextField;
	
	@FXML 
	private TextField marketTransactionsOffsetTextField;

	private final static HistoryManagementService HISTORY_MANAGEMENT_SERVICE;
	private final static String STEAM_LOGIN_URL, STEAM_COMMUNITY_COOKIE_DOMAIN, STEAM_STORE_COOKIE_DOMAIN;
	
	static {
		HISTORY_MANAGEMENT_SERVICE = new HistoryManagementService();
		STEAM_LOGIN_URL = "https://steamcommunity.com/login/home/";
		STEAM_COMMUNITY_COOKIE_DOMAIN = "steamcommunity.com";
		STEAM_STORE_COOKIE_DOMAIN = "store.steampowered.com";
	}
	
	private WebView signInThroughSteamWebView;
	private WebEngine signInThroughSteamWebEngine;
	private CookieManager signInThroughSteamCookieManager;
	private boolean isSignedIntoSteam, isReadyToGrabData;
	
	
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
		
		signInThroughSteamCookieManager = new CookieManager();
		CookieHandler.setDefault(signInThroughSteamCookieManager);	
		signInThroughSteamWebView = new WebView();
		signInThroughSteamWebView.setPrefWidth(1280);
		signInThroughSteamWebView.setPrefHeight(720);
		signInThroughSteamWebView.setContextMenuEnabled(false);
		signInThroughSteamWebView.setZoom(1);		
		signInThroughSteamWebEngine = signInThroughSteamWebView.getEngine();
		signInThroughSteamWebEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:59.0) Gecko/20100101 Firefox/59.0");
		
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
		try {
			int marketTransactionsNumber = Integer.parseInt(marketTransactionsNumberTextField.getText());
			int marketTransactionsOffset = Integer.parseInt(marketTransactionsOffsetTextField.getText());
			
			isReadyToGrabData = true;
			marketTransactionsNumberTextField.setDisable(true);
			marketTransactionsOffsetTextField.setDisable(true);
			HISTORY_MANAGEMENT_SERVICE.marketHistoryParser(signInThroughSteamWebEngine, marketTransactionsNumber, marketTransactionsOffset);
		} catch(NumberFormatException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void stopMarketHistoryParserOnClick() {
		stopParserOnWindowClose();
		marketTransactionsNumberTextField.setDisable(false);
		marketTransactionsOffsetTextField.setDisable(false);
		isReadyToGrabData = false;
	}
	
	private void disableTextFieldsAndButtons() {
		marketTransactionsNumberTextField.setDisable(true);
		marketTransactionsOffsetTextField.setDisable(true);
		startMarketHistoryParserButton.setDisable(true);
		stopMarketHistoryParserButton.setDisable(true);
	}
	
	private void enableTextFieldsAndButtons() {
		marketTransactionsNumberTextField.setDisable(false);
		marketTransactionsOffsetTextField.setDisable(false);
		startMarketHistoryParserButton.setDisable(false);
		stopMarketHistoryParserButton.setDisable(false);
	}
	
	public void stopParserOnWindowClose() {
		if(isReadyToGrabData) {
			HISTORY_MANAGEMENT_SERVICE.stopMarketHistoryParser();
		}	
	}
}