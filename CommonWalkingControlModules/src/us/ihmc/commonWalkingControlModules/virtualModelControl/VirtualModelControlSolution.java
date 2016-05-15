package us.ihmc.commonWalkingControlModules.virtualModelControl;

import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SpatialForceVector;
import us.ihmc.robotics.screwTheory.Wrench;

import java.util.List;
import java.util.Map;

public class VirtualModelControlSolution
{
   private InverseDynamicsJoint[] jointsToCompute;
   private Map<InverseDynamicsJoint, Double> jointTorques;
   private SpatialForceVector centroidalMomentumRateSolution;
   private Map<RigidBody, Wrench> externalWrenchSolution;
   private List<RigidBody> rigidBodiesWithExternalWrench;

   public VirtualModelControlSolution()
   {
   }

   public void setJointsToCompute(InverseDynamicsJoint[] jointsToCompute)
   {
      this.jointsToCompute = jointsToCompute;
   }

   public void setJointTorques(Map<InverseDynamicsJoint, Double> jointTorques)
   {
      this.jointTorques = jointTorques;
   }

   public void setCentroidalMomentumRateSolution(SpatialForceVector centroidalMomentumRateSolution)
   {
      this.centroidalMomentumRateSolution = centroidalMomentumRateSolution;
   }

   public void setExternalWrenchSolution(List<RigidBody> rigidBodiesWithExternalWrench, Map<RigidBody, Wrench> externalWrenchSolution)
   {
      this.rigidBodiesWithExternalWrench = rigidBodiesWithExternalWrench;
      this.externalWrenchSolution = externalWrenchSolution;
   }

   public List<RigidBody> getRigidBodiesWithExternalWrench()
   {
      return rigidBodiesWithExternalWrench;
   }

   public Map<RigidBody, Wrench> getExternalWrenchSolution()
   {
      return externalWrenchSolution;
   }

   public InverseDynamicsJoint[] getJointsToCompute()
   {
      return jointsToCompute;
   }

   public Map<InverseDynamicsJoint, Double> getJointTorques()
   {
      return jointTorques;
   }

   public SpatialForceVector getCentroidalMomentumRateSolution()
   {
      return centroidalMomentumRateSolution;
   }
}
