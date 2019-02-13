package us.ihmc.quadrupedFootstepPlanning.ui.viewers;

import javafx.animation.AnimationTimer;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import us.ihmc.euclid.orientation.interfaces.Orientation3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettings;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettingsReadOnly;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;

import java.util.concurrent.atomic.AtomicReference;

public class StartGoalPositionViewer extends AnimationTimer
{
   public static final double RADIUS = 0.05;

   private final Group root = new Group();
   private final ObservableList<Node> rootChildren = root.getChildren();

   private final Sphere startSphere = new Sphere(RADIUS);
   private final Sphere goalSphere = new Sphere(RADIUS);
   private final QuadrantDependentList<Sphere> startFeetSpheres = new QuadrantDependentList<>();
   private final QuadrantDependentList<Sphere> goalFeetSpheres = new QuadrantDependentList<>();
   private final Sphere lowLevelGoalSphere = new Sphere(RADIUS);
   private boolean isStartCurrentlyShown;
   private boolean isGoalCurrentlyShown;
   private boolean isIntermediateGoalCurrentlyShown;

   public static final PhongMaterial startOpaqueMaterial = new PhongMaterial(Color.GREEN);
   public static final PhongMaterial startTransparentMaterial = new PhongMaterial(toTransparentColor(Color.ORANGE, 0.7));
   public static final PhongMaterial goalOpaqueMaterial = new PhongMaterial(Color.RED);
   public static final PhongMaterial goalTransparentMaterial = new PhongMaterial(toTransparentColor(Color.ORANGE, 0.7));
   public static final PhongMaterial lowLevelGoalOpaqueMaterial = new PhongMaterial(Color.DARKRED);

   private AtomicReference<Boolean> showStartPosition = null;
   private AtomicReference<Boolean> showGoalPosition = null;
   private AtomicReference<Boolean> showLowLevelGoalPosition = null;
   private AtomicReference<Boolean> startEditModeEnabled = null;
   private AtomicReference<Boolean> goalEditModeEnabled = null;
   private AtomicReference<Point3D> startPositionReference = null;
   private AtomicReference<Quaternion> startOrientationReference = null;
   private AtomicReference<Point3D> goalPositionReference = null;
   private AtomicReference<Quaternion> goalOrientationReference = null;
   private AtomicReference<Point3D> lowLevelGoalPositionReference = null;
   private final AtomicReference<QuadrupedXGaitSettingsReadOnly> xGaitSettingsReference = new AtomicReference<>(new QuadrupedXGaitSettings());

   private final Messager messager;

   public StartGoalPositionViewer(Messager messager)
   {
      this.messager = messager;

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         Sphere startFootSphere = new Sphere(0.5 * RADIUS);
         Sphere goalFootSphere = new Sphere(0.5 * RADIUS);
         startFootSphere.setMouseTransparent(true);
         goalFootSphere.setMouseTransparent(true);

         startFeetSpheres.put(robotQuadrant, startFootSphere);
         goalFeetSpheres.put(robotQuadrant, goalFootSphere);
      }

      startSphere.setMouseTransparent(true);
      goalSphere.setMouseTransparent(true);
      lowLevelGoalSphere.setMouseTransparent(true);

      showStart(true);
      showGoal(true);
      showLowLevelGoal(true);
   }

   public StartGoalPositionViewer(Messager messager, Topic<Boolean> startEditModeEnabledTopic, Topic<Boolean> goalEditModeEnabledTopic,
                                  Topic<Point3D> startPositionTopic, Topic<Quaternion> startOrientationTopic, Topic<Point3D> lowLevelGoalPositionTopic,
                                  Topic<Point3D> goalPositionTopic, Topic<Quaternion> goalOrientationTopic,
                                  Topic<QuadrupedXGaitSettingsReadOnly> xGaitSettingsTopic)
   {
      this(messager);

      setEditStartGoalTopics(startEditModeEnabledTopic, goalEditModeEnabledTopic);
      setPositionStartGoalTopics(startPositionTopic, startOrientationTopic, lowLevelGoalPositionTopic, goalPositionTopic, goalOrientationTopic);
      setXGaitSettingsTopic(xGaitSettingsTopic);
   }

   public void setPositionStartGoalTopics(Topic<Point3D> startPositionTopic, Topic<Quaternion> startOrientationTopic, Topic<Point3D> lowLevelGoalPositionTopic,
                                          Topic<Point3D> goalPositionTopic, Topic<Quaternion> goalOrientationTopic)
   {
      startPositionReference = messager.createInput(startPositionTopic, new Point3D());
      startOrientationReference = messager.createInput(startOrientationTopic, new Quaternion());
      lowLevelGoalPositionReference = messager.createInput(lowLevelGoalPositionTopic, new Point3D());
      goalPositionReference = messager.createInput(goalPositionTopic, new Point3D());
      goalOrientationReference = messager.createInput(goalOrientationTopic, new Quaternion());
   }

   // TODO
   public void setEditStartGoalTopics(Topic<Boolean> startEditModeEnabledTopic, Topic<Boolean> goalEditModeEnabledTopic)
   {
      startEditModeEnabled = messager.createInput(startEditModeEnabledTopic, false);
      goalEditModeEnabled = messager.createInput(goalEditModeEnabledTopic, false);
   }

   public void setShowStartGoalTopics(Topic<Boolean> showStartTopic, Topic<Boolean> showLowLevelGoalTopic, Topic<Boolean> showGoalTopic)
   {
      showStartPosition = messager.createInput(showStartTopic, true);
      showLowLevelGoalPosition = messager.createInput(showLowLevelGoalTopic, true);
      showGoalPosition = messager.createInput(showGoalTopic, true);
   }

   public void setXGaitSettingsTopic(Topic<QuadrupedXGaitSettingsReadOnly> xGaitSettingsTopic)
   {
      messager.registerTopicListener(xGaitSettingsTopic, this::handleXGaitSettings);
   }

   private void handleXGaitSettings(QuadrupedXGaitSettingsReadOnly xGaitSettings)
   {
      xGaitSettingsReference.set(xGaitSettings);
   }

   @Override
   public void handle(long now)
   {
      if (showStartPosition != null)
         showStart(showStartPosition.get());

      if (showGoalPosition != null)
         showGoal(showGoalPosition.get());

      if (showLowLevelGoalPosition != null)
         showLowLevelGoal(showLowLevelGoalPosition.get());

      if (startEditModeEnabled != null && startEditModeEnabled.get())
      {
         startSphere.setMaterial(startTransparentMaterial);
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
            startFeetSpheres.get(robotQuadrant).setMaterial(startTransparentMaterial);
      }
      else
      {
         startSphere.setMaterial(startOpaqueMaterial);
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
            startFeetSpheres.get(robotQuadrant).setMaterial(startOpaqueMaterial);
      }

      if (startPositionReference != null)
      {
         Point3D startPosition = startPositionReference.get();
         if (startPosition != null)
         {
            setStartPosition(startPosition, startOrientationReference.get());
         }
      }

      if (goalEditModeEnabled != null && goalEditModeEnabled.get())
      {
         goalSphere.setMaterial(goalTransparentMaterial);
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
            goalFeetSpheres.get(robotQuadrant).setMaterial(goalTransparentMaterial);
      }
      else
      {
         goalSphere.setMaterial(goalOpaqueMaterial);
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
            goalFeetSpheres.get(robotQuadrant).setMaterial(goalOpaqueMaterial);
      }

      if (goalPositionReference != null)
      {
         Point3D goalPosition = goalPositionReference.get();
         if (goalPosition != null)
         {
            setGoalPosition(goalPosition, goalOrientationReference.get());
         }
      }

      lowLevelGoalSphere.setMaterial(lowLevelGoalOpaqueMaterial);

      if (lowLevelGoalPositionReference != null)
      {
         Point3D goalPosition = lowLevelGoalPositionReference.get();
         if (goalPosition != null)
         {
            setIntermediateGoalPosition(goalPosition);
         }
      }
   }

   private void setStartPosition(Point3DReadOnly position, QuaternionReadOnly orientation)
   {
      startSphere.setTranslateX(position.getX());
      startSphere.setTranslateY(position.getY());
      startSphere.setTranslateZ(position.getZ());

      PoseReferenceFrame xGaitFrame = new PoseReferenceFrame("xGaitFrame", ReferenceFrame.getWorldFrame());
      xGaitFrame.setPoseAndUpdate(position, orientation);

      double xOffset = 0.5 * xGaitSettingsReference.get().getStanceLength();
      double yOffset = 0.5 * xGaitSettingsReference.get().getStanceWidth();

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         FramePoint3D footPosition = new FramePoint3D(xGaitFrame, robotQuadrant.getEnd().negateIfHindEnd(xOffset),
                                                      robotQuadrant.getSide().negateIfRightSide(yOffset), 0.0);
         footPosition.changeFrame(ReferenceFrame.getWorldFrame());

         startFeetSpheres.get(robotQuadrant).setTranslateX(footPosition.getX());
         startFeetSpheres.get(robotQuadrant).setTranslateY(footPosition.getY());
         startFeetSpheres.get(robotQuadrant).setTranslateZ(footPosition.getZ());
      }
   }

   private void setGoalPosition(Point3DReadOnly position, QuaternionReadOnly orientation)
   {
      goalSphere.setTranslateX(position.getX());
      goalSphere.setTranslateY(position.getY());
      goalSphere.setTranslateZ(position.getZ());

      PoseReferenceFrame xGaitFrame = new PoseReferenceFrame("xGaitFrame", ReferenceFrame.getWorldFrame());
      xGaitFrame.setPoseAndUpdate(position, orientation);

      double xOffset = 0.5 * xGaitSettingsReference.get().getStanceLength();
      double yOffset = 0.5 * xGaitSettingsReference.get().getStanceWidth();

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         FramePoint3D footPosition = new FramePoint3D(xGaitFrame, robotQuadrant.getEnd().negateIfHindEnd(xOffset),
                                                      robotQuadrant.getSide().negateIfRightSide(yOffset), 0.0);
         footPosition.changeFrame(ReferenceFrame.getWorldFrame());

         goalFeetSpheres.get(robotQuadrant).setTranslateX(footPosition.getX());
         goalFeetSpheres.get(robotQuadrant).setTranslateY(footPosition.getY());
         goalFeetSpheres.get(robotQuadrant).setTranslateZ(footPosition.getZ());
      }
   }

   private void setIntermediateGoalPosition(Point3D position)
   {
      lowLevelGoalSphere.setTranslateX(position.getX());
      lowLevelGoalSphere.setTranslateY(position.getY());
      lowLevelGoalSphere.setTranslateZ(position.getZ());
   }

   private void showStart(boolean show)
   {
      if (show)
      {
         if (!isStartCurrentlyShown)
         {
            rootChildren.add(startSphere);
            for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
               rootChildren.add(startFeetSpheres.get(robotQuadrant));
         }
      }
      else
      {
         if (isStartCurrentlyShown)
         {
            rootChildren.remove(startSphere);
            for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
               rootChildren.remove(startFeetSpheres.get(robotQuadrant));
         }
      }
      isStartCurrentlyShown = show;
   }

   private void showGoal(boolean show)
   {
      if (show)
      {
         if (!isGoalCurrentlyShown)
         {
            rootChildren.add(goalSphere);
            for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
               rootChildren.add(goalFeetSpheres.get(robotQuadrant));
         }
      }
      else
      {
         if (isGoalCurrentlyShown)
         {
            rootChildren.remove(goalSphere);
            for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
               rootChildren.remove(goalFeetSpheres.get(robotQuadrant));
         }
      }
      isGoalCurrentlyShown = show;
   }

   private void showLowLevelGoal(boolean show)
   {
      if (show)
      {
         if (!isIntermediateGoalCurrentlyShown)
            rootChildren.add(lowLevelGoalSphere);
      }
      else
      {
         if (isIntermediateGoalCurrentlyShown)
            rootChildren.remove(lowLevelGoalSphere);
      }
      isIntermediateGoalCurrentlyShown = show;
   }

   public static Color toTransparentColor(Color opaqueColor, double opacity)
   {
      double red = opaqueColor.getRed();
      double green = opaqueColor.getGreen();
      double blue = opaqueColor.getBlue();
      return new Color(red, green, blue, opacity);
   }

   public Node getRoot()
   {
      return root;
   }
}
