package us.ihmc.quadrupedRobotics.estimator.stateEstimator;

import java.util.HashMap;
import java.util.Map;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.commonWalkingControlModules.sensors.footSwitch.FootSwitchInterface;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.model.CenterOfPressureDataHolder;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SixDoFJoint;
import us.ihmc.robotics.sensors.ForceSensorDataHolder;
import us.ihmc.sensorProcessing.model.RobotMotionStatus;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation.DRCKinematicsBasedStateEstimator;
import us.ihmc.tools.factories.FactoryTools;
import us.ihmc.tools.factories.RequiredFactoryField;

public class QuadrupedStateEstimatorFactory
{
   private final RequiredFactoryField<QuadrupedSensorInformation> sensorInformation = new RequiredFactoryField<>("sensorInformation");
   private final RequiredFactoryField<StateEstimatorParameters> stateEstimatorParameters = new RequiredFactoryField<>("stateEstimatorParameters");
   private final RequiredFactoryField<SDFFullRobotModel> fullRobotModel = new RequiredFactoryField<>("fullRobotModel");
   private final RequiredFactoryField<SensorOutputMapReadOnly> sensorOutputMapReadOnly = new RequiredFactoryField<>("sensorOutputMapReadOnly");
   private final RequiredFactoryField<QuadrantDependentList<ContactablePlaneBody>> footContactableBodies = new RequiredFactoryField<>("footContactableBodies");
   private final RequiredFactoryField<QuadrantDependentList<FootSwitchInterface>> footSwitches = new RequiredFactoryField<>("footSwitches");
   private final RequiredFactoryField<Double> gravity = new RequiredFactoryField<>("gravity");
   private final RequiredFactoryField<Double> estimatorDT = new RequiredFactoryField<>("estimatorDT");
   private final RequiredFactoryField<YoVariableRegistry> yoVariableRegistry = new RequiredFactoryField<>("yoVariableRegistry");
   private final RequiredFactoryField<YoGraphicsListRegistry> yoGraphicsListRegistry = new RequiredFactoryField<>("yoGraphicsListRegistry");
   
   public DRCKinematicsBasedStateEstimator createStateEstimator()
   {
      FactoryTools.checkAllRequiredFactoryFieldsAreSet(this);
      
      RigidBody elevator = fullRobotModel.get().getElevator();
      SixDoFJoint rootInverseDynamicsJoint = fullRobotModel.get().getRootJoint();
      RigidBody estimationLink = fullRobotModel.get().getPelvis();
      FullInverseDynamicsStructure inverseDynamicsStructure = new FullInverseDynamicsStructure(elevator, estimationLink, rootInverseDynamicsJoint);

      RobotMotionStatusHolder robotMotionStatusFromController = new RobotMotionStatusHolder();
      robotMotionStatusFromController.setCurrentRobotMotionStatus(RobotMotionStatus.IN_MOTION);
      ForceSensorDataHolder forceSensorDataHolderToUpdate = null;
      CenterOfPressureDataHolder centerOfPressureDataHolder = null;

      Map<RigidBody, ContactablePlaneBody> feetMap = new HashMap<RigidBody, ContactablePlaneBody>();
      Map<RigidBody, FootSwitchInterface> footSwitchMap = new HashMap<RigidBody, FootSwitchInterface>();
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         ContactablePlaneBody contactablePlaneBody = footContactableBodies.get().get(quadrant);
         RigidBody rigidBody = contactablePlaneBody.getRigidBody();
         feetMap.put(rigidBody, contactablePlaneBody);
         FootSwitchInterface footSwitch = footSwitches.get().get(quadrant);
         footSwitchMap.put(rigidBody, footSwitch);
      }

      String[] imuSensorsToUseInStateEstimator = sensorInformation.get().getImuNames();
      double gravityMagnitude = Math.abs(gravity.get());
      
      DRCKinematicsBasedStateEstimator stateEstimator = new DRCKinematicsBasedStateEstimator(inverseDynamicsStructure, stateEstimatorParameters.get(),
                                                                                             sensorOutputMapReadOnly.get(), forceSensorDataHolderToUpdate,
                                                                                             imuSensorsToUseInStateEstimator, gravityMagnitude, footSwitchMap,
                                                                                             centerOfPressureDataHolder , robotMotionStatusFromController, feetMap,
                                                                                             yoGraphicsListRegistry.get());
      
      yoVariableRegistry.get().addChild(stateEstimator.getYoVariableRegistry());
      
      return stateEstimator;
   }
   
   public void setSensorInformation(QuadrupedSensorInformation sensorInformation)
   {
      this.sensorInformation.set(sensorInformation);
   }
   
   public void setStateEstimatorParameters(StateEstimatorParameters stateEstimatorParameters)
   {
      this.stateEstimatorParameters.set(stateEstimatorParameters);
   }
   
   public void setFullRobotModel(SDFFullRobotModel fullRobotModel)
   {
      this.fullRobotModel.set(fullRobotModel);
   }
   
   public void setSensorOutputMapReadOnly(SensorOutputMapReadOnly sensorOutputMapReadOnly)
   {
      this.sensorOutputMapReadOnly.set(sensorOutputMapReadOnly);
   }
   
   public void setFootContactableBodies(QuadrantDependentList<ContactablePlaneBody> footContactableBodies)
   {
      this.footContactableBodies.set(footContactableBodies);
   }
   
   public void setFootSwitches(QuadrantDependentList<FootSwitchInterface> footSwitches)
   {
      this.footSwitches.set(footSwitches);
   }
   
   public void setGravity(double gravity)
   {
      this.gravity.set(gravity);
   }
   
   public void setEstimatorDT(double estimatorDT)
   {
      this.estimatorDT.set(estimatorDT);
   }
   
   public void setYoVariableRegistry(YoVariableRegistry yoVariableRegistry)
   {
      this.yoVariableRegistry.set(yoVariableRegistry);
   }
   
   public void setYoGraphicsListRegistry(YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.yoGraphicsListRegistry.set(yoGraphicsListRegistry);
   }
}
