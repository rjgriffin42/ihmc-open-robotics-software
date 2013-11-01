package us.ihmc.darpaRoboticsChallenge.outputs;

import java.util.ArrayList;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFPerfectSimulatedOutputWriter;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.visualizer.RobotVisualizer;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotDampingParameters;
import us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.maps.ObjectObjectMap;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RawOutputWriter;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.filter.DelayedDoubleYoVariable;

public class DRCSimulationOutputWriter extends SDFPerfectSimulatedOutputWriter implements DRCOutputWriter
{
   private static final int TICKS_TO_DELAY = 0;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   
   private final DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry;
   private final RobotVisualizer robotVisualizer;
   private final ObjectObjectMap<OneDoFJoint, DoubleYoVariable> rawJointTorques;
   private final ObjectObjectMap<OneDoFJoint, DelayedDoubleYoVariable> delayedJointTorques;
   
   private final ArrayList<RawOutputWriter> rawOutputWriters = new ArrayList<RawOutputWriter>();

   double[] prevError;
   public DRCSimulationOutputWriter(SDFRobot robot, YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, RobotVisualizer robotVisualizer)
   {
      super(robot);
      this.dynamicGraphicObjectsListRegistry = dynamicGraphicObjectsListRegistry;
      this.robotVisualizer = robotVisualizer;
      
      rawJointTorques = new ObjectObjectMap<OneDoFJoint, DoubleYoVariable>();
      delayedJointTorques = new ObjectObjectMap<OneDoFJoint, DelayedDoubleYoVariable>();
      
      parentRegistry.addChild(registry);
   }

   public void writeAfterController(long timestamp)
   {
      // Do not write here, because it will set the robot's torques while the simulation is running
   }

   public void writeAfterEstimator(long timestamp)
   {
      if (robotVisualizer != null)
      {
         robotVisualizer.update(timestamp);
      }
   }

   public void writeAfterSimulationTick()
   {
      for (int i = 0; i < revoluteJoints.size(); i++)
      {
         
         Pair<OneDegreeOfFreedomJoint, OneDoFJoint> jointPair = revoluteJoints.get(i);
         
         OneDegreeOfFreedomJoint pinJoint = jointPair.first();
         OneDoFJoint revoluteJoint = jointPair.second();

         double tau = revoluteJoint.getTau();
         DoubleYoVariable rawJointTorque = rawJointTorques.get(revoluteJoint);
         DelayedDoubleYoVariable delayedJointTorque = delayedJointTorques.get(revoluteJoint);

         if (rawJointTorque != null)
         {
            rawJointTorque.set(tau);
            delayedJointTorque.update();
            tau = delayedJointTorque.getDoubleValue();
         }

         pinJoint.setTau(tau);
         pinJoint.setKp(revoluteJoint.getKp());
         pinJoint.setKd(revoluteJoint.getKd());
         pinJoint.setqDesired(revoluteJoint.getqDesired());
         pinJoint.setQdDesired(revoluteJoint.getQdDesired());
         
      }
      
      for (int i=0; i<rawOutputWriters.size(); i++)
      {
         rawOutputWriters.get(i).write();
      }
      
      if(dynamicGraphicObjectsListRegistry != null)
      {
         dynamicGraphicObjectsListRegistry.update();
      }
   }

   public void setFullRobotModel(SDFFullRobotModel fullRobotModel)
   {
      super.setFullRobotModel(fullRobotModel);
      
  
      
      OneDoFJoint[] joints = ROSAtlasJointMap.getJointMap(fullRobotModel.getOneDoFJointsAsMap());
      for(int i = 0; i < joints.length; i++)
      {
         OneDoFJoint oneDoFJoint = joints[i];
         oneDoFJoint.setDampingParameter(DRCRobotDampingParameters.getAtlasDamping(i));
         
         DoubleYoVariable rawJointTorque = new DoubleYoVariable("raw_tau_"+oneDoFJoint.getName(), registry);
         rawJointTorques.add(oneDoFJoint, rawJointTorque);
         
         DelayedDoubleYoVariable delayedJointTorque = new DelayedDoubleYoVariable("delayed_tau_" + oneDoFJoint.getName(), "", rawJointTorque, TICKS_TO_DELAY, registry);
         delayedJointTorques.add(oneDoFJoint, delayedJointTorque);
      }
      
      prevError = new double[revoluteJoints.size()];
      

      if(robotVisualizer != null)
      {
         robotVisualizer.setFullRobotModel(fullRobotModel);
      }
      
   }

   public void setEstimatorModel(SDFFullRobotModel estimatorModel)
   {
      
   }
   

   public void addRawOutputWriter(RawOutputWriter rawOutputWriter)
   {
      rawOutputWriters.add(rawOutputWriter);
   }

}
