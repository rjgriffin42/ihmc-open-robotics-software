package atlas_msgs;

public interface AtlasSimInterfaceState extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "atlas_msgs/AtlasSimInterfaceState";
  static final java.lang.String _DEFINITION = "# For interfacing Boston Dynamics\' AtlasSimInterface Dynamics Behavior Library\n# Feedback from AtlasSimInterface Controller after calling process_control_input\n# This ROS message should track AtlasControlOutput struct in\n# AtlasSimInterfaceTypes.h.\n# With the exception of addition of k_effort to provide user a way to switch\n# to/from PID servo control in AtlasPlugin.cpp on a per joint basis.\n\nint32 NO_ERRORS                        =  0    # no error detected\nint32 ERROR_UNSPECIFIED                = -1    # unspecified error\nint32 ERROR_VALUE_OUT_OF_RANGE         = -2    # passed value is out of range\nint32 ERROR_INVALID_INDEX              = -3    # passed index is invalid (too low or too high)\nint32 ERROR_FAILED_TO_START_BEHAVIOR   = -4    # robot failed to start desired behavior\nint32 ERROR_NO_ACTIVE_BEHAVIOR         = -5    # robot has no active behavior\nint32 ERROR_NO_SUCH_BEHAVIOR           = -6    # behavior doesn\'t exist\nint32 ERROR_BEHAVIOR_NOT_IMPLEMENTED   = -7    # behavior exists but not implemented\nint32 ERROR_TIME_RAN_BACKWARD          = -8    # a time earlier than previous times was given\n\nint32 error_code                         # error code returned by\n                                         # process_control_input.\n                                         # See AtlasSimInterfaceTypes.h\n                                         # AtlasErrorCode for list of enums.\n                                         # The list is mimic\'d here above.\n\nint32 current_behavior                   # current active behavior.\nint32 desired_behavior                   # desired behavior specified by usesr\n                                         # input. This may lag from\n                                         # current_behavior by a few simulation\n                                         # steps.\n\n# below are information from AtlasControlOutput in AtlasSimInterfaceTypes.h\n\nfloat64[28] f_out                        # torque command from BDI controller.\n\natlas_msgs/AtlasPositionData pos_est     # Position and velocity estimate of robot pelvis\n\ngeometry_msgs/Pose[2] foot_pos_est      # World position estimate for feet\n                                         # 0 - left, 1 - right\n\natlas_msgs/AtlasBehaviorFeedback behavior_feedback\n\n# additional vector for transitioning from servo model in AtlasPlugin\n# to BDI servo.\n\nuint8[] k_effort       # k_effort can be an unsigned int 8value from 0 to 255, \n                       # at run time, a double between 0 and 1 is obtained\n                       # by dividing by 255.0d.\n\n";
  static final int NO_ERRORS = 0;
  static final int ERROR_UNSPECIFIED = -1;
  static final int ERROR_VALUE_OUT_OF_RANGE = -2;
  static final int ERROR_INVALID_INDEX = -3;
  static final int ERROR_FAILED_TO_START_BEHAVIOR = -4;
  static final int ERROR_NO_ACTIVE_BEHAVIOR = -5;
  static final int ERROR_NO_SUCH_BEHAVIOR = -6;
  static final int ERROR_BEHAVIOR_NOT_IMPLEMENTED = -7;
  static final int ERROR_TIME_RAN_BACKWARD = -8;
  int getErrorCode();
  void setErrorCode(int value);
  int getCurrentBehavior();
  void setCurrentBehavior(int value);
  int getDesiredBehavior();
  void setDesiredBehavior(int value);
  double[] getFOut();
  void setFOut(double[] value);
  atlas_msgs.AtlasPositionData getPosEst();
  void setPosEst(atlas_msgs.AtlasPositionData value);
  java.util.List<geometry_msgs.Pose> getFootPosEst();
  void setFootPosEst(java.util.List<geometry_msgs.Pose> value);
  atlas_msgs.AtlasBehaviorFeedback getBehaviorFeedback();
  void setBehaviorFeedback(atlas_msgs.AtlasBehaviorFeedback value);
  org.jboss.netty.buffer.ChannelBuffer getKEffort();
  void setKEffort(org.jboss.netty.buffer.ChannelBuffer value);
}
