package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.recursion;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

public class EntryCMPRecursionMultipliersTest
{
   private static final double epsilon = 0.0001;

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderOneStepOneRegisteredTwoCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 1;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         swingSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
         swingSplitFractions.add(swingSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions,
            registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
            swingSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = true;

         entryCMPRecursionMultipliers.compute(1, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(0).getDoubleValue()) * doubleSupportDurations.get(0).getDoubleValue() +
               swingSplitFractions.get(0).getDoubleValue() * singleSupportDurations.get(0).getDoubleValue();
         double currentTimeSpentOnExitCMP = (1.0 - swingSplitFractions.get(0).getDoubleValue()) * singleSupportDurations.get(0).getDoubleValue() +
               transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();
         double upcomingTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(1).getDoubleValue()) * doubleSupportDurations.get(1).getDoubleValue();

         double exitCMPMultiplier = Math.exp(-omega * currentTimeSpentOnExitCMP) * (1.0 - Math.exp(-omega * upcomingTimeSpentOnEntryCMP));

         Assert.assertEquals(exitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(1, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         exitCMPMultiplier = Math.exp(-omega * currentTimeSpentOnEntryCMP) * exitCMPMultiplier;
         Assert.assertEquals(exitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderOneStepTwoRegisteredTwoCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 2;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         swingSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
         swingSplitFractions.add(swingSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
            swingSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = true;

         entryCMPRecursionMultipliers.compute(1, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(0).getDoubleValue()) * doubleSupportDurations.get(0).getDoubleValue() +
               swingSplitFractions.get(0).getDoubleValue() * singleSupportDurations.get(0).getDoubleValue();
         double currentTimeSpentOnExitCMP = (1.0 - swingSplitFractions.get(0).getDoubleValue()) * singleSupportDurations.get(0).getDoubleValue() +
               transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();
         double upcomingTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(1).getDoubleValue()) * doubleSupportDurations.get(1).getDoubleValue() +
               swingSplitFractions.get(1).getDoubleValue() * singleSupportDurations.get(1).getDoubleValue();

         double exitCMPMultiplier = Math.exp(-omega * currentTimeSpentOnExitCMP) * (1.0 - Math.exp(-omega * upcomingTimeSpentOnEntryCMP));
         Assert.assertEquals(exitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(1, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         exitCMPMultiplier = Math.exp(-omega * currentTimeSpentOnEntryCMP) * exitCMPMultiplier;
         Assert.assertEquals(exitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderOneStepOneRegisteredOneCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 1;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = false;

         entryCMPRecursionMultipliers.compute(1, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentTimeOnCMP = (1.0 - transferSplitFractions.get(0).getDoubleValue()) * doubleSupportDurations.get(0).getDoubleValue() +
               singleSupportDurations.get(0).getDoubleValue() + transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();
         double upcomingStepDuration = (1.0 - transferSplitFractions.get(1).getDoubleValue()) * doubleSupportDurations.get(1).getDoubleValue();

         double entryCMPMultiplier = Math.exp(-omega * currentTimeOnCMP) * (1.0 - Math.exp(-omega * upcomingStepDuration));

         Assert.assertEquals(entryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(entryCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(1, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(entryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(entryCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderOneStepTwoRegisteredOneCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 2;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = false;

         entryCMPRecursionMultipliers.compute(1, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double timeOnCurrentCMP = (1.0 - transferSplitFractions.get(0).getDoubleValue()) * doubleSupportDurations.get(0).getDoubleValue()
               + singleSupportDurations.get(0).getDoubleValue()
               + transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();
         double timeOnNextCMP = (1.0 - transferSplitFractions.get(1).getDoubleValue()) * doubleSupportDurations.get(1).getDoubleValue()
               + singleSupportDurations.get(1).getDoubleValue()
               + transferSplitFractions.get(2).getDoubleValue() * doubleSupportDurations.get(2).getDoubleValue();

         double entryCMPMultiplier = Math.exp(-omega * timeOnCurrentCMP) * (1.0 - Math.exp(-omega * timeOnNextCMP));
         Assert.assertEquals(entryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(entryCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(1, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(entryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(entryCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderTwoStepsOneRegisteredTwoCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 1;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         swingSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
         swingSplitFractions.add(swingSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
            swingSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = true;

         entryCMPRecursionMultipliers.compute(2, 1, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();

         double currentTimeSpentOnExitCMP = (1.0 - swingSplitFractions.get(0).getDoubleValue()) * singleSupportDurations.get(0).getDoubleValue() +
               transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();
         double upcomingTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(1).getDoubleValue()) * doubleSupportDurations.get(1).getDoubleValue() +
               swingSplitFractions.get(1).getDoubleValue() * singleSupportDurations.get(1).getDoubleValue();

         double firstExitCMPMultiplier = Math.exp(-omega * currentTimeSpentOnExitCMP) * (1.0 - Math.exp(-omega * upcomingTimeSpentOnEntryCMP));

         Assert.assertEquals(firstExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(firstExitCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(2, 1, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         firstExitCMPMultiplier = Math.exp(-omega * currentStepDuration) * (1.0 - Math.exp(-omega * upcomingTimeSpentOnEntryCMP));

         Assert.assertEquals(firstExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(firstExitCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderTwoStepsTwoRegisteredTwoCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 2;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         swingSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
         swingSplitFractions.add(swingSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
            swingSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = true;

         entryCMPRecursionMultipliers.compute(2, 2, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();
         double nextStepDuration = doubleSupportDurations.get(1).getDoubleValue() + singleSupportDurations.get(1).getDoubleValue();

         double currentTimeSpentOnExitCMP = (1.0 - swingSplitFractions.get(0).getDoubleValue()) * singleSupportDurations.get(0).getDoubleValue() +
               transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();
         double upcomingTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(1).getDoubleValue()) * doubleSupportDurations.get(1).getDoubleValue() +
               swingSplitFractions.get(1).getDoubleValue() * singleSupportDurations.get(1).getDoubleValue();
         double nextNextTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(2).getDoubleValue()) * doubleSupportDurations.get(2).getDoubleValue() +
               swingSplitFractions.get(2).getDoubleValue() * singleSupportDurations.get(2).getDoubleValue();

         double firstExitCMPMultiplier = Math.exp(-omega * currentTimeSpentOnExitCMP) * (1.0 - Math.exp(-omega * upcomingTimeSpentOnEntryCMP));
         double secondExitCMPMultiplier = Math.exp(-omega * (currentTimeSpentOnExitCMP + nextStepDuration)) * (1.0 - Math.exp(-omega * nextNextTimeSpentOnEntryCMP));

         Assert.assertEquals(firstExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertEquals(secondExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(1), epsilon);
         Assert.assertFalse(Double.isNaN(firstExitCMPMultiplier));
         Assert.assertFalse(Double.isNaN(secondExitCMPMultiplier));
         for (int i = 2; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(2, 2, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         firstExitCMPMultiplier = Math.exp(-omega * currentStepDuration) * (1.0 - Math.exp(-omega * upcomingTimeSpentOnEntryCMP));
         secondExitCMPMultiplier = Math.exp(-omega * (currentStepDuration + nextStepDuration)) * (1.0 - Math.exp(-omega * nextNextTimeSpentOnEntryCMP));

         Assert.assertEquals(firstExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertEquals(secondExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(1), epsilon);
         Assert.assertFalse(Double.isNaN(firstExitCMPMultiplier));
         Assert.assertFalse(Double.isNaN(secondExitCMPMultiplier));
         for (int i = 2; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderTwoStepsThreeRegisteredTwoCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 3;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         swingSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
         swingSplitFractions.add(swingSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
            swingSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = true;

         entryCMPRecursionMultipliers.compute(2, 3, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();
         double nextStepDuration = doubleSupportDurations.get(1).getDoubleValue() + singleSupportDurations.get(1).getDoubleValue();

         double currentTimeSpentOnExitCMP = (1.0 - swingSplitFractions.get(0).getDoubleValue()) * singleSupportDurations.get(0).getDoubleValue() +
               transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();
         double upcomingTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(1).getDoubleValue()) * doubleSupportDurations.get(1).getDoubleValue() +
               swingSplitFractions.get(1).getDoubleValue() * singleSupportDurations.get(1).getDoubleValue();
         double nextNextTimeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(2).getDoubleValue()) * doubleSupportDurations.get(2).getDoubleValue() +
               swingSplitFractions.get(2).getDoubleValue() * singleSupportDurations.get(2).getDoubleValue();

         double firstExitCMPMultiplier = Math.exp(-omega * currentTimeSpentOnExitCMP) * (1.0 - Math.exp(-omega * upcomingTimeSpentOnEntryCMP));
         double secondExitCMPMultiplier = Math.exp(-omega * (currentTimeSpentOnExitCMP + nextStepDuration)) * (1.0 - Math.exp(-omega * nextNextTimeSpentOnEntryCMP));

         Assert.assertEquals(firstExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertEquals(secondExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(1), epsilon);
         Assert.assertFalse(Double.isNaN(firstExitCMPMultiplier));
         Assert.assertFalse(Double.isNaN(secondExitCMPMultiplier));
         for (int i = 2; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(2, 3, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         firstExitCMPMultiplier = Math.exp(-omega * currentStepDuration) * (1.0 - Math.exp(-omega * upcomingTimeSpentOnEntryCMP));
         secondExitCMPMultiplier = Math.exp(-omega * (currentStepDuration + nextStepDuration)) * (1.0 - Math.exp(-omega * nextNextTimeSpentOnEntryCMP));

         Assert.assertEquals(firstExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertEquals(secondExitCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(1), epsilon);
         Assert.assertFalse(Double.isNaN(firstExitCMPMultiplier));
         Assert.assertFalse(Double.isNaN(secondExitCMPMultiplier));
         for (int i = 2; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderTwoStepsOneRegisteredOneCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 1;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = false;

         entryCMPRecursionMultipliers.compute(2, 1, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();
         double nextStepDuration = doubleSupportDurations.get(1).getDoubleValue();


         double firstEntryCMPMultiplier = Math.exp(-omega * currentStepDuration) * (1.0 - Math.exp(-omega * nextStepDuration));

         Assert.assertEquals(firstEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(firstEntryCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(2, 1, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(firstEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertFalse(Double.isNaN(firstEntryCMPMultiplier));
         for (int i = 1; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderTwoStepsTwoRegisteredOneCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 2;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = false;

         entryCMPRecursionMultipliers.compute(2, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();
         double nextStepDuration = doubleSupportDurations.get(1).getDoubleValue() + singleSupportDurations.get(1).getDoubleValue();
         double nextNextStepDuration = doubleSupportDurations.get(2).getDoubleValue();

         double firstEntryCMPMultiplier = Math.exp(-omega * currentStepDuration) * (1.0 - Math.exp(-omega * nextStepDuration));
         double secondEntryCMPMultiplier = Math.exp(-omega * (currentStepDuration + nextStepDuration)) * (1.0 - Math.exp(-omega * nextNextStepDuration));

         Assert.assertEquals(firstEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertEquals(secondEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(1), epsilon);
         Assert.assertFalse(Double.isNaN(firstEntryCMPMultiplier));
         Assert.assertFalse(Double.isNaN(secondEntryCMPMultiplier));
         for (int i = 2; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(2, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(firstEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertEquals(secondEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(1), epsilon);
         Assert.assertFalse(Double.isNaN(firstEntryCMPMultiplier));
         Assert.assertFalse(Double.isNaN(secondEntryCMPMultiplier));
         for (int i = 2; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testConsiderTwoStepsThreeRegisteredOneCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int stepsRegistered = 3;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         for (int i = 0; i < stepsRegistered; i++)
         {
            doubleSupportDurations.get(i).set(2.0 * random.nextDouble());
            singleSupportDurations.get(i).set(5.0 * random.nextDouble());
            transferSplitFractions.get(i).set(0.8 * random.nextDouble());
         }
         doubleSupportDurations.get(stepsRegistered).set(2.0 * random.nextDouble());
         transferSplitFractions.get(stepsRegistered).set(0.8 * random.nextDouble());

         boolean isInTransfer = false;
         boolean useTwoCMPs = false;

         entryCMPRecursionMultipliers.compute(2, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();
         double nextStepDuration = doubleSupportDurations.get(1).getDoubleValue() + singleSupportDurations.get(1).getDoubleValue();
         double nextNextStepDuration = doubleSupportDurations.get(2).getDoubleValue() + singleSupportDurations.get(2).getDoubleValue();


         double firstEntryCMPMultiplier = Math.exp(-omega * currentStepDuration) * (1.0 - Math.exp(-omega * nextStepDuration));
         double secondEntryCMPMultiplier = Math.exp(-omega * (currentStepDuration + nextStepDuration)) * (1.0 - Math.exp(-omega * nextNextStepDuration));

         Assert.assertEquals(firstEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertEquals(secondEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(1), epsilon);
         Assert.assertFalse(Double.isNaN(firstEntryCMPMultiplier));
         Assert.assertFalse(Double.isNaN(secondEntryCMPMultiplier));
         for (int i = 2; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));

         // setup for in transfer
         isInTransfer = true;
         entryCMPRecursionMultipliers.compute(2, stepsRegistered, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(firstEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(0), epsilon);
         Assert.assertEquals(secondEntryCMPMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(1), epsilon);
         Assert.assertFalse(Double.isNaN(firstEntryCMPMultiplier));
         Assert.assertFalse(Double.isNaN(secondEntryCMPMultiplier));
         for (int i = 2; i < maxSteps; i++)
            Assert.assertTrue(Double.isNaN(entryCMPRecursionMultipliers.getEntryMultiplier(i)));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testNStepTwoCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         swingSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
         swingSplitFractions.add(swingSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int j = 1; j < maxSteps; j++)
      {
         for (int iter = 0; iter < iters; iter++)
         {
            for (int step = 0; step < maxSteps; step++)
            {
               doubleSupportDurations.get(step).set(2.0 * random.nextDouble());
               singleSupportDurations.get(step).set(5.0 * random.nextDouble());
               transferSplitFractions.get(step).set(0.8 * random.nextDouble());
               swingSplitFractions.get(step).set(0.8 * random.nextDouble());
            }

            boolean isInTransfer = false;
            boolean useTwoCMPs = true;

            entryCMPRecursionMultipliers.compute(j, j + 1, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

            double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();
            double currentTimeSpentOnExitCMP = (1.0 - swingSplitFractions.get(0).getDoubleValue()) * singleSupportDurations.get(0).getDoubleValue() +
                  transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();

            double recursionTime = currentTimeSpentOnExitCMP;
            for (int i = 0; i < j; i++)
            {
               double stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue() + singleSupportDurations.get(i + 1).getDoubleValue();
               double timeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(i).getDoubleValue()) * doubleSupportDurations.get(i).getDoubleValue() +
                     swingSplitFractions.get(i).getDoubleValue() * singleSupportDurations.get(i).getDoubleValue();
               double entryMultiplier = Math.exp(-omega * recursionTime) * ( 1.0 - Math.exp(-omega * timeSpentOnEntryCMP));

               Assert.assertEquals("j = " + j + ", i = " + i, entryMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(i), epsilon);

               recursionTime += stepDuration;
            }

            // setup for in transfer
            isInTransfer = true;
            entryCMPRecursionMultipliers.compute(j, j + 1, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

            recursionTime = currentStepDuration;
            for (int i = 0; i < j; i++)
            {
               double stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue() + singleSupportDurations.get(i + 1).getDoubleValue();
               double timeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(i).getDoubleValue()) * doubleSupportDurations.get(i).getDoubleValue() +
                     swingSplitFractions.get(i).getDoubleValue() * singleSupportDurations.get(i).getDoubleValue();
               double entryMultiplier = Math.exp(-omega * recursionTime) * ( 1.0 - Math.exp(-omega * timeSpentOnEntryCMP));

               Assert.assertEquals("j = " + j + ", i = " + i, entryMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(i), epsilon);

               recursionTime += stepDuration;
            }
         }
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testNStepTwoCMPCalculationFinalTransfer()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         swingSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
         swingSplitFractions.add(swingSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int j = 1; j < maxSteps; j++)
      {
         for (int iter = 0; iter < iters; iter++)
         {
            for (int step = 0; step < maxSteps; step++)
            {
               doubleSupportDurations.get(step).set(2.0 * random.nextDouble());
               singleSupportDurations.get(step).set(5.0 * random.nextDouble());
               transferSplitFractions.get(step).set(0.8 * random.nextDouble());
               swingSplitFractions.get(step).set(0.8 * random.nextDouble());
            }

            boolean isInTransfer = false;
            boolean useTwoCMPs = true;

            entryCMPRecursionMultipliers.compute(j, j, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

            double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();
            double currentTimeSpentOnExitCMP = (1.0 - swingSplitFractions.get(0).getDoubleValue()) * singleSupportDurations.get(0).getDoubleValue() +
                  transferSplitFractions.get(1).getDoubleValue() * doubleSupportDurations.get(1).getDoubleValue();

            double recursionTime = currentTimeSpentOnExitCMP;
            for (int i = 0; i < j; i++)
            {
               boolean isLast = i + 1 == j;
               double stepDuration;

               if (isLast)
                  stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue();
               else
                  stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue() + singleSupportDurations.get(i + 1).getDoubleValue();

               double timeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(i).getDoubleValue()) * doubleSupportDurations.get(i).getDoubleValue() +
                     swingSplitFractions.get(i).getDoubleValue() * singleSupportDurations.get(i).getDoubleValue();
               double entryMultiplier = Math.exp(-omega * recursionTime) * ( 1.0 - Math.exp(-omega * timeSpentOnEntryCMP));

               Assert.assertEquals("j = " + j + ", i = " + i, entryMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(i), epsilon);

               recursionTime += stepDuration;
            }

            // setup for in transfer
            isInTransfer = true;
            entryCMPRecursionMultipliers.compute(j, j, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

            recursionTime = currentStepDuration;
            for (int i = 0; i < j; i++)
            {
               boolean isLast = i + 1 == j;
               double stepDuration;

               if (isLast)
                  stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue();
               else
                  stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue() + singleSupportDurations.get(i + 1).getDoubleValue();

               double timeSpentOnEntryCMP = (1.0 - transferSplitFractions.get(i).getDoubleValue()) * doubleSupportDurations.get(i).getDoubleValue() +
                     swingSplitFractions.get(i).getDoubleValue() * singleSupportDurations.get(i).getDoubleValue();
               double entryMultiplier = Math.exp(-omega * recursionTime) * ( 1.0 - Math.exp(-omega * timeSpentOnEntryCMP));

               Assert.assertEquals("j = " + j + ", i = " + i, entryMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(i), epsilon);

               recursionTime += stepDuration;
            }
         }
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testNStepOneCMPCalculation()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int j = 1; j < maxSteps; j++)
      {
         for (int iter = 0; iter < iters; iter++)
         {
            for (int step = 0; step < maxSteps; step++)
            {
               doubleSupportDurations.get(step).set(2.0 * random.nextDouble());
               singleSupportDurations.get(step).set(5.0 * random.nextDouble());
               transferSplitFractions.get(step).set(0.8 * random.nextDouble());
            }

            boolean isInTransfer = false;
            boolean useTwoCMPs = false;

            entryCMPRecursionMultipliers.compute(j, j + 1, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

            double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();

            double recursionTime = currentStepDuration;
            for (int i = 0; i < j; i++)
            {
               double stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue() + singleSupportDurations.get(i + 1).getDoubleValue();

               double entryMultiplier = Math.exp(-omega * recursionTime) * (1.0 - Math.exp(-omega * stepDuration));

               Assert.assertEquals(entryMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(i), epsilon);

               recursionTime += stepDuration;
            }

            // setup for in transfer
            isInTransfer = true;
            entryCMPRecursionMultipliers.compute(j, j + 1, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

            recursionTime = currentStepDuration;
            for (int i = 0; i < j; i++)
            {
               double stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue() + singleSupportDurations.get(i + 1).getDoubleValue();

               double entryMultiplier = Math.exp(-omega * recursionTime) * (1.0 - Math.exp(-omega * stepDuration));

               Assert.assertEquals(entryMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(i), epsilon);

               recursionTime += stepDuration;
            }
         }
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testNStepOneCMPCalculationInTransfer()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoOmega = new DoubleYoVariable("omega", registry);

      double omega = 3.0;
      yoOmega.set(omega);

      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      ArrayList<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction" + i, registry);
         transferSplitFraction.setToNaN();
         transferSplitFractions.add(transferSplitFraction);
      }

      EntryCMPRecursionMultipliers entryCMPRecursionMultipliers = new EntryCMPRecursionMultipliers("", maxSteps, swingSplitFractions, transferSplitFractions, registry);

      for (int j = 1; j < maxSteps; j++)
      {
         for (int iter = 0; iter < iters; iter++)
         {
            for (int step = 0; step < maxSteps; step++)
            {
               doubleSupportDurations.get(step).set(2.0 * random.nextDouble());
               singleSupportDurations.get(step).set(5.0 * random.nextDouble());
               transferSplitFractions.get(step).set(0.8 * random.nextDouble());
            }

            boolean isInTransfer = false;
            boolean useTwoCMPs = false;

            entryCMPRecursionMultipliers.compute(j, j, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

            double currentStepDuration = doubleSupportDurations.get(0).getDoubleValue() + singleSupportDurations.get(0).getDoubleValue();

            double recursionTime = currentStepDuration;
            for (int i = 0; i < j; i++)
            {
               boolean isLastStep = i + 1 == j;
               double stepDuration;

               if (isLastStep)
                  stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue();
               else
                  stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue() + singleSupportDurations.get(i + 1).getDoubleValue();

               double entryMultiplier = Math.exp(-omega * recursionTime) * (1.0 - Math.exp(-omega * stepDuration));

               Assert.assertEquals(entryMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(i), epsilon);

               recursionTime += stepDuration;
            }

            // setup for in transfer
            isInTransfer = true;
            entryCMPRecursionMultipliers.compute(j, j, doubleSupportDurations, singleSupportDurations, useTwoCMPs, isInTransfer, omega);

            recursionTime = currentStepDuration;
            for (int i = 0; i < j; i++)
            {
               boolean isLastStep = i + 1 == j;
               double stepDuration;

               if (isLastStep)
                  stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue();
               else
                  stepDuration = doubleSupportDurations.get(i + 1).getDoubleValue() + singleSupportDurations.get(i + 1).getDoubleValue();

               double entryMultiplier = Math.exp(-omega * recursionTime) * (1.0 - Math.exp(-omega * stepDuration));

               Assert.assertEquals(entryMultiplier, entryCMPRecursionMultipliers.getEntryMultiplier(i), epsilon);

               recursionTime += stepDuration;
            }
         }
      }
   }
}

