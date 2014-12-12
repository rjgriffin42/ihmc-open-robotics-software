package us.ihmc.stateEstimation.kinematicsBasedStateEstimation;

import us.ihmc.commonWalkingControlModules.sensors.WrenchBasedFootSwitch;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.yoUtilities.graphics.plotting.YoArtifactPosition;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;


public class CenterOfPressureVisualizer
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final SideDependentList<YoFramePoint> footRawCoPPositionsInWorld = new SideDependentList<>();
   private final YoFramePoint overallRawCoPPositionInWorld;
   private final FramePoint2d tempRawCoP2d = new FramePoint2d();
   private final FramePoint tempRawCoP = new FramePoint();
   private final Wrench tempWrench = new Wrench();
   private final SideDependentList<WrenchBasedFootSwitch> footSwitches;

   public CenterOfPressureVisualizer(SideDependentList<WrenchBasedFootSwitch> footSwitches,
         YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      this.footSwitches = footSwitches;

      for (RobotSide robotSide : RobotSide.values)
      {
         String side = robotSide.getCamelCaseNameForMiddleOfExpression();
         YoFramePoint rawCoPPositionInWorld = new YoFramePoint("raw" + side + "CoPPositionsInWorld", worldFrame, registry);
         footRawCoPPositionsInWorld.put(robotSide, rawCoPPositionInWorld);

         YoGraphicPosition copDynamicGraphic = new YoGraphicPosition("Meas " + side + "CoP", rawCoPPositionInWorld, 0.008, YoAppearance.DarkRed(), GraphicType.DIAMOND);
         YoArtifactPosition copArtifact = copDynamicGraphic.createArtifact();
         yoGraphicsListRegistry.registerArtifact("StateEstimator", copArtifact);
      }

      overallRawCoPPositionInWorld = new YoFramePoint("overallRawCoPPositionInWorld", worldFrame, registry);
      YoGraphicPosition overallRawCoPDynamicGraphic = new YoGraphicPosition("Meas CoP", overallRawCoPPositionInWorld, 0.015, YoAppearance.DarkRed(), GraphicType.DIAMOND);
      YoArtifactPosition overallRawCoPArtifact = overallRawCoPDynamicGraphic.createArtifact();
      yoGraphicsListRegistry.registerArtifact("StateEstimator", overallRawCoPArtifact);

      parentRegistry.addChild(registry);
   }

   public void update()
   {
      if (footRawCoPPositionsInWorld != null)
      {
         overallRawCoPPositionInWorld.setToZero();
         double totalFootForce = 0.0;

         for (RobotSide robotSide : RobotSide.values)
         {
            footSwitches.get(robotSide).computeAndPackCoP(tempRawCoP2d);
            tempRawCoP.setIncludingFrame(tempRawCoP2d.getReferenceFrame(), tempRawCoP2d.getX(), tempRawCoP2d.getY(), 0.0);
            tempRawCoP.changeFrame(worldFrame);
            footRawCoPPositionsInWorld.get(robotSide).set(tempRawCoP);

            footSwitches.get(robotSide).computeAndPackFootWrench(tempWrench);
            double singleFootForce = tempWrench.getLinearPartZ();
            totalFootForce += singleFootForce;
            tempRawCoP.scale(singleFootForce);
            overallRawCoPPositionInWorld.add(tempRawCoP);
         }

         overallRawCoPPositionInWorld.scale(1.0 / totalFootForce);
      }
   }

   public void hide()
   {
      for (RobotSide robotSide : RobotSide.values)
         footRawCoPPositionsInWorld.get(robotSide).setToNaN();
      overallRawCoPPositionInWorld.setToNaN();
   }
}
