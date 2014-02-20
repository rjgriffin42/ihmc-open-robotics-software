package us.ihmc.darpaRoboticsChallenge;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import us.ihmc.atlas.AtlasJointMap;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.handControl.DRCHandModel;
import us.ihmc.darpaRoboticsChallenge.valkyrie.ValkyrieArmControllerParameters;
import us.ihmc.darpaRoboticsChallenge.valkyrie.ValkyrieJointMap;
import us.ihmc.darpaRoboticsChallenge.valkyrie.ValkyrieWalkingControllerParameters;

public enum DRCRobotModel
{
   ATLAS_NO_HANDS_ADDED_MASS, ATLAS_SANDIA_HANDS, ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS,
   DRC_NO_HANDS, DRC_HANDS, DRC_EXTENDED_HANDS, DRC_HOOKS,  DRC_TASK_HOSE, DRC_EXTENDED_HOOKS, VALKYRIE;
   
   public enum RobotType
   {
      ATLAS,VALKYRIE
   }
   
   public static DRCRobotModel getDefaultRobotModel()
   {
      return DRCLocalConfigParameters.robotModelToUse;
   }
   
   public ArmControllerParameters getArmControllerParameters()
   {
      switch (this)
      {
      case VALKYRIE:
         return new ValkyrieArmControllerParameters();
         
      case ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS:
      case ATLAS_NO_HANDS_ADDED_MASS:
      case ATLAS_SANDIA_HANDS:
      case DRC_EXTENDED_HANDS:
      case DRC_EXTENDED_HOOKS:
      case DRC_HANDS:
      case DRC_HOOKS:
      case DRC_NO_HANDS:
      case DRC_TASK_HOSE:
         return new AtlasArmControllerParameters(DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT);
         
      default:
         throw new RuntimeException("Unkown model");
      }
   }
   
   public WalkingControllerParameters getWalkingControlParamaters()
   {
      switch (this)
      {
      case VALKYRIE:
         return new ValkyrieWalkingControllerParameters();
         
      case ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS:
      case ATLAS_NO_HANDS_ADDED_MASS:
      case ATLAS_SANDIA_HANDS:
      case DRC_EXTENDED_HANDS:
      case DRC_EXTENDED_HOOKS:
      case DRC_HANDS:
      case DRC_HOOKS:
      case DRC_NO_HANDS:
      case DRC_TASK_HOSE:
         return new AtlasWalkingControllerParameters();
      default:
         throw new RuntimeException("Unkown model");
      }
   }

   public DRCRobotJointMap getJointMap(boolean addLoadsOfContactPoints, boolean addLoadsOfContactPointsToFeetOnly)
   {
         System.out.println(this);
         switch (this)
         {
      case VALKYRIE:
         return new ValkyrieJointMap(this, addLoadsOfContactPoints, addLoadsOfContactPointsToFeetOnly);
         
      case ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS:
      case ATLAS_NO_HANDS_ADDED_MASS:
      case ATLAS_SANDIA_HANDS:
      case DRC_EXTENDED_HANDS:
      case DRC_EXTENDED_HOOKS:
      case DRC_HANDS:
      case DRC_HOOKS:
      case DRC_NO_HANDS:
      case DRC_TASK_HOSE:
        return new AtlasJointMap(this, addLoadsOfContactPoints, addLoadsOfContactPointsToFeetOnly);
        
      default:
         throw new RuntimeException("Unkown model");
      }
   }
   
   public boolean hasIRobotHands()
   {
      switch (this)
      {
      case DRC_TASK_HOSE:
      case DRC_HANDS:
      case DRC_EXTENDED_HANDS:
         return true;

      default:
         return false;
      }
   }

   public boolean hasIRobotHandsOnExtensions()
   {
      switch (this)
      {
      case DRC_EXTENDED_HANDS:
         return true;

      default:
         return false;
      }
   }

   public boolean hasHookHands()
   {
      switch (this)
      {
      case DRC_TASK_HOSE:
      case DRC_HOOKS:
      case DRC_EXTENDED_HOOKS:
         return true;

      default:
         return false;
      }
   }

   public DRCHandModel getHandModel()
   {
      switch (this)
      {
      case DRC_HANDS:
      case DRC_EXTENDED_HANDS:
      case DRC_TASK_HOSE:
         return DRCHandModel.IROBOT;

      case ATLAS_SANDIA_HANDS:
      case ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS:
         return DRCHandModel.SANDIA;

      case ATLAS_NO_HANDS_ADDED_MASS:
      case DRC_NO_HANDS:
      case DRC_HOOKS:
      case DRC_EXTENDED_HOOKS:
      default:
         return DRCHandModel.NONE;
      }
   }

   public RobotType getType()
   {
      switch (this)
      {
      case ATLAS_NO_HANDS_ADDED_MASS:
      case ATLAS_SANDIA_HANDS:
      case ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS:
      case DRC_NO_HANDS:
      case DRC_HANDS:
      case DRC_EXTENDED_HANDS:
      case DRC_HOOKS:
      case DRC_TASK_HOSE:
      case DRC_EXTENDED_HOOKS:
         return RobotType.ATLAS;
         
      case VALKYRIE:
         return RobotType.VALKYRIE;

      default:
         throw new RuntimeException("Unkown model");
      }
   }
   
   public String getModelName()
   {
      switch (this)
      {
      case ATLAS_NO_HANDS_ADDED_MASS:
      case ATLAS_SANDIA_HANDS:
      case ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS:
      case DRC_NO_HANDS:
      case DRC_HANDS:
      case DRC_EXTENDED_HANDS:
      case DRC_HOOKS:
      case DRC_TASK_HOSE:
      case DRC_EXTENDED_HOOKS:
         return "atlas";
         
      case VALKYRIE:
         return "V1";

      default:
         throw new RuntimeException("Unkown model");
      }
   }

}
