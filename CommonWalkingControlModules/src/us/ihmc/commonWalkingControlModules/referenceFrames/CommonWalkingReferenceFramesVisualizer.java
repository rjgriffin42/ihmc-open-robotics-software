package us.ihmc.commonWalkingControlModules.referenceFrames;

import java.util.ArrayList;

import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicReferenceFrame;

import com.yobotics.simulationconstructionset.robotController.RobotController;

public class CommonWalkingReferenceFramesVisualizer implements Updatable, RobotController
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final ArrayList<YoGraphicReferenceFrame> referenceFramesVisualizers = new ArrayList<YoGraphicReferenceFrame>();

   public CommonWalkingReferenceFramesVisualizer(CommonHumanoidReferenceFrames referenceFrames,
         YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      String vizName = referenceFrames.getClass().getSimpleName();
      for (RobotSide robotSide : RobotSide.values)
      {
         YoGraphicReferenceFrame yoGraphic = new YoGraphicReferenceFrame(referenceFrames.getAnkleZUpFrame(robotSide), registry, 0.2);
         yoGraphicsListRegistry.registerYoGraphic(vizName, yoGraphic);
         referenceFramesVisualizers.add(yoGraphic);
      }

      YoGraphicReferenceFrame midFeetFrame = new YoGraphicReferenceFrame(referenceFrames.getMidFeetZUpFrame(), registry, 0.2);
      yoGraphicsListRegistry.registerYoGraphic(vizName, midFeetFrame);
      referenceFramesVisualizers.add(midFeetFrame);
      YoGraphicReferenceFrame comFrame = new YoGraphicReferenceFrame(referenceFrames.getCenterOfMassFrame(), registry, 0.2);
      yoGraphicsListRegistry.registerYoGraphic(vizName, comFrame);
      referenceFramesVisualizers.add(comFrame);

   }

   @Override
   public void initialize()
   {
      doControl();
   }

   @Override
   public void doControl()
   {
      for (int i = 0; i < referenceFramesVisualizers.size(); i++)
      {
         referenceFramesVisualizers.get(i).update();
      }
   }

   @Override
   public void update(double time)
   {
      doControl();
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public String getDescription()
   {
      return getName();
   }
}
