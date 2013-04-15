package us.ihmc.SdfLoader;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.Twist;

import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RawOutputWriter;
import com.yobotics.simulationconstructionset.robotController.RawSensorReader;

public class SDFPerfectSimulatedSensorReaderAndWriter implements RawSensorReader, RawOutputWriter
{
   private final String name;
   private final SDFRobot robot;
   private final SixDoFJoint rootJoint;
   private final CommonWalkingReferenceFrames referenceFrames;

   private final ArrayList<Pair<OneDegreeOfFreedomJoint,OneDoFJoint>> revoluteJoints = new ArrayList<Pair<OneDegreeOfFreedomJoint, OneDoFJoint>>();

   public SDFPerfectSimulatedSensorReaderAndWriter(SDFRobot robot, FullRobotModel fullRobotModel, CommonWalkingReferenceFrames referenceFrames)
   {
      this.name = robot.getName() + "SimulatedSensorReader";
      this.robot = robot;
      this.referenceFrames = referenceFrames;

      rootJoint = fullRobotModel.getRootJoint();
      
      OneDoFJoint[] revoluteJointsArray = fullRobotModel.getOneDoFJoints();

      for (OneDoFJoint revoluteJoint : revoluteJointsArray)
      {
         String name = revoluteJoint.getName();
         OneDegreeOfFreedomJoint oneDoFJoint = robot.getOneDoFJoint(name);

         Pair<OneDegreeOfFreedomJoint,OneDoFJoint> jointPair = new Pair<OneDegreeOfFreedomJoint, OneDoFJoint>(oneDoFJoint, revoluteJoint);
         this.revoluteJoints.add(jointPair);
      }

   }

   public void initialize()
   {
      read();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return null;
   }

   public String getName()
   {
      return name;
   }

   public String getDescription()
   {
      return getName();
   }

   private Transform3D temporaryRootToWorldTransform = new Transform3D();
   private Vector3d temporaryRootToWorldTranslation = new Vector3d();
   private Matrix3d temporaryRootToWorldRotation = new Matrix3d();

   public void read()
   {
      // Think about adding root body acceleration to the fullrobotmodel
      readAndUpdateOneDoFJointPositionsVelocitiesAndAccelerations();
      readAndUpdateRootJointPositionAndOrientation();
      updateReferenceFrames();
      readAndUpdateRootJointAngularAndLinearVelocity();
   }

   private void readAndUpdateRootJointAngularAndLinearVelocity()
   {
      ReferenceFrame elevatorFrame = rootJoint.getFrameBeforeJoint();
      ReferenceFrame pelvisFrame = rootJoint.getFrameAfterJoint();

      FrameVector linearVelocity = robot.getRootJointVelocity();
      linearVelocity.changeFrame(pelvisFrame);

      FrameVector angularVelocity = robot.getPelvisAngularVelocityInPelvisFrame(pelvisFrame);
      angularVelocity.changeFrame(pelvisFrame);

      Twist bodyTwist = new Twist(pelvisFrame, elevatorFrame, pelvisFrame, linearVelocity.getVector(), angularVelocity.getVector());
      rootJoint.setJointTwist(bodyTwist);
   }

   private void updateReferenceFrames()
   {
      if(referenceFrames != null)
      {
         referenceFrames.updateFrames();         
      }
   }

   private void readAndUpdateRootJointPositionAndOrientation()
   {
      readAndUpdateRootJointPosition();
      readAndUpdateRootJointOrientation();
   }
   
   private void readAndUpdateRootJointPosition()
   {
      packRootTransform(robot, temporaryRootToWorldTransform);
      temporaryRootToWorldTransform.get(temporaryRootToWorldTranslation);
      rootJoint.setPosition(temporaryRootToWorldTranslation);
   }
   
   private void readAndUpdateRootJointOrientation()
   {
      packRootTransform(robot, temporaryRootToWorldTransform);
      temporaryRootToWorldTransform.get(temporaryRootToWorldRotation);
      rootJoint.setRotation(temporaryRootToWorldRotation);
   }

   private void readAndUpdateOneDoFJointPositionsVelocitiesAndAccelerations()
   {
      for (Pair<OneDegreeOfFreedomJoint, OneDoFJoint> jointPair : revoluteJoints)
      {
         OneDegreeOfFreedomJoint pinJoint = jointPair.first();
         OneDoFJoint revoluteJoint = jointPair.second();

         revoluteJoint.setQ(pinJoint.getQ().getDoubleValue());
         revoluteJoint.setQd(pinJoint.getQD().getDoubleValue());
         revoluteJoint.setQdd(pinJoint.getQDD().getDoubleValue());

      }
   }
   
   protected void packRootTransform(SDFRobot robot, Transform3D transformToPack)
   {
      robot.getRootJointToWorldTransform(transformToPack);
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

}
