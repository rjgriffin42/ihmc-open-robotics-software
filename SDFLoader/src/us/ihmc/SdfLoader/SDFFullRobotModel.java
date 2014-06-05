package us.ihmc.SdfLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFJointNameMap.JointRole;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.Camera;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.IMU;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.ForceSensorDefinition;
import us.ihmc.utilities.IMUDefinition;
import us.ihmc.utilities.InertiaTools;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.containers.ContainerTools;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.utilities.humanoidRobot.partNames.FingerName;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.humanoidRobot.partNames.LimbName;
import us.ihmc.utilities.humanoidRobot.partNames.NeckJointName;
import us.ihmc.utilities.humanoidRobot.partNames.RobotSpecificJointNames;
import us.ihmc.utilities.humanoidRobot.partNames.SpineJointName;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RevoluteJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;

public class SDFFullRobotModel implements FullRobotModel
{
   private final SDFJointNameMap sdfJointNameMap;
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final RigidBody elevator;
   private final SixDoFJoint rootJoint;

//   private final ArrayList<RevoluteJoint> revoluteJoints = new ArrayList<RevoluteJoint>();
   private final LinkedHashMap<String, OneDoFJoint> oneDoFJoints = new LinkedHashMap<String, OneDoFJoint>();

   private final EnumMap<NeckJointName, OneDoFJoint> neckJoints = ContainerTools.createEnumMap(NeckJointName.class);
   private final EnumMap<SpineJointName, OneDoFJoint> spineJoints = ContainerTools.createEnumMap(SpineJointName.class);
   private final SideDependentList<EnumMap<ArmJointName, OneDoFJoint>> armJointLists = SideDependentList.createListOfEnumMaps(ArmJointName.class);
   private final SideDependentList<EnumMap<LegJointName, OneDoFJoint>> legJointLists = SideDependentList.createListOfEnumMaps(LegJointName.class);
   private final RigidBody[] upperBody;
   private final RigidBody[] lowerBody;

   private final RigidBody pelvis;
   private RigidBody chest;
   private RigidBody head;

   private final SideDependentList<RigidBody> feet = new SideDependentList<RigidBody>();
   private final SideDependentList<RigidBody> hands = new SideDependentList<RigidBody>();
   private final ArrayList<IMUDefinition> imuDefinitions = new ArrayList<IMUDefinition>();
   private final ArrayList<ForceSensorDefinition> forceSensorDefinitions = new ArrayList<ForceSensorDefinition>();
   private final HashMap<String, ReferenceFrame> cameraFrames = new HashMap<String, ReferenceFrame>();
   private final HashMap<String, ReferenceFrame> lidarBaseFrames = new HashMap<String, ReferenceFrame>();
   private final HashMap<String, Transform3D> lidarBaseToSensorTransform = new HashMap<String, Transform3D>();
   private final HashMap<String, FrameVector> lidarAxis = new HashMap<String, FrameVector>();

   private final SideDependentList<ReferenceFrame> soleFrames = new SideDependentList<>();
   
   public SDFFullRobotModel(SDFLinkHolder rootLink, SDFJointNameMap sdfJointNameMap)
   {
      this.sdfJointNameMap = sdfJointNameMap;

      /*
       * Create root object
       */
      ReferenceFrame elevatorFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent("elevator", worldFrame, new Transform3D());
      elevator = new RigidBody("elevator", elevatorFrame);
      rootJoint = new SixDoFJoint(rootLink.getName(), elevator, elevatorFrame);
      if (!rootLink.getName().equals(sdfJointNameMap.getPelvisName()))
      {
         throw new RuntimeException("Pelvis joint is assumed to be the root joint");
      }

//      System.out.println("Adding rigid body " + rootLink.getName() + "; Mass: " + rootLink.getMass() + "; ixx: " + rootLink.getInertia().m00 + "; iyy: " + rootLink.getInertia().m11
//            + "; izz: " + rootLink.getInertia().m22 + "; COM Offset: " + rootLink.getCoMOffset());
      pelvis = ScrewTools.addRigidBody(rootLink.getName(), rootJoint, rootLink.getInertia(), rootLink.getMass(), rootLink.getCoMOffset());

      addSensorDefinitions(rootJoint, rootLink);
      for (SDFJointHolder sdfJoint : rootLink.getChildren())
      {
         addJointsRecursively(sdfJoint, pelvis);
      }

      Set<RigidBody> excludeBodiesForUpperBody = new LinkedHashSet<RigidBody>();
      Set<RigidBody> excludeBodiesForLowerBody = new LinkedHashSet<RigidBody>();

      for (InverseDynamicsJoint joint : pelvis.getChildrenJoints())
      {
         String name = joint.getName();
         JointRole role = sdfJointNameMap.getJointRole(name);
         if(role == null)
            continue;
         switch (role)
         {
         case LEG:
            excludeBodiesForUpperBody.add(joint.getSuccessor());
            break;
         case SPINE:
            excludeBodiesForLowerBody.add(joint.getSuccessor());
            break;
         default:
            throw new RuntimeException("Assuming that the leg or the spine are connected to the pelvis");
         }
      }


      lowerBody = ScrewTools.computeRigidBodiesInOrder(elevator, excludeBodiesForLowerBody);
      upperBody = ScrewTools.computeRigidBodiesInOrder(pelvis, excludeBodiesForUpperBody);

      
      for(RobotSide robotSide : RobotSide.values)
      {
         Transform3D soleToFootTransform = sdfJointNameMap.getAnkleToSoleFrameTransform().get(robotSide);
         ReferenceFrame soleFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent(robotSide.getCamelCaseNameForStartOfExpression() + "Sole", getEndEffectorFrame(robotSide, LimbName.LEG), soleToFootTransform);
         soleFrames.put(robotSide, soleFrame); 
      }
      
   }
   
   public String getModelName()
   {
      return sdfJointNameMap.getModelName();
   }
   
   private void addSensorDefinitions(InverseDynamicsJoint joint, SDFLinkHolder child)
   {
      if (child.getSensors() != null)
      {
         for (SDFSensor sensor : child.getSensors())
         {
            Transform3D pose = SDFConversionsHelper.poseToTransform(sensor.getPose());
            if ("imu".equals(sensor.getType()))
            {
               final IMU imu = sensor.getImu();

               if (imu != null)
               {
                  IMUDefinition imuDefinition = new IMUDefinition(child.getName() + "_" + sensor.getName(), joint.getSuccessor(), pose);

                  imuDefinitions.add(imuDefinition);
               }
               else
               {
                  System.err.println("JAXB loader: No imu section defined for imu sensor " + sensor.getName() + ", ignoring sensor.");
               }
            }
            else if("multicamera".equals(sensor.getType()) || "camera".equals(sensor.getType()))
            {
               List<Camera> cameras = sensor.getCamera();
               if(cameras != null)
               {
                  ReferenceFrame sensorFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent(sensor.getName(), joint.getFrameAfterJoint(), pose);
                  for(Camera camera : cameras)
                  {
                     Transform3D cameraTransform = SDFConversionsHelper.poseToTransform(camera.getPose()); 
                     
                     ReferenceFrame cameraFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent(sensor.getName() + "_" + camera.getName(), sensorFrame, cameraTransform);
                     cameraFrames.put(cameraFrame.getName(), cameraFrame);
                  }
                  
               }
               else
               {
                  System.err.println("JAXB loader: No camera section defined for camera sensor " + sensor.getName() + ", ignoring sensor.");
               }
               
            }
            else if("ray".equals(sensor.getType()) || "gpu_ray".equals(sensor.getType()))
            {
               if(joint instanceof RevoluteJoint)
               {
                  ReferenceFrame lidarFrame = joint.getFrameBeforeJoint();
                  lidarBaseFrames.put(sensor.getName(), lidarFrame);
                  lidarBaseToSensorTransform.put(sensor.getName(), pose);
                  lidarAxis.put(sensor.getName(), ((RevoluteJoint) joint).getJointAxis());
               }
               else
               {
                  System.err.println("Not supporting lidar not connected to a revolute joint");
               }
            }

         }
      }
   }

   private void addJointsRecursively(SDFJointHolder joint, RigidBody parentBody)
   {

      Vector3d jointAxis = new Vector3d(joint.getAxisInModelFrame());
      Vector3d offset = new Vector3d(joint.getOffsetFromParentJoint());
	   
      Transform3D visualTransform = new Transform3D();
      visualTransform.setRotation(joint.getLinkRotation());
           
      
      
      OneDoFJoint inverseDynamicsJoint;
      
//      ReferenceFrame parentFrame;
//      if (parentBody.isRootBody())
//         parentFrame = parentBody.getBodyFixedFrame();
//      else
//         parentFrame = parentBody.getParentJoint().getFrameAfterJoint();

//      ReferenceFrame frameBeforeJoint = ScrewTools.createOffsetFrame(parentFrame, joint.getTransformToParentJoint(), "bla");
//      FrameVector jointAxisFrame = new FrameVector(ReferenceFrame.getWorldFrame(), jointAxis);
//      jointAxisFrame.changeFrame(frameBeforeJoint);
      
      
      switch(joint.getType())
      {
      case REVOLUTE:
         inverseDynamicsJoint = ScrewTools.addRevoluteJoint(joint.getName(), parentBody, offset, jointAxis);
         break;
      case PRISMATIC:
         inverseDynamicsJoint = ScrewTools.addPrismaticJoint(joint.getName(), parentBody, offset, jointAxis);
         break;
      default:
         throw new RuntimeException("Joint type not implemented: " + joint.getType());
      }


      inverseDynamicsJoint.setEffortLimit(joint.getEffortLimit());
      inverseDynamicsJoint.setJointLimitLower(joint.getLowerLimit());
      inverseDynamicsJoint.setJointLimitUpper(joint.getUpperLimit());

      oneDoFJoints.put(joint.getName(), inverseDynamicsJoint);
      
      SDFLinkHolder childLink = joint.getChild();
      
      double mass = childLink.getMass();
      Vector3d comOffset = new Vector3d(childLink.getCoMOffset());
      Matrix3d inertia = InertiaTools.rotate(visualTransform, childLink.getInertia());
      visualTransform.transform(comOffset);

      RigidBody rigidBody = ScrewTools.addRigidBody(childLink.getName(), inverseDynamicsJoint, inertia, mass,
            comOffset);
//      System.out.println("Adding rigid body " + childLink.getName() + "; Mass: " + childLink.getMass() + "; ixx: " + childLink.getInertia().m00 + "; iyy: " + childLink.getInertia().m11
//            + "; izz: " + childLink.getInertia().m22 + "; COM Offset: " + childLink.getCoMOffset());


      if (rigidBody.getName().equals(sdfJointNameMap.getChestName()))
      {
         chest = rigidBody;
      }
      
      if (rigidBody.getName().equals(sdfJointNameMap.getHeadName()))
      {
         head = rigidBody;
      }

      Pair<RobotSide, LimbName> limbSideAndName = sdfJointNameMap.getLimbName(childLink.getName());
      if(limbSideAndName != null)
      {
         RobotSide limbSide = limbSideAndName.first();
         LimbName limbName = limbSideAndName.second();
         switch (limbName)
         {
         case ARM:
            hands.put(limbSide, rigidBody);
            break;
         case LEG:
            feet.put(limbSide, rigidBody);
            break;
         }
      }

      JointRole jointRole = sdfJointNameMap.getJointRole(joint.getName());
      if(jointRole != null)
      {
         switch (jointRole)
         {
         //TODO: Should armJointLists use legJoingName.first or armJointName.first?? looks backwards
         case ARM:
            Pair<RobotSide, ArmJointName> armJointName = sdfJointNameMap.getArmJointName(joint.getName());
            armJointLists.get(armJointName.first()).put(armJointName.second(), inverseDynamicsJoint);
            break;
         case LEG:
            Pair<RobotSide, LegJointName> legJointName = sdfJointNameMap.getLegJointName(joint.getName());
            legJointLists.get(legJointName.first()).put(legJointName.second(), inverseDynamicsJoint);
            break;
         case NECK:
            NeckJointName neckJointName = sdfJointNameMap.getNeckJointName(joint.getName());
            neckJoints.put(neckJointName, inverseDynamicsJoint);
            break;
         case SPINE:
            SpineJointName spineJointName = sdfJointNameMap.getSpineJointName(joint.getName());
            spineJoints.put(spineJointName, inverseDynamicsJoint);
            break;
         }
      }
      
      addSensorDefinitions(inverseDynamicsJoint, childLink);
      

      for(SDFForceSensor sdfForceSensor : joint.getForceSensors())
      {
         ForceSensorDefinition forceSensorDefinition = new ForceSensorDefinition(sdfForceSensor.getName(), inverseDynamicsJoint.getSuccessor(), sdfForceSensor.getTransform());
         forceSensorDefinitions.add(forceSensorDefinition);
      }

      for (SDFJointHolder sdfJoint : childLink.getChildren())
      {
         addJointsRecursively(sdfJoint, rigidBody);
      }

   }

   public RobotSpecificJointNames getRobotSpecificJointNames()
   {
      return sdfJointNameMap;
   }

   public void updateFrames()
   {
      elevator.updateFramesRecursively();
   }

   public ReferenceFrame getWorldFrame()
   {
      return worldFrame;
   }

   public ReferenceFrame getElevatorFrame()
   {
      return elevator.getBodyFixedFrame();
   }

   public ReferenceFrame getFrameAfterLegJoint(RobotSide robotSide, LegJointName legJointName)
   {
      return getLegJoint(robotSide, legJointName).getFrameAfterJoint();
   }

   public SixDoFJoint getRootJoint()
   {
      return rootJoint;
   }

   public RigidBody getElevator()
   {
      return elevator;
   }

   public OneDoFJoint getLegJoint(RobotSide robotSide, LegJointName legJointName)
   {
      return legJointLists.get(robotSide).get(legJointName);
   }

   public OneDoFJoint getArmJoint(RobotSide robotSide, ArmJointName armJointName)
   {
      return armJointLists.get(robotSide).get(armJointName);
   }
   
   public OneDoFJoint getSpineJoint(SpineJointName spineJointName)
   {
      return spineJoints.get(spineJointName);
   }

   public OneDoFJoint getNeckJoint(NeckJointName neckJointName)
   {
      return neckJoints.get(neckJointName);
   }

   public RigidBody getPelvis()
   {
      return pelvis;
   }

   public RigidBody getFoot(RobotSide robotSide)
   {
      return getEndEffector(robotSide, LimbName.LEG);
   }

   public RigidBody getHand(RobotSide robotSide)
   {
      return getEndEffector(robotSide, LimbName.ARM);
   }

   public RigidBody getChest()
   {
      return chest;
   }

   public RigidBody getHead()
   {
      return head;
   }

   public RigidBody getEndEffector(RobotSide robotSide, LimbName limbName)
   {
      switch (limbName)
      {
      case ARM:
         return hands.get(robotSide);
      case LEG:
         return feet.get(robotSide);
      default:
         throw new RuntimeException("Unkown end effector");
      }
   }

   public ReferenceFrame getEndEffectorFrame(RobotSide robotSide, LimbName limbName)
   {
      return getEndEffector(robotSide, limbName).getParentJoint().getFrameAfterJoint();
   }

   public FramePoint getStaticWristToFingerOffset(RobotSide robotSide, FingerName fingerName)
   {
      // TODO Auto-generated method stub
      return null;
   }

   public RigidBody[] getLowerBodyRigidBodiesInOrder()
   {
      return lowerBody;
   }

   public RigidBody[] getUpperBodyRigidBodiesInOrder()
   {
      return upperBody;
   }

   public OneDoFJoint[] getOneDoFJoints()
   {
      OneDoFJoint[] oneDoFJointsAsArray = new OneDoFJoint[oneDoFJoints.size()];
      oneDoFJoints.values().toArray(oneDoFJointsAsArray);
      return oneDoFJointsAsArray;
   }
   
   public void getOneDoFJoints(ArrayList<OneDoFJoint> oneDoFJointsToPack)
   {
      Collection<OneDoFJoint> values = oneDoFJoints.values();
      oneDoFJointsToPack.addAll(values);
   }
   
   public Map<String, OneDoFJoint> getOneDoFJointsAsMap()
   {
      return Collections.unmodifiableMap(oneDoFJoints);
   }
   
   public OneDoFJoint getOneDoFJointByName(String name)
   {
      return oneDoFJoints.get(name);
   }
   
   public IMUDefinition[] getIMUDefinitions()
   {
      IMUDefinition[] imuDefinitions = new IMUDefinition[this.imuDefinitions.size()];
      this.imuDefinitions.toArray(imuDefinitions);
      return imuDefinitions;
   }
   
   public ForceSensorDefinition[] getForceSensorDefinitions()
   {
      return this.forceSensorDefinitions.toArray(new ForceSensorDefinition[this.forceSensorDefinitions.size()]);
   }
   
   public ReferenceFrame getCameraFrame(String name)
   {
      return cameraFrames.get(name);
   }
   
   public ReferenceFrame getLidarBaseFrame(String name)
   {
      return lidarBaseFrames.get(name);
   }
   
   public Transform3D getLidarBaseToSensorTransform(String name)
   {
      return lidarBaseToSensorTransform.get(name);
   }
   
   public FrameVector getLidarJointAxis(String name)
   {
      return lidarAxis.get(name);
   }

   @Override
   public ReferenceFrame getSoleFrame(RobotSide robotSide)
   {
      return soleFrames.get(robotSide);
   }

}
