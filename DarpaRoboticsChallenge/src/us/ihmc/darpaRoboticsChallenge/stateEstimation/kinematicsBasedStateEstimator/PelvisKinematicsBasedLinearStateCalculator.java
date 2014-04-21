package us.ihmc.darpaRoboticsChallenge.stateEstimation.kinematicsBasedStateEstimator;

import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.sensors.WrenchBasedFootSwitch;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameLine2d;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoFramePoint2d;
import com.yobotics.simulationconstructionset.util.math.filter.AlphaFilteredYoFrameVector;
import com.yobotics.simulationconstructionset.util.math.filter.BacklashCompensatingVelocityYoFrameVector;
import com.yobotics.simulationconstructionset.util.math.filter.FilteredVelocityYoFrameVector;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

/**
 * PelvisKinematicsBasedPositionCalculator estimates the pelvis position and linear velocity using the leg kinematics.
 * @author Sylvain
 *
 */
public class PelvisKinematicsBasedLinearStateCalculator
{
   private static final boolean VISUALIZE = true;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final TwistCalculator twistCalculator;

   private final RigidBody rootBody;
   private final SideDependentList<ContactablePlaneBody> bipedFeet;

   private final ReferenceFrame rootJointFrame;
   private final SideDependentList<ReferenceFrame> footFrames = new SideDependentList<ReferenceFrame>();
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final DoubleYoVariable alphaFootToRootJointPosition = new DoubleYoVariable("alphaFootToRootJointPosition", registry);
   private final DoubleYoVariable alphaFootToRootJointVelocity = new DoubleYoVariable("alphaFootToRootJointVelocity", registry);
   private final DoubleYoVariable alphaFootToRootJointAccel = new DoubleYoVariable("alphaFootToRootJointAcceleration", registry);

   private final YoFramePoint rootJointPosition = new YoFramePoint("estimatedRootJointPositionWithKinematics", worldFrame, registry);   
   private final YoFrameVector rootJointLinearVelocity = new YoFrameVector("estimatedRootJointVelocityWithKinematics", worldFrame, registry);
   private final YoFrameVector rootJointLinearVelocityTwist = new YoFrameVector("estimatedRootJointVelocityWithTwist", worldFrame, registry);
   private final BooleanYoVariable useTwistToComputeRootJointLinearVelocity = new BooleanYoVariable("useTwistToComputeRootJointLinearVelocity", registry);

   private final DoubleYoVariable alphaRootJointLinearVelocityBacklashKinematics = new DoubleYoVariable("alphaRootJointLinearVelocityBacklashKinematics", registry);
   private final DoubleYoVariable slopTimeRootJointLinearVelocityBacklashKinematics = new DoubleYoVariable("slopTimeRootJointLinearVelocityBacklashKinematics", registry);
   private final BacklashCompensatingVelocityYoFrameVector rootJointLinearVelocityBacklashKinematics;

   private final SideDependentList<AlphaFilteredYoFrameVector> footToRootJointPositions = new SideDependentList<AlphaFilteredYoFrameVector>();
   private final SideDependentList<FilteredVelocityYoFrameVector> footToRootJointVelocities = new SideDependentList<FilteredVelocityYoFrameVector>();
   private final SideDependentList<FilteredVelocityYoFrameVector> footToRootJointAccelerations = new SideDependentList<FilteredVelocityYoFrameVector>();
   private final SideDependentList<Twist> rootJointToFootTwists = new SideDependentList<Twist>(new Twist(), new Twist());

   private final SideDependentList<YoFramePoint> footPositionsInWorld = new SideDependentList<YoFramePoint>();

   private final SideDependentList<YoFramePoint> copPositionsInWorld = new SideDependentList<YoFramePoint>();

   private final DoubleYoVariable alphaCoPFilter = new DoubleYoVariable("alphaCoPFilter", registry);
   private final SideDependentList<AlphaFilteredYoFramePoint2d> copsFilteredInFootFrame = new SideDependentList<AlphaFilteredYoFramePoint2d>();
   private final SideDependentList<YoFramePoint2d> copsRawInFootFrame = new SideDependentList<YoFramePoint2d>();

   private final SideDependentList<FrameLine2d> ankleToCoPLines = new SideDependentList<FrameLine2d>();

   private final SideDependentList<FrameConvexPolygon2d> footPolygons = new SideDependentList<FrameConvexPolygon2d>();

   private final BooleanYoVariable kinematicsIsUpToDate = new BooleanYoVariable("kinematicsIsUpToDate", registry);
   private final BooleanYoVariable trustCoPAsNonSlippingContactPoint = new BooleanYoVariable("trustCoPAsNonSlippingContactPoint", registry);

   // temporary variables
   private final FramePoint tempFramePoint = new FramePoint();
   private final FrameVector tempFrameVector = new FrameVector();
   private final FramePoint tempPosition = new FramePoint();
   private final FrameVector tempVelocity = new FrameVector();
   private final FramePoint2d tempCoP = new FramePoint2d();
   private final FrameVector tempCoPOffset = new FrameVector();
   private final SideDependentList<FrameLineSegment2d> tempLineSegments = new SideDependentList<FrameLineSegment2d>();

   public PelvisKinematicsBasedLinearStateCalculator(FullInverseDynamicsStructure inverseDynamicsStructure, SideDependentList<ContactablePlaneBody> bipedFeet,
         double estimatorDT, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, YoVariableRegistry parentRegistry)
   {
      this.rootBody = inverseDynamicsStructure.getRootJoint().getSuccessor();
      this.bipedFeet = bipedFeet;
      this.rootJointFrame = inverseDynamicsStructure.getRootJoint().getFrameAfterJoint();
      this.twistCalculator = inverseDynamicsStructure.getTwistCalculator();

      rootJointLinearVelocityBacklashKinematics = BacklashCompensatingVelocityYoFrameVector.createBacklashCompensatingVelocityYoFrameVector("estimatedRootJointLinearVelocityBacklashKin", "", 
            alphaRootJointLinearVelocityBacklashKinematics, estimatorDT, slopTimeRootJointLinearVelocityBacklashKinematics, registry, rootJointPosition);

      for (RobotSide robotSide : RobotSide.values)
      {
         ReferenceFrame footFrame = bipedFeet.get(robotSide).getPlaneFrame();
         footFrames.put(robotSide, footFrame);

         String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();

         AlphaFilteredYoFrameVector footToRootJointPosition = AlphaFilteredYoFrameVector.createAlphaFilteredYoFrameVector(sidePrefix + "FootToRootJointPosition", "", registry, alphaFootToRootJointPosition, worldFrame);
         footToRootJointPositions.put(robotSide, footToRootJointPosition);

         FilteredVelocityYoFrameVector footToRootJointVelocity = FilteredVelocityYoFrameVector.createFilteredVelocityYoFrameVector(sidePrefix + "FootToRootJointVelocity", "", alphaFootToRootJointVelocity, estimatorDT, registry, footToRootJointPosition);
         footToRootJointVelocities.put(robotSide, footToRootJointVelocity);

         FilteredVelocityYoFrameVector footToRootJointAccel = FilteredVelocityYoFrameVector.createFilteredVelocityYoFrameVector(sidePrefix + "FootToRootJointAcceleration", "", alphaFootToRootJointAccel, estimatorDT, registry, footToRootJointVelocity);
         footToRootJointAccelerations.put(robotSide, footToRootJointAccel);

         YoFramePoint footPositionInWorld = new YoFramePoint(sidePrefix + "FootPositionInWorld", worldFrame, registry);
         footPositionsInWorld.put(robotSide, footPositionInWorld);

         FrameConvexPolygon2d footPolygon = new FrameConvexPolygon2d(bipedFeet.get(robotSide).getContactPoints2d());
         footPolygons.put(robotSide, footPolygon);

         FrameLine2d ankleToCoPLine = new FrameLine2d(footFrame, new Point2d(), new Point2d(1.0, 0.0));
         ankleToCoPLines.put(robotSide, ankleToCoPLine);

         FrameLineSegment2d tempLineSegment = new FrameLineSegment2d(new FramePoint2d(footFrame), new FramePoint2d(footFrame, 1.0, 1.0)); // TODO need to give distinct points that's not convenient
         tempLineSegments.put(robotSide, tempLineSegment);

         YoFramePoint2d copRawInFootFrame = new YoFramePoint2d(sidePrefix + "CoPRawInFootFrame", footFrames.get(robotSide), registry);
         copsRawInFootFrame.put(robotSide, copRawInFootFrame);

         AlphaFilteredYoFramePoint2d copFilteredInFootFrame = AlphaFilteredYoFramePoint2d.createAlphaFilteredYoFramePoint2d(sidePrefix + "CoPFilteredInFootFrame", "", registry, alphaCoPFilter, copRawInFootFrame);
         copFilteredInFootFrame.update(0.0, 0.0); // So the next point will be filtered
         copsFilteredInFootFrame.put(robotSide, copFilteredInFootFrame);

         YoFramePoint copPositionInWorld = new YoFramePoint(sidePrefix + "CoPPositionsInWorld", worldFrame, registry);
         copPositionsInWorld.put(robotSide, copPositionInWorld);
      }

      if (VISUALIZE && dynamicGraphicObjectsListRegistry != null)
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
            DynamicGraphicPosition copInWorld = new DynamicGraphicPosition(sidePrefix + "StateEstimatorCoP", copPositionsInWorld.get(robotSide), 0.005, YoAppearance.DeepPink());
            dynamicGraphicObjectsListRegistry.registerArtifact("StateEstimator", copInWorld.createArtifact());
         }
      }

      parentRegistry.addChild(registry);
   }

   public void setTrustCoPAsNonSlippingContactPoint(boolean trustCoP)
   {
      trustCoPAsNonSlippingContactPoint.set(trustCoP);
   }

   public void setAlphaPelvisPosition(double alphaFilter)
   {
      alphaFootToRootJointPosition.set(alphaFilter); 
   }

   public void setAlphaPelvisLinearVelocity(double alphaFilter)
   {
      alphaFootToRootJointVelocity.set(alphaFilter);
   }

   public void setPelvisLinearVelocityBacklashParameters(double alphaFilter, double sloptime)
   {
      alphaRootJointLinearVelocityBacklashKinematics.set(alphaFilter);
      slopTimeRootJointLinearVelocityBacklashKinematics.set(sloptime);
   }

   public void setAlphaCenterOfPressure(double alphaFilter)
   {
      alphaCoPFilter.set(alphaFilter);
   }
   
   public void enableTwistEstimation(boolean enable)
   {
      useTwistToComputeRootJointLinearVelocity.set(enable);
   }

   private void reset()
   {
      rootJointPosition.setToZero();
      rootJointLinearVelocity.setToZero();
      rootJointLinearVelocityTwist.setToZero();
   }

   /**
    * Estimates the foot positions corresponding to the given pelvisPosition
    * @param pelvisPosition
    */
   public void initialize(FramePoint pelvisPosition)
   {
      updateKinematics();
      setPelvisPosition(pelvisPosition);

      for(RobotSide robotSide : RobotSide.values)
         updateFootPosition(robotSide, pelvisPosition);

      kinematicsIsUpToDate.set(false);
   }

   /**
    * Estimates the pelvis position and linear velocity using the leg kinematics
    * @param trustedSide which leg is used to estimates the pelvis state
    * @param numberOfTrustedSides is only one or both legs used to estimate the pelvis state
    */
   private void updatePelvisWithKinematics(RobotSide trustedSide, int numberOfTrustedSides)
   {
      double scaleFactor = 1.0 / numberOfTrustedSides;

      footToRootJointPositions.get(trustedSide).getFrameTuple(tempPosition);
      tempPosition.scale(scaleFactor);
      rootJointPosition.add(tempPosition);
      footPositionsInWorld.get(trustedSide).getFrameTuple(tempPosition);
      tempPosition.scale(scaleFactor);
      rootJointPosition.add(tempPosition);

      footToRootJointVelocities.get(trustedSide).getFrameTuple(tempVelocity);
      tempVelocity.scale(scaleFactor);
      rootJointLinearVelocity.add(tempVelocity);

      YoFramePoint2d copPosition2d = copsFilteredInFootFrame.get(trustedSide);
      tempFramePoint.setIncludingFrame(copPosition2d.getReferenceFrame(), copPosition2d.getX(), copPosition2d.getY(), 0.0);
      tempFramePoint.changeFrame(rootJointToFootTwists.get(trustedSide).getBaseFrame());
      rootJointToFootTwists.get(trustedSide).changeFrame(rootJointToFootTwists.get(trustedSide).getBaseFrame());
      rootJointToFootTwists.get(trustedSide).packVelocityOfPointFixedInBodyFrame(tempVelocity, tempFramePoint);
      tempVelocity.changeFrame(worldFrame);
      tempVelocity.scale(-scaleFactor); // We actually want footToPelvis velocity
      rootJointLinearVelocityTwist.add(tempVelocity);
   }

   /**
    * updates the position of the swinging foot
    * @param ignoredSide side of the swinging foot
    * @param pelvisPosition the current pelvis position
    */
   private void updateFootPosition(RobotSide ignoredSide, FramePoint pelvisPosition)
   {
      YoFramePoint footPositionInWorld = footPositionsInWorld.get(ignoredSide);
      footPositionInWorld.set(footToRootJointPositions.get(ignoredSide));
      footPositionInWorld.scale(-1.0);
      footPositionInWorld.add(pelvisPosition);

      copPositionsInWorld.get(ignoredSide).set(footPositionInWorld);

      copsRawInFootFrame.get(ignoredSide).setToZero();
      copsFilteredInFootFrame.get(ignoredSide).setToZero();
   }

   /**
    * Compute the foot CoP. The CoP is the point on the support foot trusted to be not slipping.
    * @param trustedSide
    * @param footSwitch
    */
   private void updateCoPPosition(RobotSide trustedSide, WrenchBasedFootSwitch footSwitch)
   {
      AlphaFilteredYoFramePoint2d copFilteredInFootFrame = copsFilteredInFootFrame.get(trustedSide);
      ReferenceFrame footFrame = footFrames.get(trustedSide);

      footSwitch.computeAndPackCoP(tempCoP);
      
      if (trustCoPAsNonSlippingContactPoint.getBooleanValue())
      {
         if (tempCoP.containsNaN())
         {
            tempCoP.setToZero();
         }
         else
         {
            FrameConvexPolygon2d footPolygon = footPolygons.get(trustedSide);
            boolean isCoPInsideFoot = footPolygon.isPointInside(tempCoP);
            if (!isCoPInsideFoot)
            {
               FrameLineSegment2d tempLineSegment = tempLineSegments.get(trustedSide);
               footPolygon.getClosestEdge(tempLineSegment, tempCoP);

               FrameLine2d ankleToCoPLine = ankleToCoPLines.get(trustedSide);
               ankleToCoPLine.set(footFrame, 0.0, 0.0, tempCoP.getX(), tempCoP.getY());
               tempLineSegment.intersectionWith(tempCoP, ankleToCoPLine);
            }
         }
      }
      else
      {
         tempCoP.setToZero();
      }

      copsRawInFootFrame.get(trustedSide).set(tempCoP);

      tempCoPOffset.setIncludingFrame(footFrame, copFilteredInFootFrame.getX(), copFilteredInFootFrame.getY(), 0.0);
      copFilteredInFootFrame.update();
      tempCoPOffset.setIncludingFrame(footFrame, copFilteredInFootFrame.getX() - tempCoPOffset.getX(), copFilteredInFootFrame.getY() - tempCoPOffset.getY(), 0.0);

      tempCoPOffset.changeFrame(worldFrame);
      copPositionsInWorld.get(trustedSide).add(tempCoPOffset);
   }

   /**
    * Assuming the CoP is not moving, the foot position can be updated. That way we can see if the foot is on the edge.
    * @param trustedSide
    */
   private void correctFootPositionsUsingCoP(RobotSide trustedSide)
   {
      AlphaFilteredYoFramePoint2d copFilteredInFootFrame = copsFilteredInFootFrame.get(trustedSide);
      tempCoPOffset.setIncludingFrame(copFilteredInFootFrame.getReferenceFrame(), copFilteredInFootFrame.getX(), copFilteredInFootFrame.getY(), 0.0);

      tempCoPOffset.changeFrame(worldFrame);

      YoFramePoint footPositionIWorld = footPositionsInWorld.get(trustedSide);
      footPositionIWorld.set(copPositionsInWorld.get(trustedSide));
      footPositionIWorld.sub(tempCoPOffset);
   }

   /**
    * Updates the different kinematics related stuff that is used to estimate the pelvis state
    */
   public void updateKinematics()
   {
      reset();
      twistCalculator.compute();

      for(RobotSide robotSide : RobotSide.values)
      {
         tempFramePoint.setToZero(rootJointFrame);
         tempFramePoint.changeFrame(footFrames.get(robotSide));

         tempFrameVector.setIncludingFrame(tempFramePoint);
         tempFrameVector.changeFrame(worldFrame);

         footToRootJointPositions.get(robotSide).update(tempFrameVector);
         footToRootJointVelocities.get(robotSide).update();
         footToRootJointAccelerations.get(robotSide).update();

         Twist pelvisToFootTwist = rootJointToFootTwists.get(robotSide);
         twistCalculator.packRelativeTwist(pelvisToFootTwist, rootBody, bipedFeet.get(robotSide).getRigidBody());
      }

      kinematicsIsUpToDate.set(true);
   }

   public void estimatePelvisLinearStateForDoubleSupport(SideDependentList<WrenchBasedFootSwitch> footSwitches)
   {
      estimatePelvisLinearState(footSwitches, RobotSide.values());
   }

   public void estimatePelvisLinearStateForSingleSupport(FramePoint pelvisPosition, SideDependentList<WrenchBasedFootSwitch> footSwitches, RobotSide trustedSide)
   {
      estimatePelvisLinearState(footSwitches, trustedSide);
      updateFootPosition(trustedSide.getOppositeSide(), pelvisPosition);
   }

   private void estimatePelvisLinearState(SideDependentList<WrenchBasedFootSwitch> footSwitches, RobotSide...listOfTrustedSides)
   {
      if (!kinematicsIsUpToDate.getBooleanValue())
         throw new RuntimeException("Leg kinematics needs to be updated before trying to estimate the pelvis position/linear velocity.");

      for(RobotSide trustedSide : listOfTrustedSides)
      {
         updateCoPPosition(trustedSide, footSwitches.get(trustedSide));
         correctFootPositionsUsingCoP(trustedSide);
         updatePelvisWithKinematics(trustedSide, listOfTrustedSides.length);
      }
      rootJointLinearVelocityBacklashKinematics.update();

      kinematicsIsUpToDate.set(false);
   }

   public void setPelvisPosition(FramePoint pelvisPosition)
   {
      rootJointPosition.set(pelvisPosition);
   }

   public void getRootJointPositionAndVelocity(FramePoint positionToPack, FrameVector linearVelocityToPack)
   {
      getPelvisPosition(positionToPack);
      getPelvisVelocity(linearVelocityToPack);
   }

   public void getPelvisPosition(FramePoint positionToPack)
   {
      rootJointPosition.getFrameTupleIncludingFrame(positionToPack);
   }

   public void getPelvisVelocity(FrameVector linearVelocityToPack)
   {
      if (useTwistToComputeRootJointLinearVelocity.getBooleanValue())
         rootJointLinearVelocityTwist.getFrameTupleIncludingFrame(linearVelocityToPack);
      else
         rootJointLinearVelocityBacklashKinematics.getFrameTupleIncludingFrame(linearVelocityToPack);
   }

   public void getFootToPelvisPosition(FramePoint positionToPack, RobotSide robotSide)
   {
      footToRootJointPositions.get(robotSide).getFrameTupleIncludingFrame(positionToPack);
   }

   public void getFootToPelvisVelocity(FrameVector linearVelocityToPack, RobotSide robotSide)
   {
      footToRootJointVelocities.get(robotSide).getFrameTupleIncludingFrame(linearVelocityToPack);
   }

   public void getFootToPelvisAcceleration(FrameVector linearAccelerationToPack, RobotSide robotSide)
   {
      footToRootJointAccelerations.get(robotSide).getFrameTupleIncludingFrame(linearAccelerationToPack);
   }
}
