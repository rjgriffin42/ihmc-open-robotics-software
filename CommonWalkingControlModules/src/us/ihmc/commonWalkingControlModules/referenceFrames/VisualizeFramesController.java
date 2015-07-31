package us.ihmc.commonWalkingControlModules.referenceFrames;

import java.util.ArrayList;

import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.robotics.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicReferenceFrame;
import us.ihmc.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;

   public class VisualizeFramesController implements RobotController
   {
      private final YoVariableRegistry registry = new YoVariableRegistry("VisualizeFramesController");

      private final ArrayList<YoGraphicReferenceFrame> dynamicGraphicReferenceFrames = new ArrayList<YoGraphicReferenceFrame>();

      private final YoGraphicsList yoGraphicsList = new YoGraphicsList("TestFramesController");

      public VisualizeFramesController(ArrayList<ReferenceFrame> referenceFrames, YoGraphicsListRegistry yoGraphicsListRegistry, double coordinateSystemLength)
      {
         for (ReferenceFrame frame : referenceFrames)
         {
            YoGraphicReferenceFrame dynamicGraphicReferenceFrame = new YoGraphicReferenceFrame(frame, registry, coordinateSystemLength);
            dynamicGraphicReferenceFrames.add(dynamicGraphicReferenceFrame);
            yoGraphicsList.add(dynamicGraphicReferenceFrame);
         }

         yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);
      }

      public void doControl()
      {
         updateDynamicGraphicReferenceFrames();
      }

      public YoVariableRegistry getYoVariableRegistry()
      {
         return registry;
      }

      public String getName()
      {
         return "visualizeFramesController";
      }
      
      public void initialize()
      {      
      }

      public String getDescription()
      {
         return getName();
      }

      private void updateDynamicGraphicReferenceFrames()
      {
         for (YoGraphicReferenceFrame frame : dynamicGraphicReferenceFrames)
         {
            frame.update();
         }
      }
   }