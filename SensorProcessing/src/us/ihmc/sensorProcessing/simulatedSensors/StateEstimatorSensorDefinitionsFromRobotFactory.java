package us.ihmc.sensorProcessing.simulatedSensors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Transform3D;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Wrench;

import com.yobotics.simulationconstructionset.IMUMount;
import com.yobotics.simulationconstructionset.KinematicPoint;
import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.Robot;

public class StateEstimatorSensorDefinitionsFromRobotFactory
{
   private final SCSToInverseDynamicsJointMap scsToInverseDynamicsJointMap;
   private final Robot robot;

   private final LinkedHashMap<IMUMount, IMUDefinition> imuDefinitions;
   private final Map<WrenchCalculatorInterface,ForceSensorDefinition> forceSensorDefinitions;

   private final StateEstimatorSensorDefinitions stateEstimatorSensorDefinitions;

   public StateEstimatorSensorDefinitionsFromRobotFactory(SCSToInverseDynamicsJointMap scsToInverseDynamicsJointMap, Robot robot, double controlDT,
           ArrayList<IMUMount> imuMounts, ArrayList<WrenchCalculatorInterface> groundContactPointBasedWrenchCalculators,  ArrayList<KinematicPoint> positionPoints, ArrayList<KinematicPoint> velocityPoints)
   {
      this.scsToInverseDynamicsJointMap = scsToInverseDynamicsJointMap;
      this.robot = robot;

      this.imuDefinitions = generateIMUDefinitions(imuMounts);
      this.forceSensorDefinitions = generateForceSensorDefinitions(groundContactPointBasedWrenchCalculators);

      
      stateEstimatorSensorDefinitions = new StateEstimatorSensorDefinitions();

      createAndAddForceSensorDefinitions(forceSensorDefinitions);
      createAndAddOneDoFPositionAndVelocitySensors();
      createAndAddOrientationSensors(imuDefinitions);
      createAndAddAngularVelocitySensors(imuDefinitions);
      createAndAddLinearAccelerationSensors(imuDefinitions);
   }
   
   private void createAndAddForceSensorDefinitions(Map<WrenchCalculatorInterface, ForceSensorDefinition> forceSensorDefinitions)
   {
      for(ForceSensorDefinition forceSensorDefinition : forceSensorDefinitions.values())
      {
         stateEstimatorSensorDefinitions.addForceSensorDefinition(forceSensorDefinition);
      }
   }

   private LinkedHashMap<WrenchCalculatorInterface, ForceSensorDefinition> generateForceSensorDefinitions(
         ArrayList<WrenchCalculatorInterface> groundContactPointBasedWrenchCalculators)
   {
      
      LinkedHashMap<WrenchCalculatorInterface,ForceSensorDefinition> forceSensorDefinitions = new LinkedHashMap<WrenchCalculatorInterface, ForceSensorDefinition>();
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(Wrench.SIZE, Wrench.SIZE);
      CommonOps.setIdentity(selectionMatrix);
      for(WrenchCalculatorInterface groundContactPointBasedWrenchCalculator : groundContactPointBasedWrenchCalculators)
      {
         OneDegreeOfFreedomJoint forceTorqueSensorJoint = groundContactPointBasedWrenchCalculator.getJoint();
         OneDoFJoint oneDoFSensorJoint = scsToInverseDynamicsJointMap.getInverseDynamicsOneDoFJoint(forceTorqueSensorJoint);
         ReferenceFrame sensorFrame =  oneDoFSensorJoint.getFrameAfterJoint();
         ForceSensorDefinition sensorDefinition = new ForceSensorDefinition(forceTorqueSensorJoint.getName(), selectionMatrix, sensorFrame, sensorFrame);
         forceSensorDefinitions.put(groundContactPointBasedWrenchCalculator, sensorDefinition);
         
      }
      return forceSensorDefinitions;
   }

   public Map<IMUMount, IMUDefinition> getIMUDefinitions()
   {
      return imuDefinitions;
   }

   public StateEstimatorSensorDefinitions getStateEstimatorSensorDefinitions()
   {
      return stateEstimatorSensorDefinitions;
   }

   private LinkedHashMap<IMUMount, IMUDefinition> generateIMUDefinitions(ArrayList<IMUMount> imuMounts)
   {
      LinkedHashMap<IMUMount, IMUDefinition> imuDefinitions = new LinkedHashMap<IMUMount, IMUDefinition>();

      for (IMUMount imuMount : imuMounts)
      {
         RigidBody rigidBody = scsToInverseDynamicsJointMap.getRigidBody(imuMount.getParentJoint());
         Transform3D transformFromMountToJoint = new Transform3D();
         imuMount.getTransformFromMountToJoint(transformFromMountToJoint);
         IMUDefinition imuDefinition = new IMUDefinition(imuMount.getName(), rigidBody, transformFromMountToJoint);
         imuDefinitions.put(imuMount, imuDefinition);
      }

      return imuDefinitions;
   }

   public void createAndAddOneDoFPositionAndVelocitySensors()
   {
      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      robot.getAllOneDegreeOfFreedomJoints(oneDegreeOfFreedomJoints);

      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = scsToInverseDynamicsJointMap.getInverseDynamicsOneDoFJoint(oneDegreeOfFreedomJoint);

         stateEstimatorSensorDefinitions.addJointPositionSensorDefinition(oneDoFJoint);
         stateEstimatorSensorDefinitions.addJointVelocitySensorDefinition(oneDoFJoint);
      }
   }

   public void createAndAddOrientationSensors(LinkedHashMap<IMUMount, IMUDefinition> imuDefinitions)
   {
      Set<IMUMount> imuMounts = imuDefinitions.keySet();

      for (IMUMount imuMount : imuMounts)
      {
         IMUDefinition imuDefinition = imuDefinitions.get(imuMount);
         stateEstimatorSensorDefinitions.addOrientationSensorDefinition(imuDefinition);
      }
   }

   public void createAndAddAngularVelocitySensors(LinkedHashMap<IMUMount, IMUDefinition> imuDefinitions)
   {
      Set<IMUMount> imuMounts = imuDefinitions.keySet();

      for (IMUMount imuMount : imuMounts)
      {
         IMUDefinition imuDefinition = imuDefinitions.get(imuMount);
         stateEstimatorSensorDefinitions.addAngularVelocitySensorDefinition(imuDefinition);
      }
   }


   public void createAndAddLinearAccelerationSensors(LinkedHashMap<IMUMount, IMUDefinition> imuDefinitions)
   {
      Set<IMUMount> imuMounts = imuDefinitions.keySet();

      for (IMUMount imuMount : imuMounts)
      {
         IMUDefinition imuDefinition = imuDefinitions.get(imuMount);

         stateEstimatorSensorDefinitions.addLinearAccelerationSensorDefinition(imuDefinition);
      }
   }

   public Map<WrenchCalculatorInterface, ForceSensorDefinition> getForceSensorDefinitions()
   {
      return forceSensorDefinitions;
   }
}
