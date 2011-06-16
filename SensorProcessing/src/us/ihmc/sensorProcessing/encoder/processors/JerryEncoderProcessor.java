package us.ihmc.sensorProcessing.encoder.processors;


import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.IntYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class JerryEncoderProcessor extends AbstractEncoderProcessor
{
   private static final double
//   ALPHA = 0.15, BETA = 0.15, GAMMA = 0.15;
//   ALPHA = 0.3, BETA = 0.3, GAMMA = 0.3; //0.1; //0.15;
//   ALPHA = 1.0, BETA = 1.0, GAMMA = 1.0; //0.1; //0.15;

 ALPHA = 0.5, BETA = 0.5, GAMMA = 0.15; //0.1; //0.15;

   private final EnumYoVariable<EncoderState> state;

   private final IntYoVariable previousRawTicks, previousRawTicksTwoBack;
   private final IntYoVariable previousProcessedTicks, previousProcessedTicksTwoBack;
   private final DoubleYoVariable previousTime, previousTimeTwoBack;

   private final double dt;

   private final DoubleYoVariable maxPossibleRate;
   private final DoubleYoVariable minPriorRate, maxPriorRate, averagePriorRate;

   public JerryEncoderProcessor(String name, IntYoVariable rawTicks, DoubleYoVariable time, double distancePerTick, double dt, YoVariableRegistry parentRegistry)
   {
      super(name, rawTicks, time, distancePerTick, createRegistry(name));

      this.dt = dt;

      this.state = EnumYoVariable.create(name + "state", EncoderState.class, registry);

      this.minPriorRate = new DoubleYoVariable(name + "minPriorRate", registry);
      this.maxPriorRate = new DoubleYoVariable(name + "maxPriorRate", registry);


      this.maxPossibleRate = new DoubleYoVariable(name + "maxPossibleRate", registry);
      this.averagePriorRate = new DoubleYoVariable(name + "averagePriorRate", registry);

      this.previousRawTicksTwoBack = new IntYoVariable(name + "prevRawPos2", registry);
      this.previousRawTicks = new IntYoVariable(name + "prevRawPos", registry);
      this.previousTime = new DoubleYoVariable(name + "prevTime", registry);

      this.previousProcessedTicks = new IntYoVariable(name + "prevPos", registry);
      this.previousProcessedTicksTwoBack = new IntYoVariable(name + "prevPos2", registry);
      this.previousTimeTwoBack = new DoubleYoVariable(name + "prevTime2", registry);

      parentRegistry.addChild(registry);
   }

   public void update()
   {
      boolean positionChanged = rawTicks.getIntegerValue() != previousRawTicks.getIntegerValue();

      if (positionChanged)
         doStateTransitions();

      doStateActionsForPosition(positionChanged);


      if (positionChanged)
      {
         double positionChange = processedTicks.getDoubleValue() - previousProcessedTicks.getIntegerValue();
         double positionChangeInt = (int) positionChange;
         double timeChange = time.getDoubleValue() - previousTime.getDoubleValue();

//       int positionChangeInt = processedTicks.getIntegerValue() - previousProcessedTicksTwoBack.getIntegerValue();
//       double positionChange = (double) positionChangeInt;
//       double timeChange = time.getDoubleValue() - previousTimeTwoBack.getDoubleValue();

         if (positionChangeInt >= 2)
         {
            timeChange = dt; // If there were multiple time ticks and multiple position ticks, then can assume only one time tick!
            
            minPriorRate.set((positionChange - 1.0) / timeChange);
            maxPriorRate.set((positionChange + 1.0) / timeChange);
            averagePriorRate.set(positionChange / timeChange);
         }

         else if (positionChangeInt <= -2)
         {
            timeChange = dt; // If there were multiple time ticks and multiple position ticks, then can assume only one time tick!

            minPriorRate.set((positionChange + 1.0) / timeChange);
            maxPriorRate.set((positionChange - 1.0) / timeChange);
            averagePriorRate.set(positionChange / timeChange);
         }
         
         else if (timeChange > 1.5 * dt)
         {
            minPriorRate.set(positionChange / (timeChange + dt));
            maxPriorRate.set(positionChange / (timeChange - dt));
            averagePriorRate.set(positionChange / timeChange);
         }


         else if (positionChangeInt == 1)
         {
            minPriorRate.set(positionChange / (timeChange + dt));
            maxPriorRate.set((positionChange + 1.0) / timeChange);
            averagePriorRate.set(positionChange / timeChange);
         }
         
         else if (positionChangeInt == -1)
         {
            minPriorRate.set(positionChange / (timeChange + dt));
            maxPriorRate.set((positionChange - 1.0) / timeChange);
            averagePriorRate.set(positionChange / timeChange);
         }
         
         else if (positionChangeInt == 0)
         {
            maxPriorRate.set((positionChange + 1.0) / timeChange);
            minPriorRate.set((positionChange - 1.0) / timeChange);
            averagePriorRate.set(positionChange / timeChange);
         }
         
         else
         {
            System.err.println("Should never get here!");
            System.err.println("positionChangeInt = " + positionChangeInt);
            System.err.println("timeChange = " + timeChange);
//            throw new RuntimeException("Should never get here!");
         }

      }

      doStateActionsForVelocity(positionChanged);

      if (positionChanged)
      {
         this.previousProcessedTicksTwoBack.set(this.previousProcessedTicks.getIntegerValue());
         this.previousProcessedTicks.set((int) processedTicks.getDoubleValue());
         this.previousRawTicksTwoBack.set(previousRawTicks.getIntegerValue());
         this.previousRawTicks.set(rawTicks.getIntegerValue());

         this.previousTimeTwoBack.set(this.previousTime.getDoubleValue());
         this.previousTime.set(this.time.getDoubleValue());
      }
   }



   private void doStateActionsForPosition(boolean positionChanged)
   {
      int rawTicks = this.rawTicks.getIntegerValue();

      switch ((EncoderState) state.getEnumValue())
      {
         case Start :
         {
            this.previousProcessedTicksTwoBack.set(rawTicks);
            this.previousProcessedTicks.set(rawTicks);

            this.previousRawTicks.set(rawTicks);

            this.previousTimeTwoBack.set(time.getDoubleValue());
            this.previousTime.set(time.getDoubleValue());

            this.processedTicks.set(rawTicks);
            this.processedTickRate.set(0.0);

            break;
         }

         case ForwardOne :
         {
            this.processedTicks.set(rawTicks - 1);

            break;
         }

         case ForwardTwo :
         {
            this.processedTicks.set(rawTicks - 1);

            break;
         }

         case BackwardOne :
         {
            this.processedTicks.set(rawTicks);

            break;
         }

         case BackwardTwo :
         {
            this.processedTicks.set(rawTicks);

            break;
         }
      }


   }


   private void doStateActionsForVelocity(boolean positionChanged)
   {
      switch ((EncoderState) state.getEnumValue())
      {
         case Start :
         {
            break;
         }

         case ForwardOne :
         {
            this.processedTickRate.set(0.0);

            break;
         }

         case ForwardTwo :
         {
//            if (positionChanged)
            {
               if (processedTickRate.getDoubleValue() < minPriorRate.getDoubleValue())
               {
//                  this.processedTickRate.set(minPriorRate.getDoubleValue() + ALPHA * (averagePriorRate.getDoubleValue() - minPriorRate.getDoubleValue()));
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + ALPHA * (minPriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + GAMMA * (averagePriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
               }
               else if (processedTickRate.getDoubleValue() > maxPriorRate.getDoubleValue())
               {
//                  this.processedTickRate.set(maxPriorRate.getDoubleValue() + BETA * (averagePriorRate.getDoubleValue() - maxPriorRate.getDoubleValue()));
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + BETA * (maxPriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + GAMMA * (averagePriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
               }
               else
               {
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + GAMMA * (averagePriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
               }
            }
            if (!positionChanged)
            {
               double timeChange = time.getDoubleValue() - previousTime.getDoubleValue();
               maxPossibleRate.set(1.0 / timeChange);

               this.processedTickRate.set(Math.min(maxPossibleRate.getDoubleValue(), processedTickRate.getDoubleValue()));
            }

            break;
         }

         case BackwardOne :
         {
            this.processedTickRate.set(0.0);

            break;
         }

         case BackwardTwo :
         {
//            if (positionChanged)
            {
               if (processedTickRate.getDoubleValue() > minPriorRate.getDoubleValue())
               {
//                  this.processedTickRate.set(minPriorRate.getDoubleValue() + ALPHA * (averagePriorRate.getDoubleValue() - minPriorRate.getDoubleValue()));
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + ALPHA * (minPriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + GAMMA * (averagePriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
               }
               else if (processedTickRate.getDoubleValue() < maxPriorRate.getDoubleValue())
               {
//                  this.processedTickRate.set(maxPriorRate.getDoubleValue() + BETA * (averagePriorRate.getDoubleValue() - maxPriorRate.getDoubleValue()));
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + BETA * (maxPriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + GAMMA * (averagePriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
               }
               else
               {
                  this.processedTickRate.set(processedTickRate.getDoubleValue() + GAMMA * (averagePriorRate.getDoubleValue() - processedTickRate.getDoubleValue()));
               }
            } 
            if (!positionChanged)
            {
               double timeChange = time.getDoubleValue() - previousTime.getDoubleValue();
               maxPossibleRate.set(-1.0 / timeChange);

               this.processedTickRate.set(Math.max(maxPossibleRate.getDoubleValue(), processedTickRate.getDoubleValue()));
            }

            break;
         }
      }


   }

   private void doStateTransitions()
   {
      int rawTicks = this.rawTicks.getIntegerValue();
      int previousPosition = this.previousRawTicks.getIntegerValue();

      boolean increasing = rawTicks > previousPosition;
      boolean decreasing = rawTicks < previousPosition;

      boolean increasingMoreThanOne = rawTicks > previousPosition + 1;
      boolean decreasingMoreThanOne = rawTicks < previousPosition - 1;

      
      switch ((EncoderState) state.getEnumValue())
      {
         case Start :
         {
            if (increasing)
            {
               state.set(EncoderState.ForwardOne);
            }
            else if (decreasing)
            {
               state.set(EncoderState.BackwardOne);
            }

            break;
         }

         case ForwardOne :
         {
            if (increasing)
            {
               state.set(EncoderState.ForwardTwo);
            }
            else if (decreasingMoreThanOne)
            {
               state.set(EncoderState.BackwardTwo);
            }
            else if (decreasing)
            {
               state.set(EncoderState.BackwardOne);
            }

            break;
         }

         case ForwardTwo :
         {
            if (increasing)
            {
               state.set(EncoderState.ForwardTwo);
            }
            else if (decreasingMoreThanOne)
            {
               state.set(EncoderState.BackwardTwo);
            }
            else if (decreasing)
            {
               state.set(EncoderState.BackwardOne);
            }

            break;
         }

         case BackwardOne :
         {
            if (decreasing)
            {
               state.set(EncoderState.BackwardTwo);
            }
            else if (increasingMoreThanOne)
            {
               state.set(EncoderState.ForwardTwo);
            }
            else if (increasing)
            {
               state.set(EncoderState.ForwardOne);
            }

            break;
         }

         case BackwardTwo :
         {
            if (decreasing)
            {
               state.set(EncoderState.BackwardTwo);
            }
            else if (increasingMoreThanOne)
            {
               state.set(EncoderState.ForwardTwo);
            }
            else if (increasing)
            {
               state.set(EncoderState.ForwardOne);
            }
            

            break;
         }
      }

   }

   private static YoVariableRegistry createRegistry(String name)
   {
      return new YoVariableRegistry(name);
   }


   private enum EncoderState {Start, ForwardOne, ForwardTwo, BackwardOne, BackwardTwo;}

}
