package gui;

import interpreter.Interpreter;
import interpreter.data.Closure;
import interpreter.data.Literal;
import interpreter.data.Value;
import interpreter.eval.BodyEvaluator;
import interpreter.strat.CustomLiteralSelector;
import interpreter.strat.CustomRuleSelector;
import interpreter.strat.LiteralSelector;
import interpreter.strat.RandomLiteralSelector;
import interpreter.strat.RandomRuleSelector;
import interpreter.strat.RuleSelector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialogs;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import parser.lexer.Lexer;
import parser.lexer.LexerException;
import parser.parser.Parser;
import parser.parser.ParserException;
import util.PrettyPrinter;
import util.Tuple;
import checker.ContextChecker;


public class Controller {

	private final KeyCombination F1 = new KeyCodeCombination(KeyCode.F1);
	private final KeyCombination F2 = new KeyCodeCombination(KeyCode.F2);
	private final KeyCombination F3 = new KeyCodeCombination(KeyCode.F3);
	private final KeyCombination F4 = new KeyCodeCombination(KeyCode.F4);
	
    @SuppressWarnings("unused")
	@FXML
    private ResourceBundle resources;

    @SuppressWarnings("unused")
	@FXML
    private URL location;

    @FXML
    private MenuItem aboutMenuItem;

    @FXML
    private TextArea consoleTextArea;

    @FXML
    private TitledPane controlPane;

    @FXML
    private TextField currentGoalTextField;

    @FXML
    private TextField initialGoalTextField;

    @FXML
    private ListView<String> literalListView;

    @FXML
    private ComboBox<LiteralSelector> literalStrategyBox;

    @FXML
    private Button multiStepButton;

    @FXML
    private MenuItem openFileMenuItem;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private MenuItem quitProgramMenuItem;

    @FXML
    private MenuItem reloadFileMenuItem;

    @FXML
    private ListView<String> ruleListView;

    @FXML
    private ComboBox<RuleSelector> ruleStrategyBox;

    @FXML
    private MenuItem saveLogMenuItem;

    @FXML
    private MenuItem shortcutsMenuItem;

    @FXML
    private Button singleStepButton;

    @FXML
    private Button startButton;

    @FXML
    private Label statusLabel;
    
    /*-----------------------------------------------------------------------*/
    /*--Interpreter components-----------------------------------------------*/
    /*-----------------------------------------------------------------------*/
    
    private File currentFile = null;
	boolean isRunning;
    private Interpreter interpreter = null;
    private Map<String, List<Closure>> program = new HashMap<>();
    private List<Literal> goal = new LinkedList<>();
	private List<Tuple<Literal, List<Closure>>> alternatives = null;
	private List<Literal> initialGoal = new LinkedList<>();
	private boolean isMultiStepActive;
	private Task<Void> task = null;
    
    /*-----------------------------------------------------------------------*/

    @FXML
    void handleAboutMenuItem(ActionEvent event) {
    	Dialogs.showInformationDialog(
    		Main.stage,
    		"Version: 2.0\n\n" + 
    		"Author: Jacob Lorenz\n\n" + 
    		"Chair for Programming Languages and Compiler Design\n" + 
    		"University of Technology Cottbus\n\n",
    		"Koala - A Eucalyptus Interpreter",
    		"About"
    	);
    }
    
    @FXML
    void handleGoalTextInput(KeyEvent event) {
    	if(!program.isEmpty() && !initialGoalTextField.getText().isEmpty()) {
    		startButton.setDisable(false);
    	} else {
    		startButton.setDisable(true);
    	}
    }

    @FXML
    void handleKeyEvent(KeyEvent event) {
    	if(F1.match(event)) {
    		if(!startButton.isDisable()) {
    			handleStartButton(null);
    		}
    	} else if(F2.match(event)) {
    		if(isRunning) {
    			handleStartButton(null);
        		handleStartButton(null);
    		}
    	} else if(F3.match(event)) {
    		if(isRunning) {
    			handleSingleStepButton(null);
    		}
    	} else if(F4.match(event)) {
    		if(isRunning) {
    			handleMultiStepButton(null);
    		}
    	}
    }

    @FXML
    void handleLiteralListView(MouseEvent event) {
    	int index = literalListView.getSelectionModel().getSelectedIndex();
    	if(index != -1) {
    		//draw corresponding rules
    		printRulesForLiteral(index);
    		
    		/*
    		//update custom selector with current index
    		for(LiteralSelector s : literalStrategyBox.getItems()) {
	    		if(s instanceof CustomLiteralSelector) {
	    			((CustomLiteralSelector) s).setIndex(index);
	    			break;
	    		}
	    	}
	    	*/
    	}
    }

    @FXML
    void handleLiteralStrategyBox(ActionEvent event) {
    	int index = literalStrategyBox.getSelectionModel().getSelectedIndex();
    	if(index == 0) {
    		ruleStrategyBox.getSelectionModel().select(0);
    		ruleStrategyBox.setDisable(true);
    	} else {
    		ruleStrategyBox.setDisable(false);
    	}
    }

    @FXML
    void handleMultiStepButton(ActionEvent event) {
    	String input = Dialogs.showInputDialog(
    		Main.stage, 
    		"Maximum number of continuous steps"
    	);
    	if(input != null) {
    		final int n = Integer.parseInt(input);
    		if(n >= 1) {
    			final List<String> msg = new LinkedList<>();
    			task = new Task<Void>() {
    				@Override public Void call() {
    			        final int max = n;
    			        for (int i=1; i<=max; i++) {
    			            if (isCancelled()) {
    			               break;
    			            }
    			            
    			            if(i > 1) {
        	    		    	msg.add(createAlternativesString());
    			            }
    			            
    			            LiteralSelector literalSelector = literalStrategyBox.getValue();
    			    		if(literalSelector instanceof CustomLiteralSelector) {
    			    			int index = literalListView.getSelectionModel().getSelectedIndex();
    			    			((CustomLiteralSelector) literalSelector).setIndex(index);
    			    		}
    			    		
    			    		RuleSelector ruleSelector = ruleStrategyBox.getValue();
    			    		if(ruleSelector instanceof CustomRuleSelector) {
    			    			int index = ruleListView.getSelectionModel().getSelectedIndex();
    			    			((CustomRuleSelector) ruleSelector).setIndex(index);
    			    		}
    			            
    	    		    	int lit = interpreter.selectLiteral(literalStrategyBox.getValue());
    	    		    	int rul = interpreter.selectRule(ruleStrategyBox.getValue());
    	    		    	msg.add(createSelectionString(lit, rul));
    	    		    	
    	    		    	//perform single interpretation step
    	    		    	interpreter.interpret();
    	    		    	msg.add(createResultString());
    	    		    	
    	    		    	//detect further alternatives or cancel
    	    		    	interpreter.detectAlternatives();
    	    		    	if(alternatives.isEmpty()) {
    	    		    		this.cancel();
    	    		    		continue;
    	    		    	}
    			            updateProgress(i, max);
    			        }
    			        return null;
    			    }
    			};
    			
    			task.stateProperty().addListener(new ChangeListener<Worker.State>() {
    		        @Override
    		        public void changed(ObservableValue<? extends State> observable,
    		                State oldValue, Worker.State newState) {
    		            if(newState==Worker.State.SUCCEEDED ||
    		            		newState==Worker.State.CANCELLED){
    		                for(String s : msg) {
    		                	printMessage(s);
    		                }
        	    			progressBar.progressProperty().unbind();
        	    			progressBar.setProgress(0);
        	    			if(isRunning) {
        	    				disableRuntimeControls(false);
            	    			isMultiStepActive = false;
        		                update();
        	    			}
    		            }
    		        }
    		    });
    			statusLabel.setText("Status: running");
    			progressBar.progressProperty().bind(task.progressProperty());
    			this.disableRuntimeControls(true);
    			Thread th = new Thread(task);
    			th.setDaemon(true);
    			isMultiStepActive = true;
    			th.start();
    		}
    	}
    }

	@FXML
    void handleOpenFileMenuItem(ActionEvent event) {
    	File file = new FileChooser().showOpenDialog(Main.stage);
        if (file != null && file.exists()) {
        	currentFile = file;
            openFile(currentFile);
            reloadFileMenuItem.setDisable(false);
            handleGoalTextInput(null);
        }
    }

	@FXML
    void handleQuitProgramMenuItem(ActionEvent event) {
		System.exit(0);
    }

    @FXML
    void handleReloadFileMenuItem(ActionEvent event) {
    	if(currentFile != null && currentFile.exists()) {
    		openFile(currentFile);
    		handleGoalTextInput(null);
    	}
    }

    @FXML
    void handleRuleListView(MouseEvent event) {
    	/*
    	int index = ruleListView.getSelectionModel().getSelectedIndex();
    	if(index != -1) {
    		for(RuleSelector s : ruleStrategyBox.getItems()) {
        		if(s instanceof CustomRuleSelector) {
        			((CustomRuleSelector) s).setIndex(index);
        			break;
        		}
        	}
    	}
    	*/
    }

    @FXML
    void handleRuleStrategyBox(ActionEvent event) {
    	/* empty */
    }

    @FXML
    void handleSaveLogMenuItem(ActionEvent event) {
    	File file = new FileChooser().showSaveDialog(Main.stage);
        if (file != null) {
        	writeLogFile(file);
        }
    }

	@FXML
    void handleShortcutsMenuItem(ActionEvent event) {
    	Dialogs.showInformationDialog(
    		Main.stage,
    		"Open a source file: CTRL + O\n" + 
    		"Reload a source file: CTRL + SHIFT + O\n" + 
    		"Save console output: CTRL + S\n\n" + 
    		"Start or stop a simulation: F1\n" +
    		"Restart a simulation: F2\n" + 
    		"Perform a single step: F3\n" +
    		"Perform a multiple steps: F4\n",
    		"",
    		"Shortcuts"
    	);
    }

    @FXML
    void handleSingleStepButton(ActionEvent event) {	
    	
		statusLabel.setText("Status: running");
		
		LiteralSelector literalSelector = literalStrategyBox.getValue();
		if(literalSelector instanceof CustomLiteralSelector) {
			int index = literalListView.getSelectionModel().getSelectedIndex();
			((CustomLiteralSelector) literalSelector).setIndex(index);
		}
		
		RuleSelector ruleSelector = ruleStrategyBox.getValue();
		if(ruleSelector instanceof CustomRuleSelector) {
			int index = ruleListView.getSelectionModel().getSelectedIndex();
			((CustomRuleSelector) ruleSelector).setIndex(index);
		}
		
		//retrieve indices of selected objects
    	int literal = interpreter.selectLiteral(literalStrategyBox.getValue());
    	int rule = interpreter.selectRule(ruleStrategyBox.getValue());
    	printSelection(literal, rule);
    	
    	//perform single interpretation step
    	interpreter.interpret();
    	
    	//print results to console
		printResults();
    	
    	//check for further possible evaluations and stop if none exist
    	update();
    }

	@FXML
    void handleStartButton(ActionEvent event) {
    	if(isRunning) {
    		stop();
    		printMessage("> Evaluation canceled!");
    	} else {
    		start();
    	}
    }

	@FXML
    void initialize() {
        assert aboutMenuItem != null : "fx:id=\"aboutMenuItem\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert consoleTextArea != null : "fx:id=\"consoleTextArea\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert controlPane != null : "fx:id=\"controlPane\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert currentGoalTextField != null : "fx:id=\"currentGoalTextField\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert initialGoalTextField != null : "fx:id=\"initialGoalTextField\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert literalListView != null : "fx:id=\"literalListView\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert literalStrategyBox != null : "fx:id=\"literalStrategyBox\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert multiStepButton != null : "fx:id=\"multiStepButton\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert openFileMenuItem != null : "fx:id=\"openFileMenuItem\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert quitProgramMenuItem != null : "fx:id=\"quitProgramMenuItem\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert reloadFileMenuItem != null : "fx:id=\"reloadFileMenuItem\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert ruleListView != null : "fx:id=\"ruleListView\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert ruleStrategyBox != null : "fx:id=\"ruleStrategyBox\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert saveLogMenuItem != null : "fx:id=\"saveLogMenuItem\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert shortcutsMenuItem != null : "fx:id=\"shortcutsMenuItem\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert singleStepButton != null : "fx:id=\"singleStepButton\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert startButton != null : "fx:id=\"startButton\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        assert statusLabel != null : "fx:id=\"statusLabel\" was not injected: check your FXML file 'koala2-javafx.fxml'.";
        saveLogMenuItem.setDisable(true);
        reloadFileMenuItem.setDisable(true);
        isRunning = false;
        disableRuntimeControls(true);
        startButton.setDisable(true);
        isMultiStepActive = false;
        
        ObservableList<LiteralSelector> literalStrategies = 
    	    FXCollections.observableArrayList(
    	        (LiteralSelector)new RandomLiteralSelector(),
    	        (LiteralSelector)new CustomLiteralSelector()
    	    );
        literalStrategyBox.setItems(literalStrategies);
        literalStrategyBox.getSelectionModel().select(0);
        
        ObservableList<RuleSelector> ruleStrategies = 
    	    FXCollections.observableArrayList(
    	        (RuleSelector)new RandomRuleSelector(),
    	        (RuleSelector)new CustomRuleSelector()
    	    );
        ruleStrategyBox.setItems(ruleStrategies);
        ruleStrategyBox.getSelectionModel().select(0);
    }
    
    private void openFile(File file) {
    	try {
    		program.clear();
			Parser p = new Parser(new Lexer(new PushbackReader(new FileReader(file))));
			ContextChecker checker = new ContextChecker(program);
			p.parse().apply(checker);
			if(!checker.getMessages().isEmpty()) {
				printMessage("File '" + file + "' contains errors!");
				for(String msg : checker.getMessages()) {
					printMessage(msg);
				}
				program.clear();
			} else {
				printMessage("> File '" + file + "' was compiled successfully!\n");
			}
		} catch (ParserException | LexerException e) {
			printMessage("> File '" + file + "' contains errors!\n");
			printMessage(e.getMessage());
			program.clear();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
    
    private void writeLogFile(File file) {
    	try {
			if(file.exists()) {
				file.createNewFile();
			}
			BufferedWriter ostream = new BufferedWriter(new FileWriter(file));
			ostream.write(consoleTextArea.getText());
			ostream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    private boolean analyseGoal() {
    	String goalString = initialGoalTextField.getText();
    	goalString = "GOAL(A):-true:true|" + goalString + ".";
    	Parser p = new Parser(new Lexer(new PushbackReader(new StringReader(goalString))));
		Map<String, List<Closure>> goalSt = new HashMap<>(program);
		ContextChecker checker = new ContextChecker(goalSt);
		try {
			p.parse().apply(checker);
			if(!checker.getMessages().isEmpty()) {
				for(String msg : checker.getMessages()) {
					printMessage(msg);
				}
			} else {
				printMessage("> Goal was compiled successfully!\n");
				Closure c = goalSt.get("GOAL/1").get(0);
				goal.clear();
				c.getRule().apply(new BodyEvaluator(c.getEnvironment(), goal, -1));
				return true;
			}
		} catch (ParserException | LexerException e) {
			printMessage("> The initial goal contains errors!\n");
			printMessage(e.getMessage());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
    
    private void printLiterals() {
    	ObservableList<String> literals = FXCollections.observableArrayList();
    	for(Tuple<Literal, List<Closure>> t : alternatives) {
    		literals.add(printLiteral(t.getFirst()));
    	}
    	literalListView.setItems(literals);
	}

	private String printLiteral(Literal lit) {
		String s = lit.getName() + "(";
		for(Value arg : lit.getArgs()) {
			s += interpreter.getResultString(arg) + ", ";
		}
		s = s.substring(0, s.lastIndexOf(','));
		s += ")";
		return s;
	}

	private void printRulesForLiteral(int selectedLiteralIndex) {
		if(selectedLiteralIndex >= 0 && 
				selectedLiteralIndex < alternatives.size()) {
			PrettyPrinter printer = new PrettyPrinter();
			ObservableList<String> rules = FXCollections.observableArrayList();
			for(Closure c : alternatives.get(selectedLiteralIndex).getSecond()) {
				c.getRule().apply(printer);
				rules.add(printer.getString());
				printer.reset();
			}
			ruleListView.setItems(rules);
		}
	}
    
    private void printMessage(String text) {
    	consoleTextArea.appendText(text + "\n");
    	if(saveLogMenuItem.isDisable()) {
    		saveLogMenuItem.setDisable(false);
    	}
    }
    
    private void disableRuntimeControls(boolean flag) {
    	this.currentGoalTextField.setDisable(flag);
    	this.singleStepButton.setDisable(flag);
    	this.multiStepButton.setDisable(flag);
    	this.literalStrategyBox.setDisable(flag);
    	
    	int index = literalStrategyBox.getSelectionModel().getSelectedIndex();
    	this.ruleStrategyBox.setDisable(flag || (index == 0));
    	
    	this.literalListView.setDisable(flag);
    	this.ruleListView.setDisable(flag);
    }
    
    private void start() {
		if(!analyseGoal()) {
			return;
		}
		
		//create interpreter
		interpreter = new Interpreter(program, goal);
		
		printMessage("> Evaluation started!\n");
		if(!update()) {
			return;
		}
		
		//store initial goal to retrieve results
		initialGoal.clear();
		initialGoal.addAll(goal);
		
		//indicate current goal (copy of initial goal)
		currentGoalTextField.setText(initialGoalTextField.getText());

		disableRuntimeControls(false);
		startButton.setText("Stop");
		initialGoalTextField.setDisable(true);
		isRunning = true;
	}

	private void stop() {
		if(isMultiStepActive) {
			task.cancel();
			isMultiStepActive = false;
		}
		interpreter = null;
		initialGoalTextField.setDisable(false);
		startButton.setText("Start");
		disableRuntimeControls(true);
		isRunning = false;
	}
	
	private boolean update() {
		alternatives = interpreter.detectAlternatives();
		if(alternatives.isEmpty()) {
			stop();
			if(goal.isEmpty()) {
				printMessage("> Evaluation finished!");
				statusLabel.setText("Status: finished");
			} else {
				printMessage("> Evaluation locked!");
				statusLabel.setText("Status: locked");
			}
			ObservableList<String> list = literalListView.getItems();
			list.clear();
			literalListView.setItems(list);
			
			list = ruleListView.getItems();
			list.clear();
			ruleListView.setItems(list);
			
			currentGoalTextField.setText("");
			return false;
		} else {
			printCurrentGoal();
			printLiterals();
			literalListView.getSelectionModel().select(0);
			printRulesForLiteral(0);
			ruleListView.getSelectionModel().select(0);
			printAlternatives();
			statusLabel.setText("Status: ready");
			return true;
		}
	}

	private void printCurrentGoal() {
		String s = "";
		for(Tuple<Literal, List<Closure>> t : alternatives) {
    		Literal lit = t.getFirst();
    		s += lit.getName() + "(";
    		for(Value arg : lit.getArgs()) {
    			s += interpreter.getResultString(arg) + ", ";
    		}
    		s = s.substring(0, s.lastIndexOf(','));
    		s += ")";
    		s += ", ";
    	}
		s = s.substring(0, s.lastIndexOf(','));
		currentGoalTextField.setText(s);
	}
	
	private void printAlternatives() {
		printMessage(createAlternativesString());
	}
	
	private String createAlternativesString() {
		String s = "> Alternatives:\n";
		for(Tuple<Literal, List<Closure>> t : alternatives) {
			s += "\t" + printLiteral(t.getFirst()) + "\n";
			for(Closure c : t.getSecond()) {
				PrettyPrinter pr = new PrettyPrinter();
				c.getRule().apply(pr);
				s += "\t\trule  = " + pr.getString() + "\n";
			}
		}
		return s;
	}
	
	private void printSelection(int literal, int rule) {
		printMessage(createSelectionString(literal, rule));
	}
	
	private String createSelectionString(int literal, int rule) {
		Tuple<Literal, List<Closure>> tuple = alternatives.get(literal);
		String s = "> Selection:\n"; 
		s += "\tliteral: \t" + printLiteral(tuple.getFirst()) + "\n";
		PrettyPrinter pr = new PrettyPrinter();
		tuple.getSecond().get(rule).getRule().apply(pr);
		s += "\trule: \t\t" + pr.getString() + "\n";
		return s;
	}
	
	private void printResults() {
		printMessage(createResultString());
	}
	
	private String createResultString() {
		String s = "> Results:\n";
		for(Literal lit : initialGoal) {
			s += "\t" + lit.getName() + "(";
    		for(Value arg : lit.getArgs()) {
    			s += interpreter.getResultString(arg) + ", ";
    		}
    		s = s.substring(0, s.lastIndexOf(','));
    		s += ")";
    		s += ",\n";
		}
		s = s.substring(0, s.lastIndexOf(',')) + "\n";
		s += "\n--------------------------------------";
		s += "------------------------------------------\n";
		return s;
	}
}
