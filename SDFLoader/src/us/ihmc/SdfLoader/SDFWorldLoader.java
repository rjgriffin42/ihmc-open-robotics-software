package us.ihmc.SdfLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import us.ihmc.SdfLoader.xmlDescription.SDFWorld.Road;
import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.jme.JMEGeneratedHeightMap;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNodeType;
import us.ihmc.utilities.FileTools;
import us.ihmc.utilities.Pair;

public class SDFWorldLoader
{
   public HashMap<String, Graphics3DObject> visuals = new HashMap<String, Graphics3DObject>();
   private final JaxbSDFLoader jaxbSDFLoader;

   public SDFWorldLoader(File file, String resourceDirectory) throws FileNotFoundException, JAXBException
   {
      this(file, FileTools.createArrayListOfOneURL(resourceDirectory));
   }

   public SDFWorldLoader(File file, ArrayList<String> resourceDirectories) throws FileNotFoundException, JAXBException
   {
      jaxbSDFLoader = new JaxbSDFLoader(file, resourceDirectories);
      for (GeneralizedSDFRobotModel generalizedSDFRobotModel : jaxbSDFLoader.getGeneralizedSDFRobotModels())
      {
         String name = generalizedSDFRobotModel.getName();
         visuals.put(name, new SDFModelVisual(generalizedSDFRobotModel));
      }

      for (Road road : jaxbSDFLoader.getRoads())
      {
         visuals.put(road.getName(), new SDFRoadVisual(road));
      }
   }

   public GroundProfile getGroundProfile(GroundProfileObjectFilter groundProfileObjectFilter, int resolution)
   {
      ArrayList<Graphics3DNode> nodes = new ArrayList<Graphics3DNode>();
      for (Entry<String, Graphics3DObject> visual : visuals.entrySet())
      {
         if (groundProfileObjectFilter.isTerrainObject(visual.getKey()))
         {
            Graphics3DNode node = new Graphics3DNode(visual.getKey(), Graphics3DNodeType.GROUND);
            node.setGraphicsObject(visual.getValue());
            nodes.add(node);
         }
      }
      GroundProfile groundProfile = new JMEGeneratedHeightMap(nodes, resolution);

      return groundProfile;
   }

   public Pair<SDFRobot, SDFFullRobotModel> createRobotAndRemoveFromWorld(SDFJointNameMap sdfJointNameMap)
   {
      removeVisualFromWorld(sdfJointNameMap.getModelName());

      Pair<SDFRobot, SDFFullRobotModel> ret = new Pair<SDFRobot, SDFFullRobotModel>(jaxbSDFLoader.createRobot(sdfJointNameMap),
            jaxbSDFLoader.createFullRobotModel(sdfJointNameMap));

      return ret;
   }

   private void removeVisualFromWorld(String modelName)
   {
      if (!visuals.containsKey(modelName))
      {
         throw new RuntimeException("Unkown robot");
      }
      visuals.remove(modelName);
   }
   
   public GeneralizedSDFRobotModel getGeneralizedRobotModelAndRemoveFromWorld(String modelName)
   {
      removeVisualFromWorld(modelName);
      return jaxbSDFLoader.getGeneralizedSDFRobotModel(modelName);
   }

   public Graphics3DObject createGraphics3dObject()
   {
      Graphics3DObject ret = new Graphics3DObject();
      for (Graphics3DObject visual : visuals.values())
      {
         ret.combine(visual);
      }
      
      return ret;
   }

   public interface GroundProfileObjectFilter
   {
      public boolean isTerrainObject(String name);
   }
}
