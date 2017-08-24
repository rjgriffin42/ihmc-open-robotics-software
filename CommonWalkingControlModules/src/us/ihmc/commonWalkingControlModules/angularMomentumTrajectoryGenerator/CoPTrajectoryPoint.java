package us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator;

import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameTuple3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.trajectories.waypoints.YoFrameEuclideanTrajectoryPoint;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoVariable;

/**
 * Adds some visualization methods to the YoFrameEuclideanTrajectoryPoint class 
 */
public class CoPTrajectoryPoint extends YoFrameEuclideanTrajectoryPoint
{  
   protected final YoVariableRegistry registry;
   FramePoint3D localPosition = new FramePoint3D(); // dummy variable to hand out data with    
   private YoFramePoint yoFramePointInWorld;
   
   
   public CoPTrajectoryPoint(String namePrefix, String nameSuffix, YoVariableRegistry registry, ReferenceFrame[] referenceFrames)
   {
      super(namePrefix, nameSuffix, registry, referenceFrames);
      this.registry = registry;
   }
   
   public void setIncludingFrame(CoPTrajectoryPoint other)
   {
      registerReferenceFrame(other.getReferenceFrame());
      switchCurrentReferenceFrame(other.getReferenceFrame());
      set(other.getTime(), other.getPosition(), other.getLinearVelocity());
      putYoValuesIntoFrameWaypoint();
   }

   public FrameTuple3D getFrameTuple()
   {
      return yoFramePointInWorld.getFrameTuple();
   }

      
   public boolean epsilonEquals(FramePoint2D point, double threshold)
   {
      getPosition().checkReferenceFrameMatch(point);
      if (!MathTools.epsilonEquals(getPosition().getX(), point.getX(), threshold))
         return false;
      if (!MathTools.epsilonEquals(getPosition().getY(), point.getY(), threshold))
         return false;
      return true;
   }
   
   /**
    * Just a cleaner print than parent class 
    */
   @Override
   public String toString()
   {
      return "Time: " + getTime() + " Location: " + getPosition().toString();
   }
   
   public YoFramePoint buildUpdatedYoFramePointForVisualizationOnly()
   {
      if(!isReferenceFrameRegistered(ReferenceFrame.getWorldFrame()))
         registerReferenceFrame(ReferenceFrame.getWorldFrame());
      yoFramePointInWorld = new YoFramePoint(super.getNamePrefix() + "Viz", getReferenceFrame(), registry);
      getPosition().attachVariableChangedListener(new VariableChangedListener()
      {         
         private final FramePoint3D localFramePoint = new FramePoint3D();
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            getPosition().getFrameTupleIncludingFrame(localFramePoint);
            yoFramePointInWorld.setAndMatchFrame(localFramePoint);
         }
      });
      return yoFramePointInWorld;
   }
   
   public void notifyVariableChangedListeners()
   {
      getPosition().notifyVariableChangedListeners();
   }   
}
