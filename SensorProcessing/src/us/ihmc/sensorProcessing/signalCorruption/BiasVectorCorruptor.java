package us.ihmc.sensorProcessing.signalCorruption;

import java.util.Random;

import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class BiasVectorCorruptor implements SignalCorruptor<Vector3d>
{
   private final YoVariableRegistry registry;
   private final Random random;
   private final Vector3d biasVector = new Vector3d();
   private final DoubleYoVariable standardDeviation;
   private final YoFrameVector biasYoFrameVector;
   private final double squareRootOfUpdateDT;
   
   public BiasVectorCorruptor(long seed, String namePrefix, double updateDT, YoVariableRegistry parentRegistry)
   {
      this.random = new Random(seed);
      this.registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());
      this.standardDeviation = new DoubleYoVariable(namePrefix + "StdDev", registry);
      this.biasYoFrameVector = new YoFrameVector(namePrefix + "Bias", ReferenceFrame.getWorldFrame(), registry);

      this.squareRootOfUpdateDT = Math.sqrt(updateDT);
      
      parentRegistry.addChild(registry);
   }

   public void corrupt(Vector3d signal)
   {
      double std = standardDeviation.getDoubleValue();
      double biasUpdateX = std * random.nextGaussian() * squareRootOfUpdateDT;
      double biasUpdateY = std * random.nextGaussian() * squareRootOfUpdateDT;
      double biasUpdateZ = std * random.nextGaussian() * squareRootOfUpdateDT;

      biasYoFrameVector.add(biasUpdateX, biasUpdateY, biasUpdateZ);
      biasYoFrameVector.get(biasVector);

      signal.add(biasVector);
   }

   public void setStandardDeviation(double standardDeviation)
   {
      this.standardDeviation.set(standardDeviation);
   }
   
   public void setBias(Vector3d bias)
   {
      this.biasYoFrameVector.set(bias);
   }
}
