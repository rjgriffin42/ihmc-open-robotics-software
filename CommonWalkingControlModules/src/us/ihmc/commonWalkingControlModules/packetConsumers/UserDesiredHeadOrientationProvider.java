package us.ihmc.commonWalkingControlModules.packetConsumers;

import java.util.concurrent.atomic.AtomicBoolean;

import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.google.common.util.concurrent.AtomicDouble;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.VariableChangedListener;
import com.yobotics.simulationconstructionset.YoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class UserDesiredHeadOrientationProvider extends DesiredHeadOrientationProvider
{
   private final DoubleYoVariable userDesiredHeadPitch, userDesiredHeadYaw, userDesiredNeckPitch;
   private final ReferenceFrame headOrientationFrame;
   
   private final AtomicBoolean isNewHeadOrientationInformationAvailable = new AtomicBoolean(true);
   private final AtomicDouble desiredJointForExtendedNeckPitchRangeAngle = new AtomicDouble(0.0);
   private final FrameOrientation desiredHeadOrientation = new FrameOrientation();
   
   public UserDesiredHeadOrientationProvider(ReferenceFrame headOrientationFrame, YoVariableRegistry registry)
   {
      super(headOrientationFrame);

      this.headOrientationFrame = headOrientationFrame;
      
      userDesiredHeadPitch = new DoubleYoVariable("userDesiredHeadPitch", registry);
      userDesiredHeadYaw = new DoubleYoVariable("userDesiredHeadYaw", registry);
      userDesiredNeckPitch = new DoubleYoVariable("userDesiredNeckPitch", registry);

      userDesiredHeadPitch.addVariableChangedListener(new VariableChangedListener()
      {
         
         public void variableChanged(YoVariable v)
         {
            desiredJointForExtendedNeckPitchRangeAngle.set(userDesiredHeadPitch.getDoubleValue());
         }
      });
      
      setupListeners();
   }
   
   private void setupListeners()
   {
      VariableChangedListener variableChangedListener = new VariableChangedListener()
      {
         public void variableChanged(YoVariable v)
         {
            isNewHeadOrientationInformationAvailable.set(true);
            desiredHeadOrientation.set(headOrientationFrame, userDesiredHeadYaw.getDoubleValue(), userDesiredNeckPitch.getDoubleValue(), 0.0);
         }
      };

      userDesiredHeadPitch.addVariableChangedListener(variableChangedListener);
      userDesiredHeadYaw.addVariableChangedListener(variableChangedListener);
      userDesiredNeckPitch.addVariableChangedListener(variableChangedListener);
      
      variableChangedListener.variableChanged(null);
   }
   
   public double getDesiredExtendedNeckPitchJointAngle()
   {
      return desiredJointForExtendedNeckPitchRangeAngle.getAndSet(Double.NaN);
   }
   
   public boolean isNewHeadOrientationInformationAvailable()
   {
      return isNewHeadOrientationInformationAvailable.getAndSet(false);
   }

   public FrameOrientation getDesiredHeadOrientation()
   {      
      return desiredHeadOrientation;
   }
}
