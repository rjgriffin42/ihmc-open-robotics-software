package us.ihmc.quadrupedRobotics.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.quadrupedRobotics.communication.packets.QuadrupedTimedStepPacket;
import us.ihmc.quadrupedRobotics.planning.QuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.util.TimeInterval;
import us.ihmc.robotics.dataStructures.listener.VariableChangedListener;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.dataStructures.variable.YoVariable;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotQuadrant;

public class QuadrupedPreplannedStepInputProvider
{
   private final AtomicReference<QuadrupedTimedStepPacket> inputTimedStepPacket;
   private final QuadrupedTimedStepPacket emptyTimedStepPacket;
   private final QuadrupedTimedStepPacket debugTimedStepPacket;

   private final EnumYoVariable<RobotQuadrant> yoTimedStepQuadrant;
   private final DoubleYoVariable yoTimedStepDuration;
   private final DoubleYoVariable yoTimedStepGroundClearance;
   private final YoFramePoint yoTimedStepGoalPosition;

   public QuadrupedPreplannedStepInputProvider(GlobalDataProducer globalDataProducer, YoVariableRegistry registry)
   {
      inputTimedStepPacket = new AtomicReference<>(new QuadrupedTimedStepPacket());
      emptyTimedStepPacket = new QuadrupedTimedStepPacket();
      debugTimedStepPacket = new QuadrupedTimedStepPacket(Collections.singletonList(new QuadrupedTimedStep()));

      yoTimedStepQuadrant = new EnumYoVariable<>("timedStepQuadrant", registry, RobotQuadrant.class);
      yoTimedStepDuration = new DoubleYoVariable("timedStepDuration", registry);
      yoTimedStepGroundClearance = new DoubleYoVariable("timedStepGroundClearance", registry);
      yoTimedStepGoalPosition = new YoFramePoint("timedStepGoalPosition", ReferenceFrame.getWorldFrame(), registry);
      
      setupYoVariableChangedListener(yoTimedStepQuadrant);
      setupYoVariableChangedListener(yoTimedStepDuration);
      setupYoVariableChangedListener(yoTimedStepGroundClearance);
      setupYoVariableChangedListener(yoTimedStepGoalPosition.getYoX());
      setupYoVariableChangedListener(yoTimedStepGoalPosition.getYoY());
      setupYoVariableChangedListener(yoTimedStepGoalPosition.getYoZ());
      
      if (globalDataProducer != null)
      {
         globalDataProducer.attachListener(QuadrupedTimedStepPacket.class, new PacketConsumer<QuadrupedTimedStepPacket>()
         {
            @Override
            public void receivedPacket(QuadrupedTimedStepPacket packet)
            {
               inputTimedStepPacket.set(packet);
            }
         });
      }
   }
   
   private void setupYoVariableChangedListener(YoVariable<?> yoVariable)
   {
      yoVariable.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            debugTimedStepPacket.get(0).setRobotQuadrant(yoTimedStepQuadrant.getEnumValue());
            debugTimedStepPacket.get(0).setGroundClearance(yoTimedStepGroundClearance.getDoubleValue());
            debugTimedStepPacket.get(0).setGoalPosition(yoTimedStepGoalPosition.getFrameTuple().getPoint());
            debugTimedStepPacket.get(0).getTimeInterval().setInterval(0.5, 0.5 + yoTimedStepDuration.getDoubleValue());
            debugTimedStepPacket.get(0).setAbsolute(false);
            inputTimedStepPacket.set(debugTimedStepPacket);
         }
      });
   }

   public boolean isStepPlanAvailable()
   {
      ArrayList<QuadrupedTimedStep> steps = inputTimedStepPacket.get().get();
      return (steps.size() > 0);
   }

   public ArrayList<QuadrupedTimedStep> getAndClearStepPlan()
   {
      ArrayList<QuadrupedTimedStep> steps = inputTimedStepPacket.get().get();
      inputTimedStepPacket.set(emptyTimedStepPacket);
      return steps;
   }
}
