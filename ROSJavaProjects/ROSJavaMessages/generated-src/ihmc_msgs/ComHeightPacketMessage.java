package ihmc_msgs;

public interface ComHeightPacketMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/ComHeightPacketMessage";
  static final java.lang.String _DEFINITION = "# ComHeightPacketMessage\n\nfloat64 heightOffset\n\n";
  double getHeightOffset();
  void setHeightOffset(double value);
}
