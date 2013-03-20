package us.ihmc.sensorProcessing.simulatedSensors;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Quat4d;

import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameQuaternion;

public class SimulatedOrientationSensor extends SimulatedSensor<Matrix3d>
{
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final ReferenceFrame frameUsedForPerfectOrientation;

   private final Quat4d tempQuaternionOne = new Quat4d();
   private final Quat4d tempQuaternionTwo = new Quat4d();

   private final AxisAngle4d tempAxisAngle = new AxisAngle4d();
   
   private final Matrix3d rotationMatrix = new Matrix3d();
   private final YoFrameQuaternion yoFrameQuaternionPerfect, yoFrameQuaternionNoisy;
   private final DoubleYoVariable rotationAngleNoise;
   
   private final ControlFlowOutputPort<Matrix3d> orientationOutputPort = createOutputPort();

   public SimulatedOrientationSensor(String name, ReferenceFrame frameUsedForPerfectOrientation, YoVariableRegistry registry)
   {
      this.frameUsedForPerfectOrientation = frameUsedForPerfectOrientation;
      this.yoFrameQuaternionPerfect = new YoFrameQuaternion(name + "Perfect", ReferenceFrame.getWorldFrame(), registry);
      this.yoFrameQuaternionNoisy = new YoFrameQuaternion(name + "Noisy", ReferenceFrame.getWorldFrame(), registry);
            
      rotationAngleNoise = new DoubleYoVariable(name + "Noise", registry);
   }

   public void startComputation()
   {
      frameUsedForPerfectOrientation.getTransformToDesiredFrame(worldFrame).get(rotationMatrix);
      yoFrameQuaternionPerfect.set(rotationMatrix);

      corrupt(rotationMatrix);
      orientationOutputPort.setData(rotationMatrix);
      yoFrameQuaternionNoisy.set(rotationMatrix);
      
      yoFrameQuaternionPerfect.get(tempQuaternionOne);
      yoFrameQuaternionNoisy.get(tempQuaternionTwo);
      
      tempQuaternionTwo.inverse();
      tempQuaternionOne.mul(tempQuaternionTwo);
      
      tempAxisAngle.set(tempQuaternionOne);
      
      double noiseAngle = tempAxisAngle.getAngle();
      if (noiseAngle > Math.PI) noiseAngle = noiseAngle - 2.0 * Math.PI;
      if (noiseAngle < -Math.PI) noiseAngle = noiseAngle + 2.0 * Math.PI;
      
      rotationAngleNoise.set(Math.abs(noiseAngle));
   }

   public void waitUntilComputationIsDone()
   {
      // empty
   }

   public ControlFlowOutputPort<Matrix3d> getOrientationOutputPort()
   {
      return orientationOutputPort;
   }
}
