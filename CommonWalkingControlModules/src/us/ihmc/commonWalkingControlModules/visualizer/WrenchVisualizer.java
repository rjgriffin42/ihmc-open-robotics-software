package us.ihmc.commonWalkingControlModules.visualizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicVector;
import us.ihmc.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;


/**
 * @author twan
 *         Date: 6/2/13
 */
public class WrenchVisualizer
{
   private static final double FORCE_VECTOR_SCALE = 0.0015;
   private static final double TORQUE_VECTOR_SCALE = 0.0015;

   private YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final Map<RigidBody, YoFrameVector> forces = new LinkedHashMap<RigidBody, YoFrameVector>();
   private final Map<RigidBody, YoFrameVector> torques = new LinkedHashMap<RigidBody, YoFrameVector>();
   private final Map<RigidBody, YoFramePoint> pointsOfApplication = new LinkedHashMap<RigidBody, YoFramePoint>();
   private final Map<RigidBody, YoGraphicVector> forceVisualizers = new LinkedHashMap<RigidBody, YoGraphicVector>();
   private final Map<RigidBody, YoGraphicVector> torqueVisualizers = new LinkedHashMap<RigidBody, YoGraphicVector>();

   private final Wrench tempWrench = new Wrench();
   private final FrameVector tempVector = new FrameVector();
   private final FramePoint tempPoint = new FramePoint();
   private final ArrayList<RigidBody> rigidBodies = new ArrayList<RigidBody>();

   public WrenchVisualizer(String name, List<RigidBody> rigidBodies, double vizScaling, YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      this(name, rigidBodies, vizScaling, yoGraphicsListRegistry, parentRegistry, YoAppearance.OrangeRed(), YoAppearance.CornflowerBlue());
   }
   
   public WrenchVisualizer(String name, List<RigidBody> rigidBodies, double vizScaling, YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry, 
         AppearanceDefinition forceAppearance, AppearanceDefinition torqueAppearance)
   {
      YoGraphicsList yoGraphicsList = new YoGraphicsList(name);

      this.rigidBodies.addAll(rigidBodies);
      for (RigidBody rigidBody : rigidBodies)
      {
         String prefix = name + rigidBody.getName();
         YoFrameVector force = new YoFrameVector(prefix + "Force" , ReferenceFrame.getWorldFrame(), registry);
         forces.put(rigidBody, force);

         YoFrameVector torque = new YoFrameVector(prefix + "Torque" , ReferenceFrame.getWorldFrame(), registry);
         torques.put(rigidBody, torque);

         YoFramePoint pointOfApplication = new YoFramePoint(prefix + "PointOfApplication" , ReferenceFrame.getWorldFrame(), registry);
         pointsOfApplication.put(rigidBody, pointOfApplication);

         YoGraphicVector forceVisualizer = new YoGraphicVector(prefix + "ForceViz", pointOfApplication, force, FORCE_VECTOR_SCALE * vizScaling,
                                                  forceAppearance, true);
         forceVisualizers.put(rigidBody, forceVisualizer);
         yoGraphicsList.add(forceVisualizer);

         YoGraphicVector torqueVisualizer = new YoGraphicVector(prefix + "TorqueViz", pointOfApplication, torque, TORQUE_VECTOR_SCALE * vizScaling,
                                                    torqueAppearance, true);
         torqueVisualizers.put(rigidBody, torqueVisualizer);
         yoGraphicsList.add(torqueVisualizer);
      }

      yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);

      parentRegistry.addChild(registry);
   }



   public void visualize(Map<RigidBody, Wrench> wrenches)
   {
      for (int i = 0; i < rigidBodies.size(); i++)
      {
         RigidBody rigidBody = rigidBodies.get(i);
         Wrench wrench;
         if((wrench = wrenches.get(rigidBody)) != null)
         {
            tempWrench.set(wrench);
            tempWrench.changeFrame(tempWrench.getBodyFrame());
            
            YoFrameVector force = forces.get(rigidBody);
            tempVector.setToZero(tempWrench.getExpressedInFrame());
            tempWrench.packLinearPart(tempVector);
            tempVector.changeFrame(ReferenceFrame.getWorldFrame());
            force.set(tempVector);
            
            YoFrameVector torque = torques.get(rigidBody);
            tempVector.setToZero(tempWrench.getExpressedInFrame());
            tempWrench.packAngularPart(tempVector);
            tempVector.changeFrame(ReferenceFrame.getWorldFrame());
            torque.set(tempVector);
            
            YoFramePoint pointOfApplication = pointsOfApplication.get(rigidBody);
            tempPoint.setToZero(wrench.getBodyFrame());
            tempPoint.changeFrame(ReferenceFrame.getWorldFrame());
            pointOfApplication.set(tempPoint);
         }
         else
         {
            forces.get(rigidBody).set(Double.NaN, Double.NaN, Double.NaN);
            torques.get(rigidBody).set(Double.NaN, Double.NaN, Double.NaN);
            pointsOfApplication.get(rigidBody).set(Double.NaN, Double.NaN, Double.NaN);        
         }
      }
   }
}
