package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    
	public static Stage stage;
	
    public static void main(String[] args) {
    	
        Application.launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
    	Main.stage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("koala2-javafx.fxml"));
        stage.setTitle("Koala");
        stage.setScene(new Scene(root));
        stage.show();
    }
}
