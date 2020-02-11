package us.ihmc.robotEnvironmentAwareness.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import us.ihmc.javaFXToolkit.messager.MessageBidirectionalBinding.PropertyToMessageTypeConverter;
import us.ihmc.robotEnvironmentAwareness.communication.REAModuleAPI;

public class SLAMAnchorPaneController extends REABasicUIController
{
   @FXML
   private ToggleButton enableSLAMButton;

   @FXML
   private TextField queuedBufferSize;

   @FXML
   private TextField slamStatus;

   @FXML
   private ToggleButton latestFrameEnable;

   @FXML
   private ToggleButton octreeMapEnable;

   @FXML
   private ToggleButton sensorFrameEnable;

   @FXML
   private ToggleButton planarRegionsEnable;

   @FXML
   private Slider sourcePointsSlider;

   @FXML
   private Slider searchingSizeSlider;

   @FXML
   private Slider windowMarginSlider;

   @FXML
   private Slider minimumOverlappedRatioSlider;

   @FXML
   private Slider minimumInliersRatioSlider;

   public SLAMAnchorPaneController()
   {

   }

   private final PropertyToMessageTypeConverter<Integer, Number> numberToIntegerConverter = new PropertyToMessageTypeConverter<Integer, Number>()
   {
      @Override
      public Integer convert(Number propertyValue)
      {
         return propertyValue.intValue();
      }

      @Override
      public Number interpret(Integer newValue)
      {
         return new Double(newValue.doubleValue());
      }
   };

   private final PropertyToMessageTypeConverter<Double, Number> numberToDoubleConverter = new PropertyToMessageTypeConverter<Double, Number>()
   {
      @Override
      public Double convert(Number propertyValue)
      {
         return propertyValue.doubleValue();
      }

      @Override
      public Number interpret(Double newValue)
      {
         return new Double(newValue.doubleValue());
      }
   };

   @Override
   public void bindControls()
   {
      uiMessager.bindBidirectionalGlobal(REAModuleAPI.SLAMEnable, enableSLAMButton.selectedProperty());

      //      uiMessager.bindBidirectionalGlobal(REAModuleAPI.QueuedBuffers, queuedBufferSize.valueProperty(), numberToIntegerConverter);
      //      uiMessager.bindBidirectionalGlobal(REAModuleAPI.SLAMStatus, slamStatus.valueProperty(), numberToIntegerConverter);

      uiMessager.bindBidirectionalGlobal(REAModuleAPI.ShowLatestFrame, latestFrameEnable.selectedProperty());
      uiMessager.bindBidirectionalGlobal(REAModuleAPI.ShowSLAMOctreeMap, octreeMapEnable.selectedProperty());
      uiMessager.bindBidirectionalGlobal(REAModuleAPI.ShowSLAMSensorTrajectory, sensorFrameEnable.selectedProperty());
      uiMessager.bindBidirectionalGlobal(REAModuleAPI.ShowPlanarRegionsMap, planarRegionsEnable.selectedProperty());

      uiMessager.bindBidirectionalGlobal(REAModuleAPI.SLAMSourcePoints, sourcePointsSlider.valueProperty(), numberToIntegerConverter);
      uiMessager.bindBidirectionalGlobal(REAModuleAPI.SLAMSearchingSize, searchingSizeSlider.valueProperty(), numberToIntegerConverter);
      uiMessager.bindBidirectionalGlobal(REAModuleAPI.SLAMMinimumOverlappedRatio, minimumOverlappedRatioSlider.valueProperty(), numberToDoubleConverter);
      uiMessager.bindBidirectionalGlobal(REAModuleAPI.SLAMWindowMargin, windowMarginSlider.valueProperty(), numberToDoubleConverter);
      uiMessager.bindBidirectionalGlobal(REAModuleAPI.SLAMMinimumInlierRatio, minimumInliersRatioSlider.valueProperty(), numberToDoubleConverter);

   }

   @FXML
   public void clear()
   {
      System.out.println("Clear from controller");
      uiMessager.submitMessageToModule(REAModuleAPI.SLAMClear, true);
      uiMessager.submitMessageInternal(REAModuleAPI.SLAMClear, true);
   }

   @FXML
   public void initialize()
   {
      attachREAMessager(PointCloudAnchorPaneController.uiStaticMessager);
      bindControls();
   }
}
