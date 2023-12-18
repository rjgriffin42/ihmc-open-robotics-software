package us.ihmc.behaviors.simulation;

import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.yawPitchRoll.YawPitchRoll;
import us.ihmc.perception.sceneGraph.multiBodies.door.DoorDefinition;
import us.ihmc.perception.sceneGraph.rigidBody.RigidBodySceneObjectDefinitions;
import us.ihmc.scs2.simulation.robot.Robot;

import java.util.function.Function;

/**
 * These are preplaced objects used for behaviors, manipulation,
 * navigation simulations with {@link RDXSCS2SimulationSession}.
 *
 * We return Function<ReferenceFrame, Robot> because we don't know the
 * inertial frame of the Session required to make the Robot.
 *
 * TODO: This is a very early version of this. If you see an improvement,
 *   like better parameterizing initial state, that's good, let's do it.
 *   Eventually we should make an SCS 2 scene editor.
 */
public class SimulationSceneObjectRobotBuilders
{
   public static final double SPACE_TO_ALLOW_IT_TO_FALL_ONTO_SURFACE = 0.01;
   public static final double TABLE_X = 0.6;
   public static final double TABLE_Y = -0.25;
   public static final double TABLE_Z = TableDefinition.TABLE_LEG_LENGTH + SPACE_TO_ALLOW_IT_TO_FALL_ONTO_SURFACE;
   public static final double TABLE_SURFACE_Z = TableDefinition.TABLE_LEG_LENGTH + TableDefinition.TABLE_THICKNESS;

   public static Function<ReferenceFrame, Robot> getPushDoorBuilder()
   {
      return inertialFrame ->
      {
         DoorDefinition doorDefinition = getDoorWithArUcoMarkersDefinition();
         // Rotate the door so the push side is facing
         doorDefinition.getInitialSixDoFState().setConfiguration(new YawPitchRoll(Math.PI, 0.0, 0.0), new Point3D(1.3, 0.5, 0.01));
         Robot robot = new Robot(doorDefinition, inertialFrame);
         DoorDefinition.applyPDController(robot);
         return robot;
      };
   }

   public static Function<ReferenceFrame, Robot> getPullDoorBuilder()
   {
      return inertialFrame ->
      {
         DoorDefinition doorDefinition = getDoorWithArUcoMarkersDefinition();
         doorDefinition.getInitialSixDoFState().setConfiguration(new YawPitchRoll(0.0, 0.0, 0.0), new Point3D(1.0, -0.5, 0.01));
         Robot robot = new Robot(doorDefinition, inertialFrame);
         DoorDefinition.applyPDController(robot);
         return robot;
      };
   }

   private static DoorDefinition getDoorWithArUcoMarkersDefinition()
   {
      DoorDefinition doorDefinition = new DoorDefinition();
      doorDefinition.getDoorPanelDefinition().setAddArUcoMarkers(true);
      doorDefinition.build();
      // doorDefinition.getInitialHingeState().setEffort(15.0);
      return doorDefinition;
   }

   public static Function<ReferenceFrame, Robot> getTableBuilder()
   {
      return inertialFrame ->
      {
         TableDefinition tableDefinition = new TableDefinition();
         tableDefinition.setAddArUcoMarkers(true);
         tableDefinition.build();
         tableDefinition.getInitialSixDoFState().setConfiguration(new YawPitchRoll(0.0, 0.0, 0.0), new Point3D(TABLE_X, TABLE_Y, TABLE_Z));
         Robot robot = new Robot(tableDefinition, inertialFrame);
         return robot;
      };
   }

   public static Function<ReferenceFrame, Robot> getCanOfSoupOnTableBuilder()
   {
      return inertialFrame ->
      {
         CanOfSoupDefinition canOfSoupDefinition = new CanOfSoupDefinition();
         canOfSoupDefinition.build();
         canOfSoupDefinition.getInitialSixDoFState().setConfiguration(new YawPitchRoll(0.0, 0.0, 0.0),
            new Point3D(TABLE_X + TableDefinition.TABLE_ARUCO_MARKER_FROM_LEFT_EDGE + RigidBodySceneObjectDefinitions.MARKER_TO_CAN_OF_SOUP_X,
                        TABLE_Y + TableDefinition.TABLE_ARUCO_MARKER_FROM_FRONT_EDGE,
                        TABLE_SURFACE_Z + SPACE_TO_ALLOW_IT_TO_FALL_ONTO_SURFACE));
         Robot robot = new Robot(canOfSoupDefinition, inertialFrame);
         return robot;
      };
   }

   public static Function<ReferenceFrame, Robot> getBoxBuilder(double mass, Vector3D size)
   {
      return inertialFrame ->
      {
         BoxDefinition boxDefinition = new BoxDefinition();
         boxDefinition.build(mass, size);
         boxDefinition.getInitialSixDoFState().setConfiguration(new YawPitchRoll(0.0, 0.0, 0.0),
                                                                new Point3D(TABLE_X + 0.15,
                                                                            TABLE_Y + 0.25,
                                                                            TABLE_SURFACE_Z + SPACE_TO_ALLOW_IT_TO_FALL_ONTO_SURFACE));
         boxDefinition.getInitialSixDoFState().setVelocity(new Vector3D(), new Vector3D());
         return new Robot(boxDefinition, inertialFrame);
      };
   }
}
