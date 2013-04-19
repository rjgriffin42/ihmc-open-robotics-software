package point_cloud_server;

public interface StoreCloudAction extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "point_cloud_server/StoreCloudAction";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n\nStoreCloudActionGoal action_goal\nStoreCloudActionResult action_result\nStoreCloudActionFeedback action_feedback\n";
  point_cloud_server.StoreCloudActionGoal getActionGoal();
  void setActionGoal(point_cloud_server.StoreCloudActionGoal value);
  point_cloud_server.StoreCloudActionResult getActionResult();
  void setActionResult(point_cloud_server.StoreCloudActionResult value);
  point_cloud_server.StoreCloudActionFeedback getActionFeedback();
  void setActionFeedback(point_cloud_server.StoreCloudActionFeedback value);
}
