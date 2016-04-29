package us.ihmc.commonWalkingControlModules.controlModules.foot;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.CenterOfPressureCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.sensors.footSwitch.FootSwitchInterface;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class ExploreFootPolygonState extends AbstractFootControlState
{
   private boolean done = true;
   private enum ExplorationMethod
   {
      SPRIAL, LINES
   };
   private final ExplorationMethod method = ExplorationMethod.LINES;

   private final HoldPositionState internalHoldPositionState;

   private final FrameConvexPolygon2d supportPolygon = new FrameConvexPolygon2d();

   private final CenterOfPressureCommand centerOfPressureCommand = new CenterOfPressureCommand();

   private final FramePoint2d cop = new FramePoint2d();
   private final FramePoint2d desiredCoP = new FramePoint2d();
   private final PartialFootholdControlModule partialFootholdControlModule;

   private final FootSwitchInterface footSwitch;

   private final DoubleYoVariable lastShrunkTime, spiralAngle;
   private final double dt;

   /**
    * This is the amount of time after touch down during which no foothold exploration is done
    */
   private final DoubleYoVariable recoverTime;
   private final static double defaultRecoverTime = 0.1;

   public ExploreFootPolygonState(FootControlHelper footControlHelper, YoSE3PIDGainsInterface gains, YoVariableRegistry registry)
   {
      super(ConstraintType.EXPLORE_POLYGON, footControlHelper, registry);
      dt = momentumBasedController.getControlDT();

      YoVariableRegistry childRegistry = new YoVariableRegistry("ExploreFootPolygon");
      registry.addChild(childRegistry);
      internalHoldPositionState = new HoldPositionState(footControlHelper, gains, childRegistry);
      internalHoldPositionState.setDoSmartHoldPosition(false);
      internalHoldPositionState.doFootholdAdjustments(false);

      partialFootholdControlModule = footControlHelper.getPartialFootholdControlModule();
      footSwitch = momentumBasedController.getFootSwitches().get(robotSide);

      centerOfPressureCommand.setContactingRigidBody(contactableFoot.getRigidBody());
      centerOfPressureCommand.setWeight(new Vector2d(2000.0, 2000.0));

      lastShrunkTime = new DoubleYoVariable(contactableFoot.getName() + "LastShrunkTime", registry);
      spiralAngle = new DoubleYoVariable(contactableFoot.getName() + "SpiralAngle", registry);

      recoverTime = new DoubleYoVariable(contactableFoot.getName() + "RecoverTime", registry);
      recoverTime.set(defaultRecoverTime);

      desiredOrientation.setToZero();
      desiredAngularVelocity.setToZero(worldFrame);
      desiredAngularAcceleration.setToZero(worldFrame);
   }

   public void setWeight(double weight)
   {
      internalHoldPositionState.setWeight(weight);
   }

   public void setWeights(Vector3d angular, Vector3d linear)
   {
      internalHoldPositionState.setWeights(angular, linear);
   }

   @Override
   public void doTransitionIntoAction()
   {
      super.doTransitionIntoAction();
      lastShrunkTime.set(0.0);
      spiralAngle.set(0.0);
      internalHoldPositionState.doTransitionIntoAction();
      done = false;
   }

   @Override
   public void doTransitionOutOfAction()
   {
      super.doTransitionOutOfAction();
      internalHoldPositionState.doTransitionOutOfAction();
   }

   private final FramePoint2d shrunkPolygonCentroid = new FramePoint2d();
   private final FramePoint2d desiredCenterOfPressure = new FramePoint2d();
   @Override
   public void doSpecificAction()
   {
      double timeInState = getTimeInCurrentState();

      if (timeInState > recoverTime.getDoubleValue() && !done)
      {
         footSwitch.computeAndPackCoP(cop);
         momentumBasedController.getDesiredCenterOfPressure(contactableFoot, desiredCoP);
         partialFootholdControlModule.compute(desiredCoP, cop);
         YoPlaneContactState contactState = momentumBasedController.getContactState(contactableFoot);
         boolean contactStateHasChanged = partialFootholdControlModule.applyShrunkPolygon(contactState);
         if (contactStateHasChanged)
         {
            contactState.notifyContactStateHasChanged();
            lastShrunkTime.set(timeInState);
            spiralAngle.add(Math.PI/2.0);
            done = false;
         }

         // Foot exploration through CoP shifting...
         if (method == ExplorationMethod.SPRIAL)
         {
            double freq = 0.6;
            double rampOutDuration = 0.3;
            double settleDuration = 0.1;

            double percentRampOut = (timeInState - lastShrunkTime.getDoubleValue() - settleDuration) / rampOutDuration;
            rampOutDuration = MathTools.clipToMinMax(rampOutDuration, 0.0, 1.0);

            boolean doSpiral = timeInState - lastShrunkTime.getDoubleValue() - settleDuration > rampOutDuration;

            if (doSpiral)
            {
               spiralAngle.add(2.0 * Math.PI * freq * dt);
            }

            if (timeInState - lastShrunkTime.getDoubleValue() - settleDuration - rampOutDuration > 1.0/freq)
            {
               done = true;
            }

            ReferenceFrame soleFrame = footControlHelper.getContactableFoot().getSoleFrame();
            partialFootholdControlModule.getShrunkPolygonCentroid(shrunkPolygonCentroid);
            shrunkPolygonCentroid.changeFrame(soleFrame);

            desiredCenterOfPressure.setIncludingFrame(soleFrame,
                  shrunkPolygonCentroid.getX() + 0.10 * percentRampOut * Math.cos(spiralAngle.getDoubleValue()),
                  shrunkPolygonCentroid.getY() + 0.05 * percentRampOut * Math.sin(spiralAngle.getDoubleValue()));

            partialFootholdControlModule.projectOntoShrunkenPolygon(desiredCenterOfPressure);
            desiredCenterOfPressure.scale(0.9);
         }
         else if (method == ExplorationMethod.LINES)
         {
            partialFootholdControlModule.getSupportPolygon(supportPolygon);
            FramePoint2d centroid = supportPolygon.getCentroid();
            int corners = supportPolygon.getNumberOfVertices();

            double timeToGoToCorner = 0.25;
            double timeToStayAtCorner = 0.25;

            double timeExploring = timeInState - lastShrunkTime.getDoubleValue();
            double timeToExploreCorner = timeToGoToCorner + timeToStayAtCorner;

            int currentCornerIdx = (int) (timeExploring / timeToExploreCorner);
            if (currentCornerIdx >= corners)
            {
               done = true;
            }
            else
            {
               FramePoint2d curretCorner = new FramePoint2d();
               supportPolygon.getFrameVertex(currentCornerIdx, curretCorner);

               double timeExploringCurrentCorner = timeExploring - (double)currentCornerIdx * timeToExploreCorner;
               if (timeExploringCurrentCorner <= timeToGoToCorner)
               {
                  double percent = timeExploringCurrentCorner / timeToGoToCorner;
                  percent = MathTools.clipToMinMax(percent, 0.0, 1.0);
                  desiredCenterOfPressure.interpolate(centroid, curretCorner, percent);
               }
               else
               {
                  desiredCenterOfPressure.set(curretCorner);
               }
            }

         }
         centerOfPressureCommand.setDesiredCoP(desiredCenterOfPressure.getPoint());
      }
      else
      {
         lastShrunkTime.set(timeInState);
         spiralAngle.set(0.0);
      }

      internalHoldPositionState.doSpecificAction();
   }

   @Override
   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      if (getTimeInCurrentState() > recoverTime.getDoubleValue() && !done)
      {
         return centerOfPressureCommand;
      }
      return null;
   }

   @Override
   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      return internalHoldPositionState.getFeedbackControlCommand();
   }

}

