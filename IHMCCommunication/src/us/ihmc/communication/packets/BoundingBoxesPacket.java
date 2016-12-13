package us.ihmc.communication.packets;

import us.ihmc.robotics.random.RandomTools;

import java.util.Arrays;
import java.util.Random;

/**
 *
 */
public class BoundingBoxesPacket extends Packet<BoundingBoxesPacket>
{
   public int[] boundingBoxXCoordinates, boundingBoxYCoordinates, boundingBoxWidths, boundingBoxHeights;

   public BoundingBoxesPacket()
   {

   }

   public BoundingBoxesPacket(int[] packedBoxes)
   {
      int n = packedBoxes.length;
      boundingBoxXCoordinates = new int[n];
      boundingBoxYCoordinates = new int[n];
      boundingBoxWidths = new int[n];
      boundingBoxHeights = new int[n];
      for (int i = 0; i < n; i++)
      {
         boundingBoxXCoordinates[i] = packedBoxes[i * 4];
         boundingBoxYCoordinates[i] = packedBoxes[i * 4 + 1];
         boundingBoxWidths[i] = packedBoxes[i * 4 + 2];
         boundingBoxHeights[i] = packedBoxes[i * 4 + 3];
      }
   }

   public BoundingBoxesPacket(int[] boundingBoxXCoordinates, int[] boundingBoxYCoordinates, int[] boundingBoxWidths, int[] boundingBoxHeights)
   {
      this.boundingBoxXCoordinates = boundingBoxXCoordinates;
      this.boundingBoxYCoordinates = boundingBoxYCoordinates;
      this.boundingBoxWidths = boundingBoxWidths;
      this.boundingBoxHeights = boundingBoxHeights;
   }

   public BoundingBoxesPacket(Random random)
   {
      int boxesToGenerate = random.nextInt(20);

      for (int i = 0; i < boxesToGenerate; i++)
      {
         boundingBoxXCoordinates = new int[boxesToGenerate];
         boundingBoxYCoordinates = new int[boxesToGenerate];
         boundingBoxWidths = new int[boxesToGenerate];
         boundingBoxHeights = new int[boxesToGenerate];

         boundingBoxXCoordinates[i] = RandomTools.generateRandomInt(random, -1000, 1000);
         boundingBoxYCoordinates[i] = RandomTools.generateRandomInt(random, -1000, 1000);
         boundingBoxWidths[i] = RandomTools.generateRandomInt(random, 0, 1000);
         boundingBoxHeights[i] = RandomTools.generateRandomInt(random, 0, 1000);
      }
   }

   @Override public boolean epsilonEquals(BoundingBoxesPacket other, double epsilon)
   {
      return Arrays.equals(this.boundingBoxHeights, other.boundingBoxHeights) && Arrays.equals(this.boundingBoxWidths, other.boundingBoxWidths) && Arrays
            .equals(this.boundingBoxXCoordinates, other.boundingBoxXCoordinates) && Arrays.equals(this.boundingBoxYCoordinates, other.boundingBoxYCoordinates);
   }
}
