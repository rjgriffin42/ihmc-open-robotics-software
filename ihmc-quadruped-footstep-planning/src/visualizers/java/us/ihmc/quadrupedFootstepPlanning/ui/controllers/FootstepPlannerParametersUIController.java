package us.ihmc.quadrupedFootstepPlanning.ui.controllers;

import us.ihmc.quadrupedFootstepPlanning.ui.components.FootstepPlannerParametersProperty;
import us.ihmc.quadrupedFootstepPlanning.ui.components.SettableFootstepPlannerParameters;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import us.ihmc.commons.PrintTools;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.javaFXToolkit.messager.MessageBidirectionalBinding.PropertyToMessageTypeConverter;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.robotEnvironmentAwareness.io.FilePropertyHelper;

import java.io.File;
import java.io.IOException;

public class FootstepPlannerParametersUIController
{
   private JavaFXMessager messager;
   private final FootstepPlannerParametersProperty parametersProperty = new FootstepPlannerParametersProperty(this, "footstepPlannerParametersProperty");


   @FXML
   private Spinner<Double> maxStepReach;
   @FXML
   private Spinner<Double> minStepLength;
   @FXML
   private Spinner<Double> maxStepWidth;
   @FXML
   private Spinner<Double> minStepWidth;

   @FXML
   private Spinner<Double> maxStepCycleDistance;
   @FXML
   private Spinner<Double> maxStepYaw;
   @FXML
   private Spinner<Double> minStepYaw;


   @FXML
   private Spinner<Double> maxStepChangeZ;
   @FXML
   private Spinner<Double> maxStepCycleChangeZ;
   @FXML
   private Spinner<Double> bodyGroundClearance;

   @FXML
   private Spinner<Double> minXClearanceFromFoot;
   @FXML
   private Spinner<Double> minYClearanceFromFoot;
   @FXML
   private Spinner<Double> minSurfaceIncline;

   @FXML
   private Spinner<Double> forwardWeight;
   @FXML
   private Spinner<Double> lateralWeight;
   @FXML
   private Spinner<Double> yawWeight;

   @FXML
   private Spinner<Double> costPerStep;
   @FXML
   private Spinner<Double> stepUpWeight;
   @FXML
   private Spinner<Double> stepDownWeight;
   @FXML
   private Spinner<Double> heuristicsWeight;


   private static final String CONFIGURATION_FILE_NAME = "./Configurations/footstepPlannerParameters.txt";
   private FilePropertyHelper filePropertyHelper;

   public FootstepPlannerParametersUIController()
   {
      File configurationFile = new File(CONFIGURATION_FILE_NAME);
      try
      {
         configurationFile.getParentFile().mkdirs();
         configurationFile.createNewFile();
         filePropertyHelper = new FilePropertyHelper(configurationFile);
      }
      catch (IOException e)
      {
         System.out.println(configurationFile.getAbsolutePath());
         e.printStackTrace();
      }
   }

   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;
   }

   public void setPlannerParameters(FootstepPlannerParameters parameters)
   {
      parametersProperty.setPlannerParameters(parameters);
   }

   public void setupControls()
   {
      maxStepReach.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.7, 0.0, 0.05));
      minStepLength.setValueFactory(new DoubleSpinnerValueFactory(-0.5, 0.0, 0.0, 0.05));
      maxStepWidth.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.5, 0.0, 0.02));
      minStepWidth.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.3, 0.0, 0.01));

      maxStepCycleDistance.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.7, 0.0, 0.05));
      maxStepYaw.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.3, 0.0, 0.05));
      minStepYaw.setValueFactory(new DoubleSpinnerValueFactory(-0.3, 0.0, 0.0, 0.05));

      maxStepChangeZ.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.3, 0.0, 0.05));
      maxStepCycleChangeZ.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.2, 0.0, 0.05));
      bodyGroundClearance.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.1, 0.0, 0.05));

      minXClearanceFromFoot.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.2, 0.0, 0.05));
      minYClearanceFromFoot.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.2, 0.0, 0.05));
      minSurfaceIncline.setValueFactory(new DoubleSpinnerValueFactory(0.0, 0.5, 0.0, 0.05));

      forwardWeight.setValueFactory(new DoubleSpinnerValueFactory(0.0, 2.0, 0.0, 0.1));
      lateralWeight.setValueFactory(new DoubleSpinnerValueFactory(0.0, 2.0, 0.0, 0.1));
      yawWeight.setValueFactory(new DoubleSpinnerValueFactory(0.0, 2.0, 0.0, 0.1));
      costPerStep.setValueFactory(new DoubleSpinnerValueFactory(0.0, 2.0, 0.0, 0.1));
      stepUpWeight.setValueFactory(new DoubleSpinnerValueFactory(0.0, 2.0, 0.0, 0.1));
      stepDownWeight.setValueFactory(new DoubleSpinnerValueFactory(0.0, 2.0, 0.0, 0.1));
      heuristicsWeight.setValueFactory(new DoubleSpinnerValueFactory(0.0, 2.0, 0.0, 0.1));
   }

   public void bindControls()
   {
      setupControls();

      parametersProperty.bidirectionalBindMaximumStepReach(maxStepReach.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindMinimumStepLength(minStepLength.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindMaximumStepWidth(maxStepWidth.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindMinimumStepWidth(minStepWidth.getValueFactory().valueProperty());

      parametersProperty.bidirectionalBindMaximumStepCycleDistance(maxStepCycleDistance.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindMaximumStepYaw(maxStepYaw.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindMinimumStepYaw(minStepYaw.getValueFactory().valueProperty());

      parametersProperty.bidirectionalBindMaximumStepChangeZ(maxStepChangeZ.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindMaximumStepCycleChangeZ(maxStepCycleChangeZ.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindBodyGroundClearance(bodyGroundClearance.getValueFactory().valueProperty());

      parametersProperty.bidirectionalBindMinXClearanceFromFoot(minXClearanceFromFoot.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindMinYClearanceFromFoot(minYClearanceFromFoot.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindMinimumSurfaceInclineRadians(minSurfaceIncline.getValueFactory().valueProperty());

      parametersProperty.bidirectionalBindForwardWeight(forwardWeight.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindLateralWeight(lateralWeight.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindYawWeight(yawWeight.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindStepUpWeight(stepUpWeight.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindStepDownWeight(stepDownWeight.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindCostPerStep(costPerStep.getValueFactory().valueProperty());
      parametersProperty.bidirectionalBindHeuristicsWeight(heuristicsWeight.getValueFactory().valueProperty());

      messager.bindBidirectional(FootstepPlannerMessagerAPI.PlannerParametersTopic, parametersProperty, createConverter(), true);
   }

   @FXML
   public void saveToFile()
   {
      if (filePropertyHelper == null)
      {
         PrintTools.warn("Can not save to file.");
         return;
      }

      System.out.println("Saving Parameters to file.");

      filePropertyHelper.saveProperty("maxStepReach", maxStepReach.getValue());
      filePropertyHelper.saveProperty("maxStepWidth", maxStepWidth.getValue());
      filePropertyHelper.saveProperty("minStepWidth", minStepWidth.getValue());
      filePropertyHelper.saveProperty("minStepLength", minStepLength.getValue());
      filePropertyHelper.saveProperty("maxStepChangeZ", maxStepChangeZ.getValue());
      filePropertyHelper.saveProperty("maxStepCycleChangeZ", maxStepCycleChangeZ.getValue());
      filePropertyHelper.saveProperty("bodyGroundClearance", bodyGroundClearance.getValue());
      filePropertyHelper.saveProperty("maxStepCycleDistance", maxStepCycleDistance.getValue());
      filePropertyHelper.saveProperty("maxStepYaw", maxStepYaw.getValue());
      filePropertyHelper.saveProperty("minStepYaw", minStepYaw.getValue());
      filePropertyHelper.saveProperty("minXClearanceFromFoot", minXClearanceFromFoot.getValue());
      filePropertyHelper.saveProperty("minYClearanceFromFoot", minYClearanceFromFoot.getValue());

      filePropertyHelper.saveProperty("minSurfaceIncline", minSurfaceIncline.getValue());
      filePropertyHelper.saveProperty("forwardWeight", forwardWeight.getValue());
      filePropertyHelper.saveProperty("lateralWeight", lateralWeight.getValue());
      filePropertyHelper.saveProperty("yawWeight", yawWeight.getValue());
      filePropertyHelper.saveProperty("costPerStep", costPerStep.getValue());
      filePropertyHelper.saveProperty("stepUpWeight", stepUpWeight.getValue());
      filePropertyHelper.saveProperty("stepDownWeight", stepDownWeight.getValue());
      filePropertyHelper.saveProperty("heuristicsWeight", heuristicsWeight.getValue());
   }

   public void loadFromFile()
   {
      if (filePropertyHelper == null)
      {
         return;
      }

      Double value;
      if ((value = filePropertyHelper.loadDoubleProperty("maxStepLength")) != null)
         maxStepReach.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("maxStepWidth")) != null)
         maxStepWidth.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("minStepWidth")) != null)
         minStepWidth.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("minStepLength")) != null)
         minStepLength.getValueFactory().setValue(value);

      if ((value = filePropertyHelper.loadDoubleProperty("maxStepChangeZ")) != null)
         maxStepChangeZ.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("maxStepCycleChangeZ")) != null)
         maxStepCycleChangeZ.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("bodyGroundClearance")) != null)
         bodyGroundClearance.getValueFactory().setValue(value);

      if ((value = filePropertyHelper.loadDoubleProperty("maxStepCycleDistance")) != null)
         maxStepCycleDistance.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("maxStepYaw")) != null)
         maxStepYaw.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("minStepYaw")) != null)
         minStepYaw.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("minXClearanceFromFoot")) != null)
         minXClearanceFromFoot.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("minYClearanceFromFoot")) != null)
         minYClearanceFromFoot.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("minSurfaceIncline")) != null)
         minSurfaceIncline.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("forwardWeight")) != null)
         forwardWeight.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("lateralWeight")) != null)
         lateralWeight.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("yawWeight")) != null)
         yawWeight.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("costPerStep")) != null)
         costPerStep.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("stepUpWeight")) != null)
         stepUpWeight.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("stepDownWeight")) != null)
         stepDownWeight.getValueFactory().setValue(value);
      if ((value = filePropertyHelper.loadDoubleProperty("heuristicsWeight")) != null)
         heuristicsWeight.getValueFactory().setValue(value);
   }




   private PropertyToMessageTypeConverter<FootstepPlannerParameters, SettableFootstepPlannerParameters> createConverter()
   {
      return new PropertyToMessageTypeConverter<FootstepPlannerParameters, SettableFootstepPlannerParameters>()
      {
         @Override
         public FootstepPlannerParameters convert(SettableFootstepPlannerParameters propertyValue)
         {
            return propertyValue;
         }

         @Override
         public SettableFootstepPlannerParameters interpret(FootstepPlannerParameters messageContent)
         {
            return new SettableFootstepPlannerParameters(messageContent);
         }
      };
   }
}
