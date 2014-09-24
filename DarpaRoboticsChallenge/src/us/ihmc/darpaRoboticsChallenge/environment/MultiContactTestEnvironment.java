package us.ihmc.darpaRoboticsChallenge.environment;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.utilities.math.geometry.Transform3d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFPerfectSimulatedSensorReader;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonAvatarInterfaces.CommonAvatarEnvironmentInterface;
import us.ihmc.commonWalkingControlModules.referenceFrames.ReferenceFrames;
import us.ihmc.darpaRoboticsChallenge.DRCConfigParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ConvexPolygon2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.ExternalForcePoint;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.util.environments.SelectableObjectListener;
import com.yobotics.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import com.yobotics.simulationconstructionset.util.ground.RotatableConvexPolygonTerrainObject;
import com.yobotics.simulationconstructionset.util.ground.TerrainObject3D;

public class MultiContactTestEnvironment implements CommonAvatarEnvironmentInterface
{
   private final CombinedTerrainObject3D combinedTerrainObject;
   private final RobotSide[] footContactSides;
   private final RobotSide[] handContactSides;

   public MultiContactTestEnvironment(DRCRobotInitialSetup<SDFRobot> robotInitialSetup, DRCRobotModel robotModel,
                                      RobotSide[] footContactSides, RobotSide[] handContactSides, SideDependentList<Transform3d> invisibleContactablePlaneHandContactPointTransforms)
   {
      DRCRobotJointMap jointMap = robotModel.getJointMap();
      SDFRobot robotForEnvironmentSetup = robotModel.createSdfRobot(DRCConfigParameters.USE_COLLISIONS_MESHS_FOR_VISUALIZATION);
      robotInitialSetup.initializeRobot(robotForEnvironmentSetup, jointMap);
      robotForEnvironmentSetup.update();
      FullRobotModel fullRobotModelForEnvironmentSetup = robotModel.createFullRobotModel();

      CommonHumanoidReferenceFrames referenceFramesForEnvironmentSetup = new ReferenceFrames(fullRobotModelForEnvironmentSetup);
      SDFPerfectSimulatedSensorReader sensorReaderAndOutputWriter = new SDFPerfectSimulatedSensorReader(robotForEnvironmentSetup,
                                                                                fullRobotModelForEnvironmentSetup, referenceFramesForEnvironmentSetup);
      sensorReaderAndOutputWriter.read();
      
      this.footContactSides = footContactSides;
      this.handContactSides = handContactSides;

      combinedTerrainObject = createCombinedTerrainObject(referenceFramesForEnvironmentSetup, fullRobotModelForEnvironmentSetup, invisibleContactablePlaneHandContactPointTransforms);
   }

   private CombinedTerrainObject3D createCombinedTerrainObject(CommonHumanoidReferenceFrames referenceFramesForEnvironmentSetup, FullRobotModel fullRobotModel, SideDependentList<Transform3d> invisibleContactablePlaneHandContactPointTransforms)
   {
      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D(getClass().getSimpleName());
      for (RobotSide robotSide : footContactSides)
      {
         ReferenceFrame soleFrame = referenceFramesForEnvironmentSetup.getSoleFrame(robotSide);
         Transform3d transformToWorld = soleFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
         combinedTerrainObject.addTerrainObject(createConvexPolygonTerrainObject(transformToWorld));
      }

      for (RobotSide robotSide : handContactSides)
      {
         ReferenceFrame handFrame = fullRobotModel.getHand(robotSide).getParentJoint().getFrameAfterJoint();
         Transform3d handToWorld = handFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
         Transform3d handContactPlaneToWorld = new Transform3d();
         handContactPlaneToWorld.mul(handToWorld, invisibleContactablePlaneHandContactPointTransforms.get(robotSide));
         combinedTerrainObject.addTerrainObject(createConvexPolygonTerrainObject(handContactPlaneToWorld));
      }

      return combinedTerrainObject;
   }

   private TerrainObject3D createConvexPolygonTerrainObject(Transform3d transformToWorld)
   {
      Matrix3d rotationToWorld = new Matrix3d();
      transformToWorld.get(rotationToWorld);

      Vector3d normal = new Vector3d();
      rotationToWorld.getColumn(2, normal);

      Vector3d centroid = new Vector3d();
      transformToWorld.get(centroid);

      int nPoints = 5;
      double radius = 0.23;
      ConvexPolygon2d convexPolygon = createContactPolygon(centroid, nPoints, radius);

      TerrainObject3D contact = new RotatableConvexPolygonTerrainObject(normal, convexPolygon, centroid.getZ(), YoAppearance.DarkGray());

      return contact;
   }

   private ConvexPolygon2d createContactPolygon(Vector3d centroid, int nPoints, double radius)
   {
      List<Point2d> pointList = new ArrayList<Point2d>();
      double angleIncrement = 2.0 * Math.PI / ((double) nPoints);
      for (int i = 0; i < nPoints; i++)
      {
         double angle = i * angleIncrement;
         double x = radius * Math.cos(angle) + centroid.getX();
         double y = radius * Math.sin(angle) + centroid.getY();
         pointList.add(new Point2d(x, y));
      }

      ConvexPolygon2d convexPolygon = new ConvexPolygon2d(pointList);

      return convexPolygon;
   }
   
   public TerrainObject3D getTerrainObject3D()
   {
      return combinedTerrainObject;
   }

   public List<Robot> getEnvironmentRobots()
   {
      return new ArrayList<Robot>();
   }

   public void addContactPoints(ExternalForcePoint[] contactPoints)
   {
      // empty
   }

   public void createAndSetContactControllerToARobot()
   {
      // empty
   }

   public void addSelectableListenerToSelectables(SelectableObjectListener selectedListener)
   {
      // empty
   }

}
