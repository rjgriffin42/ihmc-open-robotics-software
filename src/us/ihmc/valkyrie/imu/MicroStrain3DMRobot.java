package us.ihmc.valkyrie.imu;

import java.io.IOException;

import javax.vecmath.Matrix3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.valkyrie.paramaters.ValkyrieSensorInformation;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.FloatingJoint;
import com.yobotics.simulationconstructionset.Link;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.robotController.RobotController;

@SuppressWarnings("serial")
public class MicroStrain3DMRobot extends Robot
{
   private static double MS3DM_MASS = 0.0018;
   private static double MS3DM_LENGTH = 0.044;
   private static double MS3DM_WIDTH = 0.025;
   private static double MS3DM_HEIGHT = 0.011;

   protected FloatingJoint ms3DM;
   protected final Matrix3d rotation = new Matrix3d();

   private YoVariableRegistry registry = new YoVariableRegistry("MicroStrain3DMData");

   private DoubleYoVariable yaw = new DoubleYoVariable("yaw", registry);
   private DoubleYoVariable pitch = new DoubleYoVariable("pitch", registry);
   private DoubleYoVariable roll = new DoubleYoVariable("roll", registry);

   private DoubleYoVariable wz = new DoubleYoVariable("wz", registry);
   private DoubleYoVariable wy = new DoubleYoVariable("wy", registry);
   private DoubleYoVariable wx = new DoubleYoVariable("wx", registry);

   private DoubleYoVariable xdd = new DoubleYoVariable("xdd", registry);
   private DoubleYoVariable ydd = new DoubleYoVariable("ydd", registry);
   private DoubleYoVariable zdd = new DoubleYoVariable("zdd", registry);
   
   private final Matrix3d temporaryMatrix = new Matrix3d();
   
   public MicroStrain3DMRobot()
   {
      super("MicroStrain3DMRobot");

      ms3DM = new FloatingJoint("ms3DM", new Vector3d(0.0, 0.0, 0.0), this);
      ms3DM.setLink(MS3DMLink());

      this.addRootJoint(ms3DM);

      this.setGravity(0.0, 0.0, 0.0);

      addYoVariableRegistry(registry);

   }

   public double getYaw()
   {
      return yaw.getDoubleValue();
   }

   public double getPitch()
   {
      return pitch.getDoubleValue();
   }

   public double getRoll()
   {
      return roll.getDoubleValue();
   }

   private Link MS3DMLink()
   {
      Link p = new Link("ms3DMLink");
      p.setMass(MS3DM_MASS);
      p.setComOffset(0.0, 0.0, 0.0);
      p.setMomentOfInertia(0.1, 0.1, 0.1);

      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCoordinateSystem(0.1);
      linkGraphics.addCube(MS3DM_LENGTH, MS3DM_WIDTH, MS3DM_HEIGHT, YoAppearance.Purple());

      p.setLinkGraphics(linkGraphics);
      

      return p;

   }
   
   public void set(Vector3d accel, Vector3d angRate, Quat4d orientation)
   {
      temporaryMatrix.set(orientation);
      rotation.mul(MicroStrainData.MICROSTRAIN_TO_ZUP_WORLD, temporaryMatrix);
      
      ms3DM.setRotation(rotation);
      yaw.set(RotationFunctions.getYaw(rotation));
      pitch.set(RotationFunctions.getPitch(rotation));
      roll.set(RotationFunctions.getRoll(rotation));
      
      xdd.set(accel.getX() * MicroStrainData.MICROSTRAIN_GRAVITY);
      ydd.set(accel.getY() * MicroStrainData.MICROSTRAIN_GRAVITY);
      zdd.set(accel.getZ() * MicroStrainData.MICROSTRAIN_GRAVITY);
      
      wx.set(angRate.getX());
      wy.set(angRate.getY());
      wz.set(angRate.getZ());
      
   }
   
   
   public static void main(String[] args) throws IOException
   {
      ValkyrieSensorInformation sensorInformation = new ValkyrieSensorInformation();
      final MicrostrainUDPPacketListener listener = MicrostrainUDPPacketListener.createNonRealtimeListener(sensorInformation.getImuUSBSerialIds().get("v1Pelvis_LeftIMU"));
      
      final MicroStrain3DMRobot robot = new MicroStrain3DMRobot();
      RobotController controller = new RobotController()
      {
         YoVariableRegistry registry = new YoVariableRegistry("controller");
         @Override
         public void initialize()
         {
         }
         
         @Override
         public YoVariableRegistry getYoVariableRegistry()
         {
            return registry;
         }
         
         @Override
         public String getName()
         {
            return "updater";
         }
         
         @Override
         public String getDescription()
         {
            return getName();
         }
         
         @Override
         public void doControl()
         {
            MicroStrainData data = listener.getLatestData();
            robot.set(data.getAcceleration(), data.getGyro(), data.getQuaternion());
            ThreadTools.sleep(1);
         }
      };
      
      robot.setController(controller);
      
      SimulationConstructionSet scs = new SimulationConstructionSet(robot);
      scs.setGroundVisible(false);
      scs.startOnAThread();
      
   }
}
