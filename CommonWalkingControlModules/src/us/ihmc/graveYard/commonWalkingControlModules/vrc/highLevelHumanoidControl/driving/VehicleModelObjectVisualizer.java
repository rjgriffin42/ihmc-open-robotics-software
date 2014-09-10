package us.ihmc.graveYard.commonWalkingControlModules.vrc.highLevelHumanoidControl.driving;

import us.ihmc.utilities.FormattingTools;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.yoUtilities.graphics.YoGraphic;
import us.ihmc.yoUtilities.graphics.YoGraphicReferenceFrame;



public class VehicleModelObjectVisualizer
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final YoGraphicsList yoGraphicsList;
   private final double objectFrameScale = 0.2;
   private final double vehicleFrameScale = 1.0;

   public VehicleModelObjectVisualizer(ReferenceFrame vehicleFrame, VehicleModelObjects vehicleModelObjects,
                                YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      yoGraphicsList = new YoGraphicsList("vehicleObjects");

      for (VehicleObject vehicleObject : VehicleObject.values())
      {

         FramePose framePose = vehicleModelObjects.getFramePose(vehicleFrame, vehicleObject);

         String objectName = FormattingTools.underscoredToCamelCase(vehicleObject.toString(), false);
         ReferenceFrame objectFrame = new PoseReferenceFrame(objectName, framePose);
         objectFrame.update();

         YoGraphicReferenceFrame dynamicGraphicReferenceFrame = new YoGraphicReferenceFrame(objectFrame, registry, objectFrameScale);
         yoGraphicsList.add(dynamicGraphicReferenceFrame);
      }

      YoGraphicReferenceFrame vehicleFrameViz = new YoGraphicReferenceFrame(vehicleFrame, registry, vehicleFrameScale);
      yoGraphicsList.add(vehicleFrameViz);

      yoGraphicsListRegistry.registerDynamicGraphicObjectsList(yoGraphicsList);
      parentRegistry.addChild(registry);
   }

   public void update()
   {
      for (YoGraphic yoGraphic : yoGraphicsList.getYoGraphics())
      {
         yoGraphic.update();
      }
   }

   public void setVisible(boolean visible)
   {
      yoGraphicsList.setVisible(visible);
   }
}
