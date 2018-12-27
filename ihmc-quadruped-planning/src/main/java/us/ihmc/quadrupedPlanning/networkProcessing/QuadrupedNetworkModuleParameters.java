package us.ihmc.quadrupedPlanning.networkProcessing;

import java.net.URI;
import java.net.URISyntaxException;

public class QuadrupedNetworkModuleParameters
{
   private boolean useNetworkProcessor = true;
   private boolean useRobotEnvironmentAwarenessModule;
   private boolean useBodyHeightTeleopModule;
   private boolean useStepTeleopModule;
   private boolean visualizeStepTeleopModule;
   private boolean useRemoteObjectDetectionFeedback;

   private URI rosUri;

   public void enableRobotEnvironmentAwerenessModule(boolean enable)
   {
      this.useRobotEnvironmentAwarenessModule = enable;
   }

   public boolean isRobotEnvironmentAwerenessModuleEnabled()
   {
      return useRobotEnvironmentAwarenessModule;
   }

   public void enableBodyHeightTeleopModule(boolean enable)
   {
      this.useBodyHeightTeleopModule = enable;
   }

   public boolean isBodyHeightTeleopModuleEnabled()
   {
      return useBodyHeightTeleopModule;
   }

   public void enableStepTeleopModule(boolean enable)
   {
      this.useStepTeleopModule = enable;
   }

   public boolean isStepTeleopModuleEnabled()
   {
      return useStepTeleopModule;
   }

   public void setVisualizeStepTeleopModule(boolean visualize)
   {
      this.visualizeStepTeleopModule = visualize;
   }

   public boolean visualizeStepTeleopModuleEnabled()
   {
      return visualizeStepTeleopModule;
   }

   public void enableNetworkProcessor(boolean useNetworkProcessor)
   {
      this.useNetworkProcessor = useNetworkProcessor;
   }

   public boolean isNetworkProcessorEnabled()
   {
      return useNetworkProcessor;
   }

   public boolean isRemoteObjectDetectionFeedbackEnabled()
   {
      return useRemoteObjectDetectionFeedback;
   }

   public void setUseRemoteObjectDetectionFeedbackEnabled(boolean useRemoteObjectDetectionFeedback)
   {
      this.useRemoteObjectDetectionFeedback = useRemoteObjectDetectionFeedback;
   }


   @Override
   public String toString()
   {
      return "QuadrupedNetworkModuleParameters [useRobotEnvironmentAwarenessModule=" + useRobotEnvironmentAwarenessModule;
   }
}
