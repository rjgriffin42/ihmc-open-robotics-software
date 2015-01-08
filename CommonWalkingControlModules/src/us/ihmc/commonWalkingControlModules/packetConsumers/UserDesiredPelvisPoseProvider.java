package us.ihmc.commonWalkingControlModules.packetConsumers;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.math.frames.YoFrameOrientation;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;

public class UserDesiredPelvisPoseProvider implements PelvisPoseProvider
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final DoubleYoVariable userPelvisTrajectoryTime = new DoubleYoVariable("userDesiredPelvisTrajectoryTime", registry);

   private final BooleanYoVariable isNewPelvisOrientationInformationAvailable = new BooleanYoVariable("isNewPelvisOrientationInformationAvailable", registry);
   private final YoFrameOrientation userPelvisOrientation = new YoFrameOrientation("userDesiredPelvis", null, registry);

   private final BooleanYoVariable isNewPelvisPositionInformationAvailable = new BooleanYoVariable("isNewPelvisPositionInformationAvailable", registry);
   private final YoFramePoint userPelvisPosition = new YoFramePoint("userDesiredPelvis", null, registry);

   private final Quat4d desiredQuat = new Quat4d();
   private final Point3d desiredPoint = new Point3d();
   private final FrameOrientation frameOrientation = new FrameOrientation();
   private final FramePoint framePoint = new FramePoint();

   public UserDesiredPelvisPoseProvider(YoVariableRegistry parentRegistry)
   {
      VariableChangedListener variableChangedListener = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            isNewPelvisOrientationInformationAvailable.set(true);
            userPelvisOrientation.getQuaternion(desiredQuat);
         }
      };

      userPelvisOrientation.attachVariableChangedListener(variableChangedListener);
      
      userPelvisPosition.attachVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            isNewPelvisPositionInformationAvailable.set(true);
            userPelvisPosition.get(desiredPoint);
         }
      });
      
      parentRegistry.addChild(registry);
   }

   @Override
   public boolean checkForNewOrientation()
   {
      return isNewPelvisOrientationInformationAvailable.getBooleanValue();
   }

   @Override
   public FrameOrientation getDesiredPelvisOrientation(ReferenceFrame desiredPelvisFrame)
   {
      if (!isNewPelvisOrientationInformationAvailable.getBooleanValue())
         return null;

      isNewPelvisOrientationInformationAvailable.set(false);

      frameOrientation.setIncludingFrame(desiredPelvisFrame, desiredQuat);
      
      return frameOrientation;
   }

   @Override
   public boolean checkForNewPosition()
   {
      return isNewPelvisPositionInformationAvailable.getBooleanValue();
   }

   @Override
   public FramePoint getDesiredPelvisPosition(ReferenceFrame supportFrame)
   {
      if (!isNewPelvisPositionInformationAvailable.getBooleanValue())
         return null;
      
      isNewPelvisPositionInformationAvailable.set(false);
      
      framePoint.setIncludingFrame(supportFrame, desiredPoint);
      
      return framePoint;
   }

   @Override
   public double getTrajectoryTime()
   {
      return userPelvisTrajectoryTime.getDoubleValue();
   }
}
