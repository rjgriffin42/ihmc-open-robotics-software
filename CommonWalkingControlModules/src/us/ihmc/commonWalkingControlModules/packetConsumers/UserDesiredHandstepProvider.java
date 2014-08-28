package us.ihmc.commonWalkingControlModules.packetConsumers;

import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.desiredFootStep.Handstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.HandstepHelper;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;

import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicCoordinateSystem;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class UserDesiredHandstepProvider implements HandstepProvider
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final BooleanYoVariable userHandstepTakeIt = new BooleanYoVariable("userHandstepTakeIt", registry);
   private final YoFramePoint userHandstepPosition = new YoFramePoint("userHandstepPosition", ReferenceFrame.getWorldFrame(), registry);
   private final EnumYoVariable<RobotSide> userHandstepRobotSide = new EnumYoVariable<RobotSide>("userHandstepRobotSide", registry, RobotSide.class);
   private final YoFrameVector userHandstepNormal = new YoFrameVector("userHandstepNormal", ReferenceFrame.getWorldFrame(), registry);
   private final DoubleYoVariable userHandstepRotationAboutNormal = new DoubleYoVariable("userHandstepRotationAboutNormal", registry);
   private final DoubleYoVariable swingTrajectoryTime = new DoubleYoVariable("userHandstepSwingTime", registry);

   private final DynamicGraphicCoordinateSystem userDesiredHandstepCoordinateSystem;

   private final HandstepHelper handstepHelper;
   
   public UserDesiredHandstepProvider(FullRobotModel fullRobotModel, YoVariableRegistry parentRegistry,
                                      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.handstepHelper = new HandstepHelper(fullRobotModel); 
      
      userHandstepTakeIt.set(false);
      userHandstepNormal.set(-1.0, 0.0, 0.0);
      userHandstepRobotSide.set(RobotSide.LEFT);

      userDesiredHandstepCoordinateSystem = new DynamicGraphicCoordinateSystem("userHandstepViz", "", parentRegistry, 0.3);
      
      VariableChangedListener listener = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            Vector3d position = userHandstepPosition.getVector3dCopy();
            Vector3d surfaceNormal = userHandstepNormal.getVector3dCopy();
            double rotationAngleAboutNormal = userHandstepRotationAboutNormal.getDoubleValue();
            userDesiredHandstepCoordinateSystem.setTransformToWorld(HandstepHelper.computeHandstepTransform(false, position, surfaceNormal, rotationAngleAboutNormal));
         }
      };

      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("UserDesiredHandstep", userDesiredHandstepCoordinateSystem);

      userHandstepNormal.attachVariableChangedListener(listener);
      userHandstepRotationAboutNormal.addVariableChangedListener(listener);
      userHandstepPosition.attachVariableChangedListener(listener);

      parentRegistry.addChild(registry);
   }

   public Handstep getDesiredHandstep(RobotSide robotSide)
   {
      if (!userHandstepTakeIt.getBooleanValue())
         return null;
      if (userHandstepRobotSide.getEnumValue() != robotSide)
         return null;
      
      Vector3d surfaceNormal = userHandstepNormal.getVector3dCopy();
      double rotationAngleAboutNormal = userHandstepRotationAboutNormal.getDoubleValue();
      Vector3d position = userHandstepPosition.getVector3dCopy();
      
      Handstep handstep = handstepHelper.getDesiredHandstep(robotSide, position, surfaceNormal, rotationAngleAboutNormal, swingTrajectoryTime.getDoubleValue());
      userHandstepTakeIt.set(false);
      return handstep;
   }

   public boolean checkForNewHandstep(RobotSide robotSide)
   {
      if (!userHandstepTakeIt.getBooleanValue())
         return false;
      if (userHandstepRobotSide.getEnumValue() != robotSide)
         return false;
      return true;
   }

}
