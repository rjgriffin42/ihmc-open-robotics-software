package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import us.ihmc.utilities.math.geometry.Transform3d;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.RectangularContactableBody;
import us.ihmc.commonWalkingControlModules.desiredFootStep.footstepGenerator.SimplePathParameters;
import us.ihmc.commonWalkingControlModules.desiredFootStep.footstepGenerator.TurningThenStraightFootstepGenerator;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.HeadingAndVelocityEvaluationScript;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.ManualDesiredVelocityControlModule;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.SimpleDesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.referenceFrames.VisualizeFramesController;
import us.ihmc.communication.packets.walking.FootstepData;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.ZUpFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.BagOfBalls;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.yoUtilities.graphics.YoGraphicVector;
import us.ihmc.yoUtilities.graphics.plotting.ArtifactList;
import us.ihmc.yoUtilities.graphics.plotting.YoArtifactPolygon;
import us.ihmc.yoUtilities.math.frames.YoFrameConvexPolygon2d;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.plotting.SimulationOverheadPlotter;
import com.yobotics.simulationconstructionset.robotController.RobotController;

public class DesiredFootstepVisualizer
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry("DesiredFootstepVisualizer");

   private FootstepProvider footstepProvider;
   private final SideDependentList<YoFrameConvexPolygon2d> feetPolygonsInWorld = new SideDependentList<YoFrameConvexPolygon2d>();
   private final DoubleYoVariable minZ = new DoubleYoVariable("minZ", registry);
   private final SideDependentList<SixDoFJoint> sixDoFJoints;
   private final SideDependentList<ReferenceFrame> soleFrames;
   private final SideDependentList<ReferenceFrame> ankleZUpFrames;
   private final SideDependentList<ReferenceFrame> footFrames;
   private final ArrayList<ReferenceFrame> framesToVisualize;

   private final SideDependentList<ContactablePlaneBody> bipedFeet;

   private final SideDependentList<BagOfBalls> bagsOfBalls;

   public DesiredFootstepVisualizer(YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      BagOfBalls leftBagOfBalls = new BagOfBalls(1000, 0.05, "leftBalls", YoAppearance.Red(), parentRegistry, yoGraphicsListRegistry);
      BagOfBalls rightBagOfBalls = new BagOfBalls(1000, 0.05, "rightBalls", YoAppearance.Blue(), parentRegistry, yoGraphicsListRegistry);
      bagsOfBalls = new SideDependentList<BagOfBalls>(leftBagOfBalls, rightBagOfBalls);

      framesToVisualize = new ArrayList<ReferenceFrame>();

      RigidBody elevator = new RigidBody("elevator", worldFrame);
      this.sixDoFJoints = new SideDependentList<SixDoFJoint>();
      this.soleFrames = new SideDependentList<ReferenceFrame>();
      this.footFrames = new SideDependentList<ReferenceFrame>();
      this.ankleZUpFrames = new SideDependentList<ReferenceFrame>();
      this.bipedFeet = new SideDependentList<ContactablePlaneBody>();

      double footWidth = 0.15;
      double footForward = 0.25;
      double footBackward = 0.05;
      double footHeight = 0.05;
      for (RobotSide robotSide : RobotSide.values)
      {
         String robotSideName = robotSide.getCamelCaseNameForStartOfExpression();

         SixDoFJoint sixDoFJoint = new SixDoFJoint(robotSideName + "Joint", elevator, elevator.getBodyFixedFrame());
         sixDoFJoints.put(robotSide, sixDoFJoint);
         ReferenceFrame footFrame = sixDoFJoint.getFrameAfterJoint();
         RigidBody footBody = ScrewTools.addRigidBody(robotSideName + "Foot", sixDoFJoint, new Matrix3d(), 0.0, new Vector3d());

         ReferenceFrame soleFrame = ReferenceFrame.constructBodyFrameWithUnchangingTranslationFromParent(robotSideName + "Sole", footFrame, new Vector3d(0.0,
               0.0, -footHeight));
         soleFrames.put(robotSide, soleFrame);

         footFrames.put(robotSide, footFrame);

         ZUpFrame ankleZUpFrame = new ZUpFrame(worldFrame, footFrame, robotSideName + "AnkleZUp");
         ankleZUpFrames.put(robotSide, ankleZUpFrame);

         ContactablePlaneBody foot = new RectangularContactableBody(footBody, soleFrame, footForward, -footBackward, footWidth / 2.0, -footWidth / 2.0);
         bipedFeet.put(robotSide, foot);
         framesToVisualize.add(footFrame);
      }

      elevator.updateFramesRecursively();

      for (RobotSide robotSide : RobotSide.values)
      {
         footFrames.get(robotSide).update();
         ankleZUpFrames.get(robotSide).update();
      }

      YoGraphicsList yoGraphicsList = new YoGraphicsList("FeetPolygons");
      ArtifactList artifactList = new ArtifactList("FeetPolygons");
      SideDependentList<Color> footColors = new SideDependentList<Color>(Color.pink, Color.blue);
      for (RobotSide robotSide : RobotSide.values)
      {
         int maxNumberOfVertices = bipedFeet.get(robotSide).getContactPointsCopy().size();
         String footName = robotSide.getCamelCaseNameForStartOfExpression() + "Foot";
         YoFrameConvexPolygon2d yoFrameFootPolygonInWorld = new YoFrameConvexPolygon2d(footName, "", worldFrame, maxNumberOfVertices, registry);
         this.feetPolygonsInWorld.put(robotSide, yoFrameFootPolygonInWorld);

         YoArtifactPolygon dynamicGraphicYoPolygonArtifact = new YoArtifactPolygon(footName, yoFrameFootPolygonInWorld,
               footColors.get(robotSide), false);
         artifactList.add(dynamicGraphicYoPolygonArtifact);
      }

      yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);
      yoGraphicsListRegistry.registerArtifactList(artifactList);
      parentRegistry.addChild(registry);
   }

   private SideDependentList<ReferenceFrame> getAnkleZUpFrames()
   {
      return ankleZUpFrames;
   }

   private SideDependentList<ReferenceFrame> getFootFrames()
   {
      return footFrames;
   }

   private SideDependentList<ContactablePlaneBody> getBipedFeet()
   {
      return bipedFeet;
   }

   public void setFootstepProvider(FootstepProvider footstepProvider)
   {
      this.footstepProvider = footstepProvider;
   }

   private SimulationConstructionSet createSCSAndAttachVisualizer(YoVariableRegistry registryToAddToRobot,
         YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      ArrayList<RobotController> robotControllers = new ArrayList<RobotController>();
      VisualizeFramesController visualizeFramesController = new VisualizeFramesController(framesToVisualize, yoGraphicsListRegistry, 1.0);
      robotControllers.add(visualizeFramesController);

      Robot robot = new Robot("DesiredFootstepVisualizerRobot");
      for (RobotController robotController : robotControllers)
      {
         robot.setController(robotController);
      }

      robot.getRobotsYoVariableRegistry().addChild(registryToAddToRobot);

      SimulationConstructionSet scs = new SimulationConstructionSet(robot);

      scs.addYoGraphicsListRegistry(yoGraphicsListRegistry);

      // Create and attach plotter as listener
      SimulationOverheadPlotter simulationOverheadPlotter = new SimulationOverheadPlotter();
      simulationOverheadPlotter.setDrawHistory(false);

      scs.attachPlaybackListener(simulationOverheadPlotter);
      JPanel simulationOverheadPlotterJPanel = simulationOverheadPlotter.getJPanel();
      scs.addExtraJpanel(simulationOverheadPlotterJPanel, "Plotter");
      JPanel simulationOverheadPlotterKeyJPanel = simulationOverheadPlotter.getJPanelKey();

      JScrollPane scrollPane = new JScrollPane(simulationOverheadPlotterKeyJPanel);

      scs.addExtraJpanel(scrollPane, "Plotter Legend");

      yoGraphicsListRegistry.addArtifactListsToPlotter(simulationOverheadPlotter.getPlotter());

      Thread thread = new Thread(scs);
      thread.start();

      return scs;
   }

   public Footstep takeAndVisualizeAStep(RobotSide swingLegSide)
   {
      if (footstepProvider.isEmpty())
         return null;
      Footstep desiredFootstep = footstepProvider.poll();

      FramePose pose = new FramePose();
      desiredFootstep.getPose(pose);
      pose.changeFrame(worldFrame);
      Transform3d transform = new Transform3d();
      pose.getPose(transform);
      sixDoFJoints.get(swingLegSide).setPositionAndRotation(transform);

      updateFrames();

      setFeetPolygonsInWorld();

      computeMinZ(swingLegSide);

      FramePoint desiredFootstepPosition = new FramePoint();
      desiredFootstep.getPositionIncludingFrame(desiredFootstepPosition);
      desiredFootstepPosition.changeFrame(worldFrame);
      bagsOfBalls.get(swingLegSide).setBall(desiredFootstepPosition);

      return desiredFootstep;
   }

   private void hideSwingLeg(RobotSide swingLegSide)
   {
      feetPolygonsInWorld.get(swingLegSide).hide();
   }

   private final FrameConvexPolygon2d tempFootPolygonInWorld = new FrameConvexPolygon2d();

   private void setFeetPolygonsInWorld()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         tempFootPolygonInWorld.setIncludingFrameByProjectionOntoXYPlaneAndUpdate(worldFrame, bipedFeet.get(robotSide).getContactPointsCopy());

         feetPolygonsInWorld.get(robotSide).setFrameConvexPolygon2d(tempFootPolygonInWorld);
      }
   }

   private void computeMinZ(RobotSide swingLegSide)
   {
      ContactablePlaneBody bipedFoot = bipedFeet.get(swingLegSide);
      Transform3d footToWorldTransform = sixDoFJoints.get(swingLegSide).getFrameAfterJoint().getTransformToDesiredFrame(worldFrame);
      FramePoint minZPoint = DesiredFootstepCalculatorTools.computeMinZPointInFrame(footToWorldTransform, bipedFoot, worldFrame);
      this.minZ.set(minZPoint.getZ());
   }

   private void updateFrames()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         sixDoFJoints.get(robotSide).updateFramesRecursively();
         soleFrames.get(robotSide).update();
         footFrames.get(robotSide).update();
         ankleZUpFrames.get(robotSide).update();
      }
   }

   private void setPoseToMoveToTransform(RobotSide swingLegSide, Transform3d transform)
   {
      sixDoFJoints.get(swingLegSide).setPositionAndRotation(transform);
      sixDoFJoints.get(swingLegSide).updateFramesRecursively();
   }

   public static void main(String[] args)
   {
      visualizeDesiredFootstepCalculator();
   }

   public static void visualizePathBasedFootstepListCreator()
   {
      YoVariableRegistry parentRegistry = new YoVariableRegistry("parent");
      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();

      DesiredFootstepVisualizer desiredFootstepVisualizer = new DesiredFootstepVisualizer(parentRegistry, yoGraphicsListRegistry);

      SideDependentList<ContactablePlaneBody> bipedFeet = desiredFootstepVisualizer.getBipedFeet();

      RobotSide initialStanceSide = RobotSide.LEFT;
      RobotSide swingLegSide = initialStanceSide.getOppositeSide();

      SimplePathParameters pathType = new SimplePathParameters(0.2, 0.1, 0.0, Math.PI * 0.8, Math.PI * 0.15, 0.35);

      FramePoint2d endPoint = new FramePoint2d(worldFrame, 10.0, 0.0);

      TurningThenStraightFootstepGenerator footstepGenerator = new TurningThenStraightFootstepGenerator(bipedFeet, endPoint, pathType, initialStanceSide);

      List<Footstep> footsteps = footstepGenerator.generateDesiredFootstepList();

      long dataIdentifier = 1776L;
      FootstepConsumer footstepConsumer = new FootstepConsumer(dataIdentifier, bipedFeet);

      for (Footstep footstep : footsteps)
      {
    	  Point3d location = new Point3d();
    	  Quat4d orientation = new Quat4d();
    	  footstep.getPose(location, orientation);
         FootstepData footStepData = new FootstepData(FootstepUtils.getSideFromFootstep(footstep, bipedFeet), location, orientation);
         footstepConsumer.consume(dataIdentifier, footStepData);
      }

      desiredFootstepVisualizer.setFootstepProvider(footstepConsumer);

      double controlDT = 0.1;

      SimulationConstructionSet scs = desiredFootstepVisualizer.createSCSAndAttachVisualizer(parentRegistry, yoGraphicsListRegistry);
      scs.setDT(controlDT, 1);

      int numberOfSteps = footsteps.size();

      for (int i = 0; i < numberOfSteps; i++)
      {
         desiredFootstepVisualizer.takeAndVisualizeAStep(swingLegSide);
         swingLegSide = swingLegSide.getOppositeSide();
      }
   }

   public static void visualizeDesiredFootstepCalculator()
   {
      YoVariableRegistry parentRegistry = new YoVariableRegistry("parent");
      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();

      DesiredFootstepVisualizer desiredFootstepVisualizer = new DesiredFootstepVisualizer(parentRegistry, yoGraphicsListRegistry);

      double HEADING_VIZ_Z = 0.03;
      double VELOCITY_VIZ_Z = 0.06;

      // Visualizers for the HeadingAndVelocityEvaluationScript:
      YoFramePoint position = new YoFramePoint("position", "", worldFrame, parentRegistry);
      YoFrameVector velocity = new YoFrameVector("velocity", "", worldFrame, parentRegistry);
      YoFrameVector heading = new YoFrameVector("heading", "", worldFrame, parentRegistry);

      YoGraphicVector velocityVector = new YoGraphicVector("velocity", position, velocity, YoAppearance.Yellow());
      YoGraphicVector headingVector = new YoGraphicVector("heading", position, heading, YoAppearance.Blue());

      yoGraphicsListRegistry.registerYoGraphic("velocityVector", velocityVector);
      yoGraphicsListRegistry.registerYoGraphic("headingVector", headingVector);

      BagOfBalls bagOfBalls = new BagOfBalls(1200, 0.03, YoAppearance.Red(), parentRegistry, yoGraphicsListRegistry);

      double desiredHeadingFinal = 0.0;
      double controlDT = 0.1;
      int ticksPerStep = 7;
      int ticksPerDoubleSupport = 2;

      SimpleDesiredHeadingControlModule desiredHeadingControlModule = new SimpleDesiredHeadingControlModule(desiredHeadingFinal, controlDT, parentRegistry);

      ManualDesiredVelocityControlModule desiredVelocityControlModule = new ManualDesiredVelocityControlModule(worldFrame, parentRegistry);

      ReferenceFrame footBodyFrame = desiredFootstepVisualizer.getBipedFeet().get(RobotSide.LEFT).getFrameAfterParentJoint();
      ReferenceFrame footPlaneFrame = desiredFootstepVisualizer.getBipedFeet().get(RobotSide.LEFT).getSoleFrame();
      Transform3d transformFromFootBodyFrameToFootPlaneFrame = footBodyFrame.getTransformToDesiredFrame(footPlaneFrame);
      Vector3d trans = new Vector3d();
      transformFromFootBodyFrameToFootPlaneFrame.get(trans);
      double ankleHeight = trans.getZ();
      ComponentBasedDesiredFootstepCalculator desiredFootstepCalculator = new ComponentBasedDesiredFootstepCalculator(ankleHeight,
            desiredFootstepVisualizer.getAnkleZUpFrames(), desiredFootstepVisualizer.getFootFrames(), desiredFootstepVisualizer.getBipedFeet(),
            desiredHeadingControlModule, desiredVelocityControlModule, parentRegistry);
      desiredFootstepCalculator.setInPlaceWidth(0.4);
      desiredFootstepCalculator.setMaxStepLength(0.6);
      desiredFootstepCalculator.setMinStepWidth(0.25);
      desiredFootstepCalculator.setMaxStepWidth(0.5);
      desiredFootstepCalculator.setStepPitch(-0.25);

      boolean cycleThroughAllEvents = true;
      HeadingAndVelocityEvaluationScript headingAndVelocityEvaluationScript = new HeadingAndVelocityEvaluationScript(cycleThroughAllEvents, controlDT,
            desiredHeadingControlModule, desiredVelocityControlModule, parentRegistry);

      DesiredFootstepCalculatorFootstepProviderWrapper footstepProvider = new DesiredFootstepCalculatorFootstepProviderWrapper(desiredFootstepCalculator,
            parentRegistry);
      footstepProvider.setWalk(true);
      desiredFootstepVisualizer.setFootstepProvider(footstepProvider);

      SimulationConstructionSet scs = desiredFootstepVisualizer.createSCSAndAttachVisualizer(parentRegistry, yoGraphicsListRegistry);
      scs.setDT(controlDT, 1);

      RobotSide swingLegSide = RobotSide.LEFT;
      int numberOfSteps = 100;

      double time = scs.getTime();

      for (int i = 0; i < numberOfSteps; i++)
      {
         desiredFootstepVisualizer.hideSwingLeg(swingLegSide);

         for (int j = 0; j < ticksPerStep; j++)
         {
            desiredHeadingControlModule.updateDesiredHeadingFrame();
            headingAndVelocityEvaluationScript.update(time);

            FrameVector2d desiredHeading = desiredHeadingControlModule.getDesiredHeading();
            FrameVector2d desiredVelocity = desiredVelocityControlModule.getDesiredVelocity();

            heading.set(desiredHeading.getX(), desiredHeading.getY(), HEADING_VIZ_Z);
            velocity.set(desiredVelocity.getX(), desiredVelocity.getY(), VELOCITY_VIZ_Z);

            position.add(desiredVelocity.getX() * controlDT, desiredVelocity.getY() * controlDT, 0.0);

            FramePoint location = new FramePoint(worldFrame);
            location.set(position.getX(), position.getY(), 0.0);

            bagOfBalls.setBall(location);

            scs.setTime(time);
            boolean doSleep = false;
            if (doSleep)
               ThreadTools.sleepSeconds(controlDT);
            time = time + controlDT;
            scs.doControl();
            scs.tickAndUpdate();
         }

         Footstep footstep = desiredFootstepVisualizer.takeAndVisualizeAStep(swingLegSide);

         FramePose poseToMoveTo = new FramePose(worldFrame);
         footstep.getPose(poseToMoveTo);
         poseToMoveTo.changeFrame(worldFrame);

         Transform3d transform = new Transform3d();
         poseToMoveTo.getPose(transform);
         desiredFootstepVisualizer.setPoseToMoveToTransform(swingLegSide, transform);

         for (int j = 0; j < ticksPerDoubleSupport; j++)
         {
            headingAndVelocityEvaluationScript.update(time);
            desiredHeadingControlModule.updateDesiredHeadingFrame();

            scs.setTime(time);
            boolean doSleep = false;
            if (doSleep)
               ThreadTools.sleepSeconds(controlDT);
            time = time + controlDT;
            scs.doControl();
            scs.tickAndUpdate();
         }
         swingLegSide = swingLegSide.getOppositeSide();
      }
   }
}