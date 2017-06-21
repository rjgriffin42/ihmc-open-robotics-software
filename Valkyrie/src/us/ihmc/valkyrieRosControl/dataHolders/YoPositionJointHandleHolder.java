package us.ihmc.valkyrieRosControl.dataHolders;

import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.DoubleYoVariable;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.rosControl.wholeRobot.PositionJointHandle;
import us.ihmc.sensorProcessing.model.DesiredJointDataHolder.DesiredJointData;

public class YoPositionJointHandleHolder
{
   private final String name;
   private final PositionJointHandle handle;
   private final OneDoFJoint joint;
   private final DesiredJointData desiredJointData;

   private final DoubleYoVariable q;
   private final DoubleYoVariable qd;

   private final DoubleYoVariable controllerPositionDesired;
   private final DoubleYoVariable positionDesired;

   public YoPositionJointHandleHolder(PositionJointHandle handle, OneDoFJoint joint, DesiredJointData desiredJointData, YoVariableRegistry parentRegistry)
   {
      this.name = handle.getName();
      YoVariableRegistry registry = new YoVariableRegistry(name);

      this.handle = handle;
      this.joint = joint;
      this.desiredJointData = desiredJointData;

      this.q = new DoubleYoVariable(name + "_q", registry);
      this.qd = new DoubleYoVariable(name + "_qd", registry);
      this.controllerPositionDesired = new DoubleYoVariable(name + "ControllerPositionDesired", registry);
      this.positionDesired = new DoubleYoVariable(name + "PositionDesired", registry);

      parentRegistry.addChild(registry);
   }

   public void update()
   {
      this.q.set(handle.getPosition());
      this.qd.set(handle.getVelocity());
      this.controllerPositionDesired.set(desiredJointData.getPositionDesired());
   }

   public void setDesiredPosition(double position)
   {
      this.positionDesired.set(position);
      handle.setDesiredPosition(position);
   }

   public OneDoFJoint getOneDoFJoint()
   {
      return joint;
   }

   public double getQ()
   {
      return q.getDoubleValue();
   }

   public double getQd()
   {
      return qd.getDoubleValue();
   }

   public double getControllerPositionDesired()
   {
      return controllerPositionDesired.getDoubleValue();
   }

   public String getName()
   {
      return name;
   }
}
