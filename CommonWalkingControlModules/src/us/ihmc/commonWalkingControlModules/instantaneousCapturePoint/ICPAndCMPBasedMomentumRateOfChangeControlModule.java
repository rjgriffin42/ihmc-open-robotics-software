package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.controlModules.velocityViaCoP.CapturabilityBasedDesiredCoPVisualizer;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CapturePointData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CapturePointTrajectoryData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.OrientationTrajectoryData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.MomentumRateOfChangeData;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.GroundReactionMomentControlModule;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.WrenchDistributorTools;
import us.ihmc.controlFlow.AbstractControlFlowElement;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.Momentum;
import us.ihmc.utilities.screwTheory.MomentumCalculator;
import us.ihmc.utilities.screwTheory.SpatialForceVector;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;

import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector2d;

public class ICPAndCMPBasedMomentumRateOfChangeControlModule extends AbstractControlFlowElement implements ICPBasedMomentumRateOfChangeControlModule
{
   private final ControlFlowInputPort<Double> desiredCenterOfMassHeightAccelerationInputPort = createInputPort("desiredCenterOfMassHeightAccelerationInputPort");
   private final ControlFlowInputPort<BipedSupportPolygons> bipedSupportPolygonsInputPort = createInputPort("bipedSupportPolygonsInputPort");
   private final ControlFlowInputPort<RobotSide> supportLegInputPort = createInputPort("supportLegInputPort");
   private final ControlFlowInputPort<CapturePointData> capturePointInputPort = createInputPort("capturePointInputPort");
   private final ControlFlowInputPort<OrientationTrajectoryData> desiredPelvisOrientationInputPort = createInputPort("desiredPelvisOrientationInputPort");
   private final ControlFlowInputPort<CapturePointTrajectoryData> desiredCapturePointTrajectoryInputPort = createInputPort("desiredCapturePointTrajectoryInputPort");

   private final ControlFlowOutputPort<MomentumRateOfChangeData> momentumRateOfChangeOutputPort = createOutputPort("momentumRateOfChangeOutputPort");
   private final MomentumRateOfChangeData momentumRateOfChangeData;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final ICPProportionalController icpProportionalController;
   private final GroundReactionMomentControlModule groundReactionMomentControlModule;
   private final CapturabilityBasedDesiredCoPVisualizer visualizer;

   private final DoubleYoVariable kAngularMomentumXY = new DoubleYoVariable("kAngularMomentumXY", registry);
   private final DoubleYoVariable kPelvisAxisAngle = new DoubleYoVariable("kPelvisAxisAngle", registry);

   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final ReferenceFrame pelvisFrame;
   private final ReferenceFrame centerOfMassFrame;

   private final YoFramePoint2d controlledCoP = new YoFramePoint2d("controlledCoP", "", worldFrame, registry);
   private final YoFramePoint2d controlledCMP = new YoFramePoint2d("controlledCMP", "", worldFrame, registry);
   private final YoFrameVector2d controlledDeltaCMP = new YoFrameVector2d("controlledDeltaCMP", "", worldFrame, registry);

   private final BooleanYoVariable copProjected = new BooleanYoVariable("copProjected", registry);
   private final double totalMass;
   private final double gravityZ;
   private final SpatialForceVector gravitationalWrench;

   private final EnumYoVariable<RobotSide> supportLegPreviousTick = EnumYoVariable.create("supportLegPreviousTick", "", RobotSide.class, registry, true);
   private final MomentumCalculator momentumCalculator;

   public ICPAndCMPBasedMomentumRateOfChangeControlModule(ReferenceFrame pelvisFrame,
           ReferenceFrame centerOfMassFrame, TwistCalculator twistCalculator, double controlDT, double totalMass, double gravityZ,
           YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      MathTools.checkIfInRange(gravityZ, 0.0, Double.POSITIVE_INFINITY);

      this.icpProportionalController = new ICPProportionalController(controlDT, registry, dynamicGraphicObjectsListRegistry);
      this.groundReactionMomentControlModule = new GroundReactionMomentControlModule(pelvisFrame, registry);
      this.groundReactionMomentControlModule.setGains(10.0, 100.0);    // kPelvisYaw was 0.0 for M3 movie TODO: move to setGains method

      this.momentumCalculator = new MomentumCalculator(twistCalculator);
      this.visualizer = new CapturabilityBasedDesiredCoPVisualizer(registry, dynamicGraphicObjectsListRegistry);
      this.pelvisFrame = pelvisFrame;
      this.centerOfMassFrame = centerOfMassFrame;
      this.totalMass = totalMass;
      this.gravityZ = gravityZ;
      this.gravitationalWrench = new SpatialForceVector(centerOfMassFrame, new Vector3d(0.0, 0.0, totalMass * gravityZ), new Vector3d());
      this.momentumRateOfChangeData = new MomentumRateOfChangeData(centerOfMassFrame);
      parentRegistry.addChild(registry);
      momentumRateOfChangeOutputPort.setData(momentumRateOfChangeData);
   }

   public void startComputation()
   {
      if (supportLegInputPort.getData() != supportLegPreviousTick.getEnumValue())
      {
         icpProportionalController.reset();
      }

      CapturePointData capturePointData = capturePointInputPort.getData();
      CapturePointTrajectoryData desiredCapturePointTrajectory = desiredCapturePointTrajectoryInputPort.getData();
      FrameConvexPolygon2d supportPolygon = bipedSupportPolygonsInputPort.getData().getSupportPolygonInMidFeetZUp();
      boolean projectIntoSupportPolygon = desiredCapturePointTrajectory.isProjectCMPIntoSupportPolygon();

      FramePoint2d desiredCMP = icpProportionalController.doProportionalControl(capturePointData.getCapturePoint(), desiredCapturePointTrajectory.getFinalDesiredCapturePoint(),
                                   desiredCapturePointTrajectory.getDesiredCapturePoint(), desiredCapturePointTrajectory.getDesiredCapturePointVelocity(),
                                   capturePointData.getOmega0(), projectIntoSupportPolygon, supportPolygon);

      this.controlledCMP.set(desiredCMP);

      Momentum momentum = new Momentum(centerOfMassFrame);
      momentumCalculator.computeAndPack(momentum);
      FrameOrientation desiredPelvisOrientation = desiredPelvisOrientationInputPort.getData().getOrientation();
      FrameVector2d desiredDeltaCMP = determineDesiredDeltaCMP(desiredPelvisOrientation, momentum);
      FramePoint2d desiredCoP = new FramePoint2d(desiredCMP);
      desiredCoP.sub(desiredDeltaCMP);
      desiredCoP.changeFrame(supportPolygon.getReferenceFrame());

      if (supportPolygon.isPointInside(desiredCoP))
      {
         copProjected.set(false);
      }
      else
      {
         supportPolygon.orthogonalProjection(desiredCoP);
         copProjected.set(true);
      }

      desiredCoP.changeFrame(this.controlledCoP.getReferenceFrame());
      this.controlledCoP.set(desiredCoP);
      desiredDeltaCMP.sub(desiredCMP, desiredCoP);
      this.controlledDeltaCMP.set(desiredDeltaCMP);

      visualizer.setDesiredCapturePoint(desiredCapturePointTrajectory.getDesiredCapturePoint());
      visualizer.setDesiredCoP(desiredCoP);
      visualizer.setDesiredCMP(desiredCMP);

      supportLegPreviousTick.set(supportLegInputPort.getData());

      double fZ = WrenchDistributorTools.computeFz(totalMass, gravityZ, desiredCenterOfMassHeightAccelerationInputPort.getData());

      FrameVector normalMoment = groundReactionMomentControlModule.determineGroundReactionMoment(momentum, desiredPelvisOrientation.getYawPitchRoll()[0]);

      SpatialForceVector rateOfChangeOfMomentum = computeTotalGroundReactionWrench(desiredCoP, desiredCMP, fZ, normalMoment);
      rateOfChangeOfMomentum.changeFrame(gravitationalWrench.getExpressedInFrame());
      rateOfChangeOfMomentum.sub(gravitationalWrench);
      
      momentumRateOfChangeData.set(rateOfChangeOfMomentum);
   }

   private SpatialForceVector computeTotalGroundReactionWrench(FramePoint2d cop2d, FramePoint2d cmp2d, double fZ, FrameVector normalMoment)
   {
      FramePoint centerOfMass = new FramePoint(centerOfMassFrame);
      FramePoint cmp3d = WrenchDistributorTools.computePseudoCMP3d(centerOfMass, cmp2d, fZ, totalMass, capturePointInputPort.getData().getOmega0());
      FrameVector force = WrenchDistributorTools.computeForce(centerOfMass, cmp3d, fZ);
      force.changeFrame(centerOfMassFrame);

      FramePoint cop3d = cop2d.toFramePoint();
      cop3d.changeFrame(cmp3d.getReferenceFrame());
      cop3d.setZ(cmp3d.getZ());
      FrameVector momentArm = new FrameVector(cop3d);
      momentArm.sub(centerOfMass);

      SpatialForceVector ret = SpatialForceVector.createUsingArm(centerOfMassFrame, force.getVector(), momentArm.getVector());
      normalMoment.changeFrame(ret.getExpressedInFrame());
      ret.addAngularPart(normalMoment.getVector());

      return ret;
   }

   public void setGains(double kAngularMomentumXY, double kPelvisAxisAngle, double captureKpParallelToMotion, double captureKpOrthogonalToMotion, 
         double captureKi, double captureKiBleedoff, double filterBreakFrequencyHertz, double rateLimitCMP, double accelerationLimitCMP)
   {
      this.kAngularMomentumXY.set(kAngularMomentumXY);
      this.kPelvisAxisAngle.set(kPelvisAxisAngle);
      this.icpProportionalController.setGains(captureKpParallelToMotion, captureKpOrthogonalToMotion, captureKi, captureKiBleedoff, filterBreakFrequencyHertz, rateLimitCMP, accelerationLimitCMP);
   }

   private FrameVector2d determineDesiredDeltaCMP(FrameOrientation desiredPelvisOrientation, Momentum momentum)
   {
      ReferenceFrame frame = ReferenceFrame.getWorldFrame();

      FrameVector zUnitVector = new FrameVector(frame, 0.0, 0.0, 1.0);

      FrameVector angularMomentum = new FrameVector(momentum.getExpressedInFrame(), momentum.getAngularPartCopy());
      angularMomentum.changeFrame(frame);

      FrameVector momentumPart = new FrameVector(frame);
      momentumPart.cross(angularMomentum, zUnitVector);
      momentumPart.scale(-kAngularMomentumXY.getDoubleValue());

      Matrix3d desiredPelvisToPelvis = new Matrix3d();
      desiredPelvisOrientation.changeFrame(pelvisFrame);
      desiredPelvisOrientation.getMatrix3d(desiredPelvisToPelvis);

      AxisAngle4d desiredPelvisToPelvisAxisAngle = new AxisAngle4d();
      desiredPelvisToPelvisAxisAngle.set(desiredPelvisToPelvis);
      FrameVector proportionalPart = new FrameVector(pelvisFrame, desiredPelvisToPelvisAxisAngle.getX(), desiredPelvisToPelvisAxisAngle.getY(), 0.0);
      proportionalPart.scale(desiredPelvisToPelvisAxisAngle.getAngle());
      proportionalPart.changeFrame(frame);
      proportionalPart.cross(proportionalPart, zUnitVector);
      proportionalPart.scale(kPelvisAxisAngle.getDoubleValue());

      FrameVector desiredDeltaCMP = new FrameVector(frame);
      desiredDeltaCMP.add(momentumPart, proportionalPart);
      double maxDeltaDesiredCMP = 0.02;
      if (desiredDeltaCMP.length() > maxDeltaDesiredCMP)
      {
         desiredDeltaCMP.normalize();
         desiredDeltaCMP.scale(maxDeltaDesiredCMP);
      }

      return desiredDeltaCMP.toFrameVector2d();

//      ReferenceFrame frame = ReferenceFrame.getWorldFrame();
//      
//      desiredPelvisOrientation.changeFrame(frame);
//      double[] yawPitchRoll = desiredPelvisOrientation.getYawPitchRoll();
//      double desiredPelvisPitch = yawPitchRoll[1];
//      double desiredPelvisRoll = yawPitchRoll[2];
//
//      FrameVector zUnitVector = new FrameVector(frame, 0.0, 0.0, 1.0);
//      momentum.changeFrame(centerOfMassFrame);
//      FrameVector angularMomentum = momentum.getAngularPartAsFrameVectorCopy();
//      angularMomentum.changeFrame(worldFrame);
//      FrameVector desiredDeltaCMP = new FrameVector(frame);
//      desiredDeltaCMP.cross(angularMomentum, zUnitVector);
//      desiredDeltaCMP.scale(-kAngularMomentumXY.getDoubleValue());
//
//      Transform3D pelvisToWorld = pelvisFrame.getTransformToDesiredFrame(frame);
//      Matrix3d pelvisToDesiredPelvis = new Matrix3d();
//      pelvisToWorld.get(pelvisToDesiredPelvis);
//
//      Matrix3d desiredPelvisToWorldRotation = new Matrix3d();
//      RotationFunctions.setYawPitchRoll(desiredPelvisToWorldRotation, 0.0, desiredPelvisPitch, desiredPelvisRoll);
//      pelvisToDesiredPelvis.mulTransposeLeft(desiredPelvisToWorldRotation, pelvisToDesiredPelvis);
//
//      AxisAngle4d pelvisToWorldAxisAngle = new AxisAngle4d();
//      pelvisToWorldAxisAngle.set(pelvisToDesiredPelvis);
//      FrameVector proportionalPart = new FrameVector(frame, pelvisToWorldAxisAngle.getX(), pelvisToWorldAxisAngle.getY(), 0.0);
//      proportionalPart.scale(pelvisToWorldAxisAngle.getAngle());
//      proportionalPart.cross(proportionalPart, zUnitVector);
//      proportionalPart.scale(-kPelvisAxisAngle.getDoubleValue());
//      desiredDeltaCMP.add(proportionalPart);
//      
//      double maxDeltaDesiredCMP = 0.02;
//      if (desiredDeltaCMP.length() > maxDeltaDesiredCMP)
//      {
//         desiredDeltaCMP.normalize();
//         desiredDeltaCMP.scale(maxDeltaDesiredCMP);
//      }
//
//      return desiredDeltaCMP.toFrameVector2d();
   }

   public void waitUntilComputationIsDone()
   {
      // empty
   }

   public ControlFlowInputPort<BipedSupportPolygons> getBipedSupportPolygonsInputPort()
   {
      return bipedSupportPolygonsInputPort;
   }

   public ControlFlowInputPort<RobotSide> getSupportLegInputPort()
   {
      return supportLegInputPort;
   }

   public ControlFlowInputPort<CapturePointData> getCapturePointInputPort()
   {
      return capturePointInputPort;
   }

   public ControlFlowInputPort<CapturePointTrajectoryData> getDesiredCapturePointTrajectoryInputPort()
   {
      return desiredCapturePointTrajectoryInputPort;
   }

   public ControlFlowInputPort<Double> getDesiredCenterOfMassHeightAccelerationInputPort()
   {
      return desiredCenterOfMassHeightAccelerationInputPort;
   }

   public ControlFlowInputPort<OrientationTrajectoryData> getDesiredPelvisOrientationInputPort()
   {
      return desiredPelvisOrientationInputPort;
   }

   public ControlFlowOutputPort<MomentumRateOfChangeData> getMomentumRateOfChangeOutputPort()
   {
      return momentumRateOfChangeOutputPort;
   }

   public void initialize()
   {
//    empty
   }

   public void getDesiredCMP(FramePoint2d desiredCMPToPack)
   {
      controlledCMP.getFrameTuple2dIncludingFrame(desiredCMPToPack);
   }
}
