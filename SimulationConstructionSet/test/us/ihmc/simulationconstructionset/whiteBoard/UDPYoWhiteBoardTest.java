package us.ihmc.simulationconstructionset.whiteBoard;


import java.io.IOException;

import org.junit.Test;

import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.thread.ThreadTools;

public class UDPYoWhiteBoardTest extends YoWhiteBoardTest
{
	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout=300000)
   public void testUDPWhiteBoardOne() throws IOException
   {
      String IPAddress = "localHost";
      int leftSendRightReceivePort = 8456;
      int leftReceiveRightSendPort = 8654;

      boolean throwOutStalePackets = false;

      UDPYoWhiteBoard leftWhiteBoard = new UDPYoWhiteBoard("leftTest", true, IPAddress, leftSendRightReceivePort, leftReceiveRightSendPort, throwOutStalePackets);
      UDPYoWhiteBoard rightWhiteBoard = new UDPYoWhiteBoard("rightTest", false, IPAddress, leftReceiveRightSendPort, leftSendRightReceivePort, throwOutStalePackets);

      leftWhiteBoard.startUDPThread();

      ThreadTools.sleepSeconds(2.0);

      rightWhiteBoard.startUDPThread();

      int numberOfTests = 500;
      doASynchronizedWriteThenReadTest(leftWhiteBoard, rightWhiteBoard, numberOfTests, 203, 207);
   }

	@DeployableTestMethod(estimatedDuration = 0.1)
	@Test(timeout=300000)
   public void testUDPWhiteBoardTwo() throws IOException
   {
      String IPAddress = "localHost";
      int leftSendRightReceivePort = 8456;
      int leftReceiveRightSendPort = 8654;

      boolean throwOutStalePackets = false;

      UDPYoWhiteBoard leftWhiteBoard = new UDPYoWhiteBoard("leftTest", true, IPAddress, leftSendRightReceivePort, leftReceiveRightSendPort, throwOutStalePackets);
      UDPYoWhiteBoard rightWhiteBoard = new UDPYoWhiteBoard("rightTest", false, IPAddress, leftReceiveRightSendPort, leftSendRightReceivePort, throwOutStalePackets);

      leftWhiteBoard.startUDPThread();

      ThreadTools.sleepSeconds(2.0);

      rightWhiteBoard.startUDPThread();

      int numberOfTests = 500;
      this.doAnAsynchronousTest(leftWhiteBoard, rightWhiteBoard, numberOfTests, 234, 179);
   }
}
