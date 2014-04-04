package com.yobotics.simulationconstructionset.util.math.functionGenerator;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.utilities.test.JUnitTools;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class YoFunctionGeneratorTest
{
	YoFunctionGenerator yoFunctionGenerator;
   @Before
   public void setUp() throws Exception
   {
	   YoVariableRegistry registry = new YoVariableRegistry("testRegistry");
	   yoFunctionGenerator = new YoFunctionGenerator("test", registry);
   }

   @After
   public void tearDown() throws Exception
   {
   }


   @Test
   public void testZeroFrequencyDC()
   {
	   yoFunctionGenerator.setMode(YoFunctionGeneratorMode.DC);

	   double amplitude = 1.0;
	   yoFunctionGenerator.setAmplitude(amplitude);
	   yoFunctionGenerator.setFrequency(0.0);

	   for(double time = 0.0; time< 10.0; time+=0.01)
	   {
		   assertEquals(amplitude, yoFunctionGenerator.getValue(time), 1e-10);

	   }
   }
   
   @Test
   public void testOutputContinuityDuringFrequencyChange()
   {
	   double freq0=10,amp0=10;
	   yoFunctionGenerator.setMode(YoFunctionGeneratorMode.SINE);
	   yoFunctionGenerator.setAmplitude(amp0);
	   yoFunctionGenerator.setPhase(Math.PI/2);
	   
	   double t0 = 0.134,dt=0.001;
	   
	   /*
	   //regular setFrequency
	   yoFunctionGenerator.setFrequency(freq0);
	   System.out.println(yoFunctionGenerator.getValue(t0+2*dt));
	   yoFunctionGenerator.setFrequency(freq0*3);
	   System.out.println(yoFunctionGenerator.getValue(t0+3*dt));

	   //PhaseSync setFrequency
	   System.out.println("-");
	   yoFunctionGenerator.setFrequency(freq0);
	   System.out.println(yoFunctionGenerator.getValue(t0+2*dt));
	   yoFunctionGenerator.setFrequencyPhaseSync(freq0*3);
	   System.out.println(yoFunctionGenerator.getValue(t0+3*dt));
		*/

	   //Actual test
	   double output0,output1;
	   yoFunctionGenerator.setFrequency(freq0);
	   output0 = yoFunctionGenerator.getValue(t0+2*dt);
	   yoFunctionGenerator.setFrequencyWithContinuousOutput(freq0*3);
	   output1 = yoFunctionGenerator.getValue(t0+3*dt);
	   
	   double tolerance=6*dt*amp0*freq0*3;
	   assertEquals("|"+output0+"-"+output1+"|<" + tolerance,0,output1-output0, tolerance);
   }

   @Test
   public void testZeroFrequencySine()
   {
	   yoFunctionGenerator.setMode(YoFunctionGeneratorMode.SINE);

	   double amplitude = 1.0;
	   yoFunctionGenerator.setAmplitude(amplitude);
	   yoFunctionGenerator.setFrequency(0.0);
	   yoFunctionGenerator.setPhase(Math.PI/2.0);

	   for(double time = 0.0; time< 10.0; time+=0.01)
	   {
		  assertEquals(amplitude, yoFunctionGenerator.getValue(time), 1e-10);

	   }
   }

}
