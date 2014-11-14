package us.ihmc.simulationconstructionset.whiteBoard;



import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;
import us.ihmc.yoUtilities.time.GlobalTimer;

import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.robotController.SensorProcessor;
import us.ihmc.simulationconstructionset.util.IndexOrderChecker;

public class YoWhiteBoardReadController implements RobotController, SensorProcessor
{
   private final YoVariableRegistry registry;
   private final GlobalTimer yoWhiteBoardReadControllerWaitingGlobalTimer;
   
   private final IntegerYoVariable numberOfNewDataSinceLastRead;
   private final IntegerYoVariable ticksTillNextRead;

   private final YoWhiteBoard yoWhiteBoard;
   private final boolean blockUntilNewDataIsAvailable;
   private final int readEveryNTicks;
   private final IndexOrderChecker indexOrderChecker;
   private final IntegerYoVariable previousMissedIndices;

   private final boolean readOnInitialize;

   public YoWhiteBoardReadController(String name, YoWhiteBoard yoWhiteBoard, boolean blockUntilNewDataIsAvailable, int readEveryNTicks,
                                     boolean doNotReadFirstTime, boolean readOnInitialize)
   {
      registry = new YoVariableRegistry(name + "YoWhiteBoardReadController");
      yoWhiteBoardReadControllerWaitingGlobalTimer = new GlobalTimer("whiteBoardReadWait", registry);
      numberOfNewDataSinceLastRead = new IntegerYoVariable("numberOfNewDataSinceLastRead", registry);

      this.yoWhiteBoard = yoWhiteBoard;
      this.blockUntilNewDataIsAvailable = blockUntilNewDataIsAvailable;

      if (readEveryNTicks < 1)
         throw new RuntimeException("readEveryNTicks must be 1 or larger!");
      this.readEveryNTicks = readEveryNTicks;

      if (readEveryNTicks != 1)
      {
         ticksTillNextRead = new IntegerYoVariable("ticksTillNextRead", registry);

         if (doNotReadFirstTime)
         {
            ticksTillNextRead.set(readEveryNTicks - 1);
         }
         else
         {
            ticksTillNextRead.set(0);
         }
      }
      else
      {
         ticksTillNextRead = null;
         if (doNotReadFirstTime == true)
            throw new RuntimeException("doNotReadFirstTime must be false if readEveryNTicks == 1");
      }

      this.indexOrderChecker = new IndexOrderChecker(name, registry, 1);
      this.previousMissedIndices = new IntegerYoVariable("previousMissedIndices", registry);
      this.readOnInitialize = readOnInitialize;
   }

   public void doControl()
   {
      if ((ticksTillNextRead == null) || (ticksTillNextRead.getIntegerValue() <= 0))
      {
         read(blockUntilNewDataIsAvailable);

         if (ticksTillNextRead != null)
            ticksTillNextRead.set(readEveryNTicks - 1);
      }
      else
      {
         ticksTillNextRead.decrement();
      }
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return "YoWhiteBoardReadController";
   }

   public void initialize()
   {
      if (readOnInitialize)
         read(true);
   }

   public void update()
   {
      doControl();
   }

   public String getDescription()
   {
      return "YoWhiteBoardReadController";
   }
   
   private void read(boolean block)
   {
      if (block)
      {
         synchronized (yoWhiteBoard)
         {
            while (!yoWhiteBoard.isNewDataAvailable())
            {
               yoWhiteBoardReadControllerWaitingGlobalTimer.startTimer();
               try
               {
                  yoWhiteBoard.wait();
               }
               catch (InterruptedException e)
               {
               }
               yoWhiteBoardReadControllerWaitingGlobalTimer.stopTimer();
            }
         }
      }

      numberOfNewDataSinceLastRead.set(yoWhiteBoard.getNumberOfNewDataSinceLastRead());
      yoWhiteBoard.readData();

      indexOrderChecker.update(yoWhiteBoard.getReadIndex());
      if (indexOrderChecker.getMissedIndices() - previousMissedIndices.getIntegerValue() > 0)
         System.out.println("YoWhiteBoardReadController: missed " + indexOrderChecker.getMissedIndices() + " YoWhiteBoard indices!");
      previousMissedIndices.set(indexOrderChecker.getMissedIndices());
   }
}
