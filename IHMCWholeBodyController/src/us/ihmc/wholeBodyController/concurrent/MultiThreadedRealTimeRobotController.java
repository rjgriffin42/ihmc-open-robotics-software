package us.ihmc.wholeBodyController.concurrent;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.ImmutableTriple;

import us.ihmc.affinity.Processor;
import us.ihmc.realtime.MonotonicTime;
import us.ihmc.realtime.PriorityParameters;
import us.ihmc.realtime.RealtimeThread;
import us.ihmc.simulationconstructionset.robotController.MultiThreadedRobotControlElement;

public class MultiThreadedRealTimeRobotController
{
   private final MultiThreadedRobotControlElement sensorReader;
   private final ArrayList<ImmutableTriple<MultiThreadedRobotControlElement, PriorityParameters, Processor>> robotControllers = new ArrayList<>();
   private final MonotonicTime triggerTime = new MonotonicTime();   
   private final MonotonicTime monotonicTime = new MonotonicTime();
      
   public MultiThreadedRealTimeRobotController(MultiThreadedRobotControlElement sensorReader)
   {
      this.sensorReader = sensorReader;
   }
   
   public void addController(MultiThreadedRobotControlElement robotController, PriorityParameters priorityParameters, Processor processor)
   {
      robotControllers.add(new ImmutableTriple<MultiThreadedRobotControlElement, PriorityParameters, Processor>(robotController, priorityParameters, processor));
   }
   
   public void read()
   {
      long timestamp = RealtimeThread.getCurrentMonotonicClockTime();
      sensorReader.read(timestamp);
      sensorReader.run();
      
      // Magic jitter fix
//      monotonicTime.set(0, timestamp + 500000);
//      RealtimeThread.getCurrentRealtimeThread().waitUntil(monotonicTime);
      sensorReader.write(RealtimeThread.getCurrentMonotonicClockTime());
   }
   
   public void start()
   {
      for(int i = 0; i < robotControllers.size(); i++)
      {
         ImmutableTriple<MultiThreadedRobotControlElement, PriorityParameters, Processor> controllerAndPriority = robotControllers.get(i);
         MultiThreadedRobotControlElement controller = controllerAndPriority.getLeft();
         PriorityParameters priority = controllerAndPriority.getMiddle();
         Processor processor = controllerAndPriority.getRight();
         
         controller.initialize();
         
         MultiThreadedRobotControlElementRunner runner = new MultiThreadedRobotControlElementRunner(controller, priority);
         if(processor != null)
         {
            runner.setAffinity(processor);            
         }
         runner.start();
      }
   }
   
   
   private class MultiThreadedRobotControlElementRunner extends RealtimeThread
   {
      private final MultiThreadedRobotControlElement controller;
      
      public MultiThreadedRobotControlElementRunner(MultiThreadedRobotControlElement controller, PriorityParameters priorityParameters)
      {
         super(priorityParameters);
         this.controller = controller;
      }
      
      @Override
      public void run()
      {
         while(true)
         {
            controller.read(RealtimeThread.getCurrentMonotonicClockTime());
            controller.run();
            controller.write(RealtimeThread.getCurrentMonotonicClockTime());
            
            if(controller.nextWakeupTime() != Long.MIN_VALUE)
            {
               triggerTime.set(0, controller.nextWakeupTime());
               waitUntil(triggerTime);               
            }
         }
      }
      
      
   }
   
}
