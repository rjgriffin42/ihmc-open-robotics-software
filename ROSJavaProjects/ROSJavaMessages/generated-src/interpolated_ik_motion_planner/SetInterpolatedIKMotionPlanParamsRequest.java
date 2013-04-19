package interpolated_ik_motion_planner;

public interface SetInterpolatedIKMotionPlanParamsRequest extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "interpolated_ik_motion_planner/SetInterpolatedIKMotionPlanParamsRequest";
  static final java.lang.String _DEFINITION = "#the number of steps to use when interpolating including start and finish \n#(0 means use pos_spacing and rot_spacing to calculate the number of steps, \n#anything ==1 or <0 will become 2, start and finish)\nint32 num_steps\n\n#the max angle distance (in any joint) before we declare that the path \n#is inconsistent\nfloat64 consistent_angle\n\n#how many steps between collision checks \n#(0 or 1 is check every step; 2 is every other, etc.)\nint32 collision_check_resolution\n\n#the number of steps in the plan (starting from the end and going backwards) \n#that can be invalid due to collisions or inconsistency before aborting \n#(0 is abort as soon as you find one, 1 is allow one and still \n#continue, -1 or >=num_steps to never abort early)\nint32 steps_before_abort\n\n#the max translation (m) to move the wrist between waypoints \n#(only used if num_steps is 0)\nfloat64 pos_spacing\n\n#the max rotation (rad) to move the wrist between waypoints \n#(only used if num_steps is 0)\nfloat64 rot_spacing\n\n#if this is 0, collisions won\'t be checked for \n#(returns non-collision aware IK solutions)\nbyte collision_aware\n\n#if this is 1, the planner searches for an IK solution for the end \n#first, then works backwards from there\nbyte start_from_end\n\n#a list of maximum joint velocities to use when computing times and \n#velocities for the joint trajectory (defaults to [.2]*7 if left empty)\nfloat64[] max_joint_vels\n\n#a list of maximum accelerations to use when computing times and \n#velocities for the joint trajectory (defaults to [.5]*7 if left empty)\nfloat64[] max_joint_accs\n\n";
  int getNumSteps();
  void setNumSteps(int value);
  double getConsistentAngle();
  void setConsistentAngle(double value);
  int getCollisionCheckResolution();
  void setCollisionCheckResolution(int value);
  int getStepsBeforeAbort();
  void setStepsBeforeAbort(int value);
  double getPosSpacing();
  void setPosSpacing(double value);
  double getRotSpacing();
  void setRotSpacing(double value);
  byte getCollisionAware();
  void setCollisionAware(byte value);
  byte getStartFromEnd();
  void setStartFromEnd(byte value);
  double[] getMaxJointVels();
  void setMaxJointVels(double[] value);
  double[] getMaxJointAccs();
  void setMaxJointAccs(double[] value);
}
