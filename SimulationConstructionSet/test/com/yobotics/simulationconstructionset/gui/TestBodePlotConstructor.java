package com.yobotics.simulationconstructionset.gui;

import junit.framework.TestCase;
import us.ihmc.utilities.linearDynamicSystems.TransferFunction;
import us.ihmc.utilities.test.HumanAssistedTestFrame;

/**
 * <p>Title: SimulationConstructionSet</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2000</p>
 *
 * <p>Company: Yobotics, Inc.</p>
 *
 * @author not attributable
 * @version 1.0
 */
public class TestBodePlotConstructor extends TestCase
{
   public TestBodePlotConstructor(String name)
   {
      super(name);
   }

   protected void setUp() throws Exception
   {
      super.setUp();
   }

   protected void tearDown() throws Exception
   {
      super.tearDown();
   }

   public void DONTtestSimpleFilter()
   {
      // int n = 1000;
      double T = 5.0;
      double DT = 0.0025;    // T/((int) n);

      int n = ((int) (T / DT));

      double[] time = generateLinearSpace(n, 0.0, DT);

      double freqHz = 10.0;
      @SuppressWarnings("unused")
      double amplitude = 1.0;

      double[] input = generateChirp(time, freqHz);
      double[] output = filter(filter(filter(input)));

      double[][] timeAndInputAndOutput = new double[][]
      {
         time, input, output
      };
      plotTimeInputOutputBode(timeAndInputAndOutput);

      HumanAssistedTestFrame humanAssistedTestFrame = new HumanAssistedTestFrame("HumanAssistedTestFrame");
      humanAssistedTestFrame.waitForButtonPush();
      humanAssistedTestFrame.setVisible(false);
   }

   public void DONTtestSingleFreqPhaseShift()
   {
      // int n = 1000;
      double T = 5.0;
      double DT = 0.0025;    // T/((int) n);

      int n = ((int) (T / DT));

      double[] time = generateLinearSpace(n, 0.0, DT);

      double freqHz = 10.0;
      double amplitude = 1.0;
      double phaseShiftDegrees = 90.0;    // 1.0;

      double[] input = generateSineWave(time, amplitude, freqHz, 0.0);
      double[] output = generateSineWave(time, amplitude, freqHz, phaseShiftDegrees);

      double[][] timeAndInputAndOutput = new double[][]
      {
         time, input, output
      };
      plotTimeInputOutputBode(timeAndInputAndOutput);

      HumanAssistedTestFrame humanAssistedTestFrame = new HumanAssistedTestFrame("HumanAssistedTestFrame");
      humanAssistedTestFrame.waitForButtonPush();
      humanAssistedTestFrame.setVisible(false);
   }

   public void testSecondOrderResponse()
   {
      double T = 5.0;
      double DT = 0.0025;    // T/((int) n);
      int n = ((int) (T / DT));

      double[] time = generateLinearSpace(n, 0.0, DT);
      double freqHz = 10.0;

      double[] input = generateChirp(time, freqHz);

      double wn = 2.0 * Math.PI * 10.0;
      double zeta = 0.1;

      double[] output = produceSecondOrderResponse(wn, zeta, input, DT);

      double[][] timeAndInputAndOutput = new double[][]
      {
         time, input, output
      };
      plotTimeInputOutputBode(timeAndInputAndOutput);

      deriveSecondOrderResponseUsingTransferFunctions(wn, zeta);

      HumanAssistedTestFrame humanAssistedTestFrame = new HumanAssistedTestFrame("HumanAssistedTestFrame");
      humanAssistedTestFrame.waitForButtonPush();
      humanAssistedTestFrame.setVisible(false);
   }

   private static void deriveSecondOrderResponseUsingTransferFunctions(double wn, double zeta)
   {
      // Derive the second order response using a transfer function:
      double[] numerator = new double[] {wn * wn};
      double[] denominator = new double[] {1.0, 2.0 * zeta * wn, wn * wn};
      TransferFunction secondOrderTransferFunction = new TransferFunction(numerator, denominator);

      int numFreqs = 10000;
      double w0 = 0.2;
      double wIncrement = 0.2;

      int wnIndex = (int) ((wn - w0) / wIncrement);

      double[] ws = generateLinearSpace(numFreqs, w0, wIncrement);
      double[] magnitudes = secondOrderTransferFunction.getMagnitude(ws);
      double[] phases = secondOrderTransferFunction.getPhase(ws);

      double wnMagnitude = magnitudes[wnIndex];
      double wnPhase = phases[wnIndex];

      double wnMagnitudeDecibels = 20.0 * Math.log10(wnMagnitude);
      double wnPhaseDegrees = wnPhase * 180.0 / Math.PI;

      System.out.println("wnIndex = " + wnIndex + ", wnMagnitude = " + wnMagnitude + ", wnPhase = " + wnPhase);
      System.out.println("wnMagnitudeDecibels = " + wnMagnitudeDecibels + ", wnPhaseDegrees = " + wnPhaseDegrees);

      BodePlotConstructor.plotBodeForTransferFunction("2nd order Transfer Function", secondOrderTransferFunction, ws);

   }

   private static double[] generateChirp(double[] time, double freqHz)
   {
      double[] ret = new double[time.length];

      for (int i = 0; i < time.length; i++)
      {
         ret[i] = Math.sin(2.0 * Math.PI * freqHz * time[i] * time[i]);
      }

      return ret;
   }

   private static double[] generateSineWave(double[] time, double amplitude, double freqHz, double phaseDegrees)
   {
      double[] ret = new double[time.length];

      for (int i = 0; i < time.length; i++)
      {
         ret[i] = amplitude * Math.sin(2.0 * Math.PI * freqHz * time[i] + phaseDegrees * Math.PI / 180.0);

//       input[i] = amplitude * Math.cos(2.0 * Math.PI * freq * time[i] + phase) + amplitude/10.0 * Math.cos(2.0 * Math.PI * 10.0 * freq * time[i] + phase);
      }

      return ret;
   }

   private static double[] generateLinearSpace(int numPoints, double x0, double xIncrement)
   {
      double[] ret = new double[numPoints];
      for (int i = 0; i < numPoints; i++)
      {
         ret[i] = x0 + xIncrement * ((double) i);
      }

      return ret;
   }

   private static double[] filter(double[] input)
   {
      int n = input.length;
      double[] output = new double[n];

      double alpha = 0.5;    // 0.9; // 0.99; //0.2; //0.5; //0.95;
      double previousOutput = 0.0;

      for (int i = 0; i < n; i++)
      {
         output[i] = alpha * previousOutput + (1.0 - alpha) * input[i];
         previousOutput = output[i];
      }

      return output;
   }

   private static double[] produceSecondOrderResponse(double wn, double zeta, double[] input, double DT)
   {
      double[] output = new double[input.length];

      double ydd = 0.0;
      double yd = 0.0;
      double y = 0.0;

      for (int i = 0; i < input.length; i++)
      {
         double x = input[i];

         ydd = -2.0 * zeta * wn * yd - wn * wn * y + wn * wn * x;
         yd = yd + DT * ydd;
         y = y + DT * yd;

         output[i] = y;
      }

      return output;
   }

   private static void plotTimeInputOutputBode(double[][] timeAndInputAndOutput)
   {
      double[] time = timeAndInputAndOutput[0];
      double[] input = timeAndInputAndOutput[1];
      double[] output = timeAndInputAndOutput[2];

      BodePlotConstructor.plotFFT("input", time, input);
      BodePlotConstructor.plotFFT("output", time, output);

      BodePlotConstructor.plotBodeFromInputToOutput("input", "output", time, input, output);
   }

// public static void main(String[] args)
// {
// //      testSingleFreqPhaseShift();
// //      testSimpleFilter();
//   testSecondOrderResponse();
// }


}
