package us.ihmc.robotics.filters;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.tools.testing.JUnitTools;

public class GlitchFilterForDataSetTest
{
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testBasic0()
   {
      GlitchFilterForDataSet glitchFilterForDataSet = new GlitchFilterForDataSet(10, 4.0);

      double stepSize = 0.01;
      int numberOfPoints = 1000;

      double[] data = new double[numberOfPoints];
      Random random = new Random(100L);
//      int glitchConter = 0;
      for (int i = 0; i < numberOfPoints; i++)
      {
         if (random.nextDouble() > 0.95 && i > 40 && i < (numberOfPoints - 40))
         {
            data[i] = 10.0;
//            glitchConter++;
         }
         else
            data[i] = Math.sin(i * stepSize * 2.0 * Math.PI);
      }

      double[] filteredFata = glitchFilterForDataSet.getGlitchFilteredSet(data);

      double[] answerFromMatlab = getDataforBasic0();

      JUnitTools.assertDoubleArrayEquals(filteredFata, answerFromMatlab, 1e-8);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testBasic1()
   {
      GlitchFilterForDataSet glitchFilterForDataSet = new GlitchFilterForDataSet(10, 4.0);

      double stepSize = 0.01;
      int numberOfPoints = 1000;

      double[] data = new double[numberOfPoints];

      for (int i = 0; i < numberOfPoints; i++)
      {
         data[i] = Math.sin(i * stepSize * 2.0 * Math.PI);
      }

      double[] filteredFata = glitchFilterForDataSet.getGlitchFilteredSet(data);

      JUnitTools.assertDoubleArrayEquals(filteredFata, data, 1e-8);

      ArrayList<Integer> listOfNumberOfPoints = glitchFilterForDataSet.getNumberOfPointsFiltered();

      for (Integer integer : listOfNumberOfPoints)
      {
         assertEquals((double) integer, 0.0, 1e-8);
      }

   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testBasic2()
   {
      GlitchFilterForDataSet glitchFilterForDataSet = new GlitchFilterForDataSet(10, 4.0);

      double[] data = getDataSourceforBasic2();

      double[] filteredFata = glitchFilterForDataSet.getGlitchFilteredSet(data);

      double[] answerFromMatlab = getDataAnswerforBasic2();

      JUnitTools.assertDoubleArrayEquals(filteredFata, answerFromMatlab, 1e-8);
   }

   private static double[] getDataforBasic0()
   {
      double[] ret = new double[] { 0, 0.06279052, 0.125333234, 0.187381315, 0.248689887, 0.309016994, 0.368124553, 0.425779292, 0.481753674, 0.535826795, 0.587785252, 0.63742399, 0.684547106, 0.728968627, 0.770513243, 0.809016994, 0.844327926,
            0.87630668, 0.904827052, 0.929776486, 0.951056516, 0.968583161, 0.982287251, 0.992114701, 0.998026728, 1, 0.998026728, 0.992114701, 0.982287251, 0.968583161, 0.951056516, 0.929776486, 0.904827052, 0.87630668, 0.844327926, 0.809016994,
            0.770513243, 0.728968627, 0.684547106, 0.63742399, 0.587785252, 0.535826795, 0.481753674, 0.425779292, 0.368124553, 0.309016994, 0.248689887, 0.187381315, 0.125333234, 0.06279052, 0, -0.06279052, -0.125333234, -0.187381315, -0.248689887,
            -0.309016994, -0.368124553, -0.425779292, -0.481753674, -0.535826795, -0.587785252, -0.63742399, -0.684547106, -0.728968627, -0.770513243, -0.809016994, -0.844327926, -0.87630668, -0.904827052, -0.927941784, -0.951056516, -0.968583161,
            -0.982287251, -0.992114701, -0.998026728, -1, -0.998026728, -0.992114701, -0.982287251, -0.968583161, -0.951056516, -0.929776486, -0.904827052, -0.87630668, -0.844327926, -0.809016994, -0.770513243, -0.728968627, -0.684547106,
            -0.63742399, -0.587785252, -0.535826795, -0.481753674, -0.425779292, -0.368124553, -0.309016994, -0.248689887, -0.18701156, -0.125333234, -0.06279052, 0, 0.06279052, 0.125333234, 0.187381315, 0.248689887, 0.309016994, 0.368124553,
            0.425779292, 0.481753674, 0.535826795, 0.587785252, 0.63742399, 0.684547106, 0.728968627, 0.768992811, 0.809016994, 0.844327926, 0.87630668, 0.904827052, 0.929776486, 0.951056516, 0.968583161, 0.982287251, 0.992114701, 0.998026728, 1,
            0.998026728, 0.992114701, 0.982287251, 0.966671884, 0.951056516, 0.929776486, 0.904827052, 0.87630668, 0.844327926, 0.809016994, 0.770513243, 0.728968627, 0.684547106, 0.63742399, 0.586625392, 0.535826795, 0.481753674, 0.425779292,
            0.368124553, 0.309016994, 0.248689887, 0.187381315, 0.125333234, 0.06279052, 0, -0.06279052, -0.125085917, -0.187381315, -0.248689887, -0.309016994, -0.368124553, -0.425779292, -0.481753674, -0.535826795, -0.587785252, -0.63742399,
            -0.684547106, -0.728968627, -0.770513243, -0.809016994, -0.844327926, -0.87630668, -0.904827052, -0.929776486, -0.951056516, -0.968583161, -0.982287251, -0.992114701, -0.998026728, -1, -0.998026728, -0.992114701, -0.982287251,
            -0.968583161, -0.951056516, -0.929776486, -0.904827052, -0.87630668, -0.844327926, -0.809016994, -0.770513243, -0.728968627, -0.684547106, -0.63742399, -0.587785252, -0.535826795, -0.481753674, -0.425779292, -0.368124553, -0.309016994,
            -0.248689887, -0.187381315, -0.125333234, -0.06279052, 0, 0.06279052, 0.125333234, 0.187381315, 0.248199154, 0.309016994, 0.368124553, 0.425779292, 0.481753674, 0.535826795, 0.587785252, 0.63742399, 0.684547106, 0.728968627, 0.770513243,
            0.809016994, 0.844327926, 0.87630668, 0.904827052, 0.929776486, 0.951056516, 0.968583161, 0.982287251, 0.992114701, 0.998026728, 1, 0.998026728, 0.992114701, 0.982287251, 0.968583161, 0.951056516, 0.929776486, 0.904827052, 0.87630668,
            0.844327926, 0.809016994, 0.770513243, 0.728968627, 0.684547106, 0.636166179, 0.587785252, 0.535826795, 0.481753674, 0.425779292, 0.368124553, 0.309016994, 0.248689887, 0.187381315, 0.125333234, 0.06279052, 1E-15, -0.06279052,
            -0.125333234, -0.187381315, -0.248689887, -0.309016994, -0.368124553, -0.425779292, -0.481753674, -0.535826795, -0.587785252, -0.63742399, -0.684547106, -0.728968627, -0.770513243, -0.807420584, -0.844327926, -0.87630668, -0.904827052,
            -0.929776486, -0.951056516, -0.966671884, -0.982287251, -0.992114701, -0.998026728, -1, -0.998026728, -0.992114701, -0.982287251, -0.968583161, -0.951056516, -0.929776486, -0.904827052, -0.87630668, -0.844327926, -0.809016994,
            -0.770513243, -0.728968627, -0.684547106, -0.63742399, -0.587785252, -0.535826795, -0.481753674, -0.425779292, -0.368124553, -0.309016994, -0.248689887, -0.187381315, -0.125333234, -0.062666617, -1E-15, 0.06279052, 0.125333234,
            0.187381315, 0.248199154, 0.309016994, 0.368124553, 0.425779292, 0.481753674, 0.535826795, 0.586625392, 0.63742399, 0.684547106, 0.728968627, 0.770513243, 0.809016994, 0.844327926, 0.87630668, 0.904827052, 0.929776486, 0.951056516,
            0.968583161, 0.982287251, 0.992114701, 0.998026728, 1, 0.998026728, 0.992114701, 0.982287251, 0.968583161, 0.949179824, 0.929776486, 0.904827052, 0.87630668, 0.844327926, 0.809016994, 0.770513243, 0.728968627, 0.684547106, 0.63742399,
            0.587785252, 0.535826795, 0.481753674, 0.425779292, 0.368124553, 0.309016994, 0.248689887, 0.187381315, 0.125333234, 0.06279052, 1E-15, -0.06279052, -0.125333234, -0.187381315, -0.248689887, -0.309016994, -0.368124553, -0.425779292,
            -0.481753674, -0.535826795, -0.587785252, -0.63742399, -0.684547106, -0.728968627, -0.770513243, -0.809016994, -0.844327926, -0.87630668, -0.904827052, -0.929776486, -0.951056516, -0.968583161, -0.982287251, -0.992114701, -0.998026728,
            -1, -0.998026728, -0.992114701, -0.982287251, -0.968583161, -0.951056516, -0.929776486, -0.904827052, -0.87630668, -0.844327926, -0.809016994, -0.770513243, -0.728968627, -0.684547106, -0.63742399, -0.587785252, -0.535826795,
            -0.481753674, -0.425779292, -0.368124553, -0.309016994, -0.248689887, -0.187381315, -0.125333234, -0.06279052, -1E-15, 0.062666617, 0.125333234, 0.187381315, 0.248689887, 0.309016994, 0.368124553, 0.425779292, 0.481753674, 0.535826795,
            0.587785252, 0.63742399, 0.684547106, 0.728968627, 0.770513243, 0.809016994, 0.844327926, 0.87630668, 0.904827052, 0.929776486, 0.951056516, 0.968583161, 0.982287251, 0.992114701, 0.998026728, 1, 0.998026728, 0.992114701, 0.982287251,
            0.968583161, 0.936705107, 0.936705107, 0.904827052, 0.87630668, 0.844327926, 0.809016994, 0.770513243, 0.728968627, 0.684547106, 0.63742399, 0.587785252, 0.535826795, 0.481753674, 0.425779292, 0.368124553, 0.309016994, 0.248689887,
            0.187381315, 0.125333234, 0.06279052, 1E-15, -0.06279052, -0.125333234, -0.187381315, -0.248689887, -0.309016994, -0.367398143, -0.425779292, -0.481753674, -0.535826795, -0.587785252, -0.63742399, -0.684547106, -0.728968627,
            -0.770513243, -0.809016994, -0.844327926, -0.87630668, -0.904827052, -0.929776486, -0.951056516, -0.966671884, -0.982287251, -0.99015699, -0.998026728, -1, -0.998026728, -0.992114701, -0.982287251, -0.968583161, -0.951056516,
            -0.929776486, -0.904827052, -0.87630668, -0.842661837, -0.809016994, -0.770513243, -0.728968627, -0.684547106, -0.63742399, -0.587785252, -0.535826795, -0.480803043, -0.425779292, -0.368124553, -0.309016994, -0.248689887, -0.187381315,
            -0.125333234, -0.06279052, -1E-15, 0.06279052, 0.125333234, 0.187381315, 0.248199154, 0.309016994, 0.368124553, 0.425779292, 0.481753674, 0.535826795, 0.587785252, 0.63742399, 0.684547106, 0.728968627, 0.770513243, 0.809016994,
            0.844327926, 0.87630668, 0.904827052, 0.929776486, 0.951056516, 0.968583161, 0.982287251, 0.992114701, 0.998026728, 1, 0.998026728, 0.992114701, 0.982287251, 0.968583161, 0.951056516, 0.929776486, 0.904827052, 0.87630668, 0.844327926,
            0.809016994, 0.770513243, 0.728968627, 0.684547106, 0.63742399, 0.587785252, 0.535826795, 0.481753674, 0.425779292, 0.368124553, 0.309016994, 0.248689887, 0.187381315, 0.125333234, 0.062666617, 5E-15, -0.06279052, -0.125333234,
            -0.187381315, -0.248689887, -0.309016994, -0.368124553, -0.425779292, -0.481753674, -0.535826795, -0.587785252, -0.63742399, -0.684547106, -0.728968627, -0.770513243, -0.809016994, -0.844327926, -0.87630668, -0.904827052, -0.929776486,
            -0.951056516, -0.968583161, -0.982287251, -0.992114701, -0.998026728, -1, -0.998026728, -0.992114701, -0.982287251, -0.968583161, -0.951056516, -0.929776486, -0.904827052, -0.87630668, -0.844327926, -0.807420584, -0.770513243,
            -0.727530174, -0.684547106, -0.63742399, -0.587785252, -0.535826795, -0.481753674, -0.395385334, -0.395385334, -0.309016994, -0.248689887, -0.187381315, -0.125333234, -0.06279052, -1E-15, 0.06279052, 0.125333234, 0.187381315,
            0.248689887, 0.309016994, 0.368124553, 0.425779292, 0.481753674, 0.535826795, 0.587785252, 0.63742399, 0.684547106, 0.728968627, 0.770513243, 0.809016994, 0.842661837, 0.87630668, 0.904827052, 0.929776486, 0.951056516, 0.968583161,
            0.982287251, 0.992114701, 0.996057351, 1, 0.998026728, 0.992114701, 0.982287251, 0.966671884, 0.951056516, 0.929776486, 0.904827052, 0.87630668, 0.844327926, 0.809016994, 0.770513243, 0.728968627, 0.684547106, 0.63742399, 0.587785252,
            0.535826795, 0.481753674, 0.425779292, 0.368124553, 0.309016994, 0.248689887, 0.187381315, 0.125333234, 0.06279052, -2E-15, -0.06279052, -0.125333234, -0.187381315, -0.248689887, -0.309016994, -0.368124553, -0.425779292, -0.481753674,
            -0.535826795, -0.587785252, -0.63742399, -0.684547106, -0.728968627, -0.770513243, -0.809016994, -0.844327926, -0.874577489, -0.904827052, -0.929776486, -0.951056516, -0.968583161, -0.982287251, -0.992114701, -0.998026728, -1,
            -0.998026728, -0.992114701, -0.982287251, -0.968583161, -0.951056516, -0.929776486, -0.904827052, -0.87630668, -0.844327926, -0.809016994, -0.770513243, -0.728968627, -0.684547106, -0.63742399, -0.586625392, -0.535826795, -0.481753674,
            -0.424939113, -0.368124553, -0.277752934, -0.277752934, -0.187381315, -0.125333234, -0.06279052, -2E-15, 0.06279052, 0.125333234, 0.187381315, 0.248689887, 0.309016994, 0.368124553, 0.425779292, 0.481753674, 0.535826795, 0.586625392,
            0.63742399, 0.684547106, 0.728968627, 0.770513243, 0.809016994, 0.844327926, 0.87630668, 0.904827052, 0.927941784, 0.951056516, 0.968583161, 0.982287251, 0.992114701, 0.998026728, 1, 0.998026728, 0.992114701, 0.982287251, 0.968583161,
            0.951056516, 0.929776486, 0.904827052, 0.87630668, 0.844327926, 0.809016994, 0.770513243, 0.728968627, 0.684547106, 0.63742399, 0.587785252, 0.535826795, 0.480803043, 0.425779292, 0.368124553, 0.309016994, 0.248689887, 0.187381315,
            0.125333234, 0.06279052, 5E-15, -0.06279052, -0.125333234, -0.187381315, -0.248689887, -0.309016994, -0.368124553, -0.424939113, -0.481753674, -0.535826795, -0.587785252, -0.63742399, -0.684547106, -0.728968627, -0.770513243,
            -0.809016994, -0.844327926, -0.87630668, -0.904827052, -0.929776486, -0.951056516, -0.968583161, -0.982287251, -0.992114701, -0.998026728, -1, -0.998026728, -0.992114701, -0.982287251, -0.968583161, -0.951056516, -0.929776486,
            -0.904827052, -0.87630668, -0.844327926, -0.809016994, -0.770513243, -0.728968627, -0.684547106, -0.63742399, -0.587785252, -0.535826795, -0.481753674, -0.425779292, -0.368124553, -0.309016994, -0.248689887, -0.187381315, -0.125333234,
            -0.06279052, -2E-15, 0.06279052, 0.125333234, 0.187381315, 0.248689887, 0.309016994, 0.368124553, 0.425779292, 0.480803043, 0.535826795, 0.587785252, 0.63742399, 0.684547106, 0.728968627, 0.770513243, 0.809016994, 0.844327926,
            0.87630668, 0.904827052, 0.929776486, 0.951056516, 0.968583161, 0.982287251, 0.992114701, 0.998026728, 1, 0.998026728, 0.992114701, 0.982287251, 0.968583161, 0.951056516, 0.929776486, 0.904827052, 0.87630668, 0.844327926, 0.809016994,
            0.768992811, 0.728968627, 0.684547106, 0.63742399, 0.587785252, 0.535826795, 0.481753674, 0.425779292, 0.368124553, 0.309016994, 0.248689887, 0.187381315, 0.125333234, 0.06279052, 2E-15, -0.06279052, -0.125333234, -0.187381315,
            -0.248689887, -0.309016994, -0.368124553, -0.425779292, -0.481753674, -0.535826795, -0.587785252, -0.63742399, -0.683196309, -0.728968627, -0.770513243, -0.809016994, -0.844327926, -0.874577489, -0.904827052, -0.929776486, -0.951056516,
            -0.968583161, -0.982287251, -0.992114701, -0.998026728, -1, -0.998026728, -0.992114701, -0.982287251, -0.968583161, -0.951056516, -0.929776486, -0.904827052, -0.87630668, -0.844327926, -0.809016994, -0.770513243, -0.727530174,
            -0.684547106, -0.63742399, -0.587785252, -0.535826795, -0.481753674, -0.425779292, -0.368124553, -0.309016994, -0.248689887, -0.187381315, -0.125085917, -0.06279052, -2E-15, 0.06279052, 0.125333234, 0.187381315, 0.248689887, 0.309016994,
            0.368124553, 0.425779292, 0.481753674, 0.535826795, 0.587785252, 0.63742399, 0.684547106, 0.728968627, 0.770513243, 0.809016994, 0.844327926, 0.87630668, 0.904827052, 0.929776486, 0.951056516, 0.968583161, 0.982287251, 0.992114701,
            0.998026728, 1, 0.998026728, 0.992114701, 0.982287251, 0.968583161, 0.951056516, 0.929776486, 0.904827052, 0.87630668, 0.844327926, 0.809016994, 0.770513243, 0.728968627, 0.684547106, 0.63742399, 0.587785252, 0.535826795, 0.481753674,
            0.425779292, 0.368124553, 0.309016994, 0.248689887, 0.187381315, 0.125333234, 0.06279052, 6E-15, -0.06279052, -0.125085917, -0.187381315, -0.248689887, -0.309016994, -0.368124553, -0.425779292, -0.481753674, -0.534769463, -0.587785252,
            -0.63742399, -0.684547106, -0.728968627, -0.770513243, -0.809016994, -0.844327926, -0.87630668, -0.904827052, -0.929776486, -0.951056516, -0.968583161, -0.982287251, -0.992114701, -0.998026728, -1, -0.998026728, -0.992114701,
            -0.982287251, -0.968583161, -0.951056516, -0.929776486, -0.904827052, -0.87630668, -0.844327926, -0.809016994, -0.770513243, -0.728968627, -0.684547106, -0.63742399, -0.587785252, -0.535826795, -0.481753674, -0.425779292, -0.368124553,
            -0.309016994, -0.248689887, -0.187381315, -0.125333234, -0.06279052,

      };

      return ret;

   }

   private static double[] getDataSourceforBasic2()
   {
      double[] ret = new double[] { -19.64800508, -19.93579817, -18.94707837, -16.769374, 10.2870273, -16.03098517, -16.73191188, -17.77069787, -17.72063168, -18.40895431, -19.635401, -21.39997175, -22.18842677, -18.22129362, -23.50240177,
            -23.92778936, -24.11545006, -23.94039344, -15.55553138, -23.17679646, -22.43875774, -22.55114409, -21.9380958, -21.33730148, -20.86184769, -20.47357211, -19.83531567, -18.80948386, -18.30882193, -19.76039143, -21.14964078, -22.25109704,
            -22.68908871, -22.20068074, -21.46229191, -20.44906418, -19.88573197, -20.66158292, -20.99909209, -22.62571821, -23.1144763, -22.86379522, -22.42615366, -22.35122943, -22.07534019, -21.48785017, -21.14964078, -20.59926276, -19.86052382,
            -18.90961625, -17.55747891, -17.90829238, -18.78427571, -20.4865263, -21.56207418, -22.03822819, -39.97172872, -21.0866204, -2.678366309, -20.1112049, -19.93614828, -19.41027819, -19.3101458, -20.13571283, -20.02297636, -20.57405461,
            -21.04950839, -21.66290679, -21.81345549, -21.55017033, -20.87445177, -19.94805213, 32.71318097, -17.95800846, -17.32010213, -17.72063168, 31.24900739, -6.420376649, -21.42482979, -21.41222571, -20.56145053, -20.03558044, -18.85919994,
            -18.74716371, -18.40895431, -18.48457877, -3.153820092, -18.19608546, -18.57140686, -19.2968415, -20.1112049, -20.66158292, -20.41160206, -19.73518328, -18.28361377, -17.11983735, -15.71868416, -15.13049391, -15.16760591, -16.82014042,
            -18.63442724, -19.27233357, -19.14699303, -18.82208794, -18.29621785, -31.31132754, -16.4812308, -16.83204427, -16.51904303, -15.73058801, -15.09268168, -15.65566377, -16.88246058, -3.6166698, -19.23522157, -19.38507004, -18.43416247,
            -17.23257382, -15.16760591, -14.34133866, -13.15305431, -12.42691945, -13.61590402, -15.78100431, -16.08070125, -16.30617418, -16.58206341, -15.5058153, -15.12979368, -14.79228451, -14.59201974, -14.55490774, -13.50316755, -12.60267629,
            -12.11391821, -13.2902987, -14.64173582, -16.53094688, -18.37114208, -3.753213964, -15.81811632, -14.05354557, -13.11524208, -27.63093713, -12.10131413, -12.2511626, -13.22867877, -13.31550686, -13.22727832, -13.6656201, -14.71736028,
            -14.36584658, -14.31683073, -13.11524208, -12.47663553, -11.35067134, -10.54961224, -10.52440408, -10.6252367, -12.46403145, -14.56751181, -16.74381573, 1.100756036, -14.45407512, -12.88976915, -11.30165548, -10.61123217, -10.32413931,
            -10.11267091, -10.07485868, -9.925010211, -9.81157352, -10.8367051, -12.46403145, -13.04101807, 3.729406264, -11.71338866, -10.8633137, -9.685532753, -8.523156786, -7.682885003, -8.146434937, -9.586100592, -11.60135242, -12.87716508,
            -13.61660424, -12.07750643, -11.20082287, -9.710740906, -11.77640904, -13.72864048, -14.35464296, -12.96399316, -11.88844528, -11.37587949, -9.798969444, -8.947494037, -8.335496088, -7.646473226, -7.745905387, -7.859342078, -7.507828382,
            -7.171719669, -7.446208451, -7.583452842, -7.395792144, -7.283755906, -7.29495953, -7.146511515, -7.108699285, -7.096095208, -7.159115592, -7.107298832, -6.958850817, -6.89442998, -6.957450364, -7.007866671, -7.007866671, -7.045678901,
            -7.170319216, -7.345375837, -7.521832912, -7.633869149, -7.684285456, -7.684285456, -7.684285456, -7.684285456, -7.621265073, -7.509228835, -7.371984444, -7.157715139, -6.895830433, -6.733377888, -6.408472799, -5.907110635, -5.269904533,
            -4.631297978, -3.291064484, -1.490081962, 0.312301013, 1.728158967, 4.39182052, 5.405748471, 5.968730566, 7.558244689, 8.810249646, 10.17429084, 12.97799769, 14.06614965, 15.34336276, 17.12053758, 18.02102884, 18.37114208, 18.93412418,
            19.11058125, 18.78427571, 18.6596354, 18.98454049, 19.36126234, 19.93544805, 20.76171531, 21.42553002, 22.26300089, 23.08926815, 23.9155354, 24.69138635, 25.16754036,

      };

      return ret;
   }

   private static double[] getDataAnswerforBasic2()
   {
      double[] ret = new double[] { -19.64800508, -19.93579817, -18.94707837, -16.769374, -16.40017958, -16.03098517, -16.73191188, -17.77069787, -17.72063168, -18.40895431, -19.635401, -21.39997175, -19.81063268, -18.22129362, -21.07454149,
            -23.92778936, -24.11545006, -23.64612326, -23.64612326, -23.17679646, -22.43875774, -22.55114409, -21.9380958, -21.33730148, -20.86184769, -20.47357211, -19.83531567, -18.80948386, -18.30882193, -19.76039143, -21.14964078, -22.25109704,
            -22.68908871, -22.20068074, -21.46229191, -20.44906418, -19.88573197, -20.66158292, -20.99909209, -22.62571821, -23.1144763, -22.86379522, -22.42615366, -22.35122943, -22.07534019, -21.48785017, -21.14964078, -20.59926276, -19.86052382,
            -18.90961625, -17.55747891, -17.90829238, -18.78427571, -20.4865263, -21.56207418, -22.03822819, -21.56242429, -21.0866204, -20.59891265, -20.1112049, -19.93614828, -19.41027819, -19.3101458, -20.13571283, -20.02297636, -20.57405461,
            -21.04950839, -21.66290679, -21.81345549, -21.55017033, -20.87445177, -19.94805213, -18.95303029, -17.95800846, -17.32010213, -17.72063168, -17.23414933, -19.32948956, -21.42482979, -21.41222571, -20.56145053, -20.03558044, -18.85919994,
            -18.74716371, -18.40895431, -18.48457877, -18.34033212, -18.19608546, -18.57140686, -19.2968415, -20.1112049, -20.66158292, -20.41160206, -19.73518328, -18.28361377, -17.11983735, -15.71868416, -15.13049391, -15.16760591, -16.82014042,
            -18.63442724, -19.27233357, -19.14699303, -18.82208794, -18.29621785, -17.38872432, -16.4812308, -16.83204427, -16.51904303, -15.73058801, -15.09268168, -15.65566377, -16.88246058, -18.05884107, -19.23522157, -19.38507004, -18.43416247,
            -17.23257382, -15.16760591, -14.34133866, -13.15305431, -12.42691945, -13.61590402, -15.78100431, -16.08070125, -16.30617418, -16.58206341, -15.5058153, -15.12979368, -14.79228451, -14.59201974, -14.55490774, -13.50316755, -12.60267629,
            -12.11391821, -13.2902987, -14.64173582, -16.53094688, -18.37114208, -17.0946292, -15.81811632, -14.05354557, -13.11524208, -12.60827811, -12.10131413, -12.2511626, -13.22867877, -13.31550686, -13.22727832, -13.6656201, -14.71736028,
            -14.36584658, -14.31683073, -13.11524208, -12.47663553, -11.35067134, -10.54961224, -10.52440408, -10.6252367, -12.46403145, -14.56751181, -16.74381573, -15.59894543, -14.45407512, -12.88976915, -11.30165548, -10.61123217, -10.32413931,
            -10.11267091, -10.07485868, -9.925010211, -9.81157352, -10.8367051, -12.46403145, -13.04101807, -12.37720336, -11.71338866, -10.8633137, -9.685532753, -8.523156786, -7.682885003, -8.146434937, -9.586100592, -11.60135242, -12.87716508,
            -13.61660424, -12.07750643, -11.20082287, -9.710740906, -11.77640904, -13.72864048, -14.35464296, -12.96399316, -11.88844528, -11.37587949, -9.798969444, -8.947494037, -8.335496088, -7.646473226, -7.745905387, -7.859342078, -7.507828382,
            -7.171719669, -7.446208451, -7.583452842, -7.395792144, -7.283755906, -7.29495953, -7.146511515, -7.108699285, -7.096095208, -7.159115592, -7.107298832, -6.958850817, -6.89442998, -6.957450364, -7.007866671, -7.007866671, -7.045678901,
            -7.170319216, -7.345375837, -7.521832912, -7.633869149, -7.684285456, -7.684285456, -7.684285456, -7.684285456, -7.621265073, -7.509228835, -7.371984444, -7.157715139, -6.895830433, -6.733377888, -6.408472799, -5.907110635, -5.269904533,
            -4.631297978, -3.291064484, -1.490081962, 0.312301013, 1.728158967, 4.39182052, 5.405748471, 5.968730566, 7.558244689, 8.810249646, 10.17429084, 12.97799769, 14.06614965, 15.34336276, 17.12053758, 18.02102884, 18.37114208, 18.93412418,
            19.11058125, 18.78427571, 18.6596354, 18.98454049, 19.36126234, 19.93544805, 20.76171531, 21.42553002, 22.26300089, 23.08926815, 23.9155354, 24.69138635, 25.16754036,

      };

      return ret;
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testNull()
   {
      double[] dataSet = null;
      double[] ret = GlitchFilterForDataSet.getSetFilteredWithWindowedAverage(dataSet, 1);

      assertEquals(null, ret);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testWindowSize()
   {
      double[] dataSet = new double[] { 1.0, 5345.345, 34598034.40899, 48904980.9322398, 234.234, 7654.23890 };
      int windowSizeOnEitherSide = 3;
      double[] ret = GlitchFilterForDataSet.getSetFilteredWithWindowedAverage(dataSet, windowSizeOnEitherSide);

      JUnitTools.assertDoubleArrayEquals(dataSet, ret, 1e-8);

      dataSet = new double[] { 1.0, 5345.345, 34598034.40899, 48904980.9322398, 234.234, 7654.23890, 567.234 };
      ret = GlitchFilterForDataSet.getSetFilteredWithWindowedAverage(dataSet, windowSizeOnEitherSide);

      for(int i = 0; i < dataSet.length; i++)
      {
         assertNotEquals(dataSet[i], ret[i], 1e-8);
      }
   }

   
   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testData()
   {
      double[] dataSet = new double[] { 0.191761336, 0.701365152, 0.670840424, 0.219631948, 0.08436868, 0.563715164, 0.900281017, 0.910817898, 0.465219833, 0.252544002, 0.605959691, 0.179189433, 0.990209646, 0.845536796, 0.803289242, 0.206219591,
            0.961587009, 0.042696015, 0.129714883, 0.331480168, 0.688581645, 0.370834714, 0.299117554, 0.133836507, 0.853608653, 0.043567527, 0.060239285, 0.069681999, 0.664349517, 0.212212533, 0.63246065, 0.97953661, 0.955628809, 0.073849966,
            0.858234479, 0.009643666, 0.429069297, 0.925070226, 0.727177783, 0.931716238, 0.212067611, 0.025299622, 0.826825957, 0.913296044, 0.962683791, 0.75052438, 0.607596536, 0.17719696, 0.968524695, 0.528280179, 0.659702894, 0.249516652,
            0.774485908, 0.885395586, 0.670705795, 0.220334711, 0.113960946, 0.462188593 };

      double[] dataSetOriginal = Arrays.copyOf(dataSet, dataSet.length);

      double[] answer = new double[] { 0.373593508, 0.405280451, 0.475994817, 0.530347702, 0.523111272, 0.529864902, 0.519264295, 0.464636407, 0.550256152, 0.634830387, 0.661449729, 0.584331792, 0.589972805, 0.543025714, 0.529378034, 0.498880309,
            0.555479444, 0.486660007, 0.425946758, 0.351563121, 0.423495239, 0.321493074, 0.323442326, 0.316772006, 0.353757489, 0.300827588, 0.329897136, 0.405499253, 0.496809509, 0.410169655, 0.500688205, 0.49506647, 0.534998392, 0.56396736,
            0.621185721, 0.654436342, 0.569162008, 0.465792099, 0.549456098, 0.555574049, 0.661467397, 0.697184628, 0.661909774, 0.600800793, 0.604890622, 0.640025352, 0.710514604, 0.646369126, 0.630945777, 0.622358199, 0.613489467, 0.570460376,
            0.563434152, 0.507174585, 0.504536386, 0.482369742, 0.52117859, 0.470517126, };

      int windowSizeOnEitherSide = 4;
      double[] ret = GlitchFilterForDataSet.getSetFilteredWithWindowedAverage(dataSet, windowSizeOnEitherSide);

      JUnitTools.assertDoubleArrayEquals(ret, answer, 1e-8);

      //Make sure that its does not change the original data set
      JUnitTools.assertDoubleArrayEquals(dataSet, dataSetOriginal, 1e-8);

      windowSizeOnEitherSide = 0;
      ret = GlitchFilterForDataSet.getSetFilteredWithWindowedAverage(dataSet, windowSizeOnEitherSide);

      JUnitTools.assertDoubleArrayEquals(ret, dataSet, 1e-8);
   }
}
