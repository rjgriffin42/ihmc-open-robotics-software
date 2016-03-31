package us.ihmc.commonWalkingControlModules.desiredFootStep;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.communication.controllerAPI.command.CommandArrayDeque;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.FootTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.FootstepDataControllerCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.FootstepDataListCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PauseWalkingCommand;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingStatusMessage;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.trajectories.TrajectoryType;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class WalkingMessageHandler
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   // TODO Need to find something better than an ArrayList.
   private final List<Footstep> upcomingFootsteps = new ArrayList<>();
   private final SideDependentList<? extends ContactablePlaneBody> contactableFeet;
   private final SideDependentList<Footstep> footstepsAtCurrentLocation = new SideDependentList<>();

   private final SideDependentList<CommandArrayDeque<FootTrajectoryCommand>> upcomingFootTrajectoryCommandListForFlamingoStance = new SideDependentList<>();

   private final StatusMessageOutputManager statusOutputManager;

   private final IntegerYoVariable currentFootstepIndex = new IntegerYoVariable("currentFootstepIndex", registry);
   private final IntegerYoVariable currentNumberOfFootsteps = new IntegerYoVariable("currentNumberOfFootsteps", registry);
   private final BooleanYoVariable isWalkingPaused = new BooleanYoVariable("isWalkingPaused", registry);
   private final DoubleYoVariable transferTime = new DoubleYoVariable("transferTime", registry);
   private final DoubleYoVariable swingTime = new DoubleYoVariable("swingTime", registry);

   private final int numberOfFootstepsToVisualize = 4;
   @SuppressWarnings("unchecked")
   private final EnumYoVariable<RobotSide>[] upcomingFoostepSide = new EnumYoVariable[numberOfFootstepsToVisualize];

   private final FootstepListVisualizer footstepListVisualizer;

   public WalkingMessageHandler(double defaultTransferTime, double defaultSwingTime, SideDependentList<? extends ContactablePlaneBody> contactableFeet,
         StatusMessageOutputManager statusOutputManager, YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      this.contactableFeet = contactableFeet;
      this.statusOutputManager = statusOutputManager;

      transferTime.set(defaultTransferTime);
      swingTime.set(defaultSwingTime);

      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody contactableFoot = contactableFeet.get(robotSide);
         RigidBody endEffector = contactableFoot.getRigidBody();
         ReferenceFrame soleFrame = contactableFoot.getSoleFrame();
         String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
         PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame(sidePrefix + "FootstepAtCurrentLocation", worldFrame);
         Footstep footstepAtCurrentLocation = new Footstep(endEffector, robotSide, soleFrame, poseReferenceFrame);
         footstepsAtCurrentLocation.put(robotSide, footstepAtCurrentLocation);

         upcomingFootTrajectoryCommandListForFlamingoStance.put(robotSide, new CommandArrayDeque<>(FootTrajectoryCommand.class));
      }

      for (int i = 0; i < numberOfFootstepsToVisualize; i++)
         upcomingFoostepSide[i] = new EnumYoVariable<>("upcomingFoostepSide" + i, registry, RobotSide.class, true);

      footstepListVisualizer = new FootstepListVisualizer(contactableFeet, yoGraphicsListRegistry, registry);
      updateVisualization();

      parentRegistry.addChild(registry);
   }

   public void handleFootstepDataListCommand(FootstepDataListCommand command)
   {
      if (command.getNumberOfFootsteps() > 0)
      {
         upcomingFootsteps.clear();
         currentFootstepIndex.set(0);
         isWalkingPaused.set(false);
         clearFootTrajectory();
      }

      double commandTransferTime = command.getTransferTime();
      double commandSwingTime = command.getSwingTime();
      if (!Double.isNaN(commandSwingTime) && commandSwingTime > 1.0e-2 && !Double.isNaN(commandTransferTime) && commandTransferTime > 1.0e-2)
      {
         transferTime.set(commandTransferTime);
         swingTime.set(commandSwingTime);
      }

      currentNumberOfFootsteps.set(command.getNumberOfFootsteps());

      for (int i = 0; i < command.getNumberOfFootsteps(); i++)
      {
         Footstep newFootstep = createFootstep(command.getFootstep(i));
         upcomingFootsteps.add(newFootstep);
      }
      updateVisualization();
   }

   public void handlePauseWalkingCommand(PauseWalkingCommand command)
   {
      isWalkingPaused.set(command.isPauseRequested());
   }

   public void handleFootTrajectoryCommand(List<FootTrajectoryCommand> commands)
   {
      for (int i = 0; i < commands.size(); i++)
      {
         FootTrajectoryCommand command = commands.get(i);
         upcomingFootTrajectoryCommandListForFlamingoStance.get(command.getRobotSide()).addLast(command);
      }
   }

   public Footstep peek(int i)
   {
      if (i >= upcomingFootsteps.size())
         return null;
      else
         return upcomingFootsteps.get(i);
   }

   public Footstep poll()
   {
      if (upcomingFootsteps.isEmpty())
         return null;
      else
      {
         updateVisualization();
         currentNumberOfFootsteps.decrement();
         currentFootstepIndex.increment();
         return upcomingFootsteps.remove(0);
      }
   }

   public FootTrajectoryCommand pollFootTrajectoryForFlamingoStance(RobotSide swingSide)
   {
      return upcomingFootTrajectoryCommandListForFlamingoStance.get(swingSide).poll();
   }

   public void insertNextFootstep(Footstep newNextFootstep)
   {
      if (newNextFootstep != null)
         upcomingFootsteps.add(0, newNextFootstep);
   }

   public boolean hasUpcomingFootsteps()
   {
      return !upcomingFootsteps.isEmpty() && !isWalkingPaused.getBooleanValue();
   }

   public boolean isNextFootstepFor(RobotSide swingSide)
   {
      if (!hasUpcomingFootsteps())
         return false;
      else
         return peek(0).getRobotSide() == swingSide;
   }

   public boolean hasFootTrajectoryForFlamingoStance()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         if (hasFootTrajectoryForFlamingoStance(robotSide))
            return true;
      }
      return false;
   }

   public boolean hasFootTrajectoryForFlamingoStance(RobotSide swingSide)
   {
      return !upcomingFootTrajectoryCommandListForFlamingoStance.get(swingSide).isEmpty();
   }

   public boolean isWalkingPaused()
   {
      return isWalkingPaused.getBooleanValue();
   }

   public void clearFootTrajectory(RobotSide robotSide)
   {
      upcomingFootTrajectoryCommandListForFlamingoStance.get(robotSide).clear();
   }

   public void clearFootTrajectory()
   {
      for (RobotSide robotSide : RobotSide.values)
         clearFootTrajectory(robotSide);
   }

   public void clearFootsteps()
   {
      upcomingFootsteps.clear();
      currentNumberOfFootsteps.set(0);
      currentFootstepIndex.set(0);
      updateVisualization();
   }

   private final Point3d actualFootPositionInWorld = new Point3d();
   private final Quat4d actualFootOrientationInWorld = new Quat4d();

   public void reportFootstepStarted(RobotSide robotSide)
   {
      statusOutputManager.reportStatusMessage(new FootstepStatus(FootstepStatus.Status.STARTED, currentFootstepIndex.getIntegerValue()));
   }

   public void reportFootstepCompleted(RobotSide robotSide, FramePose actualFootPoseInWorld)
   {
      actualFootPoseInWorld.getPose(actualFootPositionInWorld, actualFootOrientationInWorld);
      statusOutputManager.reportStatusMessage(new FootstepStatus(FootstepStatus.Status.COMPLETED, currentFootstepIndex.getIntegerValue(),
            actualFootPositionInWorld, actualFootOrientationInWorld, robotSide));
   }

   public void reportWalkingComplete()
   {
      WalkingStatusMessage walkingStatusMessage = new WalkingStatusMessage();
      walkingStatusMessage.setWalkingStatus(WalkingStatusMessage.Status.COMPLETED);
      statusOutputManager.reportStatusMessage(walkingStatusMessage);
   }

   public void reportWalkingAbortRequested()
   {
      WalkingStatusMessage walkingStatusMessage = new WalkingStatusMessage();
      walkingStatusMessage.setWalkingStatus(WalkingStatusMessage.Status.ABORT_REQUESTED);
      statusOutputManager.reportStatusMessage(walkingStatusMessage);
   }

   private final FramePose tempPose = new FramePose();

   public Footstep getFootstepAtCurrentLocation(RobotSide robotSide)
   {
      tempPose.setToZero(contactableFeet.get(robotSide).getFrameAfterParentJoint());
      tempPose.changeFrame(worldFrame);
      Footstep footstep = footstepsAtCurrentLocation.get(robotSide);
      footstep.setPose(tempPose);
      return footstep;
   }

   public double getTransferTime()
   {
      return transferTime.getDoubleValue();
   }

   public double getSwingTime()
   {
      return swingTime.getDoubleValue();
   }

   public double getStepTime()
   {
      return transferTime.getDoubleValue() + swingTime.getDoubleValue();
   }

   private void updateVisualization()
   {
      for (int i = 0; i < upcomingFootsteps.size(); i++)
      {
         if (i < numberOfFootstepsToVisualize)
            upcomingFoostepSide[i].set(upcomingFootsteps.get(i).getRobotSide());
      }

      for (int i = upcomingFootsteps.size(); i < numberOfFootstepsToVisualize; i++)
      {
         upcomingFoostepSide[i].set(null);
      }

      footstepListVisualizer.update(upcomingFootsteps);
   }

   private Footstep createFootstep(FootstepDataControllerCommand footstepData)
   {
      FramePose footstepPose = new FramePose(worldFrame, footstepData.getPosition(), footstepData.getOrientation());
      PoseReferenceFrame footstepPoseFrame = new PoseReferenceFrame("footstepPoseFrame", footstepPose);

      List<Point2d> contactPoints;
      if (footstepData.getPredictedContactPoints().isEmpty())
         contactPoints = null;
      else
      {
         contactPoints = new ArrayList<>();
         for (int i = 0; i < footstepData.getPredictedContactPoints().size(); i++)
            contactPoints.add(footstepData.getPredictedContactPoints().get(i));
      }

      RobotSide robotSide = footstepData.getRobotSide();
      TrajectoryType trajectoryType = footstepData.getTrajectoryType();

      ContactablePlaneBody contactableFoot = contactableFeet.get(robotSide);
      ReferenceFrame soleFrame = contactableFoot.getSoleFrame();
      RigidBody rigidBody = contactableFoot.getRigidBody();

      Footstep footstep = new Footstep(rigidBody, robotSide, soleFrame, footstepPoseFrame, true, contactPoints);

      footstep.trajectoryType = trajectoryType;
      footstep.swingHeight = footstepData.getSwingHeight();
      switch (footstepData.getOrigin())
      {
      case AT_ANKLE_FRAME:
         break;
      case AT_SOLE_FRAME:
         footstep.setSolePose(footstepPose);
         break;
      default:
         throw new RuntimeException("Should not get there.");
      }
      return footstep;
   }
}
