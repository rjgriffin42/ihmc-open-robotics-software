package us.ihmc.SdfLoader;

import com.yobotics.simulationconstructionset.*;
import com.yobotics.simulationconstructionset.simulatedSensors.*;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.Camera;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.IMU;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.IMU.IMUNoise;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.IMU.IMUNoise.NoiseParameters;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.Ray;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.Ray.Noise;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.Ray.Range;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.Ray.Scan;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.Ray.Scan.HorizontalScan;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.HumanoidRobot;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.lidar.polarLidar.geometry.LIDARScanDefinition;
import us.ihmc.utilities.lidar.polarLidar.geometry.PolarLidarScanParameters;
import us.ihmc.utilities.screwTheory.RigidBodyInertia;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class SDFRobot extends Robot implements HumanoidRobot    // TODO: make an SDFHumanoidRobot
{
   private static final boolean DEBUG = false;
   private static final boolean SHOW_CONTACT_POINTS = true;
   private static final boolean USE_POLAR_LIDAR_MODEL = true;
   private static final boolean SHOW_COM_REFERENCE_FRAMES = false;

   private static final long serialVersionUID = 5864358637898048080L;

   private final ArrayList<String> resourceDirectories;

   private final LinkedHashMap<String, OneDegreeOfFreedomJoint> oneDoFJoints = new LinkedHashMap<String, OneDegreeOfFreedomJoint>();
   private final LinkedHashMap<String, Transform3D> jointTransforms = new LinkedHashMap<String, Transform3D>();


   private final FloatingJoint rootJoint;

   private final SideDependentList<ArrayList<GroundContactPoint>> footGroundContactPoints = new SideDependentList<ArrayList<GroundContactPoint>>();

   private final LinkedHashMap<String, SDFCamera> cameras = new LinkedHashMap<String, SDFCamera>();

   public SDFRobot(GeneralizedSDFRobotModel generalizedSDFRobotModel, SDFJointNameMap sdfJointNameMap, boolean useCollisionMeshes)
   {
      this(generalizedSDFRobotModel, sdfJointNameMap, useCollisionMeshes, true, true);
   }

   protected SDFRobot(GeneralizedSDFRobotModel generalizedSDFRobotModel, SDFJointNameMap sdfJointNameMap, boolean useCollisionMeshes,
                      boolean enableTorqueVelocityLimits, boolean enableDamping)
   {
      super(generalizedSDFRobotModel.getName());
      this.resourceDirectories = generalizedSDFRobotModel.getResourceDirectories();

      System.out.println("Creating root joints for root links");

      ArrayList<SDFLinkHolder> rootLinks = generalizedSDFRobotModel.getRootLinks();

      if (rootLinks.size() > 1)
      {
         throw new RuntimeException("Can only accomodate one root link for now");
      }

      SDFLinkHolder rootLink = rootLinks.get(0);

      Vector3d offset = new Vector3d();
      Quat4d orientation = new Quat4d();
      generalizedSDFRobotModel.getTransformToRoot().get(orientation, offset);
      rootJoint = new FloatingJoint(rootLink.getName(), new Vector3d(), this);
      setPositionInWorld(offset);
      setOrientation(orientation);

      addSensors(rootJoint, rootLink);

      Link scsRootLink = createLink(rootLink, new Transform3D(), useCollisionMeshes);
      rootJoint.setLink(scsRootLink);

      addRootJoint(rootJoint);

      if (sdfJointNameMap != null)
      {
         enableTorqueVelocityLimits = enableTorqueVelocityLimits && sdfJointNameMap.enableTorqueVelocityLimits();
      }

      jointTransforms.put(rootJoint.getName(), new Transform3D());

      for (SDFJointHolder child : rootLink.getChildren())
      {
         addJointsRecursively(child, rootJoint, MatrixTools.IDENTITY, useCollisionMeshes, enableTorqueVelocityLimits, enableDamping);
      }

      for (RobotSide robotSide : RobotSide.values)
      {
         footGroundContactPoints.put(robotSide, new ArrayList<GroundContactPoint>());
      }

      LinkedHashMap<String, Integer> counters = new LinkedHashMap<String, Integer>();
      if (sdfJointNameMap != null)
      {
         for (Pair<String, Vector3d> jointContactPoint : sdfJointNameMap.getJointNameGroundContactPointMap())
         {
            String jointName = jointContactPoint.first();

            int count;
            if (counters.get(jointName) == null)
               count = 0;
            else
               count = counters.get(jointName);

            Vector3d gcOffset = jointContactPoint.second();
            jointTransforms.get(jointName).transform(gcOffset);


            GroundContactPoint groundContactPoint = new GroundContactPoint("gc_" + SDFConversionsHelper.sanitizeJointName(jointName) + "_" + count++, gcOffset,
                                                       this);

            ExternalForcePoint externalForcePoint = new ExternalForcePoint("ef_" + SDFConversionsHelper.sanitizeJointName(jointName) + "_" + count++, gcOffset,
                                                       this);

            Joint joint;
            if (jointName.equals(rootJoint.getName()))
               joint = rootJoint;
            else
            {
               joint = oneDoFJoints.get(jointName);
            }

            joint.addGroundContactPoint(groundContactPoint);
            joint.addExternalForcePoint(externalForcePoint);

            counters.put(jointName, count);

            if (SHOW_CONTACT_POINTS)
            {
               Graphics3DObject graphics = joint.getLink().getLinkGraphics();
               graphics.identity();
               graphics.translate(jointContactPoint.second());
               double radius = 0.01;
               graphics.addSphere(radius, YoAppearance.Orange());
            }

            for (RobotSide robotSide : RobotSide.values)
            {
               if (jointName.equals(sdfJointNameMap.getJointBeforeFootName(robotSide)))
                  footGroundContactPoints.get(robotSide).add(groundContactPoint);
            }
         }
      }

      Point3d centerOfMass = new Point3d();
      double totalMass = computeCenterOfMass(centerOfMass);
      System.out.println("SDFRobot: Total robot mass: " + totalMass);

   }

   public Quat4d getRootJointToWorldRotationQuaternion()
   {
      return rootJoint.getQuaternion();
   }

   public void getRootJointToWorldTransform(Transform3D transform)
   {
      rootJoint.getTransformToWorld(transform);
   }

   public void setPositionInWorld(Vector3d offset)
   {
      rootJoint.setPosition(offset);
   }

   public void setOrientation(double yaw, double pitch, double roll)
   {
      rootJoint.setYawPitchRoll(yaw, pitch, roll);
   }

   public void setOrientation(Quat4d quaternion)
   {
      rootJoint.setQuaternion(quaternion);
   }

   public void setAngularVelocity(Vector3d velocity)
   {
      rootJoint.setAngularVelocityInBody(velocity);
   }

   public void setLinearVelocity(Vector3d velocity)
   {
      rootJoint.setVelocity(velocity);
   }

   public OneDegreeOfFreedomJoint getOneDoFJoint(String name)
   {
      return oneDoFJoints.get(name);
   }

   public FloatingJoint getRootJoint()
   {
      return rootJoint;
   }

   public Collection<OneDegreeOfFreedomJoint> getOneDoFJoints()
   {
      return oneDoFJoints.values();
   }

   private void addJointsRecursively(SDFJointHolder joint, Joint scsParentJoint, Matrix3d chainRotationIn, boolean useCollisionMeshes,
                                     boolean enableTorqueVelocityLimits, boolean enableDamping)
   {
      Matrix3d rotation = new Matrix3d();
      Vector3d offset = new Vector3d();
      joint.getTransformToParentJoint().get(rotation, offset);
      Vector3d jointAxis = new Vector3d(joint.getAxis());

      Matrix3d chainRotation = new Matrix3d(chainRotationIn);
      chainRotation.mul(rotation);


      Transform3D rotationTransform = new Transform3D();
      rotationTransform.setRotation(chainRotationIn);
      rotationTransform.transform(offset);



      Transform3D jointTransform = new Transform3D();
      jointTransform.setRotation(chainRotation);
      jointTransforms.put(joint.getName(), jointTransform);



      String sanitizedJointName = SDFConversionsHelper.sanitizeJointName(joint.getName());


      Joint scsJoint;
      switch (joint.getType())
      {
         case REVOLUTE :
            PinJoint pinJoint = new PinJoint(sanitizedJointName, offset, this, jointAxis);
            if (joint.hasLimits())
            {
               if (isFinger(joint))
               {
                  pinJoint.setLimitStops(joint.getLowerLimit(), joint.getUpperLimit(), 10.0, 2.5);
               }
               else
               {
                  if ((joint.getContactKd() == 0.0) && (joint.getContactKp() == 0.0))
                  {
                     pinJoint.setLimitStops(joint.getLowerLimit(), joint.getUpperLimit(), 100.0, 20.0);
                  }
                  else
                  {
                     pinJoint.setLimitStops(joint.getLowerLimit(), joint.getUpperLimit(), 0.0001 * joint.getContactKp(), joint.getContactKd());
                  }
               }
            }

            if (enableDamping)
            {
               pinJoint.setDamping(joint.getDamping());
            }
            else
            {
               pinJoint.setDampingParameterOnly(joint.getDamping());
            }

            if (enableTorqueVelocityLimits)
            {
               if (!isFinger(joint))
               {
                  if (!Double.isNaN(joint.getEffortLimit()))
                  {
                     pinJoint.setTorqueLimits(joint.getEffortLimit());
                  }

                  if (!Double.isNaN(joint.getVelocityLimit()))
                  {
                     if (!isFinger(joint))
                     {
                        pinJoint.setVelocityLimits(joint.getVelocityLimit(), 500.0);
                     }
                  }
               }
            }


            oneDoFJoints.put(joint.getName(), pinJoint);
            scsJoint = pinJoint;

            break;

         case PRISMATIC :
            SliderJoint sliderJoint = new SliderJoint(sanitizedJointName, offset, this, jointAxis);
            if (joint.hasLimits())
            {
               if ((joint.getContactKd() == 0.0) && (joint.getContactKp() == 0.0))
               {
                  sliderJoint.setLimitStops(joint.getLowerLimit(), joint.getUpperLimit(), 100.0, 20.0);
               }
               else
               {
                  sliderJoint.setLimitStops(joint.getLowerLimit(), joint.getUpperLimit(), 0.0001 * joint.getContactKp(), joint.getContactKd());
               }
            }


            if (enableDamping)
            {
               sliderJoint.setDamping(joint.getDamping());
            }
            else
            {
               sliderJoint.setDampingParameterOnly(joint.getDamping());
            }

            oneDoFJoints.put(joint.getName(), sliderJoint);

            scsJoint = sliderJoint;

            break;

         default :
            throw new RuntimeException("Joint type not implemented: " + joint.getType());
      }

      scsJoint.setLink(createLink(joint.getChild(), rotationTransform, useCollisionMeshes));
      scsParentJoint.addJoint(scsJoint);

      if (DEBUG)
         if ("hokuyo_joint".equals(scsJoint.getName()))
            System.out.println("hokuyo joint's parent is : " + scsParentJoint.getName());

      addSensors(scsJoint, joint.getChild());




      for (SDFJointHolder child : joint.getChild().getChildren())
      {
         addJointsRecursively(child, scsJoint, chainRotation, useCollisionMeshes, enableTorqueVelocityLimits, enableDamping);
      }

   }

   private boolean isFinger(SDFJointHolder pinJoint)
   {
      return pinJoint.getName().contains("f0") || pinJoint.getName().contains("f1") || pinJoint.getName().contains("f2") || pinJoint.getName().contains("f3");
   }


   private void addSensors(Joint scsJoint, SDFLinkHolder child)
   {
      addCameraMounts(scsJoint, child);
      addLidarMounts(scsJoint, child);
      addIMUMounts(scsJoint, child);
   }

   private void addCameraMounts(Joint scsJoint, SDFLinkHolder child)
   {
      if (child.getSensors() != null)
      {
         for (SDFSensor sensor : child.getSensors())
         {
            if ("camera".equals(sensor.getType()) || "multicamera".equals(sensor.getType()))
            {
               // TODO: handle left and right sides of multicamera
               final List<Camera> cameras = sensor.getCamera();

               if (cameras != null)
               {
                  Transform3D pose = SDFConversionsHelper.poseToTransform(sensor.getPose());
                  for (Camera camera : cameras)
                  {
                     Transform3D cameraTransform = new Transform3D(pose);
                     cameraTransform.mul(SDFConversionsHelper.poseToTransform(camera.getPose()));

                     double fieldOfView = Double.parseDouble(camera.getHorizontalFov());
                     double clipNear = Double.parseDouble(camera.getClip().getNear());
                     double clipFar = Double.parseDouble(camera.getClip().getFar());
                     CameraMount mount = new CameraMount(sensor.getName() + "_" + camera.getName(), cameraTransform, fieldOfView, clipNear, clipFar, this);
                     scsJoint.addCameraMount(mount);

                     SDFCamera sdfCamera = new SDFCamera(Integer.parseInt(camera.getImage().getWidth()), Integer.parseInt(camera.getImage().getHeight()));
                     this.cameras.put(sensor.getName(), sdfCamera);
                  }
               }
               else
               {
                  System.err.println("JAXB loader: No camera section defined for camera sensor " + sensor.getName() + ", ignoring sensor.");
               }
            }

         }
      }
   }

   private void addIMUMounts(Joint scsJoint, SDFLinkHolder child)
   {
      if (child.getSensors() != null)
      {
         for (SDFSensor sensor : child.getSensors())
         {
            if ("imu".equals(sensor.getType()))
            {
               // TODO: handle left and right sides of multicamera
               final IMU imu = sensor.getImu();

               if (imu != null)
               {
                  Transform3D pose = SDFConversionsHelper.poseToTransform(sensor.getPose());

                  IMUMount imuMount = new IMUMount(child.getName() + "_" + sensor.getName(), pose, this);

                  IMUNoise noise = imu.getNoise();
                  if (noise != null)
                  {
                     if ("gaussian".equals(noise.getType()))
                     {
                        NoiseParameters accelerationNoise = noise.getAccel();
                        NoiseParameters angularVelocityNoise = noise.getRate();


                        imuMount.setAccelerationNoiseParameters(Double.parseDouble(accelerationNoise.getMean()),
                                Double.parseDouble(accelerationNoise.getStddev()));
                        imuMount.setAccelerationBiasParameters(Double.parseDouble(accelerationNoise.getBias_mean()),
                                Double.parseDouble(accelerationNoise.getBias_stddev()));


                        imuMount.setAngularVelocityNoiseParameters(Double.parseDouble(angularVelocityNoise.getMean()),
                                Double.parseDouble(angularVelocityNoise.getStddev()));
                        imuMount.setAngularVelocityBiasParameters(Double.parseDouble(angularVelocityNoise.getBias_mean()),
                                Double.parseDouble(angularVelocityNoise.getBias_stddev()));

                     }
                     else
                     {
                        throw new RuntimeException("Unknown IMU noise model: " + noise.getType());
                     }
                  }

                  scsJoint.addIMUMount(imuMount);
               }
               else
               {
                  System.err.println("JAXB loader: No imu section defined for imu sensor " + sensor.getName() + ", ignoring sensor.");
               }
            }

         }
      }
   }


   private void addLidarMounts(Joint scsJoint, SDFLinkHolder child)
   {
      if (child.getSensors() != null)
      {
         for (SDFSensor sensor : child.getSensors())
         {
            if ("ray".equals(sensor.getType()) || "gpu_ray".equals(sensor.getType()))
            {
               if (DEBUG)
                  System.out.println("SDFRobot has a lidar!");
               if (DEBUG)
                  System.out.println("SDFRobot: the lidar is attached to link: " + scsJoint.getName());
               Ray sdfRay = sensor.getRay();
               if (sdfRay == null)
               {
                  System.err.println("SDFRobot: lidar not present in ray type sensor " + sensor.getName() + ". Ignoring this sensor.");
               }
               else
               {
                  Range sdfRange = sdfRay.getRange();
                  Scan sdfScan = sdfRay.getScan();
                  double sdfMaxRange = Double.parseDouble(sdfRange.getMax());
                  double sdfMinRange = Double.parseDouble(sdfRange.getMin());
                  HorizontalScan sdfHorizontalScan = sdfScan.getHorizontal();
                  double sdfMaxAngle = Double.parseDouble(sdfHorizontalScan.getMaxAngle());
                  double sdfMinAngle = Double.parseDouble(sdfHorizontalScan.getMinAngle());

//                double sdfAngularResolution = Double.parseDouble(sdfHorizontalScan.getSillyAndProbablyNotUsefulResolution());
                  int sdfSamples = Integer.parseInt(sdfHorizontalScan.getSamples());
                  double sdfRangeResolution = Double.parseDouble(sdfRay.getRange().getResolution());


                  boolean sdfAlwaysOn = true;

                  double sdfGaussianStdDev = 0.0;
                  double sdfGaussianMean = 0.0;
                  double sdfUpdateRate = Double.parseDouble(sensor.getUpdateRate());

                  Noise sdfNoise = sdfRay.getNoise();
                  if (sdfNoise != null)
                  {
                     if ("gaussian".equals(sdfNoise.getType()))
                     {
                        sdfGaussianStdDev = Double.parseDouble(sdfNoise.getStddev());
                        sdfGaussianMean = Double.parseDouble(sdfNoise.getMean());
                     }
                     else
                     {
                        System.err.println("Unknown noise model: " + sdfNoise.getType());
                     }
                  }

                  PolarLidarScanParameters polarDefinition = new PolarLidarScanParameters(false, sdfSamples, 1, (float) sdfMaxAngle, (float) sdfMinAngle, 0.0f,
                                                                0.0f, 0.0f, 0.0f, 0.0f, (float) sdfMinRange, (float) sdfMaxRange);
                  LIDARScanDefinition lidarScanDefinition = LIDARScanDefinition.PlanarSweep(sdfMaxAngle - sdfMinAngle, sdfSamples);
                  Transform3D transform3d = SDFConversionsHelper.poseToTransform(sensor.getPose());

                  SimulatedLIDARSensorNoiseParameters noiseParameters = new SimulatedLIDARSensorNoiseParameters();
                  noiseParameters.setGaussianNoiseStandardDeviation(sdfGaussianStdDev);
                  noiseParameters.setGaussianNoiseMean(sdfGaussianMean);

                  SimulatedLIDARSensorLimitationParameters limitationParameters = new SimulatedLIDARSensorLimitationParameters();
                  limitationParameters.setMaxRange(sdfMaxRange);
                  limitationParameters.setMinRange(sdfMinRange);
                  limitationParameters.setQuantization(sdfRangeResolution);

                  SimulatedLIDARSensorUpdateParameters updateParameters = new SimulatedLIDARSensorUpdateParameters();
                  updateParameters.setAlwaysOn(sdfAlwaysOn);
                  updateParameters.setUpdateRate(sdfUpdateRate);

//                updateParameters.setServerPort() We can't know the server port in SDF Uploaders, so this must be specified afterwords, but searching the robot tree and assigning numbers.

                  if (!USE_POLAR_LIDAR_MODEL)
                  {
                     RayTraceLIDARSensor scsLidar = new RayTraceLIDARSensor(transform3d, lidarScanDefinition);
                     scsLidar.setNoiseParameters(noiseParameters);
                     scsLidar.setSensorLimitationParameters(limitationParameters);
                     scsLidar.setLidarDaemonParameters(updateParameters);
                     scsJoint.addSensor(scsLidar);
                  }
                  else
                  {
                     FastPolarRayCastLIDAR scsLidar = new FastPolarRayCastLIDAR(transform3d, polarDefinition);
                     scsLidar.setNoiseParameters(noiseParameters);
                     scsLidar.setSensorLimitationParameters(limitationParameters);
                     scsLidar.setLidarDaemonParameters(updateParameters);
                     scsJoint.addSensor(scsLidar);
                  }

               }
            }

         }
      }
   }



   private Link createLink(SDFLinkHolder link, Transform3D rotationTransform, boolean useCollisionMeshes)
   {
      Link scsLink = new Link(link.getName());
      if (useCollisionMeshes)
      {
         SDFGraphics3DObject linkGraphics = new SDFGraphics3DObject(link.getCollisions(), resourceDirectories, rotationTransform);
         scsLink.setLinkGraphics(linkGraphics);
      }
      else if (link.getVisuals() != null)
      {
         SDFGraphics3DObject linkGraphics = new SDFGraphics3DObject(link.getVisuals(), resourceDirectories, rotationTransform);
         scsLink.setLinkGraphics(linkGraphics);
      }



      double mass = link.getMass();
      Matrix3d inertia = link.getInertia();
      Vector3d CoMOffset = link.getCoMOffset();

      if (link.getJoint() != null)
      {
         if (isFinger(link.getJoint()))
         {
            inertia.mul(100.0);
         }
      }

      RigidBodyInertia rigidBodyInertia = new RigidBodyInertia(ReferenceFrame.getWorldFrame(), inertia, mass);
      ReferenceFrame jointFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent("toroidFrame", ReferenceFrame.getWorldFrame(),
                                     rotationTransform);
      rigidBodyInertia.changeFrame(jointFrame);

      rotationTransform.transform(CoMOffset);


      scsLink.setComOffset(CoMOffset);
      scsLink.setMass(mass);
      scsLink.setMomentOfInertia(rigidBodyInertia.getMassMomentOfInertiaPartCopy());

      if (SHOW_COM_REFERENCE_FRAMES)
      {
         scsLink.addCoordinateSystemToCOM(0.1);
      }

      return scsLink;

   }

   public FrameVector getRootJointVelocity()
   {
      FrameVector ret = new FrameVector(ReferenceFrame.getWorldFrame());
      rootJoint.getVelocity(ret.getVector());

      return ret;
   }

   public FrameVector getPelvisAngularVelocityInPelvisFrame(ReferenceFrame pelvisFrame)
   {
      Vector3d angularVelocity = rootJoint.getAngularVelocityInBody();

      return new FrameVector(pelvisFrame, angularVelocity);
   }

   public Graphics3DObject getGraphicsObject(String name)
   {
      if (rootJoint.getName().equals(name))
      {
         return rootJoint.getLink().getLinkGraphics();
      }

      return oneDoFJoints.get(name).getLink().getLinkGraphics();
   }

   public SDFCamera getCamera(String name)
   {
      return cameras.get(name);
   }

   public List<GroundContactPoint> getFootGroundContactPoints(RobotSide robotSide)
   {
      return footGroundContactPoints.get(robotSide);
   }

   public Vector3d getPositionInWorld()
   {
      Vector3d position = new Vector3d();
      getPositionInWorld(position);

      return position;
   }

   public void getPositionInWorld(Vector3d vectorToPack)
   {
      rootJoint.getPosition(vectorToPack);
   }

   public void getVelocityInWorld(Vector3d vectorToPack)
   {
      rootJoint.getVelocity(vectorToPack);
   }

   public void getOrientationInWorld(Quat4d quaternionToPack)
   {
      rootJoint.getQuaternion(quaternionToPack);
   }

   public void getAngularVelocityInBody(Vector3d vectorToPack)
   {
      rootJoint.getAngularVelocityInBody(vectorToPack);
   }

   public Joint getChestJoint()
   {
      return getJoint("back_ubx");
   }

   private Joint getJoint(String jointName)
   {
      return getJointRecursively(this.getRootJoints(), jointName);
   }

   private Joint getJointRecursively(ArrayList<Joint> joints, String jointName)
   {
      for (Joint joint : joints)
      {
         String nextJointName = joint.getName();

//       System.out.println(nextJointName);
         if (nextJointName.equals(jointName))
         {
            return joint;
         }

         ArrayList<Joint> children = joint.getChildrenJoints();

         Joint foundJoint = getJointRecursively(children, jointName);
         if (foundJoint != null)
            return foundJoint;
      }

      return null;
   }

   public FloatingJoint getPelvisJoint()
   {
      return getRootJoint();
   }
}
