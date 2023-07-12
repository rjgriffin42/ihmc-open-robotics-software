package us.ihmc.perception.sceneGraph.multiBodies.door;

/**
 * The parameters for a door.
 * TODO: Currently this is the simulation door.
 *   - Turn into a stored property set to represent both
 *     simulation and real lab door parameters
 *
 * Remeasured by dcalvert on 7/11/23:
 * Push door (real):
 * Thickness - 3.4 cm
 * Lever axis inset - 6.2 cm
 * Lever axis height - 91.5 cm
 * 91.4 cm panel width
 * 203.3 cm panel height
 * 104.4 marker z from bottom of panel
 * 12.7 cm lever below marker
 * 8.85 cm lever right of marker
 * 5 cm lever away from panel
 * 9 cm lever length
 */
public class DoorModelParameters
{
   /* These measurements from the simulation door model, measured in Blender. */
   /** The thickness of the door panel. */
   public static final double DOOR_PANEL_THICKNESS = 0.034;
   /** The vertical length of the panel. */
   public static final double DOOR_PANEL_HEIGHT = 2.033;
   /** The horizontal length of the panel. */
   public static final double DOOR_PANEL_WIDTH = 0.914;
   /** Distance the handle joint in from the edge of the panel. */
   public static final double DOOR_LEVER_HANDLE_INSET = 0.062;
   /** We place the lever handle up from the bottom of the panel as measured on our lab door. */
   public static final double DOOR_LEVER_HANDLE_FROM_BOTTOM_OF_PANEL = 0.915;
   /** Mount the panel up off the ground a little so it's not dragging. */
   public static final double DOOR_PANEL_GROUND_GAP_HEIGHT = 0.02;
   /** Place the panel away from the hinge a little.
    *  TODO: Rethink this, it's not very realistic. */
   public static final double DOOR_PANEL_HINGE_OFFSET = 0.005;
   /** Distance from the frame post to the frame model's origin. */
   public static final double DOOR_FRAME_HINGE_OFFSET = 0.006;

   /**
    * It is actually important to measure the ArUco marker pose relative to the lever handle,
    * as that's what's most important to get right.
    */
   public static final double PUSH_SIDE_ARUCO_MARKER_TO_LEVER_AXIS_Z = 0.127;
   public static final double PUSH_SIDE_ARUCO_MARKER_TO_LEVER_AXIS_Y = 0.0885;

   public static final double PULL_SIDE_ARUCO_MARKER_TO_LEVER_AXIS_Z = 0.127;
   public static final double PULL_SIDE_ARUCO_MARKER_TO_LEVER_AXIS_Y = 0.0885;
}
