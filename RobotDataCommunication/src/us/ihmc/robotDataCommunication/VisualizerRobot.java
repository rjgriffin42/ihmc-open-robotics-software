package us.ihmc.robotDataCommunication;

import us.ihmc.SdfLoader.SDFHumanoidJointNameMap;
import us.ihmc.SdfLoader.HumanoidFloatingRootJointRobot;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.robotDescription.RobotDescription;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;

public class VisualizerRobot extends HumanoidFloatingRootJointRobot
{
   private final YoVariableRegistry reducedRegistry;


   public VisualizerRobot(RobotDescription robotDescription, SDFHumanoidJointNameMap sdfJointNameMap)
   {
      super(robotDescription, sdfJointNameMap);
      this.reducedRegistry = new YoVariableRegistry(robotDescription.getName());

      reducedRegistry.registerVariable(getRootJoint().getQx());
      reducedRegistry.registerVariable(getRootJoint().getQy());
      reducedRegistry.registerVariable(getRootJoint().getQz());

      reducedRegistry.registerVariable(getRootJoint().getQdx());
      reducedRegistry.registerVariable(getRootJoint().getQdy());
      reducedRegistry.registerVariable(getRootJoint().getQdz());


      reducedRegistry.registerVariable(getRootJoint().getQuaternionQs());
      reducedRegistry.registerVariable(getRootJoint().getQuaternionQx());
      reducedRegistry.registerVariable(getRootJoint().getQuaternionQy());
      reducedRegistry.registerVariable(getRootJoint().getQuaternionQz());

      reducedRegistry.registerVariable(getRootJoint().getAngularVelocityX());
      reducedRegistry.registerVariable(getRootJoint().getAngularVelocityY());
      reducedRegistry.registerVariable(getRootJoint().getAngularVelocityZ());

      for(OneDegreeOfFreedomJoint joint : getOneDegreeOfFreedomJoints())
      {
         reducedRegistry.registerVariable(joint.getQYoVariable());
         reducedRegistry.registerVariable(joint.getQDYoVariable());
      }

   }

   @Override
   public YoVariableRegistry getRobotsYoVariableRegistry()
   {
      if(this.reducedRegistry == null)
      {
         // Hack to avoid null registry errors on startup.
         return super.getRobotsYoVariableRegistry();
      }
      else
      {
         return this.reducedRegistry;
      }
   }
}
