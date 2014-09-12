package us.ihmc.commonWalkingControlModules.kinematics;

import us.ihmc.CapturePointCalculator.LinearInvertedPendulumCapturePointCalculator;
import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.DesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.ProcessedSensorsInterface;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;


public class BodyPositionInTimeEstimator
{
   private final ProcessedSensorsInterface processedSensors;
   private final ReferenceFrame pelvisFrame;
   private final ReferenceFrame desiredHeadingFrame;
   private final SideDependentList<ReferenceFrame> ankleZUpFrames;
   private final CouplingRegistry couplingRegistry;
   
   private final YoVariableRegistry registry = new YoVariableRegistry("BodyPositionInTimeEstimator");
   private final DoubleYoVariable currentCoMHeight = new DoubleYoVariable("currentCoMHeight", registry);
   private final YoFramePoint estimatedUpperBodyPosition;
   private final YoFramePoint currentUpperBodyPosition; 
   
   private final YoFrameVector currentUpperBodyVelocity;
   private final YoFrameVector estimatedUpperBodyVelocity;
   
   public BodyPositionInTimeEstimator(ProcessedSensorsInterface processedSensors, CommonWalkingReferenceFrames referenceFrames,
         DesiredHeadingControlModule desiredHeadingControlModule,
         CouplingRegistry couplingRegistry, YoVariableRegistry parentRegistry)
   {
      this.processedSensors = processedSensors;
      this.pelvisFrame = referenceFrames.getPelvisFrame();
      this.desiredHeadingFrame = desiredHeadingControlModule.getDesiredHeadingFrame();
      this.ankleZUpFrames = referenceFrames.getAnkleZUpReferenceFrames();
      this.couplingRegistry = couplingRegistry;
      this.estimatedUpperBodyPosition = new YoFramePoint("estimatedUpperBodyPosition", desiredHeadingFrame, registry);
      this.currentUpperBodyPosition = new YoFramePoint("currentUpperBodyPosition", desiredHeadingFrame, registry);
      
      currentUpperBodyVelocity = new YoFrameVector("currentUpperBodyVelocity", desiredHeadingFrame, registry);
      estimatedUpperBodyVelocity = new YoFrameVector("estimatedUpperBodyVelocity", desiredHeadingFrame, registry);
      referenceFrames.getABodyAttachedZUpFrame();
      
      referenceFrames.getFootReferenceFrames();
      referenceFrames.getAnkleZUpReferenceFrames();
      parentRegistry.addChild(registry);
      
   }
   
   
   public Pair<FramePose, FrameVector> getPelvisPoseAndVelocityInTime(double t, RobotSide swingFoot)
   {
      FramePoint2d currentCoPPosition = couplingRegistry.getDesiredCoP();
      currentCoPPosition.changeFrame(desiredHeadingFrame);
      FramePoint currentCoMPosition = processedSensors.getCenterOfMassPositionInFrame(desiredHeadingFrame);
      FrameVector currentCoMVelocity = processedSensors.getCenterOfMassVelocityInFrame(desiredHeadingFrame);
      
      
      FramePoint bodyPosition = new FramePoint(pelvisFrame);      
      bodyPosition.changeFrame(desiredHeadingFrame);
      
      currentUpperBodyPosition.set(bodyPosition);
      currentUpperBodyVelocity.set(currentCoMVelocity);
      
      // LIPM Model in time
      FramePoint currentCoMPositionInStanceFoot = new FramePoint(currentCoMPosition);
      currentCoMPositionInStanceFoot.changeFrame(ankleZUpFrames.get(swingFoot.getOppositeSide()));
      double comHeight = currentCoMPositionInStanceFoot.getZ();
      currentCoMHeight.set(comHeight);
      
      double gravity = Math.abs(processedSensors.getGravityInWorldFrame().getZ());
      
      double[] xCoMInTime = LinearInvertedPendulumCapturePointCalculator.calculatePredictedCoMState(
                           currentCoMPosition.getX(), currentCoPPosition.getX(), currentCoMVelocity.getX(), 
                           gravity, comHeight, 0.0, t);
      double[] yCoMInTime = LinearInvertedPendulumCapturePointCalculator.calculatePredictedCoMState(
                           currentCoMPosition.getY(), currentCoPPosition.getY(), currentCoMVelocity.getY(), 
                           gravity, comHeight, 0.0, t);
      
      double xDeltaInTime = xCoMInTime[0] - currentCoMPosition.getX();
      double yDeltaInTime = yCoMInTime[0] - currentCoMPosition.getY();
      bodyPosition.setX(bodyPosition.getX() + xDeltaInTime);
      bodyPosition.setY(bodyPosition.getY() + yDeltaInTime);
      estimatedUpperBodyPosition.set(bodyPosition);
      bodyPosition.changeFrame(pelvisFrame);
      
      FramePose pelvisPoseInTime = new FramePose(pelvisFrame);
      pelvisPoseInTime.setPosition(bodyPosition);
      
      
      
      FrameVector pelvisVelocityInTime = new FrameVector(desiredHeadingFrame, currentCoMVelocity.getX(), currentCoMVelocity.getY(), 0.0);
      estimatedUpperBodyVelocity.set(pelvisVelocityInTime);
      pelvisVelocityInTime.changeFrame(pelvisFrame);
      
      
      return new Pair<FramePose, FrameVector>(pelvisPoseInTime, pelvisVelocityInTime);
      
   }
}
