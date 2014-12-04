package us.ihmc.commonWalkingControlModules.momentumBasedController;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import us.ihmc.commonWalkingControlModules.sensors.WrenchBasedFootSwitch;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.InverseDynamicsCalculator;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class AntiGravityJointTorquesVisualizer
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final LinkedHashMap<OneDoFJoint, DoubleYoVariable> antiGravityJointTorques;
   
   private final InverseDynamicsCalculator inverseDynamicsCalculator;
   private final SideDependentList<WrenchBasedFootSwitch> wrenchBasedFootSwitches;
   private final InverseDynamicsJoint[] allJoints;
   private final OneDoFJoint[] allOneDoFJoints;
   private final Wrench tempWrench = new Wrench();

   public AntiGravityJointTorquesVisualizer(FullRobotModel fullRobotModel, TwistCalculator twistCalculator, SideDependentList<WrenchBasedFootSwitch> wrenchBasedFootSwitches, YoVariableRegistry parentRegistry, double gravity)
   {
      SpatialAccelerationVector rootAcceleration = ScrewTools.createGravitationalSpatialAcceleration(twistCalculator.getRootBody(), gravity);
      this.inverseDynamicsCalculator = new InverseDynamicsCalculator(ReferenceFrame.getWorldFrame(), rootAcceleration, new LinkedHashMap<RigidBody, Wrench>(), new ArrayList<InverseDynamicsJoint>(), false, false, twistCalculator);
      this.wrenchBasedFootSwitches = wrenchBasedFootSwitches;
      allJoints = ScrewTools.computeSubtreeJoints(inverseDynamicsCalculator.getSpatialAccelerationCalculator().getRootBody());
      allOneDoFJoints = ScrewTools.filterJoints(allJoints, OneDoFJoint.class);
      
      antiGravityJointTorques = new LinkedHashMap<>(allOneDoFJoints.length);
      
      for (int i = 0; i < allOneDoFJoints.length; i++)
      {
         OneDoFJoint oneDoFJoint = allOneDoFJoints[i];
         DoubleYoVariable antiGravityJointTorque = new DoubleYoVariable("antiGravity_tau_" + oneDoFJoint.getName(), registry);
         antiGravityJointTorques.put(oneDoFJoint, antiGravityJointTorque);
      }
      parentRegistry.addChild(registry);
   }

   public void computeAntiGravityJointTorques()
   {
      reset();
      
      setFootMeasuredWrenches();
      inverseDynamicsCalculator.compute();
      for (int i = 0; i < allOneDoFJoints.length; i++)
      {
         OneDoFJoint oneDoFJoint = allOneDoFJoints[i];
         antiGravityJointTorques.get(oneDoFJoint).set(oneDoFJoint.getTau());
         oneDoFJoint.setTau(0.0);
      }
      reset();
   }

   private void setFootMeasuredWrenches()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         WrenchBasedFootSwitch wrenchBasedFootSwitch = wrenchBasedFootSwitches.get(robotSide);
         wrenchBasedFootSwitch.computeAndPackFootWrench(tempWrench);
         RigidBody foot = wrenchBasedFootSwitch.getContactablePlaneBody().getRigidBody();
         tempWrench.changeBodyFrameAttachedToSameBody(foot.getBodyFixedFrame());
         tempWrench.changeFrame(foot.getBodyFixedFrame());
         inverseDynamicsCalculator.setExternalWrench(foot, tempWrench);
      }
   }

   private void reset()
   {
      inverseDynamicsCalculator.reset();
   }
}
