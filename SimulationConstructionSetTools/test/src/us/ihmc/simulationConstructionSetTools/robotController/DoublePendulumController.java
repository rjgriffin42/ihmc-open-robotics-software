package us.ihmc.simulationConstructionSetTools.robotController;

import us.ihmc.commons.Conversions;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoLong;

public class DoublePendulumController implements MultiThreadedRobotControlElement
{
   private final YoVariableRegistry registry = new YoVariableRegistry("DoublePendulumController");
   private final YoDouble q_j1 = new YoDouble("q_j1", registry);
   private final YoDouble qd_j1 = new YoDouble("qd_j1", registry);
   private final YoDouble q_j2 = new YoDouble("q_j2", registry);
   private final YoDouble qd_j2 = new YoDouble("qd_j2", registry);

   private final YoDouble tau_j1 = new YoDouble("tau_j1", registry);
   private final YoDouble tau_j2 = new YoDouble("tau_j2", registry);

   private final YoDouble q_j1_d = new YoDouble("q_j1_d", registry);
   private final YoDouble q_j2_d = new YoDouble("q_j2_d", registry);

   private final YoDouble kp = new YoDouble("kp", registry);
   private final YoDouble kd = new YoDouble("kd", registry);
   
   private final YoLong tick = new YoLong("tick", registry);

   private final DoublePendulum doublePendulum;

   public DoublePendulumController(DoublePendulum doublePendulum)
   {
      this.doublePendulum = doublePendulum;
   }

   @Override
   public void initialize()
   {
      this.kp.set(100.0);
      this.kd.set(10.0);
      
      doublePendulum.getJ1().setQ(0.1);
   }

   @Override
   public void read(long currentClockTime)
   {
      q_j1.set(doublePendulum.getJ1().getQYoVariable().getDoubleValue());
      qd_j1.set(doublePendulum.getJ1().getQDYoVariable().getDoubleValue());
      q_j2.set(doublePendulum.getJ2().getQYoVariable().getDoubleValue());
      qd_j2.set(doublePendulum.getJ2().getQDYoVariable().getDoubleValue());
   }

   @Override
   public void run()
   {
      q_j1_d.set(q_j1_d.getDoubleValue() + 0.1);
      q_j2_d.set(q_j2_d.getDoubleValue() + 0.05);
      
      tau_j1.set(kp.getDoubleValue() * (q_j1_d.getDoubleValue() - q_j1.getDoubleValue()) + kd.getDoubleValue() * (-qd_j1.getDoubleValue()));
      tau_j2.set(kp.getDoubleValue() * (q_j2_d.getDoubleValue() - q_j2.getDoubleValue()) + kd.getDoubleValue() * (-qd_j2.getDoubleValue()));
      
      tick.increment();
      
      long start = System.nanoTime();
      while(System.nanoTime() - start < Conversions.millisecondsToNanoseconds(10))
      {
         // Busy wait
      }
   }

   @Override
   public void write(long timestamp)
   {
      doublePendulum.getJ1().setTau(tau_j1.getDoubleValue());
      doublePendulum.getJ2().setTau(tau_j2.getDoubleValue());
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return getClass().getSimpleName();
   }

   @Override
   public YoGraphicsListRegistry getYoGraphicsListRegistry()
   {
      return null;
   }

   @Override
   public long nextWakeupTime()
   {
      return 0;
   }

}
