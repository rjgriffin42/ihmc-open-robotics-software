package us.ihmc.sensorProcessing.simulatedSensors;

import javax.vecmath.Vector3d;

import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class SimulatedPointVelocitySensor extends SimulatedSensor<Vector3d>
{
   private final RigidBody rigidBody;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final FramePoint pointToMeasureVelocityOf;
   private final FramePoint tempPointToMeasureVelocityOf = new FramePoint();
   private final FrameVector pointVelocityFrameVector = new FrameVector();

   private final Twist twist = new Twist();

   private final Vector3d pointVelocity = new Vector3d();
   private final YoFrameVector yoFrameVectorPerfect, yoFrameVectorNoisy;

   private final TwistCalculator twistCalculator;

   private final ControlFlowOutputPort<Vector3d> pointVelocityOutputPort = createOutputPort("pointVelocityOutputPort");

   public SimulatedPointVelocitySensor(String name, RigidBody rigidBody, FramePoint pointToMeasureVelocityOf,
           TwistCalculator twistCalculator, YoVariableRegistry registry)
   {
      this.rigidBody = rigidBody;
      this.twistCalculator = twistCalculator;

      this.pointToMeasureVelocityOf = new FramePoint(pointToMeasureVelocityOf);

      this.yoFrameVectorPerfect = new YoFrameVector(name + "Perfect", ReferenceFrame.getWorldFrame(), registry);
      this.yoFrameVectorNoisy = new YoFrameVector(name + "Noisy", ReferenceFrame.getWorldFrame(), registry);
   }

   public void startComputation()
   {
      twistCalculator.packTwistOfBody(twist, rigidBody);
      twist.changeFrame(twist.getBaseFrame());
      
      tempPointToMeasureVelocityOf.setIncludingFrame(pointToMeasureVelocityOf);
      tempPointToMeasureVelocityOf.changeFrame(twist.getBaseFrame());
      twist.packVelocityOfPointFixedInBodyFrame(pointVelocityFrameVector, tempPointToMeasureVelocityOf);
      
      pointVelocityFrameVector.changeFrame(worldFrame);
      pointVelocityFrameVector.get(pointVelocity);
      
      yoFrameVectorPerfect.set(pointVelocity);

      corrupt(pointVelocity);
      yoFrameVectorNoisy.set(pointVelocity);
      
      pointVelocityOutputPort.setData(pointVelocity);
   }

   public void waitUntilComputationIsDone()
   {
      // empty
   }

   public ControlFlowOutputPort<Vector3d> getPointVelocityOutputPort()
   {
      return pointVelocityOutputPort;
   }

   public void initialize()
   {
//    empty
   }
}
