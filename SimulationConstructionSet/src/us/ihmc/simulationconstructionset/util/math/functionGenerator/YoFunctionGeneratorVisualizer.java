package us.ihmc.simulationconstructionset.util.math.functionGenerator;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;

import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.robotController.RobotController;

public class YoFunctionGeneratorVisualizer implements RobotController
{
   private YoVariableRegistry registry = new YoVariableRegistry("YoFunGenViz");
   
   private DoubleYoVariable valueCheck;
   
   private YoFunctionGenerator yoFunctionGenerator;
   private DoubleYoVariable time;
   
   private DoubleYoVariable resetTime;
   private DoubleYoVariable maxSweepFreq;
   private DoubleYoVariable amplitude;

   private BooleanYoVariable hasBeenReset;

   private final EnumYoVariable<YoFunctionGeneratorMode> mode;

   
   public YoFunctionGeneratorVisualizer(YoFunctionGenerator yoFunctionGenerator)
   {
      this.yoFunctionGenerator = yoFunctionGenerator;
      
      mode = EnumYoVariable.create("Mode", YoFunctionGeneratorMode.class, registry);
      
      resetTime = new DoubleYoVariable("resetTime", registry);
      
      resetTime.set(20.0);
      
      maxSweepFreq = new DoubleYoVariable("maxSweepFreq", registry);
      maxSweepFreq.set(60.0);
      
      amplitude = new DoubleYoVariable("amplitude", registry);
      amplitude.set(1.0);
      
      valueCheck = new DoubleYoVariable("valueCheck", registry);
      
      hasBeenReset = new BooleanYoVariable("hasBeenReset", registry);
      hasBeenReset.set(true);
   }
   
   public void initialize()
   {
      
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return null;
   }

   public String getDescription()
   {
      return null;
   }
   
   public void setTimeVariable(DoubleYoVariable time)
   {
      this.time = time;
   }
   
   
   public void doControl()
   {
      if (!hasBeenReset.getBooleanValue() && !mode.getEnumValue().equals(YoFunctionGeneratorMode.OFF) && yoFunctionGenerator.getMode().equals(YoFunctionGeneratorMode.OFF))
      {
         mode.set(YoFunctionGeneratorMode.OFF);
         hasBeenReset.set(true);
      }
      
      if (!yoFunctionGenerator.getMode().equals(YoFunctionGeneratorMode.OFF))
         hasBeenReset.set(false);
      
      yoFunctionGenerator.setMode(mode.getEnumValue());
      yoFunctionGenerator.setResetTime(resetTime.getDoubleValue());
      yoFunctionGenerator.setChirpFrequencyMaxHz(maxSweepFreq.getDoubleValue());
      yoFunctionGenerator.setAmplitude(amplitude.getDoubleValue());
      
      valueCheck.set(this.yoFunctionGenerator.getValue(time.getDoubleValue()));
      
      try
      {
         Thread.sleep(1);
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }
   }
   
   public static void main(String[] args)
   {
      Robot robot = new Robot("Robot");
      
      YoVariableRegistry registry = robot.getRobotsYoVariableRegistry();
      
      YoFunctionGenerator yoFunctionGenerator = new YoFunctionGenerator("FunGen", registry);
      YoFunctionGeneratorVisualizer yoFunctionGeneratorVisualizer = new YoFunctionGeneratorVisualizer(yoFunctionGenerator);
      
      yoFunctionGeneratorVisualizer.setTimeVariable(robot.getYoTime());
      
      robot.setController(yoFunctionGeneratorVisualizer);
      
      SimulationConstructionSet scs = new SimulationConstructionSet(robot);

      scs.setDT(0.001, 10);
      
      Thread thread = new Thread(scs);

      thread.start();
      
//      try
//      {
//         Thread.sleep(1000);
//      }
//      catch (InterruptedException e)
//      {
//         e.printStackTrace();
//      }
//      
      scs.hideViewport();
   }

}


