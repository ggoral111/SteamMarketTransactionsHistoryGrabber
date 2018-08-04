package model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public enum LogFlow {
	LOG;
	
	private final static int TEXT_FLOW_LIST_SIZE, TEXT_FLOW_CLEAR_SIZE;
		
	static {
		TEXT_FLOW_LIST_SIZE = 80;
		TEXT_FLOW_CLEAR_SIZE = 10;
	}
	
	private Lock textFlowLock;
	private final SimpleDateFormat timeFormatter;
	
	LogFlow() {
		textFlowLock  = new ReentrantLock();
		timeFormatter = new SimpleDateFormat("HH:mm:ss");
	}
	
	public void addTextToTextFlow(final String text, final int color, TextFlow logTextFlow) {
		Platform.runLater(() -> {
			try {
				textFlowLock.lock();
				String textToAppend = new String(text);
				
				if(logTextFlow != null) {
					if(color == 0) {
						textToAppend = "[ERROR] [" + timeFormatter.format(Calendar.getInstance().getTime()) + "] " + textToAppend;
					} else if(color == 2) {
						textToAppend = "[INFO] [" + timeFormatter.format(Calendar.getInstance().getTime()) + "] " + textToAppend;
					}
					
					Text info = new Text(textToAppend);
					
					if(color == 0) {
						info.setFill(Color.RED);
					} else if (color == 1){
						info.setFill(Color.BLACK);
					} else if (color == 2) {
						info.setFill(Color.CORNFLOWERBLUE);
					}
					
					info.setFont(Font.font("System", FontWeight.THIN, 10)); 
					
					if(logTextFlow.getChildren().size() == TEXT_FLOW_LIST_SIZE) {
						List<Node> tempTransactionsLogList = logTextFlow.getChildren().stream().skip(logTextFlow.getChildren().size() - TEXT_FLOW_CLEAR_SIZE).collect(Collectors.toCollection(ArrayList::new));
						logTextFlow.getChildren().clear();
						logTextFlow.getChildren().addAll(tempTransactionsLogList);
					}
					
					logTextFlow.getChildren().add(info);
				}
			} finally {
				textFlowLock.unlock();
			}	
		});
	}
}
