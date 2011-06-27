package us.ihmc.sensorProcessing.encoder.processors;

import com.yobotics.simulationconstructionset.AbstractYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.IntYoVariable;
import com.yobotics.simulationconstructionset.VariableChangedListener;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public abstract class AbstractEncoderProcessor implements EncoderProcessor
{
   protected final YoVariableRegistry registry;
   protected final IntYoVariable rawTicks;
   protected final DoubleYoVariable time;

   protected final DoubleYoVariable processedTicks;
   protected final DoubleYoVariable processedTickRate;

   private final DoubleYoVariable processedPosition;
   private final DoubleYoVariable processedVelocity;

   public AbstractEncoderProcessor(String name, IntYoVariable rawTicks, DoubleYoVariable time, double distancePerTick, YoVariableRegistry registry)
   {
      this.registry = registry;
      this.rawTicks = rawTicks;
      this.time = time;
      this.processedTicks = new DoubleYoVariable(name + "ProcTicks", registry);
      this.processedTickRate = new DoubleYoVariable(name + "ProcTickRate", registry);
      this.processedPosition = new DoubleYoVariable(name + "ProcPos", registry);
      this.processedVelocity = new DoubleYoVariable(name + "ProcVel", registry);

      processedTicks.addVariableChangedListener(new MultiplicationVariableChangedListener(processedPosition, distancePerTick));
      processedTickRate.addVariableChangedListener(new MultiplicationVariableChangedListener(processedVelocity, distancePerTick));
   }

   public final double getQ()
   {
      return processedPosition.getDoubleValue();
   }

   public final double getQd()
   {
      return processedVelocity.getDoubleValue();
   }

   public abstract void initialize();

   public abstract void update();

   private static final class MultiplicationVariableChangedListener implements VariableChangedListener
   {
      private final AbstractYoVariable output;
      private final double multiplicationFactor;

      public MultiplicationVariableChangedListener(AbstractYoVariable output, double multiplicationFactor)
      {
         this.output = output;
         this.multiplicationFactor = multiplicationFactor;
      }

      public void variableChanged(AbstractYoVariable variable)
      {
         output.setValueFromDouble(variable.getValueAsDouble() * multiplicationFactor);
      }
   }
}
