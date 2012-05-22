package us.ihmc.commonWalkingControlModules.controlModuleInterfaces;

import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FrameLine2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.GeometryTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class UpcomingSwingLegOrientationControllerAndDoubleSupportForceDistrubutor implements DoubleSupportForceDistributor
{
   
   private final YoVariableRegistry registry = new YoVariableRegistry("UpcomingSwingLegOrientationControllerAndDoubleSupportForceDistrubutor");
   private final ReferenceFrame pelvisFrame;
   private final ReferenceFrame midFeetZUpFrame;
   private final SideDependentList<ReferenceFrame> ankleZUpFrames;
   private final CouplingRegistry couplingRegistry;
   private final SideDependentList<FramePoint> toeMidPoints = new SideDependentList<FramePoint>();

   private final DoubleYoVariable footYawError = new DoubleYoVariable("footYawError", registry);
   private final DoubleYoVariable footYawKp = new DoubleYoVariable("footYawKp", registry);

   public UpcomingSwingLegOrientationControllerAndDoubleSupportForceDistrubutor(CommonWalkingReferenceFrames referenceFrames, double footForward,
         CouplingRegistry couplingRegistry, YoVariableRegistry parentRegistry)
   {
      this.pelvisFrame = referenceFrames.getPelvisFrame();
      this.midFeetZUpFrame = referenceFrames.getMidFeetZUpFrame();
      this.ankleZUpFrames = referenceFrames.getAnkleZUpReferenceFrames();
      this.couplingRegistry = couplingRegistry;
      for(RobotSide robotSide : RobotSide.values)
      {
         FramePoint toeMidPoint = new FramePoint(referenceFrames.getFootFrame(robotSide), footForward, 0.0, 0.0);
         toeMidPoints.put(robotSide, toeMidPoint);
      }
      
      footYawKp.set(10.0);
      
      parentRegistry.addChild(registry);
   }

   private double determineUpcomingSwingLegYawTorque(RobotSide upcomingSwingLeg)
   {
      RobotSide upcomingSupportLeg = upcomingSwingLeg.getOppositeSide();
      
      FramePoint swingToePoint = toeMidPoints.get(upcomingSwingLeg).changeFrameCopy(midFeetZUpFrame);
      FramePoint2d sweetPoint = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(upcomingSupportLeg).changeFrameCopy(midFeetZUpFrame);
      
      FramePoint2d swingToePoint2d = swingToePoint.toFramePoint2d();
      FrameLine2d controlLine = new FrameLine2d(swingToePoint2d, sweetPoint);
      FrameVector swingFootHeading = new FrameVector(ankleZUpFrames.get(upcomingSwingLeg), 1.0, 0.0, 0.0);
      swingFootHeading.changeFrame(midFeetZUpFrame);
      FrameVector2d swingFootHeading2d = swingFootHeading.toFrameVector2d();
      swingFootHeading2d.normalize();
      
      
      footYawError.set(GeometryTools.getAngleFromFirstToSecondVector(swingFootHeading2d.getVector(), controlLine.getNormalizedFrameVector().getVector()));
      
      return -footYawError.getDoubleValue() * footYawKp.getDoubleValue();
      
   }
   
   public void packForcesAndTorques(SideDependentList<Double> zForcesInPelvisFrameToPack, SideDependentList<FrameVector> torquesInPelvisFrameToPack,
         double zForceInPelvisFrameTotal, FrameVector torqueInPelvisFrameTotal, SideDependentList<Double> legStrengths,
         SideDependentList<FramePoint2d> virtualToePoints)
   {
      RobotSide upcomingSwingLeg = couplingRegistry.getUpcomingSupportLeg().getOppositeSide();
      
   // Choose toe points to use
      FramePoint upcomingSupportFootPosition = new FramePoint(ankleZUpFrames.get(upcomingSwingLeg.getOppositeSide()));
      FramePoint toeMidPoint = toeMidPoints.get(upcomingSwingLeg);
      upcomingSupportFootPosition.changeFrame(toeMidPoint.getReferenceFrame());

      // If upcoming support foot is not in front of upcoming swing foot
      boolean useAlternateToePoints = upcomingSupportFootPosition.getX() > toeMidPoint.getX() && virtualToePoints != null;
      
      for (RobotSide robotSide : RobotSide.values)
      {
         double legStrength = legStrengths.get(robotSide);

         zForcesInPelvisFrameToPack.put(robotSide, zForceInPelvisFrameTotal * legStrength);

         FrameVector torque = torqueInPelvisFrameTotal.changeFrameCopy(pelvisFrame);
         torque.scale(legStrength);
         
         if(robotSide.equals(upcomingSwingLeg) && useAlternateToePoints)
         {
            torque.setZ(determineUpcomingSwingLegYawTorque(upcomingSwingLeg) * (1.0 - legStrength));
         }

         torquesInPelvisFrameToPack.set(robotSide, torque);
      }
   }

}
