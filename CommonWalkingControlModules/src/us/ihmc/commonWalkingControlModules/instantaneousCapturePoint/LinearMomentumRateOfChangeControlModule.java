package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import static us.ihmc.graphicsDescription.appearance.YoAppearance.Purple;

import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.MomentumRateCommand;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationController;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.WrenchDistributorTools;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.YoArtifactPosition;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.robotics.MathTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.frames.YoFrameConvexPolygon2d;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
import us.ihmc.sensorProcessing.frames.ReferenceFrames;

public abstract class LinearMomentumRateOfChangeControlModule
{
   protected static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   protected final YoVariableRegistry registry;

   protected final YoFrameVector defaultLinearMomentumRateWeight;
   protected final YoFrameVector defaultAngularMomentumRateWeight;
   protected final YoFrameVector highLinearMomentumRateWeight;
   protected final YoFrameVector angularMomentumRateWeight;
   protected final YoFrameVector linearMomentumRateWeight;

   protected final YoEnum<RobotSide> supportLegPreviousTick;
   protected final YoBoolean minimizeAngularMomentumRateZ;

   protected final YoFrameVector controlledCoMAcceleration;

   protected final MomentumRateCommand momentumRateCommand = new MomentumRateCommand();
   protected final SelectionMatrix6D linearAndAngularZSelectionMatrix = new SelectionMatrix6D();
   protected final SelectionMatrix6D linearXYSelectionMatrix = new SelectionMatrix6D();
   protected final SelectionMatrix6D linearXYAndAngularZSelectionMatrix = new SelectionMatrix6D();

   protected double omega0 = 0.0;
   protected double totalMass;
   protected double gravityZ;

   protected final ReferenceFrame centerOfMassFrame;
   protected final FramePoint3D centerOfMass;
   protected final FramePoint2d centerOfMass2d = new FramePoint2d();

   protected final FramePoint2d capturePoint = new FramePoint2d();
   protected final FramePoint2d desiredCapturePoint = new FramePoint2d();
   protected final FrameVector2d desiredCapturePointVelocity = new FrameVector2d();
   protected final FramePoint2d finalDesiredCapturePoint = new FramePoint2d();

   protected final FramePoint2d perfectCMP = new FramePoint2d();
   protected final FramePoint2d desiredCMP = new FramePoint2d();

   protected final FrameConvexPolygon2d supportPolygon = new FrameConvexPolygon2d();

   protected final CMPProjector cmpProjector;
   protected final FrameConvexPolygon2d areaToProjectInto = new FrameConvexPolygon2d();
   protected final FrameConvexPolygon2d safeArea = new FrameConvexPolygon2d();

   protected final YoBoolean desiredCMPinSafeArea;

   private boolean controlHeightWithMomentum;

   protected final YoFramePoint2d yoUnprojectedDesiredCMP;
   protected final YoFrameConvexPolygon2d yoSafeAreaPolygon;
   protected final YoFrameConvexPolygon2d yoProjectionPolygon;

   protected final FrameVector2d achievedCoMAcceleration2d = new FrameVector2d();
   protected double desiredCoMHeightAcceleration = 0.0;

   protected RobotSide supportSide = null;
   protected RobotSide transferToSide = null;

   public LinearMomentumRateOfChangeControlModule(String namePrefix, ReferenceFrames referenceFrames, double gravityZ,
         double totalMass, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry, boolean use2DProjection)
   {
      MathTools.checkIntervalContains(gravityZ, 0.0, Double.POSITIVE_INFINITY);

      this.totalMass = totalMass;
      this.gravityZ = gravityZ;

      registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());

      if (use2DProjection)
         cmpProjector = new SmartCMPProjector(yoGraphicsListRegistry, registry);
      else
         cmpProjector = new SmartCMPPlanarProjector(registry);

      centerOfMassFrame = referenceFrames.getCenterOfMassFrame();
      centerOfMass = new FramePoint3D(centerOfMassFrame);

      controlledCoMAcceleration = new YoFrameVector(namePrefix + "ControlledCoMAcceleration", "", centerOfMassFrame, registry);

      defaultLinearMomentumRateWeight = new YoFrameVector(namePrefix + "DefaultLinearMomentumRateWeight", worldFrame, registry);
      defaultAngularMomentumRateWeight = new YoFrameVector(namePrefix + "DefaultAngularMomentumRateWeight", worldFrame, registry);
      highLinearMomentumRateWeight = new YoFrameVector(namePrefix + "HighLinearMomentumRateWeight", worldFrame, registry);
      angularMomentumRateWeight = new YoFrameVector(namePrefix + "AngularMomentumRateWeight", worldFrame, registry);
      linearMomentumRateWeight = new YoFrameVector(namePrefix + "LinearMomentumRateWeight", worldFrame, registry);

      supportLegPreviousTick = YoEnum.create(namePrefix + "SupportLegPreviousTick", "", RobotSide.class, registry, true);
      minimizeAngularMomentumRateZ = new YoBoolean(namePrefix + "MinimizeAngularMomentumRateZ", registry);

      desiredCMPinSafeArea = new YoBoolean("DesiredCMPinSafeArea", registry);

      yoUnprojectedDesiredCMP = new YoFramePoint2d("unprojectedDesiredCMP", worldFrame, registry);
      yoSafeAreaPolygon = new YoFrameConvexPolygon2d("yoSafeAreaPolygon", worldFrame, 10, registry);
      yoProjectionPolygon = new YoFrameConvexPolygon2d("yoProjectionPolygon", worldFrame, 10, registry);

      linearAndAngularZSelectionMatrix.selectAngularX(false);
      linearAndAngularZSelectionMatrix.selectAngularY(false);

      linearXYSelectionMatrix.setToLinearSelectionOnly();
      linearXYSelectionMatrix.selectLinearZ(false); // remove height

      linearXYAndAngularZSelectionMatrix.setToLinearSelectionOnly();
      linearXYAndAngularZSelectionMatrix.selectLinearZ(false); // remove height
      linearXYAndAngularZSelectionMatrix.selectAngularZ(true);

      angularMomentumRateWeight.set(defaultAngularMomentumRateWeight);
      linearMomentumRateWeight.set(defaultLinearMomentumRateWeight);

      momentumRateCommand.setWeights(0.0, 0.0, 0.0, linearMomentumRateWeight.getX(), linearMomentumRateWeight.getY(), linearMomentumRateWeight.getZ());

      if (yoGraphicsListRegistry != null)
      {
         String graphicListName = getClass().getSimpleName();
         YoGraphicPosition unprojectedDesiredCMPViz = new YoGraphicPosition("Unprojected Desired CMP", yoUnprojectedDesiredCMP, 0.008, Purple(), YoGraphicPosition.GraphicType.BALL_WITH_ROTATED_CROSS);
         YoArtifactPosition artifact = unprojectedDesiredCMPViz.createArtifact();
         artifact.setVisible(false);
         yoGraphicsListRegistry.registerArtifact(graphicListName, artifact);

         //         YoArtifactPolygon yoSafeArea = new YoArtifactPolygon("SafeArea", yoSafeAreaPolygon, Color.GREEN, false);
         //         yoGraphicsListRegistry.registerArtifact(graphicListName, yoSafeArea);
         //
         //         YoArtifactPolygon yoProjectionArea = new YoArtifactPolygon("ProjectionArea", yoProjectionPolygon, Color.RED, false);
         //         yoGraphicsListRegistry.registerArtifact(graphicListName, yoProjectionArea);
      }
      yoUnprojectedDesiredCMP.setToNaN();

      parentRegistry.addChild(registry);
   }


   public void setMomentumWeight(Vector3D angularWeight, Vector3D linearWeight)
   {
      defaultLinearMomentumRateWeight.set(linearWeight);
      defaultAngularMomentumRateWeight.set(angularWeight);
   }

   public void setMomentumWeight(Vector3D linearWeight)
   {
      defaultLinearMomentumRateWeight.set(linearWeight);
   }

   public void setAngularMomentumWeight(Vector3D angularWeight)
   {
      defaultAngularMomentumRateWeight.set(angularWeight);
   }

   public void setHighMomentumWeightForRecovery(Vector3D highLinearWeight)
   {
      highLinearMomentumRateWeight.set(highLinearWeight);
   }

   public void setSupportLeg(RobotSide newSupportSide)
   {
      supportSide = newSupportSide;
   }

   public void setTransferToSide(RobotSide transferToSide)
   {
      this.transferToSide = transferToSide;
   }

   public void setTransferFromSide(RobotSide robotSide)
   {
      if (robotSide != null)
         this.transferToSide = robotSide.getOppositeSide();
   }


   public void setOmega0(double omega0)
   {
      if (Double.isNaN(omega0))
         throw new RuntimeException("omega0 is NaN");
      this.omega0 = omega0;
   }

   public void setCapturePoint(FramePoint2d capturePoint)
   {
      this.capturePoint.setIncludingFrame(capturePoint);
   }

   public void setDesiredCapturePoint(FramePoint2d desiredCapturePoint)
   {
      this.desiredCapturePoint.setIncludingFrame(desiredCapturePoint);
   }

   public void setDesiredCapturePointVelocity(FrameVector2d desiredCapturePointVelocity)
   {
      this.desiredCapturePointVelocity.setIncludingFrame(desiredCapturePointVelocity);
   }

   public void setHighMomentumWeight()
   {
      linearMomentumRateWeight.set(highLinearMomentumRateWeight);
      angularMomentumRateWeight.set(defaultAngularMomentumRateWeight);
   }

   public void setDefaultMomentumWeight()
   {
      linearMomentumRateWeight.set(defaultLinearMomentumRateWeight);
      angularMomentumRateWeight.set(defaultAngularMomentumRateWeight);
   }

   public void setDesiredCenterOfMassHeightAcceleration(double desiredCenterOfMassHeightAcceleration)
   {
      desiredCoMHeightAcceleration = desiredCenterOfMassHeightAcceleration;
   }

   public MomentumRateCommand getMomentumRateCommand()
   {
      return momentumRateCommand;
   }

   public void computeAchievedCMP(FrameVector achievedLinearMomentumRate, FramePoint2d achievedCMPToPack)
   {
      if (achievedLinearMomentumRate.containsNaN())
         return;

      centerOfMass2d.setToZero(centerOfMassFrame);
      centerOfMass2d.changeFrame(worldFrame);

      achievedCoMAcceleration2d.setByProjectionOntoXYPlaneIncludingFrame(achievedLinearMomentumRate);
      achievedCoMAcceleration2d.scale(1.0 / totalMass);
      achievedCoMAcceleration2d.changeFrame(worldFrame);

      achievedCMPToPack.set(achievedCoMAcceleration2d);
      achievedCMPToPack.scale(-1.0 / (omega0 * omega0));
      achievedCMPToPack.add(centerOfMass2d);
   }

   private final FramePoint3D cmp3d = new FramePoint3D();
   private final FrameVector groundReactionForce = new FrameVector();

   protected FrameVector computeGroundReactionForce(FramePoint2d cmp2d, double fZ)
   {
      centerOfMass.setToZero(centerOfMassFrame);
      WrenchDistributorTools.computePseudoCMP3d(cmp3d, centerOfMass, cmp2d, fZ, totalMass, omega0);

      centerOfMass.setToZero(centerOfMassFrame);
      WrenchDistributorTools.computeForce(groundReactionForce, centerOfMass, cmp3d, fZ);
      groundReactionForce.changeFrame(centerOfMassFrame);

      return groundReactionForce;
   }

   private boolean desiredCMPcontainedNaN = false;

   public void compute(FramePoint2d desiredCMPPreviousValue, FramePoint2d desiredCMPToPack)
   {
      computeCMPInternal(desiredCMPPreviousValue);

      capturePoint.changeFrame(worldFrame);
      desiredCMP.changeFrame(worldFrame);
      if (desiredCMP.containsNaN())
      {
         if (!desiredCMPcontainedNaN)
            PrintTools.error("Desired CMP containes NaN, setting it to the ICP - only showing this error once");
         desiredCMP.set(capturePoint);
         desiredCMPcontainedNaN = true;
      }
      else
      {
         desiredCMPcontainedNaN = false;
      }

      desiredCMPToPack.setIncludingFrame(desiredCMP);
      desiredCMPToPack.changeFrame(worldFrame);

      supportLegPreviousTick.set(supportSide);

      double fZ = WrenchDistributorTools.computeFz(totalMass, gravityZ, desiredCoMHeightAcceleration);
      FrameVector linearMomentumRateOfChange = computeGroundReactionForce(desiredCMP, fZ);
      linearMomentumRateOfChange.changeFrame(centerOfMassFrame);
      linearMomentumRateOfChange.setZ(linearMomentumRateOfChange.getZ() - totalMass * gravityZ);

      if (linearMomentumRateOfChange.containsNaN())
         throw new RuntimeException("linearMomentumRateOfChange = " + linearMomentumRateOfChange);

      controlledCoMAcceleration.set(linearMomentumRateOfChange);
      controlledCoMAcceleration.scale(1.0 / totalMass);

      linearMomentumRateOfChange.changeFrame(worldFrame);
      momentumRateCommand.setLinearMomentumRate(linearMomentumRateOfChange);

      if (minimizeAngularMomentumRateZ.getBooleanValue())
      {
         if (!controlHeightWithMomentum)
            momentumRateCommand.setSelectionMatrix(linearXYAndAngularZSelectionMatrix);
         else
            momentumRateCommand.setSelectionMatrix(linearAndAngularZSelectionMatrix);
      }
      else
      {
         if (!controlHeightWithMomentum)
            momentumRateCommand.setSelectionMatrix(linearXYSelectionMatrix);
         else
            momentumRateCommand.setSelectionMatrixForLinearControl();
      }

      momentumRateCommand.setWeights(angularMomentumRateWeight.getX(), angularMomentumRateWeight.getY(), angularMomentumRateWeight.getZ(),
            linearMomentumRateWeight.getX(), linearMomentumRateWeight.getY(), linearMomentumRateWeight.getZ());
   }

   public void setCMPProjectionArea(FrameConvexPolygon2d areaToProjectInto, FrameConvexPolygon2d safeArea)
   {
      this.areaToProjectInto.setIncludingFrameAndUpdate(areaToProjectInto);
      this.safeArea.setIncludingFrameAndUpdate(safeArea);

      yoSafeAreaPolygon.setFrameConvexPolygon2d(safeArea);
      yoProjectionPolygon.setFrameConvexPolygon2d(areaToProjectInto);
   }

   public abstract void computeCMPInternal(FramePoint2d desiredCMPPreviousValue);

   public void minimizeAngularMomentumRateZ(boolean enable)
   {
      minimizeAngularMomentumRateZ.set(enable);
   }

   public void setFinalDesiredCapturePoint(FramePoint2d finalDesiredCapturePoint)
   {
      this.finalDesiredCapturePoint.setIncludingFrame(finalDesiredCapturePoint);
   }

   public void setPerfectCMP(FramePoint2d perfectCMP)
   {
      this.perfectCMP.setIncludingFrame(perfectCMP);
   }

   /**
    * Sets whether or not to include the momentum rate of change in the vertical direction in the whole body optimization.
    * If false, it will be controlled by attempting to drive the legs to a certain position in the null space
    * @param controlHeightWithMomentum boolean variable on whether or not to control the height with momentum.
    */
   public void setControlHeightWithMomentum(boolean controlHeightWithMomentum)
   {
      this.controlHeightWithMomentum = controlHeightWithMomentum;
   }

   public abstract void clearPlan();

   public abstract void addFootstepToPlan(Footstep footstep, FootstepTiming timing);

   public abstract void setFinalTransferDuration(double finalTransferDuration);

   public abstract void initializeForStanding();

   public abstract void initializeForSingleSupport();

   public abstract void initializeForTransfer();

   public abstract boolean getUpcomingFootstepSolution(Footstep footstepToPack);

   public abstract void submitRemainingTimeInSwingUnderDisturbance(double remainingTimeForSwing);

   public abstract ICPOptimizationController getICPOptimizationController();

   public abstract double getOptimizedTimeRemaining();

   public abstract void setReferenceICPVelocity(FrameVector2d referenceICPVelocity);
}
