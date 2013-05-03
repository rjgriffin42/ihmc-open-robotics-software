package us.ihmc.SdfLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.media.j3d.Transform3D;

import us.ihmc.SdfLoader.xmlDescription.SDFJoint;
import us.ihmc.SdfLoader.xmlDescription.SDFLink;
import us.ihmc.SdfLoader.xmlDescription.SDFModel;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;

import com.yobotics.simulationconstructionset.graphics.GraphicsObjectsHolder;

public class GeneralizedSDFRobotModel implements GraphicsObjectsHolder
{
   private final String name;
   private final ArrayList<String> resourceDirectories;
   private final ArrayList<SDFLinkHolder> rootLinks = new ArrayList<SDFLinkHolder>();
   private final Transform3D transformToRoot;
   private final LinkedHashMap<String, SDFJointHolder> joints = new LinkedHashMap<String, SDFJointHolder>();
   private final LinkedHashMap<String, SDFLinkHolder> links = new LinkedHashMap<String, SDFLinkHolder>();
   
   
   public GeneralizedSDFRobotModel(String name, SDFModel model, ArrayList<String> resourceDirectories)
   {
      this.name = name;
      this.resourceDirectories = resourceDirectories;
      List<SDFLink> sdfLinks = model.getLinks();
      List<SDFJoint> sdfJoints = model.getJoints();
      
      

      // Populate maps
      for (SDFLink sdfLink : sdfLinks)
      {
         links.put(SDFConversionsHelper.sanitizeJointName(sdfLink.getName()), new SDFLinkHolder(sdfLink));
      }
      
      if(sdfJoints != null)
      {
         for (SDFJoint sdfJoint : sdfJoints)
         {
            String parent = SDFConversionsHelper.sanitizeJointName(sdfJoint.getParent());
            String child = SDFConversionsHelper.sanitizeJointName(sdfJoint.getChild());
            try
            {
               joints.put(SDFConversionsHelper.sanitizeJointName(sdfJoint.getName()), new SDFJointHolder(sdfJoint, links.get(parent), links.get(child)));
            }
            catch (IOException e)
            {
               System.err.println(e);
            }
         }
      }

      // Calculate transformations between joints
      for (Entry<String, SDFJointHolder> joint : joints.entrySet())
      {
         joint.getValue().calculateTransformToParent();
      }

      for (Entry<String, SDFLinkHolder> link : links.entrySet())
      {
         link.getValue().calculateCoMOffset();
      }

      findRootLinks(links);
      
      transformToRoot = SDFConversionsHelper.poseToTransform(model.getPose());

   }

   private void findRootLinks(HashMap<String, SDFLinkHolder> links)
   {
      for (Entry<String, SDFLinkHolder> linkEntry : links.entrySet())
      {
         SDFLinkHolder link = linkEntry.getValue();
         if (link.getJoint() == null)
         {
            rootLinks.add(link);
         }
      }
   }

   public ArrayList<SDFLinkHolder> getRootLinks()
   {
      return rootLinks;
   }
   
   public Transform3D getTransformToRoot()
   {
      return transformToRoot;
   }
   
   public String getName()
   {
      return name;
   }

   public ArrayList<String> getResourceDirectories()
   {
      return resourceDirectories;
   }

   public Graphics3DObject getGraphicsObject(String name)
   {
      
      for(SDFLinkHolder linkHolder : rootLinks)
      {
         if(linkHolder.getName().equals(name))
         {
            return new SDFGraphics3DObject(linkHolder.getVisuals(), resourceDirectories);
         }
      }
      
      return new SDFGraphics3DObject(joints.get(name).getChild().getVisuals(), resourceDirectories);
   }

}
