package us.ihmc.manipulation.planning.avatartest;

import us.ihmc.avatar.networkProcessor.kinematicsToolboxModule.KinematicsToolboxController;
import us.ihmc.manipulation.planning.robotcollisionmodel.RobotCollisionModel;
import us.ihmc.robotModels.FullHumanoidRobotModel;

public abstract class WholeBodyPoseValidityTesterOld
{
   protected KinematicsToolboxController ikToolboxController;
   protected FullHumanoidRobotModel ikFullRobotModel;
   protected RobotCollisionModel robotCollisionModel;

   public boolean isValid = true;
   protected boolean collisionFree = true;
   protected boolean jointlimitFree = true;

   public WholeBodyPoseValidityTesterOld(KinematicsToolboxController ikToolboxController)
   {
      this.ikToolboxController = ikToolboxController;
      this.ikFullRobotModel = this.ikToolboxController.getDesiredFullRobotModel();

      this.robotCollisionModel = new RobotCollisionModel(this.ikFullRobotModel);
   }
   
   private void getResult()
   {
      // collision tester
      ikToolboxController.updateTools();
      
      robotCollisionModel.update();
      collisionFree = robotCollisionModel.getCollisionResult();

      // joint limit tester
      /*
       * joint limit should be added. 170401
       */
      jointlimitFree = true;
   }

   public boolean isValid()
   {
      getResult();
      
      if (collisionFree == false || jointlimitFree == false)
      {
         isValid = false;
      }
      else
      {
         isValid = true;
      }
      
      return isValid;
   }

   public RobotCollisionModel getRobotCollisionModel()
   {
      return robotCollisionModel;
   }

   public abstract void addEnvironmentCollisionModel();

}
