package us.ihmc.darpaRoboticsChallenge;

import java.util.ArrayList;

import us.ihmc.commonAvatarInterfaces.CommonAvatarEnvironmentInterface;
import us.ihmc.commonWalkingControlModules.terrain.CommonTerrain;
import us.ihmc.commonWalkingControlModules.terrain.TerrainType;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.projectM.R2Sim02.initialSetup.ScsInitialSetup;

import com.yobotics.simulationconstructionset.DynamicIntegrationMethod;
import com.yobotics.simulationconstructionset.GroundProfile;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.LinearGroundContactModel;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.ground.steppingStones.SteppingStones;

public class DRCSCSInitialSetup implements ScsInitialSetup
{
   private static final boolean SHOW_WORLD_COORDINATE_FRAME = false;
   private double simulateDT = 0.0001;    // 0.00005; //
   private int recordFrequency = 1;    // 10;


   private int simulationDataBufferSize = 16000;
   private double gravity = -9.81;
   private final CommonTerrain commonTerrain;

   private DynamicIntegrationMethod dynamicIntegrationMethod = DynamicIntegrationMethod.RUNGE_KUTTA_FOURTH_ORDER;
   
   public DRCSCSInitialSetup(TerrainType terrainType)
   {
      System.out.println("terrainType = " + terrainType);
      commonTerrain = new CommonTerrain(terrainType);
   }

   public DRCSCSInitialSetup(GroundProfile groundProfile)
   {
      commonTerrain = new CommonTerrain(groundProfile);
   }

   public DRCSCSInitialSetup(CommonAvatarEnvironmentInterface commonAvatarEnvironmentInterface)
   {
      this(commonAvatarEnvironmentInterface.getTerrainObject());
   }

   public void initializeRobot(Robot robot, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      robot.setGravity(gravity);

      double groundKz = 165.0;
      double groundBz = 55.0;
      double groundKxy = 27500.0;
      double groundBxy = 1100.0;

//      double alphaStick = 1.0;
//      double alphaSlip = 0.5;
//      LinearStickSlipGroundContactModel groundContactModel = new LinearStickSlipGroundContactModel(robot, groundKxy, groundBxy, groundKz, groundBz, alphaSlip,
//                                                                alphaStick, robot.getRobotsYoVariableRegistry());

      LinearGroundContactModel groundContactModel = new LinearGroundContactModel(robot, groundKxy, groundBxy, groundKz, groundBz,
            robot.getRobotsYoVariableRegistry());


      if ((commonTerrain.getSteppingStones() != null) && (dynamicGraphicObjectsListRegistry != null))
         commonTerrain.registerSteppingStonesArtifact(dynamicGraphicObjectsListRegistry);

      groundContactModel.setGroundProfile(commonTerrain.getGroundProfile());

      boolean drawGround = false;
      if (drawGround)
      {
         boolean drawGroundBelow = false;
         ArrayList<Graphics3DObject> branchGroup = commonTerrain.createLinkGraphics(drawGroundBelow);
         robot.addStaticLinkGraphics(branchGroup);
      }

      // TODO: change this to scs.setGroundContactModel(groundContactModel);
      robot.setGroundContactModel(groundContactModel);
   }

   public DynamicIntegrationMethod getDynamicIntegrationMethod()
   {
      return dynamicIntegrationMethod;
   }
   
   public void setDynamicIntegrationMethod(DynamicIntegrationMethod dynamicIntegrationMethod)
   {
      this.dynamicIntegrationMethod = dynamicIntegrationMethod;
   }
   
   public double getDT()
   {
      return simulateDT;
   }

   public int getSimulationDataBufferSize()
   {
      return simulationDataBufferSize;
   }

   public void initializeSimulation(SimulationConstructionSet scs)
   {
      scs.setDT(simulateDT, recordFrequency);

      if (SHOW_WORLD_COORDINATE_FRAME)
      {
         Graphics3DObject linkGraphics = new Graphics3DObject();
         linkGraphics.addCoordinateSystem(0.3);
         scs.addStaticLinkGraphics(linkGraphics);
      }



      /*
       * This makes sure that the initial values of all YoVariables that are added to the scs (i.e. at index 0 of the data buffer)
       * are properly stored in the data buffer
       */
      scs.getDataBuffer().copyValuesThrough();

   }

   public void setSimulationDataBufferSize(int simulationDataBufferSize)
   {
      this.simulationDataBufferSize = simulationDataBufferSize;
   }

   public void setRecordFrequency(int recordFrequency)
   {
      this.recordFrequency = recordFrequency;
   }

   public int getRecordFrequency()
   {
      return recordFrequency;
   }

   public double getGravity()
   {
      return gravity;
   }

   public GroundProfile getGroundProfile()
   {
      return commonTerrain.getGroundProfile();
   }

   public SteppingStones getSteppingStones()
   {
      return commonTerrain.getSteppingStones();
   }
}
