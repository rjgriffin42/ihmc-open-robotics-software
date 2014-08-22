package us.ihmc.darpaRoboticsChallenge.sensors.multisense;

import java.io.File;
import java.io.IOException;

import org.ros.exception.RemoteException;
import org.ros.node.NodeConfiguration;
import org.ros.node.parameter.ParameterListener;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceResponseListener;

import us.ihmc.darpaRoboticsChallenge.networkProcessor.ros.RosNativeNetworkProcessor;
import us.ihmc.darpaRoboticsChallenge.networking.DRCNetworkProcessorNetworkingManager;
import us.ihmc.darpaRoboticsChallenge.networking.dataProducers.MultisenseParameterPacket;
import us.ihmc.utilities.processManagement.ProcessStreamGobbler;
import us.ihmc.utilities.ros.RosDoublePublisher;
import us.ihmc.utilities.ros.RosMainNode;
import us.ihmc.utilities.ros.RosServiceClient;
import dynamic_reconfigure.BoolParameter;
import dynamic_reconfigure.DoubleParameter;
import dynamic_reconfigure.Reconfigure;
import dynamic_reconfigure.ReconfigureRequest;
import dynamic_reconfigure.ReconfigureResponse;
import dynamic_reconfigure.StrParameter;

public class MultiSenseParamaterSetter
{
   private static double gain;
   private static double motorSpeed;
   private static boolean ledEnable;
   private static boolean flashEnable;
   private static double dutyCycle;
   private static boolean autoExposure;
   private static boolean autoWhitebalance;
   private static String resolution = new String("1024x544x128");
   private final RosServiceClient<ReconfigureRequest, ReconfigureResponse> multiSenseClient;
   private RosMainNode rosMainNode;
   private DRCNetworkProcessorNetworkingManager networkingManager;
   private ParameterTree params;
   
   public MultiSenseParamaterSetter(RosMainNode rosMainNode, DRCNetworkProcessorNetworkingManager networkingManager)
   {
      this.rosMainNode = rosMainNode;
      this.networkingManager = networkingManager;
      multiSenseClient = new RosServiceClient<ReconfigureRequest, ReconfigureResponse>(Reconfigure._TYPE);      
      rosMainNode.attachServiceClient("multisense/set_parameters", multiSenseClient);
   }

   public MultiSenseParamaterSetter(RosMainNode rosMainNode2)
   {
     this.rosMainNode = rosMainNode2;
     multiSenseClient = new RosServiceClient<ReconfigureRequest, ReconfigureResponse>(Reconfigure._TYPE);    
   }

   public void setupNativeROSCommunicator(RosNativeNetworkProcessor rosNativeNetworkProcessor, double lidarSpindleVelocity)
   {
      String rosPrefix = "/opt/ros";
      if (useRosHydro(rosPrefix))
      {
         System.out.println("using hydro");
         String[] hydroSpindleSpeedShellString = {"sh", "-c",
               ". /opt/ros/hydro/setup.sh; rosrun dynamic_reconfigure dynparam set /multisense motor_speed " + lidarSpindleVelocity
                     + "; rosrun dynamic_reconfigure dynparam set /multisense network_time_sync true"};
         shellOutSpindleSpeedCommand(hydroSpindleSpeedShellString);
      }
      else if (useRosGroovy(rosPrefix))
      {
         System.out.println("using groovy");
         String[] groovySpindleSpeedShellString = {"sh", "-c",
               ". /opt/ros/groovy/setup.sh; rosrun dynamic_reconfigure dynparam set /multisense motor_speed " + lidarSpindleVelocity
               + "; rosrun dynamic_reconfigure dynparam set /multisense network_time_sync true"};
         shellOutSpindleSpeedCommand(groovySpindleSpeedShellString);
      }
      else if (useRosFuerte(rosPrefix))
      {
         System.out.println("using fuerte");
         String[] fuerteSpindleSpeedShellString = {"sh", "-c",
               ". /opt/ros/fuerte/setup.sh; rosrun dynamic_reconfigure dynparam set /multisense motor_speed " + lidarSpindleVelocity
               + "; rosrun dynamic_reconfigure dynparam set /multisense network_time_sync true"};
         shellOutSpindleSpeedCommand(fuerteSpindleSpeedShellString);
      }
   }
   
   private void shellOutSpindleSpeedCommand(String[] shellCommandString)
   {
      ProcessBuilder builder = new ProcessBuilder(shellCommandString);
      try
      {
         Process p = builder.start();
         System.out.println("Process started.");
         new ProcessStreamGobbler("ROS Shellout", p.getErrorStream(), System.err).start();
         new ProcessStreamGobbler("ROS Shellout", p.getInputStream(), System.out).start();
         p.waitFor();
         System.out.println("Process done.");
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }
   }

   private  boolean useRosFuerte(String rosPrefix)
   {
      return new File(rosPrefix + "/fuerte").exists();
   }

   private  boolean useRosGroovy(String rosPrefix)
   {
      return new File(rosPrefix + "/groovy").exists();
   }

   private  boolean useRosHydro(String rosPrefix)
   {
      return new File(rosPrefix + "/hydro").exists();
   }

   public  void setupMultisenseSpindleSpeedPublisher(RosMainNode rosMainNode, final double lidarSpindleVelocity)
   {
      final RosDoublePublisher rosDoublePublisher = new RosDoublePublisher(true)
      {
         @Override
         public void connected()
         {
            publish(lidarSpindleVelocity);
         }
      };
      rosMainNode.attachPublisher("/multisense/set_spindle_speed", rosDoublePublisher);
   }
  
   public void handleMultisenseParameters(MultisenseParameterPacket object)
   {
      if (object.isFromUI())
      {
         if (rosMainNode.isStarted())
         {
            params = rosMainNode.getParameters();
            send();

         }
      }
      else
         setMultisenseParameters(object);

   }
   
   
   private void send()
   {
      if (params == null)
      {
         //System.out.println("params are null");
         return;
      }

      networkingManager.getControllerStateHandler().sendSerializableObject(
            new MultisenseParameterPacket(false, params.getDouble("/multisense/gain"), params.getDouble("/multisense/motor_speed"), params
                  .getDouble("/multisense/led_duty_cycle"), params.getString("/multisense/resolution"), params.getBoolean("/multisense/lighting"), params
                  .getBoolean("/multisense/flash"), params.getBoolean("multisense/auto_exposure"), params.getBoolean("multisense/auto_white_balance")));
   }
   
   
   public void initializeParameterListeners()
   {

      //System.out.println("------------initialise blackfly parameteres--------------");

      rosMainNode.attachParameterListener("/multisense/motor_speed", new ParameterListener()
      {

         @Override
         public void onNewValue(Object value)
         {
            //System.out.println("new motor speed received");
            send();
         }
      });
      rosMainNode.attachParameterListener("/multisense/gain", new ParameterListener()
      {

         @Override
         public void onNewValue(Object value)
         {
            //System.out.println("new gain received");
            send();
         }
      });

      rosMainNode.attachParameterListener("/multisense/led_duty_cycle", new ParameterListener()
      {

         @Override
         public void onNewValue(Object value)
         {
            //System.out.println("new dutyCycle received");
            send();
         }
      });
      rosMainNode.attachParameterListener("/multisense/lighting", new ParameterListener()
      {

         @Override
         public void onNewValue(Object value)
         {
            //System.out.println("new led received");
            send();
         }
      });
      rosMainNode.attachParameterListener("/multisense/flash", new ParameterListener()
      {

         @Override
         public void onNewValue(Object value)
         {
           // System.out.println("new flash received");
            send();
         }
      });

      rosMainNode.attachParameterListener("/multisense/auto_exposure", new ParameterListener()
      {

         @Override
         public void onNewValue(Object value)
         {
            //System.out.println("new auto expo received");
            send();
         }
      });

      rosMainNode.attachParameterListener("/multisense/motor_speed", new ParameterListener()
      {

         @Override
         public void onNewValue(Object value)
         {
            //System.out.println("new motor speed received");
            send();
         }
      });
      
      rosMainNode.attachParameterListener("/multisense/auto_white_balance", new ParameterListener()
      {

         @Override
         public void onNewValue(Object value)
         {
            //System.out.println("new auto white balance received");
            send();
         }
      });

   }

  public  void setMultisenseResolution(RosMainNode rosMainNode)
   {
      try
      {
         
         Thread setupThread = new Thread()
         {
            public void run()
            {
               multiSenseClient.waitTillConnected();
               ReconfigureRequest request = multiSenseClient.getMessage();
               StrParameter resolutionParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(StrParameter._TYPE);
               resolutionParam.setName("resolution");
               //resolutionParam.setValue("2048x1088x64");
               //System.out.println("Setting multisense resolution to 2048x1088");
               System.out.println("Setting multisense resolution to 1024x544x128");
               resolutionParam.setValue("1024x544x128");
               request.getConfig().getStrs().add(resolutionParam);

               DoubleParameter fpsParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(DoubleParameter._TYPE);
               fpsParam.setName("fps");
               fpsParam.setValue(30.0);
               request.getConfig().getDoubles().add(fpsParam);
               
               
               DoubleParameter gainParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(DoubleParameter._TYPE);
               gainParam.setName("gain");
               gainParam.setValue(3.2);
               request.getConfig().getDoubles().add(gainParam);
               
               
               multiSenseClient.call(request, new ServiceResponseListener<ReconfigureResponse>()
               {

                  public void onSuccess(ReconfigureResponse response)
                  {
                     System.out.println("Set resolution to " + response.getConfig().getStrs().get(0).getValue());
                  }

                  public void onFailure(RemoteException e)
                  {
                     e.printStackTrace();
                  }
               });
            }
         };

         setupThread.start();

      }
      catch (Exception e)
      {
         System.err.println(e.getMessage());
      }
   }
   
   

   public void setMultisenseParameters(MultisenseParameterPacket object)
   {
     
     // System.out.println("object received with gain "+ object.getGain()+" speed "+ object.getMotorSpeed()+" dutycycle"+object.getDutyCycle()+" resolution"+ object.getResolution());
     
      
      
      multiSenseClient.waitTillConnected();
      ReconfigureRequest request = multiSenseClient.getMessage();
      if(object.getGain() != gain){
      gain = object.getGain();
      DoubleParameter gainParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(DoubleParameter._TYPE);
      gainParam.setName("gain");
      gainParam.setValue(gain);
      request.getConfig().getDoubles().add(gainParam);
      }
      
      if(object.getMotorSpeed() != motorSpeed){
      motorSpeed = object.getMotorSpeed();
      DoubleParameter motorSpeedParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(DoubleParameter._TYPE);
      motorSpeedParam.setName("motor_speed");
      motorSpeedParam.setValue(motorSpeed);
      request.getConfig().getDoubles().add(motorSpeedParam);
      }
      
      if(object.getDutyCycle() != dutyCycle){
      dutyCycle = object.getDutyCycle();
      DoubleParameter dutyCycleParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(DoubleParameter._TYPE);
      dutyCycleParam.setName("led_duty_cycle");
      dutyCycleParam.setValue(dutyCycle);
      request.getConfig().getDoubles().add(dutyCycleParam);
      }
      
      if(object.isLedEnable() != ledEnable){
      ledEnable = object.isLedEnable();
      BoolParameter ledEnableParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(BoolParameter._TYPE);
      ledEnableParam.setName("lighting");
      ledEnableParam.setValue(ledEnable);
      request.getConfig().getBools().add(ledEnableParam);
      }
      
      if(object.isFlashEnable() != flashEnable){
      flashEnable = object.isFlashEnable();
      BoolParameter flashEnableParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(BoolParameter._TYPE);
      flashEnableParam.setName("flash");
      flashEnableParam.setValue(flashEnable);
      request.getConfig().getBools().add(flashEnableParam);
      }
      
      if(object.isAutoExposure() != autoExposure){
      autoExposure = object.isAutoExposure();
      BoolParameter autoExposureParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(BoolParameter._TYPE);
      autoExposureParam.setName("auto_exposure");
      autoExposureParam.setValue(autoExposure);
      request.getConfig().getBools().add(autoExposureParam);
      }
      
      if(object.isAutoWhiteBalance() != autoWhitebalance){
         autoWhitebalance = object.isAutoWhiteBalance();
         BoolParameter autoWhiteBalanceParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(BoolParameter._TYPE);
         autoWhiteBalanceParam.setName("auto_white_balance");
         autoWhiteBalanceParam.setValue(autoWhitebalance);
         request.getConfig().getBools().add(autoWhiteBalanceParam);
         }
      
      if(!resolution.equals(object.getResolution())){
      resolution = object.getResolution();
      StrParameter resolutionParam = NodeConfiguration.newPrivate().getTopicMessageFactory().newFromType(StrParameter._TYPE);
      resolutionParam.setName("resolution");
      resolutionParam.setValue(resolution);
      request.getConfig().getStrs().add(resolutionParam);
      }
      
      multiSenseClient.call(request, new ServiceResponseListener<ReconfigureResponse>()
            {

               public void onSuccess(ReconfigureResponse response)
               {
                  System.out.println("successful" + response.getConfig().getDoubles().get(0).getValue());
               }

               public void onFailure(RemoteException e)
               {
                  e.printStackTrace();
               }
            });
   }

   
}
