package us.ihmc.SdfLoader;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import us.ihmc.SdfLoader.models.FullQuadrupedRobotModel;
import us.ihmc.SdfLoader.partNames.JointRole;
import us.ihmc.SdfLoader.partNames.QuadrupedJointName;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;

public class SDFFullQuadrupedRobotModel extends SDFFullRobotModel implements FullQuadrupedRobotModel
{
   private final BiMap<QuadrupedJointName, OneDoFJoint> jointNameOneDoFJointBiMap = HashBiMap.create();

   private final QuadrantDependentList<List<OneDoFJoint>> legOneDoFJoints = new QuadrantDependentList<>();

   private QuadrantDependentList<RigidBody> feet;

   public SDFFullQuadrupedRobotModel(SDFLinkHolder rootLink, SDFQuadrupedJointNameMap sdfJointNameMap, String[] sensorLinksToTrack)
   {
      super(rootLink, sdfJointNameMap, sensorLinksToTrack);

      for (OneDoFJoint oneDoFJoint : getOneDoFJoints())
      {
         QuadrupedJointName quadrupedJointName = sdfJointNameMap.getJointNameForSDFName(oneDoFJoint.getName());
         jointNameOneDoFJointBiMap.put(quadrupedJointName, oneDoFJoint);

         if (quadrupedJointName.getRole() == JointRole.LEG)
         {
            RobotQuadrant quadrant = quadrupedJointName.getQuadrant();
            if (legOneDoFJoints.get(quadrant) == null)
            {
               legOneDoFJoints.set(quadrant, new ArrayList<OneDoFJoint>());
            }

            legOneDoFJoints.get(quadrant).add(oneDoFJoint);
         }
      }
   }

   @Override
   protected void mapRigidBody(SDFJointHolder joint, OneDoFJoint inverseDynamicsJoint, RigidBody rigidBody)
   {
      if(feet == null)
      {
         feet = new QuadrantDependentList<RigidBody>();
      }

      super.mapRigidBody(joint, inverseDynamicsJoint, rigidBody);

      SDFQuadrupedJointNameMap jointMap = (SDFQuadrupedJointNameMap) sdfJointNameMap;
      for(RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         String jointBeforeFootName = jointMap.getJointBeforeFootName(robotQuadrant);

         if(jointBeforeFootName.equals(joint.getName()))
         {
            feet.set(robotQuadrant, rigidBody);
         }
      }
   }

   @Override
   public RigidBody getFoot(RobotQuadrant robotQuadrant)
   {
      return feet.get(robotQuadrant);
   }

   public List<OneDoFJoint> getLegOneDoFJoints(RobotQuadrant quadrant)
   {
      return legOneDoFJoints.get(quadrant);
   }

   public OneDoFJoint getOneDoFJointBeforeFoot(RobotQuadrant quadrant)
   {
      return (OneDoFJoint) getFoot(quadrant).getParentJoint();
   }

   public OneDoFJoint getOneDoFJointByName(QuadrupedJointName name)
   {
      return jointNameOneDoFJointBiMap.get(name);
   }

   public QuadrupedJointName getNameForOneDoFJoint(OneDoFJoint oneDoFJoint)
   {
      return jointNameOneDoFJointBiMap.inverse().get(oneDoFJoint);
   }
}
