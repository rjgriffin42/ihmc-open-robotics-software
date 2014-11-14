package us.ihmc.sensorProcessing;

import java.util.Random;

import javax.vecmath.Vector3d;

import org.junit.Before;
import org.junit.Test;

import us.ihmc.sensorProcessing.ProcessedSensorsReadWrite;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.test.JUnitTools;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import us.ihmc.simulationconstructionset.UnreasonableAccelerationException;
import us.ihmc.simulationconstructionset.simulatedSensors.PerfectSimulatedIMURawSensorReader;
import us.ihmc.simulationconstructionset.simulatedSensors.SimulatedIMURawSensorReader;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;



public class SimulatedIMURawSensorReader2Test
{
   private Random random;
   
   @Before
   public void setUp()
   {
      random = new Random(1776L);
   }

   @Test
   public void test() throws SimulationExceededMaximumTimeException, UnreasonableAccelerationException
   {
      SingleRigidBodyRobot robot = new SingleRigidBodyRobot();
      robot.setPosition(RandomTools.generateRandomVector(random));
      robot.setYawPitchRoll(random.nextDouble(), random.nextDouble(), random.nextDouble());
      robot.setAngularVelocity(RandomTools.generateRandomVector(random));
      robot.setLinearVelocity(RandomTools.generateRandomVector(random));
      robot.setExternalForce(RandomTools.generateRandomVector(random));
      
      SimulatedSensorsTestFullRobotModel fullRobotModel = new SimulatedSensorsTestFullRobotModel();
      YoVariableRegistry registry = new YoVariableRegistry("test");
      RawSensors rawSensors = new RawSensors(registry);
      ReferenceFrame imuFrame = fullRobotModel.getRootJoint().getFrameAfterJoint();
      ProcessedSensorsReadWrite processedSensors = new ProcessedSensorsReadWrite(imuFrame, registry);
      int imuIndex = 0;
      RigidBody rigidBody = fullRobotModel.getBodyLink();
      RigidBody rootBody = fullRobotModel.getElevator();
      SpatialAccelerationVector rootAcceleration = new SpatialAccelerationVector(rootBody.getBodyFixedFrame(), ReferenceFrame.getWorldFrame(), rootBody.getBodyFixedFrame());
      SimulatedIMURawSensorReader simulatedIMURawSensorReader = new PerfectSimulatedIMURawSensorReader(rawSensors, imuIndex, rigidBody, imuFrame, rootBody, rootAcceleration);
      PerfectIMUSensorProcessor imuSensorProcessor = new PerfectIMUSensorProcessor(rawSensors, processedSensors);

      simulatedIMURawSensorReader.initialize();
      imuSensorProcessor.initialize();
      robot.doDynamicsButDoNotIntegrate();
      fullRobotModel.update(robot);
      simulatedIMURawSensorReader.read();
      imuSensorProcessor.update();

      Vector3d linearAccelerationFromRobot = robot.getBodyAcceleration().getVectorCopy();
      Vector3d linearAccelerationFromIMU = processedSensors.getAcceleration(imuIndex).getVectorCopy();

//      System.out.println("linear from robot: " + linearAccelerationFromRobot);
//      System.out.println("linear from imu: " + linearAccelerationFromIMU);
      
      JUnitTools.assertTuple3dEquals(linearAccelerationFromRobot, linearAccelerationFromIMU, 1e-9);
   }
}
