package us.ihmc.gdx.simulation.scs2;

import imgui.ImGui;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.initialSetup.RobotInitialSetup;
import us.ihmc.avatar.scs2.SCS2AvatarSimulation;
import us.ihmc.avatar.scs2.SCS2AvatarSimulationFactory;
import us.ihmc.commonWalkingControlModules.desiredFootStep.footstepGenerator.HeadingAndVelocityEvaluationScriptParameters;
import us.ihmc.communication.CommunicationMode;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.gdx.imgui.ImGuiPanel;
import us.ihmc.gdx.simulation.environment.object.objects.FlatGroundDefinition;
import us.ihmc.gdx.ui.GDXImGuiBasedUI;
import us.ihmc.ros2.RealtimeROS2Node;
import us.ihmc.scs2.definition.robot.RobotDefinition;
import us.ihmc.scs2.simulation.SimulationSession;
import us.ihmc.scs2.simulation.robot.Robot;
import us.ihmc.simulationConstructionSetTools.util.HumanoidFloatingRootJointRobot;

import java.util.ArrayList;

public class GDXSCS2EnvironmentManager
{
   private GDXSCS2SimulationSession scs2SimulationSession;
   private final ImGuiPanel managerPanel = new ImGuiPanel("SCS 2 Simulation Session", this::renderImGuiWidgets);
   private SCS2AvatarSimulation avatarSimulation;
   private RealtimeROS2Node realtimeROS2Node;
   private RobotInitialSetup<HumanoidFloatingRootJointRobot> robotInitialSetup;
   private HeadingAndVelocityEvaluationScriptParameters walkingScriptParameters;
   private boolean useVelocityAndHeadingScript;
   private GDXImGuiBasedUI baseUI;
   private int recordFrequency;
   private DRCRobotModel robotModel;
   private CommunicationMode ros2CommunicationMode;
   private final ArrayList<Robot> secondaryRobots = new ArrayList<>();

   public void create(GDXImGuiBasedUI baseUI, DRCRobotModel robotModel, CommunicationMode ros2CommunicationMode)
   {
      this.baseUI = baseUI;
      this.robotModel = robotModel;
      this.ros2CommunicationMode = ros2CommunicationMode;

      //      recordFrequency = (int) Math.max(1.0, Math.round(robotModel.getControllerDT() / robotModel.getSimulateDT()));
      recordFrequency = 1;

      useVelocityAndHeadingScript = true;
      walkingScriptParameters = new HeadingAndVelocityEvaluationScriptParameters();

      double initialYaw = 0.3;
      robotInitialSetup = robotModel.getDefaultRobotInitialSetup(0.0, initialYaw);

      rebuildSimulation();
   }

   private void renderImGuiWidgets()
   {
      if (ImGui.button("Rebuild simulation"))
      {
         rebuildSimulation();
      }
      scs2SimulationSession.renderImGuiWidgets();
   }

   private void rebuildSimulation()
   {
      if (scs2SimulationSession != null)
      {
         baseUI.getImGuiPanelManager().queueRemovePanel(managerPanel);
         destroy(baseUI);
      }

      realtimeROS2Node = ROS2Tools.createRealtimeROS2Node(ros2CommunicationMode.getPubSubImplementation(),
                                                          "flat_ground_walking_track_simulation");

      SCS2AvatarSimulationFactory avatarSimulationFactory = new SCS2AvatarSimulationFactory();
      avatarSimulationFactory.setRobotModel(robotModel);
      avatarSimulationFactory.setRealtimeROS2Node(realtimeROS2Node);
      avatarSimulationFactory.setDefaultHighLevelHumanoidControllerFactory(useVelocityAndHeadingScript, walkingScriptParameters);
      avatarSimulationFactory.setTerrainObjectDefinition(new FlatGroundDefinition());
      for (Robot secondaryRobot : secondaryRobots)
      {
         avatarSimulationFactory.addSecondaryRobot(secondaryRobot);
      }
      avatarSimulationFactory.setRobotInitialSetup(robotInitialSetup);
      avatarSimulationFactory.setSimulationDataRecordTickPeriod(recordFrequency);
      avatarSimulationFactory.setCreateYoVariableServer(true);
      avatarSimulationFactory.setUseBulletPhysicsEngine(true);
      avatarSimulationFactory.setUseDescriptionCollisions(true);
      avatarSimulationFactory.setShowGUI(false);

      avatarSimulation = avatarSimulationFactory.createAvatarSimulation();
      avatarSimulation.setSystemExitOnDestroy(false);

      scs2SimulationSession = new GDXSCS2SimulationSession(avatarSimulation.getSimulationSession());

      avatarSimulation.beforeSessionThreadStart();

      scs2SimulationSession.setDT(robotModel.getEstimatorDT());
      scs2SimulationSession.create(baseUI, managerPanel);

      avatarSimulation.afterSessionThreadStart();

      scs2SimulationSession.getControlPanel().getIsShowing().set(true);
      baseUI.getImGuiPanelManager().queueAddPanel(managerPanel);
   }

   public void update()
   {
      scs2SimulationSession.update();
   }

   public void destroy(GDXImGuiBasedUI baseUI)
   {
      avatarSimulation.destroy();
      scs2SimulationSession.destroy(baseUI);
   }

   public void addSecondaryRobot(RobotDefinition robotDefinition)
   {
      Robot robot = new Robot(robotDefinition, SimulationSession.DEFAULT_INERTIAL_FRAME);
      secondaryRobots.add(robot);
   }

   public GDXSCS2SimulationSession getSCS2SimulationSession()
   {
      return scs2SimulationSession;
   }
}
