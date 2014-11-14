package us.ihmc.simulationconstructionset.robotcommprotocol;

import java.io.IOException;

public interface UserCommandNetworkReader
{
   public int readInt() throws IOException;

   public float readFloat() throws IOException;

   public double readDouble() throws IOException;
}
