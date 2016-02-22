package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;

import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

public class DesiredOneDoFJointTorqueHolder
{
   private final List<MutableDouble> unusedMutableDoubles;
   private final List<OneDoFJoint> jointsWithDesiredTorques;
   private final Map<String, MutableDouble> jointDesiredTorqueMap;

   public DesiredOneDoFJointTorqueHolder()
   {
      this(50);
   }

   public DesiredOneDoFJointTorqueHolder(int initialCapacity)
   {
      unusedMutableDoubles = new ArrayList<>(initialCapacity);
      jointsWithDesiredTorques = new ArrayList<>(initialCapacity);
      jointDesiredTorqueMap = new HashMap<>(initialCapacity);

   }

   public void reset()
   {
      for (int i = 0; i < jointsWithDesiredTorques.size(); i++)
         unusedMutableDoubles.add(jointDesiredTorqueMap.remove(jointsWithDesiredTorques.get(i).getName()));
      jointsWithDesiredTorques.clear();
   }

   public int getNumberOfJoints()
   {
      return jointsWithDesiredTorques.size();
   }

   public void extractDesiredTorquesFromInverseDynamicsJoints(InverseDynamicsJoint[] inverseDynamicsJoints)
   {
      for (int i = 0; i < inverseDynamicsJoints.length; i++)
      {
         if (inverseDynamicsJoints[i] instanceof OneDoFJoint)
         {
            OneDoFJoint oneDoFJoint = (OneDoFJoint) inverseDynamicsJoints[i];
            registerDesiredTorque(oneDoFJoint, oneDoFJoint.getTau());
         }
      }
   }

   public void retrieveJointsFromName(Map<String, OneDoFJoint> nameToJointMap)
   {
      for (int i = 0; i < jointsWithDesiredTorques.size(); i++)
      {
         jointsWithDesiredTorques.set(i, nameToJointMap.get(jointsWithDesiredTorques.get(i).getName()));
      }
   }

   public void registerDesiredTorque(OneDoFJoint oneDoFJoint, double tauDesired)
   {
      jointsWithDesiredTorques.add(oneDoFJoint);

      if (jointDesiredTorqueMap.containsKey(oneDoFJoint.getName()))
         throw new RuntimeException("Reset before registering new joint torques.");

      MutableDouble jointMutableTorque;

      if (unusedMutableDoubles.isEmpty())
         jointMutableTorque = new MutableDouble();
      else
         jointMutableTorque = unusedMutableDoubles.remove(unusedMutableDoubles.size() - 1);
      jointDesiredTorqueMap.put(oneDoFJoint.getName(), jointMutableTorque);
      jointMutableTorque.setValue(tauDesired);
   }

   public void insertDesiredTorquesIntoOneDoFJoints(OneDoFJoint[] oneDoFJoints)
   {
      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         OneDoFJoint joint = oneDoFJoints[i];
         joint.setTau(jointDesiredTorqueMap.get(joint.getName()).doubleValue());
      }
   }

   public OneDoFJoint getOneDoFJoint(int index)
   {
      return jointsWithDesiredTorques.get(index);
   }

   public double getDesiredJointTorque(int index)
   {
      return jointDesiredTorqueMap.get(jointsWithDesiredTorques.get(index).getName()).doubleValue();
   }

   public void set(DesiredOneDoFJointTorqueHolder other)
   {
      reset();
      for (int i = 0; i < other.getNumberOfJoints(); i++)
      {
         OneDoFJoint oneDoFJoint = other.getOneDoFJoint(i);
         double desiredJointTorque = other.getDesiredJointTorque(i);
         registerDesiredTorque(oneDoFJoint, desiredJointTorque);
      }
   }
}