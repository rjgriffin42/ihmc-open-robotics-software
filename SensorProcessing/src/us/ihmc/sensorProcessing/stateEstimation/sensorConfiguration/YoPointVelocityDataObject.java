package us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

/**
 * @author twan
 *         Date: 4/27/13
 */
public class YoPointVelocityDataObject extends PointVelocityDataObject
{
   private final YoFramePoint yoMeasurementPointInBodyFrame;
   private final YoFrameVector yoVelocityOfMeasurementPointInWorldFrame;

   public YoPointVelocityDataObject(String namePrefix, RigidBody body, YoVariableRegistry registry)
   {
      this(namePrefix, body.getParentJoint().getFrameAfterJoint(), registry);
      rigidBodyName = body.getName();
   }

   public YoPointVelocityDataObject(String namePrefix, ReferenceFrame frame, YoVariableRegistry registry)
   {
      bodyFixedReferenceFrameName = frame.getName();
      yoMeasurementPointInBodyFrame = new YoFramePoint(namePrefix + "PointBody", frame, registry);
      yoVelocityOfMeasurementPointInWorldFrame = new YoFrameVector(namePrefix + "PointVelocityWorld", ReferenceFrame.getWorldFrame(), registry);
   }

   @Override
   public void set(RigidBody rigidBody, FramePoint measurementPointInBodyFrame, FrameVector velocityOfMeasurementPointInWorldFrame, boolean isPointVelocityValid)
   {
      if (!this.rigidBodyName.isEmpty() && !this.rigidBodyName.equals(rigidBody.getName()))
      {
         throw new RuntimeException("Rigid body name does not match, desired: " + rigidBodyName + ", expected: " + rigidBody.getName());
      }
         
      this.rigidBodyName = rigidBody.getName();
      this.bodyFixedReferenceFrameName = measurementPointInBodyFrame.getReferenceFrame().getName();
      this.isPointVelocityValid = isPointVelocityValid;
      measurementPointInBodyFrame.getPoint(this.measurementPointInBodyFrame);
      velocityOfMeasurementPointInWorldFrame.getVector(this.velocityOfMeasurementPointInWorldFrame);

      yoMeasurementPointInBodyFrame.set(measurementPointInBodyFrame);
      yoVelocityOfMeasurementPointInWorldFrame.set(velocityOfMeasurementPointInWorldFrame);
   }

   @Override
   public Vector3d getVelocityOfMeasurementPointInWorldFrame()
   {
      yoVelocityOfMeasurementPointInWorldFrame.getVector(velocityOfMeasurementPointInWorldFrame);

      return velocityOfMeasurementPointInWorldFrame;
   }

   @Override
   public Point3d getMeasurementPointInBodyFrame()
   {
      yoMeasurementPointInBodyFrame.getPoint(measurementPointInBodyFrame);

      return measurementPointInBodyFrame;
   }
   
   public ReferenceFrame getReferenceFrame()
   {
      return yoMeasurementPointInBodyFrame.getReferenceFrame();
   }
   
   @Override
   public void set(PointVelocityDataObject other)
   {
      if(!other.bodyFixedReferenceFrameName.equals(bodyFixedReferenceFrameName))
      {
         throw new RuntimeException("Frame name does not match, desired: " + bodyFixedReferenceFrameName + ", expected: "
               + other.bodyFixedReferenceFrameName);  
      }
      
      rigidBodyName = other.rigidBodyName;
      isPointVelocityValid = other.isPointVelocityValid;
      yoMeasurementPointInBodyFrame.set(other.measurementPointInBodyFrame);
      yoVelocityOfMeasurementPointInWorldFrame.set(other.velocityOfMeasurementPointInWorldFrame);
   }
}
