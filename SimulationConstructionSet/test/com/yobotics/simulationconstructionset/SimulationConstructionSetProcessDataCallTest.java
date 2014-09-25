package com.yobotics.simulationconstructionset;

import static org.junit.Assert.*;
import org.junit.Test;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class SimulationConstructionSetProcessDataCallTest
{   
   private YoVariableRegistry registry;
      
   @Test
   public void testForwardCount()
   {
      Robot robot = new Robot("testRobot");
      SimulationConstructionSet scs = new SimulationConstructionSet(robot, false, 8192);

      registry = new YoVariableRegistry("testRegustry");
      DoubleYoVariable dataSet = new DoubleYoVariable("dataSet", registry);
      scs.addYoVariableRegistry(registry);
      
      int startValue = 5;
      int maxValue = 15;
      //Generate and populate data
      for(int i = startValue; i < maxValue; i++)
      {
         dataSet.set(i);
         scs.tickAndUpdate();
      }
 
      //Crop data to get rid of the first data point
      scs.setOutPoint();
      scs.setIndex(1);
      scs.setInPoint();
      scs.cropBuffer();
      
      //Apply the Data Processing Class
      DataProcessingFunction counterProcessingFunction = new CounterProcessingFunction(registry);
      scs.applyDataProcessingFunction(counterProcessingFunction);
      
      //Exact data from Data Processing Class
      DoubleYoVariable counterVariable = ((CounterProcessingFunction) counterProcessingFunction).getCountVariable();
      
      //Print out the data (for DEBUG)
      System.out.println("===testForwardCount===");
      for (int i = 0; i < maxValue-startValue; i++)
      {
         scs.setIndex(i);
         System.out.println(dataSet.getDoubleValue() + "\t" + counterVariable.getDoubleValue());
      }     
      
      //Test
      double testNum1, testNum2;
      for (int i = 0; i < maxValue-startValue; i++)
      {
         scs.setIndex(i);
         testNum1 = dataSet.getDoubleValue();
         testNum2 = counterVariable.getDoubleValue();
         
         assertEquals(testNum1-testNum2, startValue, 0);
      }    
   }
   
   @Test
   public void testBackwardCount()
   {
      Robot robot = new Robot("testRobot");
      SimulationConstructionSet scs = new SimulationConstructionSet(robot, false, 8192);

      registry = new YoVariableRegistry("testRegustry");
      DoubleYoVariable dataSet = new DoubleYoVariable("dataSet", registry);
      scs.addYoVariableRegistry(registry);
      
      int startValue = 5;
      int maxValue = 15;
      //Generate and populate data
      for(int i = startValue; i < maxValue; i++)
      {
         dataSet.set(i);
         scs.tickAndUpdate();
      }
 
      //Crop data to get rid of the first data point
      scs.setOutPoint();
      scs.setIndex(1);
      scs.setInPoint();
      scs.cropBuffer();
      
      //Apply the Data Processing Class
      DataProcessingFunction counterProcessingFunction = new CounterProcessingFunction(registry);
      scs.applyDataProcessingFunctionBackward(counterProcessingFunction);
      
      //Exact data from Data Processing Class
      DoubleYoVariable counterVariable = ((CounterProcessingFunction) counterProcessingFunction).getCountVariable();
      
      //Print out the data (for DEBUG)
      System.out.println("===testBackwardCount===");
      for (int i = 0; i < maxValue-startValue; i++)
      {
         scs.setIndex(i);
         System.out.println(dataSet.getDoubleValue() + "\t" + counterVariable.getDoubleValue());
      }
      
      //Test
      double testNum1, testNum2;
      for (int i = 0; i < maxValue-startValue; i++)
      {
         scs.setIndex(i);
         testNum1 = dataSet.getDoubleValue();
         testNum2 = counterVariable.getDoubleValue();
         
         assertEquals(testNum1+testNum2, maxValue-1, 0);
      }    
   }
   
   @Test
   public void testForwardCopy()
   {
      Robot robot = new Robot("testRobot");
      SimulationConstructionSet scs = new SimulationConstructionSet(robot, false, 8192);
      
      registry = new YoVariableRegistry("testRegustry");
      DoubleYoVariable dataSet = new DoubleYoVariable("dataSet", registry);
      scs.addYoVariableRegistry(registry);
      
      int startValue = 5;
      int maxValue = 15;
      //Generate and populate data
      for(int i = startValue; i < maxValue; i++)
      {
         dataSet.set(i);
         scs.tickAndUpdate();
      }
 
      //Crop data to get rid of the first data point
      scs.setOutPoint();
      scs.setIndex(1);
      scs.setInPoint();
      scs.cropBuffer();
      
      //Apply the Data Processing Class
      DataProcessingFunction copierProcessingFunction = new CopierProcessingFunction(dataSet, registry);
      scs.applyDataProcessingFunction(copierProcessingFunction);
      
      //Exact data from Data Processing Class
      DoubleYoVariable copierVariable = ((CopierProcessingFunction) copierProcessingFunction).getCopyVariable();
      
      //Print out the data (for DEBUG)
      System.out.println("===testForwardCopy===");
      for (int i = 0; i < maxValue-startValue; i++)
      {
         scs.setIndex(i);
         System.out.println(dataSet.getDoubleValue() + "\t" + copierVariable.getDoubleValue());
      }
      
      //Test
      double testNum1, testNum2;
      for (int i = 0; i < maxValue-startValue; i++)
      {
         scs.setIndex(i);
         testNum1 = dataSet.getDoubleValue();
         testNum2 = copierVariable.getDoubleValue();
         
         assertEquals(testNum1-testNum2, 0, 0);
      }    
   }
   
   @Test
   public void testBackwardCopy()
   {
      Robot robot = new Robot("testRobot");
      SimulationConstructionSet scs = new SimulationConstructionSet(robot, false, 8192);
      
      registry = new YoVariableRegistry("testRegustry");
      DoubleYoVariable dataSet = new DoubleYoVariable("dataSet", registry);
      scs.addYoVariableRegistry(registry);
      
      int startValue = 5;
      int maxValue = 15;
      //Generate and populate data
      for(int i = startValue; i < maxValue; i++)
      {
         dataSet.set(i);
         scs.tickAndUpdate();
      }
 
      //Crop data to get rid of the first data point
      scs.setOutPoint();
      scs.setIndex(1);
      scs.setInPoint();
      scs.cropBuffer();
      
      //Apply the Data Processing Class
      DataProcessingFunction copierProcessingFunction = new CopierProcessingFunction(dataSet, registry);
      scs.applyDataProcessingFunctionBackward(copierProcessingFunction);
      
      //Exact data from Data Processing Class
      DoubleYoVariable copierVariable = ((CopierProcessingFunction) copierProcessingFunction).getCopyVariable();
      
      //Print out the data (for DEBUG)
      System.out.println("===testBackwardCopy===");
      for (int i = 0; i < maxValue-startValue; i++)
      {
         scs.setIndex(i);
         System.out.println(dataSet.getDoubleValue() + "\t" + copierVariable.getDoubleValue());
      }
      
      //Test     
      double testNum1, testNum2;
      for (int i = 0; i < maxValue-startValue; i++)
      {
         scs.setIndex(i);
         testNum1 = dataSet.getDoubleValue();
         testNum2 = copierVariable.getDoubleValue();
         
         assertEquals(testNum1-testNum2, 0, 0);
      }    

   }
   
   public static class CopierProcessingFunction implements DataProcessingFunction
   {
      private DoubleYoVariable copyVariable;
      private DoubleYoVariable testVariable;
      
      public CopierProcessingFunction(DoubleYoVariable inputData, YoVariableRegistry registry)
      {
         testVariable = inputData;
         copyVariable = new DoubleYoVariable("copyVariable", registry);
      }
      
      public void processData()
      {
         double holderDouble;
         
         holderDouble = testVariable.getDoubleValue();
         copyVariable.set(holderDouble);
      }
      
      public DoubleYoVariable getCopyVariable()
      {
         return copyVariable;
      }
   }
   
   public static class CounterProcessingFunction implements DataProcessingFunction
   {
      private DoubleYoVariable countVariable;
      private int count = 0;
      
      public CounterProcessingFunction(YoVariableRegistry registry)
      {
         countVariable = new DoubleYoVariable("countVariable", registry);
      }
      
      public void processData()
      {
         countVariable.set(count);         
         count++;
      }
      
      public DoubleYoVariable getCountVariable()
      {
         return countVariable;
      }
   }   
}
