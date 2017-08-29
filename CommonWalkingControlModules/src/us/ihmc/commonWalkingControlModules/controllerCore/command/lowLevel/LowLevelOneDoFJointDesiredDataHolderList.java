package us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel;

import java.util.HashMap;

import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.tools.lists.PairList;

public class LowLevelOneDoFJointDesiredDataHolderList implements LowLevelOneDoFJointDesiredDataHolderReadOnly
{
   private final PairList<OneDoFJoint, LowLevelJointData> jointsAndData = new PairList<>();
   private final HashMap<OneDoFJoint, LowLevelJointData> jointMap = new HashMap<>();

   public LowLevelOneDoFJointDesiredDataHolderList(OneDoFJoint[] joints)
   {
      for (OneDoFJoint joint : joints)
      {
         LowLevelJointData data = new LowLevelJointData();
         jointsAndData.add(joint, data);
         jointMap.put(joint, data);
      }
   }

   @Override
   public boolean hasDataForJoint(OneDoFJoint joint)
   {
      return jointMap.containsKey(joint);
   }

   @Override
   public OneDoFJoint getOneDoFJoint(int index)
   {
      return jointsAndData.first(index);
   }

   @Override
   public LowLevelJointDataReadOnly getLowLevelJointData(OneDoFJoint joint)
   {
      return jointMap.get(joint);
   }

   @Override
   public int getNumberOfJointsWithLowLevelData()
   {
      return jointsAndData.size();
   }

   @Override
   public LowLevelJointDataReadOnly getLowLevelJointData(int index)
   {
      return jointsAndData.second(index);
   }

}
