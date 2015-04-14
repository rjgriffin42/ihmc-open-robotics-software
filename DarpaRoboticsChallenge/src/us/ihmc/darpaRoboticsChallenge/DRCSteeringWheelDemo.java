package us.ihmc.darpaRoboticsChallenge;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.JointPositionControllerFactory;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.environment.CommonAvatarEnvironmentInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCSteeringWheelEnvironment;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.yoUtilities.controllers.GainCalculator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;

public class DRCSteeringWheelDemo
{
   private final DRCSimulationFactory drcSimulationFactory;

   public DRCSteeringWheelDemo(DRCRobotModel model, DRCNetworkModuleParameters networkParameters, double desiredPelvisHeight)
   {
      Point3d steeringWheelCenterInPelvisFrame = new Point3d(0.254, 0.86, 0.0);
      CommonAvatarEnvironmentInterface environment = new DRCSteeringWheelEnvironment(steeringWheelCenterInPelvisFrame.x, steeringWheelCenterInPelvisFrame.y,
            0.85, 0.0, 57.0);

      DRCSimulationStarter simStarter = new DRCSimulationStarter(model, environment);
//      simStarter.registerHighLevelController(new JointPDHighLevelHumanoidControllerFactory(10.0, 1.0, true));
//      simStarter.registerHighLevelController(new InverseDynamicsJointControllerFactory(true));
      simStarter.registerHighLevelController(new JointPositionControllerFactory(true));

      simStarter.setRunMultiThreaded(true);
      simStarter.setStartingLocationOffset(new Vector3d(0.0, 0.0, desiredPelvisHeight), 0.0);
      simStarter.setInitializeEstimatorToActual(true);

      DRCSimulationTools.startSimulationWithGraphicSelector(simStarter);

      drcSimulationFactory = simStarter.getDRCSimulationFactory();

      SDFRobot robot = drcSimulationFactory.getRobot();
      LockPelvisController controller = new LockPelvisController(robot, drcSimulationFactory.getSimulationConstructionSet(), model.createFullRobotModel(),
            desiredPelvisHeight);
      robot.setController(controller);
      controller.initialize();
   }

   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return drcSimulationFactory.getSimulationConstructionSet();
   }

   private class LockPelvisController implements RobotController
   {
      private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

      private final ArrayList<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      private final ArrayList<Vector3d> efp_offsetFromRootJoint = new ArrayList<>();
      private final double dx = 0.5, dy = 0.5, dz = 0.4 * 0.0;

      private final ArrayList<Vector3d> initialPositions = new ArrayList<>();

      private final DoubleYoVariable holdPelvisKp = new DoubleYoVariable("holdPelvisKp", registry);
      private final DoubleYoVariable holdPelvisKv = new DoubleYoVariable("holdPelvisKv", registry);
      private final DoubleYoVariable desiredHeight = new DoubleYoVariable("desiredHeight", registry);
      private final double robotMass, robotWeight;

      private final SDFRobot robot;

      private final YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();
      private final ArrayList<YoGraphicPosition> efp_positionViz = new ArrayList<>();

      public LockPelvisController(SDFRobot robot, SimulationConstructionSet scs, SDFFullRobotModel sdfFullRobotModel, double desiredHeight)
      {
         this.robot = robot;
         robotMass = robot.computeCenterOfMass(new Point3d());
         robotWeight = robotMass * Math.abs(robot.getGravityZ());
         this.desiredHeight.set(desiredHeight);

         Joint jointToAddExternalForcePoints;
         try
         {
            //            String lastSpineJointName = sdfFullRobotModel.getChest().getParentJoint().getName();
            jointToAddExternalForcePoints = robot.getJoint(sdfFullRobotModel.getPelvis().getParentJoint().getName());
         }
         catch (NullPointerException e)
         {
            System.err.println("No chest or spine found. Stack trace:");
            e.printStackTrace();

            jointToAddExternalForcePoints = robot.getPelvisJoint();
         }

         holdPelvisKp.set(5000.0);
         holdPelvisKv.set(GainCalculator.computeDampingForSecondOrderSystem(robotMass, holdPelvisKp.getDoubleValue(), 0.6));

         efp_offsetFromRootJoint.add(new Vector3d(dx, dy, dz));
         efp_offsetFromRootJoint.add(new Vector3d(dx, -dy, dz));
         efp_offsetFromRootJoint.add(new Vector3d(-dx, dy, dz));
         efp_offsetFromRootJoint.add(new Vector3d(-dx, -dy, dz));

         for (int i = 0; i < efp_offsetFromRootJoint.size(); i++)
         {
            initialPositions.add(new Vector3d());

            String linkName = jointToAddExternalForcePoints.getLink().getName();
            ExternalForcePoint efp = new ExternalForcePoint("efp_" + linkName + "_" + String.valueOf(i) + "_", efp_offsetFromRootJoint.get(i),
                  robot.getRobotsYoVariableRegistry());
            externalForcePoints.add(efp);
            jointToAddExternalForcePoints.addExternalForcePoint(efp);

            efp_positionViz.add(new YoGraphicPosition(efp.getName(), efp.getYoPosition(), 0.05, YoAppearance.Red()));
         }

         yoGraphicsListRegistry.registerYoGraphics("EFP", efp_positionViz);
         scs.addYoGraphicsListRegistry(yoGraphicsListRegistry);
      }

      @Override
      public void initialize()
      {
         robot.update();
         for (int i = 0; i < efp_offsetFromRootJoint.size(); i++)
         {
            externalForcePoints.get(i).getYoPosition().get(initialPositions.get(i));
            desiredHeight.add(initialPositions.get(i).z / initialPositions.size());
            efp_positionViz.get(i).update();
         }

         for (int i = 0; i < efp_offsetFromRootJoint.size(); i++)
            initialPositions.get(i).setZ(desiredHeight.getDoubleValue());

         doControl();
      }

      private final Vector3d proportionalTerm = new Vector3d();
      private final Vector3d derivativeTerm = new Vector3d();
      private final Vector3d pdControlOutput = new Vector3d();

      @Override
      public void doControl()
      {
         for (int i = 0; i < efp_offsetFromRootJoint.size(); i++)
         {
            initialPositions.get(i).setZ(desiredHeight.getDoubleValue());

            ExternalForcePoint efp = externalForcePoints.get(i);
            efp.getYoPosition().get(proportionalTerm);
            proportionalTerm.sub(initialPositions.get(i));
            proportionalTerm.scale(-holdPelvisKp.getDoubleValue());
            proportionalTerm.setZ(Math.max(proportionalTerm.getZ(), 0.0));

            efp.getYoVelocity().get(derivativeTerm);
            derivativeTerm.scale(-holdPelvisKv.getDoubleValue());

            pdControlOutput.add(proportionalTerm, derivativeTerm);

            efp.setForce(pdControlOutput);
            efp.getYoForce().getYoZ().add(robotWeight / efp_offsetFromRootJoint.size());

            efp_positionViz.get(i).update();
         }
      }

      @Override
      public YoVariableRegistry getYoVariableRegistry()
      {
         return registry;
      }

      @Override
      public String getName()
      {
         return registry.getName();
      }

      @Override
      public String getDescription()
      {
         return getName();
      }
   }
}
