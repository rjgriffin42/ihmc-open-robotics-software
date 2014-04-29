package us.ihmc.SdfLoader.sdfWriter;

import com.yobotics.simulationconstructionset.*;
import us.ihmc.SdfLoader.xmlDescription.*;
import us.ihmc.SdfLoader.xmlDescription.SDFGeometry.Mesh;
import us.ihmc.SdfLoader.xmlDescription.SDFJoint.Axis;
import us.ihmc.SdfLoader.xmlDescription.SDFJoint.Axis.Dynamics;
import us.ihmc.SdfLoader.xmlDescription.SDFJoint.Axis.Limit;
import us.ihmc.SdfLoader.xmlDescription.SDFLink.Inertial;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.IMU;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.IMU.IMUNoise;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor.IMU.IMUNoise.NoiseParameters;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddModelFileInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DPrimitiveInstruction;
import us.ihmc.utilities.math.geometry.TransformTools;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class SDFRobotWriter
{
   private final JAXBContext context = JAXBContext.newInstance(SDFRoot.class);
   private final Marshaller marshaller = context.createMarshaller();

   private final Robot scsRobot;
   private final SDFRoot sdfRobot = new SDFRoot();
   private final List<SDFModel> models = new ArrayList<>();

   private final String sdfFilePath;
   private final String sdfModelName;
   private static final String sdfSimulatedIMUName = "SimulatedIMU";

   public SDFRobotWriter(Class<? extends Robot> robotClass) throws JAXBException, InstantiationException, IllegalAccessException
   {
      System.out.println("Creating SDFRobot for: " + robotClass.getSimpleName());
      scsRobot = robotClass.newInstance();
      scsRobot.update();
      String resourceDirectory = robotClass.getResource(".").getFile();

      sdfModelName = scsRobot.getName();

      sdfFilePath = resourceDirectory + sdfModelName + ".sdf";
      System.out.println("SDF file location: " + sdfFilePath);
      writeSDFRobotDescriptionFile();
   }

   public static String getSimulatedIMUName()
   {
      return sdfSimulatedIMUName;
   }

   public String getModelName()
   {
      return sdfModelName;
   }

   public String getSDFFilePath()
   {
      return sdfFilePath;
   }

   public void writeSDFRobotDescriptionFile() throws PropertyException, JAXBException
   {
      File output = new File(sdfFilePath);
      models.add(createSDFModel());
      sdfRobot.setModels(models);

      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(sdfRobot, output);
   }

   private SDFModel createSDFModel()
   {
      SDFModel model = new SDFModel();

      List<SDFJoint> sdfJoints = new ArrayList<>();
      List<SDFLink> sdfLink = new ArrayList<>();

      ArrayList<OneDegreeOfFreedomJoint> scsJoints = new ArrayList<>();
      scsRobot.getAllOneDegreeOfFreedomJoints(scsJoints);

      if (scsRobot.getRootJoints().size() > 1)
         throw new RuntimeException("Cannot handle multiple root joints for now.");

      sdfLink.add(createSDFLink(scsRobot.getRootJoints().get(0).getLink(), true));

      for (OneDegreeOfFreedomJoint scsJoint : scsJoints)
      {
         sdfJoints.add(createSDFJoint(scsJoint));
         sdfLink.add(createSDFLink(scsJoint.getLink(), false));
      }

      model.setName(sdfModelName);
      model.setJoints(sdfJoints);
      model.setLinks(sdfLink);

      return model;
   }

   private SDFJoint createSDFJoint(OneDegreeOfFreedomJoint scsJoint)
   {
      SDFJoint sdfJoint = new SDFJoint();

      sdfJoint.setAxis(createSDFJointAxis(scsJoint));
      sdfJoint.setChild(scsJoint.getLink().getName());
      sdfJoint.setName(scsJoint.getName());
      sdfJoint.setParent(scsJoint.getParentJoint().getLink().getName());

      Transform3D scsJointOffset = new Transform3D();
      scsJoint.getTransformToWorld(scsJointOffset);
      sdfJoint.setPose(getPoseFromTransform3D(scsJointOffset));

      String type;
      if (scsJoint instanceof PinJoint)
      {
         type = "revolute";
      }
      else
      {
         throw new RuntimeException("Implement me!");
      }
      sdfJoint.setType(type);

      return sdfJoint;
   }

   private Axis createSDFJointAxis(OneDegreeOfFreedomJoint scsJoint)
   {
      Axis sdfJointAxis = new Axis();

      Vector3d scsJointAxis = new Vector3d();
      scsJoint.physics.getJointAxis(scsJointAxis);

      String xyz = String.valueOf(scsJointAxis.x) + " " + String.valueOf(scsJointAxis.y) + " " + String.valueOf(scsJointAxis.z);
      sdfJointAxis.setXyz(xyz);

      sdfJointAxis.setDynamics(createJointDynamics(scsJoint));
      sdfJointAxis.setLimit(createJointLimit(scsJoint));

      return sdfJointAxis;
   }

   private Dynamics createJointDynamics(OneDegreeOfFreedomJoint scsJoint)
   {
      Dynamics sdfJointDynamics = new Dynamics();

      String damping = String.valueOf(scsJoint.getDamping());
      sdfJointDynamics.setDamping(damping);

      String friction = String.valueOf(scsJoint.getJointStiction());
      sdfJointDynamics.setFriction(friction);

      return sdfJointDynamics;
   }

   private final Limit createJointLimit(OneDegreeOfFreedomJoint scsJoint)
   {
      Limit sdfJointLimit = new Limit();

      String effort = String.valueOf(scsJoint.getTorqueLimit());
      sdfJointLimit.setEffort(effort);

      String velocity = String.valueOf(scsJoint.getVelocityLimit());
      sdfJointLimit.setVelocity(velocity);

      String lower = String.valueOf(scsJoint.getJointLowerLimit());
      sdfJointLimit.setLower(lower);

      String upper = String.valueOf(scsJoint.getJointUpperLimit());
      sdfJointLimit.setUpper(upper);

      return sdfJointLimit;
   }

   private SDFLink createSDFLink(Link scsLink, boolean addSimulatedIMU)
   {
      SDFLink sdfLink = new SDFLink();

      sdfLink.setInertial(createSDFInertial(scsLink));
      sdfLink.setName(scsLink.getName());
      String pose = "0 0 0 0 0 0";
      sdfLink.setPose(pose);
      sdfLink.setVisuals(createSDFVisual(scsLink));

      if (addSimulatedIMU)
      {
         List<SDFSensor> sdfSensors = new ArrayList<>();
         sdfSensors.add(createSDFSimulatedIMU(scsLink));
         sdfLink.setSensors(sdfSensors);
      }

      return sdfLink;
   }

   private SDFSensor createSDFSimulatedIMU(Link scsLink)
   {
      SDFSensor sdfSensor = new SDFSensor();
      sdfSensor.setName(sdfSimulatedIMUName);
      sdfSensor.setType("imu");
      IMU sdfSimulatedIMU = new IMU();
      IMUNoise imuNoise = new IMUNoise();
      NoiseParameters noiseParameters = new NoiseParameters();
      noiseParameters.setBias_mean("0");
      noiseParameters.setBias_stddev("0");
      noiseParameters.setMean("0");
      noiseParameters.setStddev("0");
      imuNoise.setAccel(noiseParameters);
      imuNoise.setRate(noiseParameters);
      imuNoise.setType("gaussian");
      sdfSimulatedIMU.setNoise(imuNoise);
      sdfSensor.setImu(sdfSimulatedIMU);
      sdfSensor.setPose("0 0 0 0 0 0");
      return sdfSensor;
   }

   private List<SDFVisual> createSDFVisual(Link scsLink)
   {
      List<SDFVisual> sdfVisuals = new ArrayList<>();

      ArrayList<Graphics3DPrimitiveInstruction> graphics3dInstructions = scsLink.getLinkGraphics().getGraphics3DInstructions();

      Joint parentJoint = scsLink.getParentJoint();

      for (Graphics3DPrimitiveInstruction instruction : graphics3dInstructions)
      {
         sdfVisuals.add(createSDFVisual(parentJoint, instruction));
      }

      return sdfVisuals;
   }

   private SDFVisual createSDFVisual(Joint parentJoint, Graphics3DPrimitiveInstruction instruction)
   {
      SDFGeometry sdfGeometry = new SDFGeometry();

      if (instruction instanceof Graphics3DAddModelFileInstruction)
      {
         Graphics3DAddModelFileInstruction modelFileInstruction = (Graphics3DAddModelFileInstruction) instruction;

         if (modelFileInstruction.getResourceDirectories().size() > 0)
            System.err.println("Doesn't handle resource directories: " + instruction.getClass().getSimpleName());

         Mesh mesh = new Mesh();
         String uri = modelFileInstruction.getFileName();
         mesh.setUri(uri);
         sdfGeometry.setMesh(mesh);
      }
      else
      {
         System.err.println("Doesn't handle: " + instruction.getClass().getSimpleName());
      }

      SDFVisual sdfVisual = new SDFVisual();
      sdfVisual.setGeometry(sdfGeometry);

      return sdfVisual;
   }

   private Inertial createSDFInertial(Link scsLink)
   {
      Inertial sdfInertial = new Inertial();

      sdfInertial.setInertia(createSDFInertia(scsLink));
      sdfInertial.setMass(String.valueOf(scsLink.getMass()));

      Transform3D comOffsetInWorld = new Transform3D();
      scsLink.getParentJoint().getTransformToWorld(comOffsetInWorld);
      Vector3d com = new Vector3d();
      scsLink.physics.getComOffset(com);

      Transform3D comOffset = TransformTools.createTranslationTransform(com);
      comOffsetInWorld.mul(comOffset);

      sdfInertial.setPose(getPoseFromTransform3D(comOffsetInWorld));

      return sdfInertial;
   }

   private SDFInertia createSDFInertia(Link scsLink)
   {
      SDFInertia sdfInertia = new SDFInertia();

      Matrix3d scsInertia = new Matrix3d();
      scsLink.physics.getMomentOfInertia(scsInertia);

      sdfInertia.setIxx(String.valueOf(scsInertia.m00));
      sdfInertia.setIyy(String.valueOf(scsInertia.m11));
      sdfInertia.setIzz(String.valueOf(scsInertia.m22));
      sdfInertia.setIxy(String.valueOf(scsInertia.m01));
      sdfInertia.setIxz(String.valueOf(scsInertia.m02));
      sdfInertia.setIyz(String.valueOf(scsInertia.m12));

      return sdfInertia;
   }

   private String getPoseFromTransform3D(Transform3D scsJointOffset)
   {
      Vector3d translation = new Vector3d();
      scsJointOffset.get(translation);
      Matrix3d rotation = new Matrix3d();
      scsJointOffset.get(rotation);
      Vector3d eulerAngles = new Vector3d();
      eulerAngles.y = Math.asin(rotation.m02);
      double cosY = Math.cos(eulerAngles.y);
      if (Math.abs(cosY) > 1e-5)
      {
         eulerAngles.x = -Math.atan2(rotation.m12 / cosY, rotation.m22 / cosY);
         eulerAngles.z = -Math.atan2(rotation.m01 / cosY, rotation.m00 / cosY);
      }
      else
      {
         eulerAngles.x = Math.atan2(rotation.m21, rotation.m11);
         eulerAngles.z = 0.0;
      }
      String pose = String.valueOf(translation.x) + " " + String.valueOf(translation.y) + " " + String.valueOf(translation.z) + " "
            + String.valueOf(eulerAngles.x) + " " + String.valueOf(eulerAngles.y) + " " + String.valueOf(eulerAngles.z);
      return pose;
   }
}
