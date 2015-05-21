package us.ihmc.acsell;

import javax.vecmath.Vector3d;

import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.utilities.math.TimeTools;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.humanoidRobot.visualizer.RobotVisualizer;

/**
 * Hacked to be outside the main DRC code, move after code freeze
 * 
 * @author jesper
 *
 */
public class CostOfTransportCalculator implements RobotVisualizer
{

   private final RobotVisualizer superVisualizer;
   
   private DoubleYoVariable costOfTransport;
   
   private FullRobotModel fullRobotModel;
   private DoubleYoVariable totalWorkVariable;
   
   private final int samples;
   
   private final double robotMass; 
   private final double gravity;
   
   private final double[] robotTime;
   
   private final double[] totalWork;
   
   private final double[] xPosition;
   private final double[] yPosition;
   private final double[] zPosition;
   
   private long index = 0;
   
   private final Vector3d position = new Vector3d();
   
   public CostOfTransportCalculator(double robotMass, double gravity, double measurementTime, double dt, RobotVisualizer superVisualizer)
   {
      this.robotMass = robotMass;
      this.gravity = Math.abs(gravity);
      
      this.samples = (int) (measurementTime / dt);
      
      this.robotTime = new double[samples];
      this.totalWork = new double[samples];
      this.xPosition = new double[samples];
      this.yPosition = new double[samples];
      this.zPosition = new double[samples];
      
      this.superVisualizer = superVisualizer;
   }
   
   private int sampleIndex(long index)
   {
      return (int) (index % samples);
   }
   
   @Override
   public void update(long timestamp)
   {
      int currentSample = sampleIndex(index);
      int historicSample = sampleIndex(index + 1);
      
      this.robotTime[currentSample] = TimeTools.nanoSecondstoSeconds(timestamp);
      this.totalWork[currentSample] = totalWorkVariable.getDoubleValue();
      
      fullRobotModel.getRootJoint().packTranslation(position);
      this.xPosition[currentSample] = position.getX();
      this.yPosition[currentSample] = position.getY();
      this.zPosition[currentSample] = position.getZ();
      
      // Only update if we have enough samples
      if(index > samples)
      {
         double deltaTime = this.robotTime[currentSample] - this.robotTime[historicSample];
         double deltaWork = this.totalWork[currentSample] - this.totalWork[historicSample];
         
         double dx = this.xPosition[currentSample] - this.xPosition[historicSample];
         double dy = this.yPosition[currentSample] - this.yPosition[historicSample];

         double distance = Math.sqrt(dx*dx+dy*dy);
         
         double avarageVelocity = distance/deltaTime;
         
         costOfTransport.set(deltaWork / (robotMass * gravity * avarageVelocity));
         
      }
      
      ++index;

      superVisualizer.update(timestamp);
   }

   

   @Override
   public void update(long timestamp, YoVariableRegistry registry)
   {
      superVisualizer.update(timestamp, registry);
   }

   @Override
   public void setMainRegistry(YoVariableRegistry registry, FullRobotModel fullRobotModel, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      PrintTools.info(this, "Initializing cost of transport calculator. Robot mass is " + robotMass + "kg");
      this.costOfTransport = new DoubleYoVariable("CostOfTransport", registry);
      this.fullRobotModel = fullRobotModel;
      
      superVisualizer.setMainRegistry(registry, fullRobotModel, yoGraphicsListRegistry);
   }

   @Override
   public void addRegistry(YoVariableRegistry registry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      superVisualizer.addRegistry(registry, yoGraphicsListRegistry);
   }

   @Override
   public void close()
   {
      superVisualizer.close();
   }

   public void setTotalWorkVariable(DoubleYoVariable totalWorkVariable)
   {
      this.totalWorkVariable = totalWorkVariable;
   }
}
