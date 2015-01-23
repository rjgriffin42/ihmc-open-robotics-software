package us.ihmc.simulationconstructionset.gui;

import java.util.ArrayList;

import javax.swing.JFrame;

import us.ihmc.simulationconstructionset.DataBufferEntry;
import us.ihmc.simulationconstructionset.dataBuffer.DataEntry;
import us.ihmc.simulationconstructionset.dataBuffer.DataEntryHolder;
import us.ihmc.simulationconstructionset.dataBuffer.TimeDataHolder;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

public class YoGraphTester
{
   public void testYoGraph()
   {
      SelectedVariableHolder selectedVariableHolder = new SelectedVariableHolder();


      JFrame jFrame = new JFrame("testYoGraph");


      YoGraphRemover yoGraphRemover = new YoGraphRemover()
      {
         public void removeGraph(YoGraph yoGraph)
         {
         }
      };

      DataEntryHolder dataEntryHolder = new DataEntryHolder()
      {
         public DataEntry getEntry(YoVariable yoVariable)
         {
            return null;
         }
      };

      TimeDataHolder timeDataHolder = new MinimalTimeDataHolder(200);
      

      GraphIndicesHolder graphIndicesHolder = new MinimalGraphIndicesHolder();

      YoGraph yoGraph = new YoGraph(graphIndicesHolder, yoGraphRemover, selectedVariableHolder, dataEntryHolder, timeDataHolder, jFrame);

      int nPoints = 200;
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable yoVariable = new DoubleYoVariable("variableOne", registry);
      
      DataBufferEntry dataEntry = new DataBufferEntry(yoVariable, nPoints);
      
      double value = 0.0;
      
      for (int i=0; i<nPoints; i++)
      {
         yoVariable.set(value);
         value = value + 0.001;
         dataEntry.setDataAtIndexToYoVariableValue(i);
      }
      
      yoGraph.addVariable(dataEntry);

      jFrame.getContentPane().add(yoGraph);
      jFrame.setSize(800, 200);
      jFrame.setVisible(true);

//      while (true)
//      {
//         try
//         {
//            Thread.sleep(1000);
//         }
//         catch (InterruptedException e)
//         {
//         }
//
//      }
   }

   
   private class MinimalTimeDataHolder implements TimeDataHolder
   {
      private double[] timeData;
      
      MinimalTimeDataHolder(int nPoints)
      {
         timeData = new double[nPoints];
         double time = 1.0;
         
         for (int i=0; i<nPoints; i++)
         {
            timeData[i] = time;
            time = time + 0.01;
         }
      }
      
      public double[] getTimeData()
      {
         return timeData;
      }
      
   }
   
   private class MinimalGraphIndicesHolder implements GraphIndicesHolder
   {
      ArrayList<Integer> keyPoints = new ArrayList<Integer>();
      
      int leftPlotIndex = 0;
      int rightPlotIndex = 100;
      int inPoint = 50;
      int index = 60;
      int outPoint = 75;
      int maxIndex = 200;
      
      public void tickLater(int i)
      {
         index = index + i;
      }

      public void setRightPlotIndex(int newRightIndex)
      {
         this.rightPlotIndex = newRightIndex;
      }

      public void setLeftPlotIndex(int newLeftIndex)
      {
         this.leftPlotIndex = newLeftIndex;
      }

      public void setIndexLater(int newIndex)
      {
         this.index = newIndex;
      }

      public int getRightPlotIndex()
      {
         return rightPlotIndex;
      }

      public int getMaxIndex()
      {
         return maxIndex;
      }

      public int getLeftPlotIndex()
      {
         return leftPlotIndex;
      }

      public ArrayList<Integer> getKeyPoints()
      {
         return keyPoints;
      }

      public int getIndex()
      {
         return index;
      }

      public int getInPoint()
      {
         return inPoint;
      }
      
      public int getOutPoint()
      {
         return outPoint;
      }

      public boolean isIndexAtOutPoint()
      {
         return (getIndex() == getOutPoint());
      }
   }

   public static void main(String[] args)
   {
      new YoGraphTester().testYoGraph();
   }
}
