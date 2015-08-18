package us.ihmc.multicastLogDataProtocol.modelLoaders;

import us.ihmc.SdfLoader.SDFRobot;

public interface LogModelLoader
{
   public void load(String modelName, byte[] model, String[] resourceDirectories, byte[] resourceZip);
   public SDFRobot createRobot();
   
   public String getModelName();
   public byte[] getModel();
   public String[] getResourceDirectories();
   public byte[] getResourceZip();
}
