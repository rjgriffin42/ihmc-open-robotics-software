package us.ihmc.robotics.screwTheory;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.ImmutablePair;

import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;

public abstract class AbstractInverseDynamicsCopier
{  
   private final ArrayList<ImmutablePair<JointBasics, JointBasics>> jointPairs = new ArrayList<ImmutablePair<JointBasics,JointBasics>>();
   
   
   public AbstractInverseDynamicsCopier(RigidBody originalBody, RigidBody targetBody)
   {
      setRigidBodies(originalBody, targetBody);
   }

   public void setRigidBodies(RigidBody originalBody, RigidBody targetBody)
   {
      jointPairs.clear();
      JointBasics[] originalJoints = ScrewTools.computeSubtreeJoints(originalBody);
      JointBasics[] targetJoints = ScrewTools.computeSubtreeJoints(targetBody);
      
      for(int i = 0; i < originalJoints.length; i++)
      {      
         JointBasics originalJoint = originalJoints[i];
         JointBasics targetJoint = targetJoints[i];
         
         areJointsTheSame(originalJoint, targetJoint);
         
         ImmutablePair<JointBasics, JointBasics> jointPair = new ImmutablePair<JointBasics, JointBasics>(originalJoint, targetJoint);
         
         jointPairs.add(jointPair);
      }
   }
   
   
   public final void copy()
   {
      for(int i = 0; i <  jointPairs.size(); i++)
      {
         ImmutablePair<JointBasics,JointBasics> jointPair = jointPairs.get(i);
         JointBasics originalJoint = jointPair.getLeft();
         JointBasics targetJoint = jointPair.getRight();
         
         copyJoint(originalJoint, targetJoint);
         
      }
   }
   
   protected abstract void copyJoint(JointBasics originalJoint, JointBasics targetJoint);
   
   
   private static final void areJointsTheSame(JointBasics originalJoint, JointBasics targetJoint)
   {
      if(!(originalJoint.getClass().equals(targetJoint.getClass()) && 
            originalJoint.getName().equals(targetJoint.getName())))
      {
         throw new RuntimeException(originalJoint.getName() + " differs from " + targetJoint);
      }
      
   }
}
