package us.ihmc.valkyrie;

import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.visualizer.CommonInertiaElipsoidsVisualizer;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;

import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.Link;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

/**
 * Created by dstephen on 2/7/14.
 */
public class ValkyrieSDFLoadingDemo
{
   private static final boolean SHOW_ELLIPSOIDS = true;

   private SimulationConstructionSet scs;

   public ValkyrieSDFLoadingDemo()
   {
      ValkyrieRobotModel robotModel = new ValkyrieRobotModel(false, false);
      
      SDFRobot valkyrieRobot = robotModel.createSdfRobot(false);
      valkyrieRobot.setPositionInWorld(new Vector3d());
      
      if (SHOW_ELLIPSOIDS)
      {
         addIntertialEllipsoidsToVisualizer(valkyrieRobot);
      }

      SDFFullRobotModel sdfFullRobotModel = robotModel.createFullRobotModel();
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();
      CommonInertiaElipsoidsVisualizer inertiaVis = new CommonInertiaElipsoidsVisualizer(sdfFullRobotModel.getElevator(), dynamicGraphicObjectsListRegistry);
      inertiaVis.update();
      
      
      scs = new SimulationConstructionSet(valkyrieRobot);
      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
      scs.setGroundVisible(false);
      scs.startOnAThread();
   }

   private void addIntertialEllipsoidsToVisualizer(SDFRobot valkyrieRobot)
   {
      ArrayList<Joint> joints = new ArrayList<>();
      joints.add(valkyrieRobot.getRootJoint());

      HashSet<Link> links = getAllLinks(joints, new HashSet<Link>());

      for (Link l : links)
      {
         AppearanceDefinition appearance = YoAppearance.Green();
         appearance.setTransparency(0.6);
         l.addEllipsoidFromMassProperties(appearance);
         l.addCoordinateSystemToCOM(0.5);
//         l.addBoxFromMassProperties(appearance);
      }
   }

   private HashSet<Link> getAllLinks(ArrayList<Joint> joints, HashSet<Link> links)
   {
      for (Joint j : joints)
      {
         links.add(j.getLink());

         if (!j.getChildrenJoints().isEmpty())
         {
            links.addAll(getAllLinks(j.getChildrenJoints(), links));
         }
      }

      return links;
   }

   public static void main(String[] args)
   {
      new ValkyrieSDFLoadingDemo();
   }

}
