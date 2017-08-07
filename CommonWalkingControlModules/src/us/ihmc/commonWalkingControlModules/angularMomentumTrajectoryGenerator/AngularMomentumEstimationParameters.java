package us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator;

import us.ihmc.commonWalkingControlModules.configurations.CoPPointName;
import us.ihmc.commonWalkingControlModules.configurations.SmoothCMPPlannerParameters;
import us.ihmc.robotModels.FullHumanoidRobotModel;

public class AngularMomentumEstimationParameters
{
   /**
    * Defines the percentage of the total robot mass that is to be considered as the swing leg
    */
   private double percentageSwingLegMass = 0.1;
   /**
    * Defines the percentage of the total robot mass that is to be considered as the support leg
    */
   private double percentageSupportLegMass = 0.1;
   
   private final SmoothCMPPlannerParameters copPlannerParameters;
   private final FullHumanoidRobotModel robotModel;

   public AngularMomentumEstimationParameters(FullHumanoidRobotModel robotModel, SmoothCMPPlannerParameters cmpPlannerParameters)
   {
      this.copPlannerParameters = cmpPlannerParameters;
      this.robotModel = robotModel;
   }

   public boolean computePredictedAngularMomentum()
   {
      return false;
   }

   public CoPPointName getEntryCoPName()
   {
      return this.copPlannerParameters.getEntryCoPName();
   }

   public CoPPointName getExitCoPName()
   {
      return this.copPlannerParameters.getExitCoPName();
   }

   public CoPPointName getEndCoPName()
   {
      return this.copPlannerParameters.getEndCoPName();
   }

   public double getSwingLegMass()
   {
      if(robotModel == null)
         return 0.0;
      else 
         return robotModel.getTotalMass() * percentageSwingLegMass;
   }

   public double getSupportLegMass()
   {
      if(robotModel == null)
         return 0.0;
      else
         return robotModel.getTotalMass() * percentageSupportLegMass;
   }

   public double getTotalMass()
   {
      if(robotModel == null)
         return 0.0;
      else
         return robotModel.getTotalMass() * (1 - percentageSupportLegMass - percentageSwingLegMass);
   }

   public CoPPointName getInitialCoPPointName()
   {
      return getEndCoPName();
   }

   public CoPPointName getEndCoPPointName()
   {
      return getEndCoPName();
   }

   public CoPPointName getInitialDepartureReferenceName()
   {
      return getEntryCoPName();
   }

   public CoPPointName getFinalApproachReferenceName()
   {
      return getExitCoPName();
   }

   public int getNumberOfFootstepsToConsider()
   {
      return this.copPlannerParameters.getNumberOfFootstepsToConsider();
   }

   public SmoothCMPPlannerParameters getCoPPlannerParameters()
   {
      return this.copPlannerParameters;
   }

   public CoPPointName[] getCoPPointList()
   {
      return copPlannerParameters.getCoPPointsToPlan();
   }

   public double getCoMHeight()
   {
      return 0.33;
   }

   public double getSwingFootMaxLift()
   {
      return 0.10;
   }

   public int getNumberOfPointsToSampleForTransfer()
   {
      return 20;
   }

   public int getNumberOfPointsToSampleForSwing()
   {
      return 20;
   }

   public AngularMomentumSplineType getSplineType()
   {
      return AngularMomentumSplineType.LINEAR;
   }

   public String getYoTimeVariableName()
   {
      return "t";
   }
}
