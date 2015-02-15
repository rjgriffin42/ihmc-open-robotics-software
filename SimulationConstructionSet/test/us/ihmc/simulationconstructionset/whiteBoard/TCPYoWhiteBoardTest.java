package us.ihmc.simulationconstructionset.whiteBoard;


import java.io.IOException;

import org.junit.Test;

import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;

public class TCPYoWhiteBoardTest extends YoWhiteBoardTest
{

	@AverageDuration
	@Test(timeout = 30000l)
   public void testTCPWhiteBoardOne() throws IOException
   {
      String IPAddress = "localHost";
      int port = 8456;

      TCPYoWhiteBoard leftWhiteBoard = new TCPYoWhiteBoard("leftTest", port);
      TCPYoWhiteBoard rightWhiteBoard = new TCPYoWhiteBoard("rightTest", IPAddress, port);

      Thread leftWhiteBoardThread = new Thread(leftWhiteBoard);
      Thread rightWhiteBoardThread = new Thread(rightWhiteBoard);

      leftWhiteBoardThread.start();
      rightWhiteBoardThread.start();

      int numberOfTests = 500;
      doASynchronizedWriteThenReadTest(leftWhiteBoard, rightWhiteBoard, numberOfTests, 501, 1001);
   }

	@AverageDuration
	@Test(timeout = 30000l)
   public void testTCPWhiteBoardTwo() throws IOException
   {
      String IPAddress = "localHost";
      int port = 8456;

      TCPYoWhiteBoard leftWhiteBoard = new TCPYoWhiteBoard("leftTest", port);
      TCPYoWhiteBoard rightWhiteBoard = new TCPYoWhiteBoard("rightTest", IPAddress, port);

      Thread leftWhiteBoardThread = new Thread(leftWhiteBoard);
      Thread rightWhiteBoardThread = new Thread(rightWhiteBoard);

      leftWhiteBoardThread.start();
      rightWhiteBoardThread.start();

      int numberOfTests = 500;
      doAnAsynchronousTest(leftWhiteBoard, rightWhiteBoard, numberOfTests, 500, 1000);
   }
}
