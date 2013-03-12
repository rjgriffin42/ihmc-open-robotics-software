package us.ihmc.SdfLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import us.ihmc.SdfLoader.xmlDescription.SDFModel;
import us.ihmc.SdfLoader.xmlDescription.SDFRoot;
import us.ihmc.SdfLoader.xmlDescription.SDFWorld;
import us.ihmc.SdfLoader.xmlDescription.SDFWorld.Road;
import us.ihmc.utilities.FileTools;

public class JaxbSDFLoader
{
   private final HashMap<String, GeneralizedSDFRobotModel> generalizedSDFRobotModels = new HashMap<String, GeneralizedSDFRobotModel>();
   private final ArrayList<SDFWorld.Road> roads = new ArrayList<SDFWorld.Road>();

   
   public JaxbSDFLoader(File file, String resourceDirectory) throws JAXBException, FileNotFoundException
   {
      this(file, FileTools.createArrayListOfOneURL(resourceDirectory));
   }
   
   public JaxbSDFLoader(File file, ArrayList<String> resourceDirectories) throws JAXBException, FileNotFoundException
   {
      JAXBContext context = JAXBContext.newInstance(SDFRoot.class);
      Unmarshaller um = context.createUnmarshaller();
      SDFRoot sdfRoot = (SDFRoot) um.unmarshal(new FileReader(file));

      List<SDFModel> models;
      if(sdfRoot.getWorld() != null)
      {
         models = sdfRoot.getWorld().getModels();
         
         if(sdfRoot.getWorld().getRoads() != null)
         {
            roads.addAll(sdfRoot.getWorld().getRoads());
         }
         
      }
      else
      {
         models = sdfRoot.getModels();
      }
      for (SDFModel modelInstance : models)
      {
         final String modelName = modelInstance.getName();
         generalizedSDFRobotModels.put(modelName, new GeneralizedSDFRobotModel(modelName, modelInstance, resourceDirectories));
      }
   }
   
   public Collection<GeneralizedSDFRobotModel> getGeneralizedSDFRobotModels()
   {
      return generalizedSDFRobotModels.values();
   }
   
   public List<Road> getRoads()
   {
      return roads;
   }
   
   private void checkModelName(String name)
   {
      if (!generalizedSDFRobotModels.containsKey(name))
      {
         throw new RuntimeException(name + " not found");
      }
   }
   public GeneralizedSDFRobotModel getGeneralizedSDFRobotModel(String name)
   {
      checkModelName(name);
      return generalizedSDFRobotModels.get(name);
   }

   public SDFRobot createRobot(SDFJointNameMap sdfJointNameMap)
   {
      return createRobot(sdfJointNameMap.getModelName(), sdfJointNameMap);
   }
   
   public SDFRobot createRobot(String modelName)
   {
      return createRobot(modelName, null);
   }
   
   private SDFRobot createRobot(String modelName, SDFJointNameMap sdfJointNameMap)
   {
      checkModelName(modelName);
      return new SDFRobot(generalizedSDFRobotModels.get(modelName), sdfJointNameMap);
   }

   public SDFFullRobotModel createFullRobotModel(SDFJointNameMap sdfJointNameMap)
   {
      if(sdfJointNameMap != null)
      {
         String modelName = sdfJointNameMap.getModelName();
         checkModelName(modelName);
         return new SDFFullRobotModel(generalizedSDFRobotModels.get(modelName).getRootLinks().get(0), sdfJointNameMap);
      }
      else
      {
         throw new RuntimeException("Cannot make a fullrobotmodel without a sdfJointNameMap");
      }
   }
}
