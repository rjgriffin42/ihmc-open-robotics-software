package us.ihmc.atlas;

import java.util.List;
import java.util.function.Consumer;

import us.ihmc.atlas.parameters.AtlasSensorInformation;
import us.ihmc.avatar.factory.RobotDefinitionTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.scs2.definition.geometry.ModelFileGeometryDefinition;
import us.ihmc.scs2.definition.robot.IMUSensorDefinition;
import us.ihmc.scs2.definition.robot.JointDefinition;
import us.ihmc.scs2.definition.robot.MomentOfInertiaDefinition;
import us.ihmc.scs2.definition.robot.RigidBodyDefinition;
import us.ihmc.scs2.definition.robot.RobotDefinition;
import us.ihmc.scs2.definition.robot.WrenchSensorDefinition;
import us.ihmc.scs2.definition.visual.VisualDefinition;

public class AtlasRobotDefinitionMutator implements Consumer<RobotDefinition>
{
   private final AtlasJointMap jointMap;
   private final AtlasSensorInformation sensorInformation;

   public AtlasRobotDefinitionMutator(AtlasJointMap jointMap, AtlasSensorInformation sensorInformation)
   {
      this.jointMap = jointMap;
      this.sensorInformation = sensorInformation;
   }

   @Override
   public void accept(RobotDefinition robotDefinition)
   {
      for (RigidBodyDefinition body : robotDefinition.getAllRigidBodies())
      {
         for (VisualDefinition visual : body.getVisualDefinitions())
         {
            if (visual.getGeometryDefinition() instanceof ModelFileGeometryDefinition)
            {
               ModelFileGeometryDefinition geometry = (ModelFileGeometryDefinition) visual.getGeometryDefinition();
               geometry.setFileName(geometry.getFileName().replace(".dae", ".obj"));
            }
         }
      }

      mutateChest(robotDefinition.getRigidBodyDefinition(jointMap.getChestName()));
      modifyHokuyoInertia(robotDefinition.getRigidBodyDefinition("hokuyo_link"));

      List<IMUSensorDefinition> imus = robotDefinition.getRigidBodyDefinition(jointMap.getPelvisName()).getParentJoint()
                                                      .getSensorDefinitions(IMUSensorDefinition.class);

      for (IMUSensorDefinition imu : imus)
      {
         if (imu.getName().equals("imu_sensor"))
            imu.setName("imu_sensor_at_pelvis_frame");
      }

      for (String forceSensorName : sensorInformation.getForceSensorNames())
      {
         JointDefinition jointDefinition = robotDefinition.getJointDefinition(forceSensorName);
         jointDefinition.addSensorDefinition(new WrenchSensorDefinition(forceSensorName, new RigidBodyTransform()));
      }

      if (jointMap.getModelScale() != 1.0)
         RobotDefinitionTools.scaleRobotDefinition(robotDefinition,
                                                   jointMap.getModelScale(),
                                                   jointMap.getMassScalePower(),
                                                   j -> !j.getName().contains("hokuyo"));
   }

   private void mutateChest(RigidBodyDefinition chest)
   {
      if (AtlasRobotModel.BATTERY_MASS_SIMULATOR_IN_ROBOT)
      {
         chest.getInertiaPose().setToZero();
         chest.setCenterOfMassOffset(-0.043, 0.00229456, 0.316809);
         chest.setMass(84.609);
      }
      else
      {
         chest.getInertiaPose().setToZero();
         chest.setCenterOfMassOffset(0.017261, 0.0032352, 0.3483);
         chest.setMass(60.009);
         double ixx = 1.5;
         double ixy = 0.0;
         double ixz = 0.1;
         double iyy = 1.5;
         double iyz = 0.0;
         double izz = 0.5;
         chest.setMomentOfInertia(new MomentOfInertiaDefinition(ixx, iyy, izz, ixy, ixz, iyz));
      }

      addChestIMU(chest);
   }

   private void addChestIMU(RigidBodyDefinition chest)
   {
      IMUSensorDefinition chestIMU = new IMUSensorDefinition();
      chestIMU.setName("imu_sensor_chest");
      chestIMU.getTransformToJoint().getTranslation().set(-0.15, 0.0, 0.3);
      chestIMU.getTransformToJoint().getRotation().setYawPitchRoll(-Math.PI / 2.0, 0.0, Math.PI / 2.0);
      chest.getParentJoint().addSensorDefinition(chestIMU);
   }

   private void modifyHokuyoInertia(RigidBodyDefinition hokuyo)
   {
      double ixx = 4.01606E-4;
      double iyy = 0.00208115;
      double izz = 0.00178402;
      double ixy = 4.9927E-8;
      double ixz = 1.0997E-5;
      double iyz = -9.8165E-9;
      hokuyo.setMomentOfInertia(new MomentOfInertiaDefinition(ixx, iyy, izz, ixy, ixz, iyz));
   }
}
