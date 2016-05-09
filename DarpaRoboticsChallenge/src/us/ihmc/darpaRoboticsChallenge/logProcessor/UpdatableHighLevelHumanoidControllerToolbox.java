package us.ihmc.darpaRoboticsChallenge.logProcessor;

import java.util.ArrayList;

import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.commonWalkingControlModules.momentumBasedController.GeometricJacobianHolder;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commonWalkingControlModules.momentumBasedController.PlaneContactWrenchProcessor;
import us.ihmc.commonWalkingControlModules.sensors.footSwitch.FootSwitchInterface;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactableFoot;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.robotics.sensors.ForceSensorDataReadOnly;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class UpdatableHighLevelHumanoidControllerToolbox extends HighLevelHumanoidControllerToolbox
{
   private final YoFramePoint capturePointUpdatedFromSCS;
   private final SideDependentList<YoFramePoint2d> desiredCoPsUpdatedFromSCS = new SideDependentList<>();

   public UpdatableHighLevelHumanoidControllerToolbox(SimulationConstructionSet scs,
         FullHumanoidRobotModel fullRobotModel, GeometricJacobianHolder robotJacobianHolder,
         CommonHumanoidReferenceFrames referenceFrames, SideDependentList<FootSwitchInterface> footSwitches,
         SideDependentList<ForceSensorDataReadOnly> wristForceSensors, DoubleYoVariable yoTime, double gravityZ, double omega0, TwistCalculator twistCalculator,
         SideDependentList<ContactableFoot> feet, SideDependentList<ContactablePlaneBody> hands, double controlDT, ArrayList<Updatable> updatables,
         YoGraphicsListRegistry yoGraphicsListRegistry, InverseDynamicsJoint... jointsToIgnore)
   {
      super(fullRobotModel, robotJacobianHolder, referenceFrames, footSwitches, wristForceSensors, yoTime, gravityZ, omega0, twistCalculator, feet, hands, controlDT,
            updatables, yoGraphicsListRegistry, jointsToIgnore);

      String capturePointNameSpace = HighLevelHumanoidControllerToolbox.class.getSimpleName();
      DoubleYoVariable capturePointX = (DoubleYoVariable) scs.getVariable(capturePointNameSpace, "capturePointX");
      DoubleYoVariable capturePointY = (DoubleYoVariable) scs.getVariable(capturePointNameSpace, "capturePointY");
      DoubleYoVariable capturePointZ = (DoubleYoVariable) scs.getVariable(capturePointNameSpace, "capturePointZ");
      capturePointUpdatedFromSCS = new YoFramePoint(capturePointX, capturePointY, capturePointZ, worldFrame);

      for (RobotSide robotSide : RobotSide.values)
      {
         String side = robotSide.getCamelCaseNameForMiddleOfExpression();
         String desiredCoPNameSpace = PlaneContactWrenchProcessor.class.getSimpleName();
         String desiredCoPName = side + "SoleCoP2d";
         DoubleYoVariable desiredCoPx = (DoubleYoVariable) scs.getVariable(desiredCoPNameSpace, desiredCoPName + "X");
         DoubleYoVariable desiredCoPy = (DoubleYoVariable) scs.getVariable(desiredCoPNameSpace, desiredCoPName + "Y");
         ReferenceFrame soleFrame = referenceFrames.getSoleFrame(robotSide);
         YoFramePoint2d desiredCoP = new YoFramePoint2d(desiredCoPx, desiredCoPy, soleFrame);
         desiredCoPsUpdatedFromSCS.put(robotSide, desiredCoP);
      }
   }

   @Override
   public void update()
   {
      // update the yoCapturePoint
      yoCapturePoint.set(capturePointUpdatedFromSCS);

      // update the bipedSupportPolygons
      updateBipedSupportPolygons();

      // update the footDesiredCenterOfPressures
      for (RobotSide robotSide : RobotSide.values)
      {
         ContactableFoot contactableFoot = feet.get(robotSide);
         FramePoint2d desiredCop = desiredCoPsUpdatedFromSCS.get(robotSide).getFrameTuple2d();
         setDesiredCenterOfPressure(contactableFoot, desiredCop);
      }
   }
}
