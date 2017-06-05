package us.ihmc.simulationconstructionset.gui.dialogs;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

import javax.swing.*;
import java.awt.*;

public class PlaybackPropertiesDialog extends Stage implements EventHandler {
    private static final long serialVersionUID = -8226475433536336684L;
    private Button okButton, applyButton, cancelButton;
    private PlaybackPropertiesPanel playbackPropertiesPanel;
    @SuppressWarnings("unused")
    private JFrame ownerFrame;
    private Container parentContainer;

    private SimulationConstructionSet sim;

    public PlaybackPropertiesDialog(Container parentContainer, JFrame ownerFrame, SimulationConstructionSet sim) {
        super();

        this.setTitle("Playback Properties");

        this.parentContainer = parentContainer;
        this.ownerFrame = ownerFrame;
        this.sim = sim;

        GridPane pane = new GridPane();

        RowConstraints properties = new RowConstraints();
        properties.setPercentHeight(80.00);
        RowConstraints buttons = new RowConstraints();
        buttons.setPercentHeight(20.00);

        pane.getRowConstraints().addAll(
                properties,
                buttons
        );

        playbackPropertiesPanel = new PlaybackPropertiesPanel();
        GridPane.setConstraints(playbackPropertiesPanel, 0, 0);
        GridPane.setMargin(playbackPropertiesPanel, new javafx.geometry.Insets(10, 10, 5, 10));

        okButton = new Button("OK");
        okButton.addEventHandler(javafx.event.ActionEvent.ACTION, this);

        applyButton = new Button("Apply");
        applyButton.addEventHandler(javafx.event.ActionEvent.ACTION, this);
        HBox.setMargin(applyButton, new javafx.geometry.Insets(0, 5, 0, 5));

        cancelButton = new Button("Cancel");
        cancelButton.addEventHandler(javafx.event.ActionEvent.ACTION, this);

        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(
                okButton,
                applyButton,
                cancelButton
        );
        hbox.setPrefSize(200, okButton.getHeight());
        GridPane.setConstraints(hbox, 0, 1);

        pane.getChildren().addAll(
                playbackPropertiesPanel,
                hbox
        );

        parentContainer.repaint();    // This is a horrible way to get the graphs to repaint...

        Dimension frameSize = parentContainer.getSize();

        this.setScene(new Scene(pane));
        this.setX(frameSize.width / 2);
        this.setY(frameSize.height / 4);
        this.setResizable(false);
        this.show();
    }

    @Override
    public void handle(Event event) {
        if (event.getSource() == cancelButton)
            this.hide();

        if (event.getSource() == applyButton) {
            playbackPropertiesPanel.commitChanges();
        }

        if (event.getSource() == okButton) {
            playbackPropertiesPanel.commitChanges();
            this.hide();
        }

        parentContainer.repaint();    // This is a horrible way to get the graphs to repaint...
    }

    public class PlaybackPropertiesPanel extends GridPane implements EventHandler {
        private double newRealTimeVal, newFrameRateVal, newSimulateDurationVal;

        private CheckBox updateGraphsDuringPlaybackCheckbox;
        private Label realTimeRateLabel;
        private Label desiredFrameRateLabel;
        private Label simulateDurationLabel;
        private TextField realTimeTextField;
        private TextField frameRateTextField;
        private TextField simulateDurationTextField;
        private CheckBox simulateNoFasterThanRealTimeCheckbox;

        public PlaybackPropertiesPanel() {
            updateGraphsDuringPlaybackCheckbox = new CheckBox("Update Graphs");
            simulateNoFasterThanRealTimeCheckbox = new CheckBox("Simulate No Faster Than Real Time");

            realTimeRateLabel = new Label("Real Time Rate:");
            desiredFrameRateLabel = new Label("Desired Frame Rate:");
            simulateDurationLabel = new Label("Simulate Duration:");

            realTimeTextField = new TextField();
            realTimeTextField.setPrefSize(60, 21);

            frameRateTextField = new TextField();
            frameRateTextField.setPrefSize(60, 21);

            simulateDurationTextField = new TextField();
            simulateDurationTextField.setPrefSize(60, 21);

            GridPane.setConstraints(updateGraphsDuringPlaybackCheckbox, 0, 0);
            GridPane.setConstraints(realTimeRateLabel, 0, 1);
            GridPane.setConstraints(realTimeTextField, 1, 1);
            GridPane.setConstraints(desiredFrameRateLabel, 0, 2);
            GridPane.setConstraints(frameRateTextField, 1, 2);
            GridPane.setConstraints(simulateDurationLabel, 0, 3);
            GridPane.setConstraints(simulateDurationTextField, 1, 3);
            GridPane.setConstraints(simulateNoFasterThanRealTimeCheckbox, 0, 4);

            newRealTimeVal = sim.getPlaybackRealTimeRate();
            newFrameRateVal = sim.getPlaybackFrameRate();
            newSimulateDurationVal = sim.getSimulateDuration();

            updateGraphsDuringPlaybackCheckbox.setSelected(sim.areGraphsUpdatedDuringPlayback());
            realTimeTextField.setText(String.valueOf(newRealTimeVal));
            frameRateTextField.setText(String.valueOf(newFrameRateVal));
            simulateDurationTextField.setText(String.valueOf(newSimulateDurationVal));
            simulateNoFasterThanRealTimeCheckbox.setSelected(sim.getSimulateNoFasterThanRealTime());

            // sim.setUpdateGraphsDuringPlayback(updateGraphsCheckbox.isSelected());

            this.getChildren().addAll(
                    updateGraphsDuringPlaybackCheckbox,
                    realTimeRateLabel,
                    realTimeTextField,
                    desiredFrameRateLabel,
                    frameRateTextField,
                    simulateDurationLabel,
                    simulateDurationTextField,
                    simulateNoFasterThanRealTimeCheckbox
            );
        }

        public void commitChanges() {
            updateRealTimeTextField();
            updateFrameRateTextField();
            updateSimulateDurationTextField();

            sim.setSimulateNoFasterThanRealTime(simulateNoFasterThanRealTimeCheckbox.isSelected());
            sim.setPlaybackRealTimeRate(newRealTimeVal);
            sim.setPlaybackDesiredFrameRate(newFrameRateVal);
            sim.setSimulateDuration(newSimulateDurationVal);
            sim.setGraphsUpdatedDuringPlayback(updateGraphsDuringPlaybackCheckbox.isSelected());

         /*
          * dataBuffer.setMaxBufferSize(newMaxVal);
          * dataBuffer.changeBufferSize(newCurrentVal);
          *
          * dataBuffer.setWrapBuffer(wrapButton.isSelected());
          */
        }

        private void updateRealTimeTextField() {
            String text = realTimeTextField.getText();

            try {
                double val = Double.parseDouble(text);
                newRealTimeVal = val;
            } catch (NumberFormatException e) {
                realTimeTextField.setText(String.valueOf(newRealTimeVal));
            }

        }

        private void updateFrameRateTextField() {
            String text = frameRateTextField.getText();

            try {
                double val = Double.parseDouble(text);
                newFrameRateVal = val;
            } catch (NumberFormatException e) {
                frameRateTextField.setText(String.valueOf(newFrameRateVal));
            }
        }

        private void updateSimulateDurationTextField() {
            String text = simulateDurationTextField.getText();

            try {
                double val = Double.parseDouble(text);
                newSimulateDurationVal = val;
            } catch (NumberFormatException e) {
                simulateDurationTextField.setText(String.valueOf(newSimulateDurationVal));
            }
        }

        @Override
        public void handle(Event event) {
            if (event.getSource() == realTimeTextField) {
                updateRealTimeTextField();
            } else if (event.getSource() == frameRateTextField) {
                updateFrameRateTextField();
            } else if (event.getSource() == simulateDurationTextField) {
                updateSimulateDurationTextField();
            }
        }
    }
}
