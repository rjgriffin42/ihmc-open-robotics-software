package us.ihmc.SdfLoader;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;

import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.SixDoFJoint;

import com.yobotics.simulationconstructionset.FloatingJoint;
import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RawOutputWriter;

public class SDFPerfectSimulatedOutputWriter implements RawOutputWriter
{
   private final String name;
   protected final SDFRobot robot;
   protected Pair<FloatingJoint, SixDoFJoint> rootJointPair;
   protected final ArrayList<Pair<OneDegreeOfFreedomJoint,OneDoFJoint>> revoluteJoints = new ArrayList<Pair<OneDegreeOfFreedomJoint, OneDoFJoint>>();
   
   public SDFPerfectSimulatedOutputWriter(SDFRobot robot)
   {
      this.name = robot.getName() + "SimulatedSensorReader";
      this.robot = robot;
   }
   
   public SDFPerfectSimulatedOutputWriter(SDFRobot robot, FullRobotModel fullRobotModel)
   {
      this.name = robot.getName() + "SimulatedSensorReader";
      this.robot = robot;

      setFullRobotModel(fullRobotModel);
   }

   public void initialize()
   {
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return null;
   }
   
   public void setFullRobotModel(FullRobotModel fullRobotModel)
   {
      revoluteJoints.clear();
      OneDoFJoint[] revoluteJointsArray = fullRobotModel.getOneDoFJoints();
      
      for (OneDoFJoint revoluteJoint : revoluteJointsArray)
      {
         String name = revoluteJoint.getName();
         OneDegreeOfFreedomJoint oneDoFJoint = robot.getOneDoFJoint(name);
         
         Pair<OneDegreeOfFreedomJoint,OneDoFJoint> jointPair = new Pair<OneDegreeOfFreedomJoint, OneDoFJoint>(oneDoFJoint, revoluteJoint);
         this.revoluteJoints.add(jointPair);
      }
      
      rootJointPair = new Pair<FloatingJoint, SixDoFJoint>(robot.getRootJoint(), fullRobotModel.getRootJoint());
   }

   public String getName()
   {
      return name;
   }

   public String getDescription()
   {
      return getName();
   }

   public void write()
   {
      for (Pair<OneDegreeOfFreedomJoint, OneDoFJoint> jointPair : revoluteJoints)
      {
         OneDegreeOfFreedomJoint pinJoint = jointPair.first();
         OneDoFJoint revoluteJoint = jointPair.second();

         pinJoint.setTau(revoluteJoint.getTau());
      }
   }

   public void updateRobotConfigurationBasedOnFullRobotModel()
   {
      for (Pair<OneDegreeOfFreedomJoint, OneDoFJoint> jointPair : revoluteJoints)
      {
         OneDegreeOfFreedomJoint pinJoint = jointPair.first();
         OneDoFJoint revoluteJoint = jointPair.second();

         pinJoint.setQ(revoluteJoint.getQ());
      }
      
      FloatingJoint floatingJoint = rootJointPair.first();
      SixDoFJoint sixDoFJoint = rootJointPair.second();
      
      Transform3D transform = sixDoFJoint.getJointTransform3D();
      floatingJoint.setRotationAndTranslation(transform);
   }
}
