package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.RectangularContactableBody;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.HeadingAndVelocityEvaluationScript;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.ManualDesiredVelocityControlModule;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.SimpleDesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.referenceFrames.VisualizeFramesController;
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

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.plotting.DynamicGraphicYoPolygonArtifact;
import com.yobotics.simulationconstructionset.plotting.SimulationOverheadPlotter;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.graphics.ArtifactList;
import com.yobotics.simulationconstructionset.util.graphics.BagOfBalls;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameConvexPolygon2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;
import com.yobotics.simulationconstructionset.util.trajectory.OverheadPath;

public class DesiredFootstepVisualizer
{
   private final YoVariableRegistry registry = new YoVariableRegistry("DesiredFootstepVisualizer");

   private FootstepProvider footstepProvider;
   private final SideDependentList<YoFrameConvexPolygon2d> feetPolygonsInWorld = new SideDependentList<YoFrameConvexPolygon2d>();
   private final DoubleYoVariable minZ = new DoubleYoVariable("minZ", registry);
   private final SideDependentList<SixDoFJoint> sixDoFJoints;
   private final SideDependentList<ReferenceFrame> soleFrames;
   private final SideDependentList<ReferenceFrame> ankleZUpFrames;
   private final ArrayList<ReferenceFrame> framesToVisualize;

   private final SideDependentList<ContactablePlaneBody> bipedFeet;

   private final SideDependentList<BagOfBalls> bagsOfBalls;

   public DesiredFootstepVisualizer(YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      BagOfBalls leftBagOfBalls = new BagOfBalls(1000, 0.05, "leftBalls", YoAppearance.Red(), parentRegistry, dynamicGraphicObjectsListRegistry);
      BagOfBalls rightBagOfBalls = new BagOfBalls(1000, 0.05, "rightBalls", YoAppearance.Blue(), parentRegistry, dynamicGraphicObjectsListRegistry);
      bagsOfBalls = new SideDependentList<BagOfBalls>(leftBagOfBalls, rightBagOfBalls);
      
      framesToVisualize = new ArrayList<ReferenceFrame>();

      RigidBody elevator = new RigidBody("elevator", ReferenceFrame.getWorldFrame());
      this.sixDoFJoints = new SideDependentList<SixDoFJoint>();
      this.soleFrames = new SideDependentList<ReferenceFrame>();
      this.ankleZUpFrames = new SideDependentList<ReferenceFrame>();
      this.bipedFeet = new SideDependentList<ContactablePlaneBody>();

      double footWidth = 0.15;
      double footForward = 0.25;
      double footBackward = 0.05;
      double footHeight = 0.05;
      for (RobotSide robotSide : RobotSide.values())
      {
         String robotSideName = robotSide.getCamelCaseNameForStartOfExpression();

         SixDoFJoint sixDoFJoint = new SixDoFJoint(robotSideName + "Joint", elevator, elevator.getBodyFixedFrame());
         sixDoFJoints.put(robotSide, sixDoFJoint);
         ReferenceFrame footFrame = sixDoFJoint.getFrameAfterJoint();
         RigidBody footBody = ScrewTools.addRigidBody(robotSideName + "Foot", sixDoFJoint, new Matrix3d(), 0.0, new Vector3d());

         ReferenceFrame soleFrame = ReferenceFrame.constructBodyFrameWithUnchangingTranslationFromParent(robotSideName + "Sole", footFrame,
                                       new Vector3d(0.0, 0.0, -footHeight));
         soleFrames.put(robotSide, soleFrame);

         ZUpFrame ankleZUpFrame = new ZUpFrame(ReferenceFrame.getWorldFrame(), footFrame, robotSideName + "AnkleZUp");
         ankleZUpFrames.put(robotSide, ankleZUpFrame);

         ContactablePlaneBody foot = new RectangularContactableBody(footBody, soleFrame, footForward, -footBackward, footWidth / 2.0, -footWidth / 2.0);
         bipedFeet.put(robotSide, foot);
         framesToVisualize.add(footFrame);
      }

      elevator.updateFramesRecursively();

      for (RobotSide robotSide : RobotSide.values())
      {
         ankleZUpFrames.get(robotSide).update();
      }
      
      DynamicGraphicObjectsList dynamicGraphicObjectsList = new DynamicGraphicObjectsList("FeetPolygons");
      ArtifactList artifactList = new ArtifactList("FeetPolygons");
      SideDependentList<Color> footColors = new SideDependentList<Color>(Color.pink, Color.blue);
      for (RobotSide robotSide : RobotSide.values())
      {
         int maxNumberOfVertices = bipedFeet.get(robotSide).getContactPoints().size();
         String footName = robotSide.getCamelCaseNameForStartOfExpression() + "Foot";
         YoFrameConvexPolygon2d yoFrameFootPolygonInWorld = new YoFrameConvexPolygon2d(footName, "", ReferenceFrame.getWorldFrame(), maxNumberOfVertices,
                                                               registry);
         this.feetPolygonsInWorld.put(robotSide, yoFrameFootPolygonInWorld);

         DynamicGraphicYoPolygonArtifact dynamicGraphicYoPolygonArtifact = new DynamicGraphicYoPolygonArtifact(footName, yoFrameFootPolygonInWorld,
                                                                              footColors.get(robotSide), false);
         artifactList.add(dynamicGraphicYoPolygonArtifact);
      }

      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);
      dynamicGraphicObjectsListRegistry.registerArtifactList(artifactList);
      parentRegistry.addChild(registry);
   }
   
   private SideDependentList<ReferenceFrame> getAnkleZUpFrames()
   {
      return ankleZUpFrames;
   }

   private SideDependentList<? extends ContactablePlaneBody> getBipedFeet()
   {
      return bipedFeet;
   }
   
   public void setFootstepProvider (FootstepProvider footstepProvider)
   {
      this.footstepProvider = footstepProvider;
   }
   
   private SimulationConstructionSet createSCSAndAttachVisualizer(YoVariableRegistry registryToAddToRobot,
           DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      ArrayList<RobotController> robotControllers = new ArrayList<RobotController>();
      VisualizeFramesController visualizeFramesController = new VisualizeFramesController(framesToVisualize, dynamicGraphicObjectsListRegistry, 1.0);
      robotControllers.add(visualizeFramesController);
      
      Robot robot = new Robot("DesiredFootstepVisualizerRobot");
      for (RobotController robotController : robotControllers)
      {
         robot.setController(robotController);
      }

      robot.getRobotsYoVariableRegistry().addChild(registryToAddToRobot);

      SimulationConstructionSet scs = new SimulationConstructionSet(robot);

      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);

//    // Create and attach plotter as listener
      SimulationOverheadPlotter simulationOverheadPlotter = new SimulationOverheadPlotter();
      simulationOverheadPlotter.setDrawHistory(false);

//    simulationOverheadPlotter.setXVariableToTrack(processedSensors.getYoBodyPositionInWorld().getYoX());
//    simulationOverheadPlotter.setYVariableToTrack(processedSensors.getYoBodyPositionInWorld().getYoY());

      scs.attachPlaybackListener(simulationOverheadPlotter);
      JPanel simulationOverheadPlotterJPanel = simulationOverheadPlotter.getJPanel();
      scs.addExtraJpanel(simulationOverheadPlotterJPanel, "Plotter");
      JPanel simulationOverheadPlotterKeyJPanel = simulationOverheadPlotter.getJPanelKey();

      JScrollPane scrollPane = new JScrollPane(simulationOverheadPlotterKeyJPanel);

      scs.addExtraJpanel(scrollPane, "Plotter Legend");

      dynamicGraphicObjectsListRegistry.addArtifactListsToPlotter(simulationOverheadPlotter.getPlotter());


      Thread thread = new Thread(scs);
      thread.start();

      return scs;
   }

   public Footstep takeAndVisualizeAStep(RobotSide swingLegSide)
   {
//      desiredFootstepCalculator.initializeDesiredFootstep(supportLegSide);
//      Footstep desiredFootstep = desiredFootstepCalculator.updateAndGetDesiredFootstep(supportLegSide);

      if (footstepProvider.isEmpty()) return null;
      Footstep desiredFootstep = footstepProvider.poll();
      
//    System.out.println("desiredFootstep = " + desiredFootstep);

      FramePose pose = desiredFootstep.getPose();
      pose = pose.changeFrameCopy(ReferenceFrame.getWorldFrame());
      Transform3D transform = new Transform3D();
      pose.getTransform3D(transform);
      sixDoFJoints.get(swingLegSide).setPositionAndRotation(transform);

      updateFrames();

      setFeetPolygonsInWorld();

      computeMinZ(swingLegSide);

      bagsOfBalls.get(swingLegSide).setBall(desiredFootstep.getPositionInFrame(ReferenceFrame.getWorldFrame()));

      return desiredFootstep;
   }

   private void hideSwingLeg(RobotSide swingLegSide)
   {
      feetPolygonsInWorld.get(swingLegSide).hide();
   }


   private void setFeetPolygonsInWorld()
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         FrameConvexPolygon2d footPolygonInWorld = FrameConvexPolygon2d.constructByProjectionOntoXYPlane(bipedFeet.get(robotSide).getContactPoints(), ReferenceFrame.getWorldFrame());

         feetPolygonsInWorld.get(robotSide).setFrameConvexPolygon2d(footPolygonInWorld);
      }
   }

   private void computeMinZ(RobotSide swingLegSide)
   {
      ContactablePlaneBody bipedFoot = bipedFeet.get(swingLegSide);
      Transform3D footToWorldTransform = sixDoFJoints.get(swingLegSide).getFrameAfterJoint().getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
      FramePoint minZPoint = DesiredFootstepCalculatorTools.computeMinZPointInFrame(footToWorldTransform, bipedFoot, ReferenceFrame.getWorldFrame());
      this.minZ.set(minZPoint.getZ());
   }

   private void updateFrames()
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         sixDoFJoints.get(robotSide).updateFramesRecursively();
         soleFrames.get(robotSide).update();
         ankleZUpFrames.get(robotSide).update();
      }
   }
   
   private void setPoseToMoveToTransform(RobotSide swingLegSide, Transform3D transform)
   {
      sixDoFJoints.get(swingLegSide).setPositionAndRotation(transform);
      sixDoFJoints.get(swingLegSide).updateFramesRecursively();
   }
  
   public static void main(String[] args)
   {
      visualizeDesiredFootstepCalculator();
//      visualizePathBasedFootstepListCreator();
   }
   
   public static void visualizePathBasedFootstepListCreator()
   {
      YoVariableRegistry parentRegistry = new YoVariableRegistry("parent");
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();

      DesiredFootstepVisualizer desiredFootstepVisualizer = new DesiredFootstepVisualizer(parentRegistry, dynamicGraphicObjectsListRegistry);
      
      SideDependentList<? extends ContactablePlaneBody> bipedFeet =  desiredFootstepVisualizer.getBipedFeet();
      PathBasedFootstepListCreator pathBasedFootstepListCreator = new PathBasedFootstepListCreator(ReferenceFrame.getWorldFrame(), bipedFeet);
      
      OverheadPath footstepPath = new OverheadPath()
      {
         private final double totalDistance = 10.0;
         private double distanceAlongPath;
         

         public void compute(double distanceAlongPath)
         {     
            this.distanceAlongPath = distanceAlongPath;
         }

         public double getTotalDistance()
         {
            return totalDistance;
         }

         public FramePoint2d getPosition()
         {
            return new FramePoint2d(ReferenceFrame.getWorldFrame(), distanceAlongPath, 0.0);
         }

         public double getHeadingInFrame(ReferenceFrame frame)
         {
            return 0.0;
         }};
      
      pathBasedFootstepListCreator.setFootstepPath(footstepPath);
      
      RobotSide initialStanceSide = RobotSide.LEFT;
      RobotSide swingLegSide = initialStanceSide.getOppositeSide();
      
      RigidBody endEffector = bipedFeet.get(initialStanceSide).getRigidBody();
      FramePose pose = new FramePose(ReferenceFrame.getWorldFrame());
      List<FramePoint> expectedContactPoints = bipedFeet.get(initialStanceSide).getContactPoints();
      
      Footstep initialStanceFootstep = new Footstep(endEffector , pose, expectedContactPoints);
      pathBasedFootstepListCreator.setStepLength(0.2);
      pathBasedFootstepListCreator.setStepWidth(0.1);
      List<Footstep> footsteps = pathBasedFootstepListCreator.compute(initialStanceFootstep);
      
      long dataIdentifier = 1776L;
      FootstepConsumer footstepConsumer = new FootstepConsumer(dataIdentifier, bipedFeet.values());

      for (Footstep footstep : footsteps)
      {
         footstepConsumer.consume(dataIdentifier, footstep);
      }
      
      desiredFootstepVisualizer.setFootstepProvider(footstepConsumer);
      
      double controlDT = 0.1;

      SimulationConstructionSet scs = desiredFootstepVisualizer.createSCSAndAttachVisualizer(parentRegistry, dynamicGraphicObjectsListRegistry);
      scs.setDT(controlDT, 1);
      
      int numberOfSteps = footsteps.size();
      
      for (int i=0; i<numberOfSteps; i++)
      {
         desiredFootstepVisualizer.takeAndVisualizeAStep(swingLegSide);
         swingLegSide = swingLegSide.getOppositeSide();
      }
   }
   
   public static void visualizeDesiredFootstepCalculator()
   {
      YoVariableRegistry parentRegistry = new YoVariableRegistry("parent");
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();

      DesiredFootstepVisualizer desiredFootstepVisualizer = new DesiredFootstepVisualizer(parentRegistry, dynamicGraphicObjectsListRegistry);
      
      double HEADING_VIZ_Z = 0.03;
      double VELOCITY_VIZ_Z = 0.06;

      // Visualizers for the HeadingAndVelocityEvaluationScript:
      YoFramePoint position = new YoFramePoint("position", "", ReferenceFrame.getWorldFrame(), parentRegistry);
      YoFrameVector velocity = new YoFrameVector("velocity", "", ReferenceFrame.getWorldFrame(), parentRegistry);
      YoFrameVector heading = new YoFrameVector("heading", "", ReferenceFrame.getWorldFrame(), parentRegistry);

      DynamicGraphicVector velocityVector = new DynamicGraphicVector("velocity", position, velocity, YoAppearance.Yellow());
      DynamicGraphicVector headingVector = new DynamicGraphicVector("heading", position, heading, YoAppearance.Blue());

      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("velocityVector", velocityVector);
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("headingVector", headingVector);

      BagOfBalls bagOfBalls = new BagOfBalls(1200, 0.03, YoAppearance.Red(), parentRegistry, dynamicGraphicObjectsListRegistry);

      double desiredHeadingFinal = 0.0;
      double controlDT = 0.1;
      int ticksPerStep = 7;
      int ticksPerDoubleSupport = 2;

      SimpleDesiredHeadingControlModule desiredHeadingControlModule = new SimpleDesiredHeadingControlModule(desiredHeadingFinal, controlDT, parentRegistry);

      ManualDesiredVelocityControlModule desiredVelocityControlModule = new ManualDesiredVelocityControlModule(ReferenceFrame.getWorldFrame(), parentRegistry);

//    SimpleDesiredFootstepCalculator desiredFootstepCalculator = new SimpleDesiredFootstepCalculator(ankleZUpFrames,
//                                                                                    desiredHeadingControlModule,
//                                                                                    parentRegistry);

      ComponentBasedDesiredFootstepCalculator desiredFootstepCalculator = new ComponentBasedDesiredFootstepCalculator(desiredFootstepVisualizer.getAnkleZUpFrames(), desiredFootstepVisualizer.getBipedFeet(),
                                                                             desiredHeadingControlModule, desiredVelocityControlModule, parentRegistry);
      desiredFootstepCalculator.setInPlaceWidth(0.4);
      desiredFootstepCalculator.setMaxStepLength(0.6);
      desiredFootstepCalculator.setMinStepWidth(0.25);
      desiredFootstepCalculator.setMaxStepWidth(0.5);
      desiredFootstepCalculator.setStepPitch(-0.25);

//    HeadingAndVelocityBasedDesiredFootstepCalculator desiredFootstepCalculator = new HeadingAndVelocityBasedDesiredFootstepCalculator(ankleZUpFrames,
//          desiredHeadingControlModule, desiredVelocityControlModule,
//          parentRegistry);


      boolean cycleThroughAllEvents = true;
      HeadingAndVelocityEvaluationScript headingAndVelocityEvaluationScript = new HeadingAndVelocityEvaluationScript(cycleThroughAllEvents, controlDT,
                                                                                 desiredHeadingControlModule, desiredVelocityControlModule, parentRegistry);

      
      DesiredFootstepCalculatorFootstepProviderWrapper footstepProvider = new DesiredFootstepCalculatorFootstepProviderWrapper(desiredFootstepCalculator, parentRegistry);
      footstepProvider.setWalk(true); 
      desiredFootstepVisualizer.setFootstepProvider(footstepProvider);
      
      SimulationConstructionSet scs = desiredFootstepVisualizer.createSCSAndAttachVisualizer(parentRegistry, dynamicGraphicObjectsListRegistry);
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

            FramePoint location = new FramePoint(ReferenceFrame.getWorldFrame());
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

         FramePose poseToMoveTo = new FramePose(ReferenceFrame.getWorldFrame());
         footstep.getPose(poseToMoveTo);
         poseToMoveTo.changeFrame(ReferenceFrame.getWorldFrame());

         Transform3D transform = new Transform3D();
         poseToMoveTo.getTransform3D(transform);
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
