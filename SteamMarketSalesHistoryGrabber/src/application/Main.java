package application;
	
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class Main extends Application {
	
	private MainController mainController;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
			VBox root = (VBox) loader.load();
			Scene scene = new Scene(root,1270,790);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());	
			
			// Load controller
			mainController = (MainController)loader.getController();
			
			primaryStage.setScene(scene);
			primaryStage.getIcons().add(new Image(this.getClass().getResource("SteamIcon.png").toString()));
			primaryStage.setTitle("Steam Marketplace Transactions History Grabber");
			primaryStage.setResizable(false);
			primaryStage.show();
			
			primaryStage.maximizedProperty().addListener((observable, oldValue, newValue) -> {
	            if(newValue) {
	            	primaryStage.setMaximized(false);         	
	            }           	
	        });	
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop() throws Exception {
	    super.stop();
	    
	    if(mainController != null) {
	    	mainController.stopParserOnWindowClose();
	    }

	    Platform.exit();
	    System.exit(0);
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
