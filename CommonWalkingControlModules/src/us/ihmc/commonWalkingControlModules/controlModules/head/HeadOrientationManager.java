package us.ihmc.commonWalkingControlModules.controlModules.head;

import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.TaskspaceConstraintData;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredHeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.CurrentOrientationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.OrientationInterpolationTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.OrientationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.SettableOrientationProvider;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.trajectory.ConstantDoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleProvider;

public class HeadOrientationManager
{
   private final YoVariableRegistry registry;
   private final HeadOrientationControlModule headOrientationControlModule;
   private final MomentumBasedController momentumBasedController;
   private final DesiredHeadOrientationProvider desiredHeadOrientationProvider;
   private final DoubleYoVariable yoTime;
   private final DoubleYoVariable receivedNewHeadOrientationTime;
   private final OrientationInterpolationTrajectoryGenerator orientationTrajectoryGenerator;

   private final BooleanYoVariable isTrackingOrientation;
   
   private final ReferenceFrame headOrientationExpressedInFrame;
   private final SettableOrientationProvider finalOrientationProvider;
   private int jacobianId = -1;

   public HeadOrientationManager(MomentumBasedController momentumBasedController, HeadOrientationControlModule headOrientationControlModule,
                                 DesiredHeadOrientationProvider desiredHeadOrientationProvider, double trajectoryTime, YoVariableRegistry parentRegistry)
   {
      this.momentumBasedController = momentumBasedController;
      this.yoTime = momentumBasedController.getYoTime();
      this.desiredHeadOrientationProvider = desiredHeadOrientationProvider;
      headOrientationExpressedInFrame = desiredHeadOrientationProvider.getHeadOrientationExpressedInFrame();
      
      this.headOrientationControlModule = headOrientationControlModule;
      
      registry = new YoVariableRegistry(getClass().getSimpleName());
      
      receivedNewHeadOrientationTime = new DoubleYoVariable("receivedNewHeadOrientationTime", registry);

      isTrackingOrientation = new BooleanYoVariable("isTrackingOrientation", registry);
      
      DoubleProvider trajectoryTimeProvider = new ConstantDoubleProvider(trajectoryTime);
      OrientationProvider initialOrientationProvider = new CurrentOrientationProvider(headOrientationExpressedInFrame, headOrientationControlModule.getHead().getBodyFixedFrame());
      finalOrientationProvider = new SettableOrientationProvider("headFinalOrientation", headOrientationExpressedInFrame, registry);
      orientationTrajectoryGenerator = new OrientationInterpolationTrajectoryGenerator("headOrientation",
            headOrientationExpressedInFrame, trajectoryTimeProvider, initialOrientationProvider, finalOrientationProvider,
            registry);
      parentRegistry.addChild(registry);
   }

   private final FrameOrientation desiredOrientation = new FrameOrientation();
   
   public void compute()
   {
      checkForNewDesiredOrientationInformation();
      
      if (isTrackingOrientation.getBooleanValue())
      {
         double deltaTime = yoTime.getDoubleValue() - receivedNewHeadOrientationTime.getDoubleValue();
         orientationTrajectoryGenerator.compute(deltaTime);
         orientationTrajectoryGenerator.get(desiredOrientation);
         headOrientationControlModule.setOrientationToTrack(desiredOrientation);
         isTrackingOrientation.set(!orientationTrajectoryGenerator.isDone());
      }

      if (jacobianId >= 0)
      {
         headOrientationControlModule.compute();

         TaskspaceConstraintData taskspaceConstraintData = headOrientationControlModule.getTaskspaceConstraintData();
         momentumBasedController.setDesiredSpatialAcceleration(jacobianId, taskspaceConstraintData);
      }
   }

   private void checkForNewDesiredOrientationInformation()
   {
      if (desiredHeadOrientationProvider == null)
         return;
      
      if (isTrackingOrientation.getBooleanValue())
         return;
      
      if (desiredHeadOrientationProvider.isNewHeadOrientationInformationAvailable())
      {
         finalOrientationProvider.setOrientation(desiredHeadOrientationProvider.getDesiredHeadOrientation());
         receivedNewHeadOrientationTime.set(yoTime.getDoubleValue());
         orientationTrajectoryGenerator.initialize();
         isTrackingOrientation.set(true);
      }
      else if (desiredHeadOrientationProvider.isNewLookAtInformationAvailable())
      {
         headOrientationControlModule.setPointToTrack(desiredHeadOrientationProvider.getLookAtPoint());
         headOrientationControlModule.packDesiredFrameOrientation(desiredOrientation);
         desiredOrientation.changeFrame(headOrientationExpressedInFrame);
         finalOrientationProvider.setOrientation(desiredOrientation);
         receivedNewHeadOrientationTime.set(yoTime.getDoubleValue());
         orientationTrajectoryGenerator.initialize();
         isTrackingOrientation.set(true);
      }
   }
   
   public int createJacobian(FullRobotModel fullRobotModel, String[] headOrientationControlJointNames)
   {
      InverseDynamicsJoint[] allJoints = ScrewTools.computeSupportAndSubtreeJoints(fullRobotModel.getRootJoint().getSuccessor());
      InverseDynamicsJoint[] headOrientationControlJoints = ScrewTools.findJointsWithNames(allJoints, headOrientationControlJointNames);

      int jacobianId = momentumBasedController.getOrCreateGeometricJacobian(headOrientationControlJoints, headOrientationControlModule.getHead().getBodyFixedFrame());
      return jacobianId;
   }
   
   public void setUp(RigidBody base, int jacobianId)
   {
      this.jacobianId = jacobianId;
      headOrientationControlModule.setBase(base);
      headOrientationControlModule.setJacobian(momentumBasedController.getJacobian(jacobianId));
   }
   
   public void setUp(RigidBody base, int jacobianId, double proportionalGainX, double proportionalGainY, double proportionalGainZ,
                     double derivativeGainX, double derivativeGainY, double derivativeGainZ)
   {
      this.jacobianId = jacobianId;
      headOrientationControlModule.setBase(base);
      headOrientationControlModule.setJacobian(momentumBasedController.getJacobian(jacobianId));
      headOrientationControlModule.setProportionalGains(proportionalGainX, proportionalGainY, proportionalGainZ);
      headOrientationControlModule.setDerivativeGains(derivativeGainX, derivativeGainY, derivativeGainZ);
   }

   public void setControlGains(double proportionalGain, double derivativeGain)
   {
      headOrientationControlModule.setProportionalGains(proportionalGain, proportionalGain, proportionalGain);
      headOrientationControlModule.setDerivativeGains(derivativeGain, derivativeGain, derivativeGain);
   }
   
   public void turnOff()
   {
      setUp(null, -1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   }

   
}
