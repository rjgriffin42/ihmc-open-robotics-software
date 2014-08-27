package us.ihmc.commonWalkingControlModules.visualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix3d;

import us.ihmc.commonWalkingControlModules.controllers.Updatable;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.RigidBodyInertia;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import com.mathworks.jama.Matrix;
import com.mathworks.jama.SingularValueDecomposition;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObject;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicShape;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameOrientation;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class CommonInertiaElipsoidsVisualizer implements Updatable, RobotController
{
   private final String name = getClass().getSimpleName();

   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   
   private final YoFrameVector inertiaEllipsoidGhostOffset = new YoFrameVector("inertiaEllipsoidGhostOffset", "", worldFrame, registry);
   
   private final ArrayList<DynamicGraphicObject> dynamicGraphicObjects = new ArrayList<DynamicGraphicObject>();
   
   private final DoubleYoVariable minimumMassOfRigidBodies = new DoubleYoVariable("minimumMassOfRigidBodies", registry);
   private final DoubleYoVariable maximumMassOfRigidBodies = new DoubleYoVariable("maximumMassOfRigidBodies", registry);
   
   private class RigidBodyVisualizationData
   {
      public RigidBody rigidBody;
      public YoFramePoint position;
      public YoFrameOrientation orientation;
      
      public RigidBodyVisualizationData(RigidBody rigidBody, YoFramePoint position, YoFrameOrientation orientation)
      {
         this.rigidBody = rigidBody;
         this.position = position;
         this.orientation = orientation;
      }
      
   }
   private final ArrayList<RigidBodyVisualizationData> centerOfMassData = new ArrayList<RigidBodyVisualizationData>();

   public CommonInertiaElipsoidsVisualizer(RigidBody rootBody, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, YoVariableRegistry parentRegistry)
   {
      this(rootBody, dynamicGraphicObjectsListRegistry);
      parentRegistry.addChild(registry);
   }  
   public CommonInertiaElipsoidsVisualizer(RigidBody rootBody, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
      {
      inertiaEllipsoidGhostOffset.set(0, 0.0, 0.0);

      findMinimumAndMaximumMassOfRigidBodies(rootBody);
      addRigidBodyAndChilderenToVisualization(rootBody);
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjects(name, dynamicGraphicObjects);
      update();
   
   }
   
   private void findMinimumAndMaximumMassOfRigidBodies(RigidBody body)
   {
      RigidBodyInertia inertia = body.getInertia();
      if(inertia != null)
      {
         double mass = body.getInertia().getMass();
         
         if(mass < minimumMassOfRigidBodies.getDoubleValue() && mass > 1e-3)
            minimumMassOfRigidBodies.set(mass);
         
         
         if(mass > maximumMassOfRigidBodies.getDoubleValue())
            maximumMassOfRigidBodies.set(mass);
         
      }
      
      if(body.hasChildrenJoints())
      {
         List<InverseDynamicsJoint> childJoints = body.getChildrenJoints();
       
         for(InverseDynamicsJoint joint : childJoints)
         {
            RigidBody nextBody = joint.getSuccessor();
            if(nextBody != null)
               findMinimumAndMaximumMassOfRigidBodies(nextBody);
         }
         
      }
   }
   
   public Color getColor(double mass)
   {
      // Color from 
      // http://stackoverflow.com/questions/340209/generate-colors-between-red-and-green-for-a-power-meter
      
      if( mass < minimumMassOfRigidBodies.getDoubleValue())
         mass = minimumMassOfRigidBodies.getDoubleValue();
      
      float massScale = (float) ((mass - minimumMassOfRigidBodies.getDoubleValue()) / (maximumMassOfRigidBodies.getDoubleValue() - minimumMassOfRigidBodies.getDoubleValue()));
      
      float H = (1.0f - massScale) * 0.4f;
      float S = 0.9f;
      float B = 0.9f;
      
      return Color.getHSBColor(H, S, B);
   }
   
   private void addRigidBodyAndChilderenToVisualization(RigidBody currentRigidBody)
   {

      RigidBodyInertia inertia = currentRigidBody.getInertia();
      
      if(inertia != null)
      {
         Matrix3d inertiaMatrix = inertia.getMassMomentOfInertiaPartCopy();
         double mass = inertia.getMass();
         
//         Vector3d principalMomentsOfInertia = new Vector3d(inertiaMatrix.m00, inertiaMatrix.m11, inertiaMatrix.m22);
//         Vector3d radii = InertiaTools.getInertiaEllipsoidRadii(principalMomentsOfInertia, mass);
//         if(radii.length() > 1e-4)
//         {
            String rigidBodyName = currentRigidBody.getName();
            YoFramePoint comPosition = new YoFramePoint("centerOfMassPosition", rigidBodyName, worldFrame, registry);
            YoFrameOrientation comOrientation = new YoFrameOrientation("rigidBodyOrientation", rigidBodyName, worldFrame, registry);
            RigidBodyVisualizationData comData = new RigidBodyVisualizationData(currentRigidBody, comPosition, comOrientation);
            centerOfMassData.add(comData);
            
            AppearanceDefinition appearance = YoAppearance.Orange();
            appearance.setTransparency(0.6);
            
            
//            new DynamicGraphicShape(rigidBodyName, linkGraphics, framePose, scale)
            DynamicGraphicShape comViz = new DynamicGraphicShape(rigidBodyName + "CoMEllipsoid", createEllipsoid(inertiaMatrix, mass, appearance), comPosition, comOrientation, 1.0);  
            dynamicGraphicObjects.add(comViz);
//         }
      }
      
      if(currentRigidBody.hasChildrenJoints())
      {
         List<InverseDynamicsJoint> childJoints = currentRigidBody.getChildrenJoints();
       
         for(InverseDynamicsJoint joint : childJoints)
         {
            RigidBody nextRigidBody = joint.getSuccessor();
            if(nextRigidBody != null)
               addRigidBodyAndChilderenToVisualization(nextRigidBody);
         }
         
      }
      
   }
   
   public void update(double time)
   {
      update();
   }

   public void update()
   {

      FramePoint tempCoMPosition = new FramePoint(worldFrame); 
      for (RigidBodyVisualizationData comData : centerOfMassData)
      {
         comData.rigidBody.packCoMOffset(tempCoMPosition);
         tempCoMPosition.changeFrame(worldFrame);
         tempCoMPosition.add(inertiaEllipsoidGhostOffset.getFrameVectorCopy());
         
         FrameOrientation orientation = new FrameOrientation(comData.rigidBody.getBodyFixedFrame());
         orientation.changeFrame(worldFrame);
         
         comData.position.set(tempCoMPosition);
         comData.orientation.set(orientation);
      }
   }

   public void initialize()
   {
      update();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return name;
   }

   public String getDescription()
   {
      return getName();
   }

   public void doControl()
   {
      update();
   }
   
   private Graphics3DObject createEllipsoid(Matrix3d inertia, double mass, AppearanceDefinition appearance)
   {
      double[][] moiArray = new double[][]
            { {inertia.m00, inertia.m01, inertia.m02}, 
            {inertia.m10, inertia.m11, inertia.m12}, 
            {inertia.m20, inertia.m21, inertia.m22} };

       

       Matrix Inertia = new Matrix(moiArray);
       SingularValueDecomposition inertiaSvd = new SingularValueDecomposition(Inertia);

       Matrix S = inertiaSvd.getS();
//       @SuppressWarnings("unused") Matrix U = inertiaSvd.getU();
       Matrix V = inertiaSvd.getV();

       // System.out.println("S:\n " + MatrixToString(S));
       // System.out.println("U:\n " + MatrixToString(U));
       // System.out.println("V:\n " + MatrixToString(V));

       Matrix3d rotationMatrix3d = new Matrix3d(V.getRowPackedCopy());

       // Moment of inertia of an ellipsoid is 1/5 * mass * (ry^2+rz^2, rx^2+rz^2, rx^2+ry^2).  Backing this out:
       double a = 5.0 * S.get(0, 0) / mass, b = 5.0 * S.get(1, 1) / mass, c = 5.0 * S.get(2, 2) / mass;
       double rx = Math.sqrt(0.5 * (-a + b + c)), ry = Math.sqrt(0.5 * (a - b + c)), rz = Math.sqrt(0.5 * (a + b - c));
       
       
       Graphics3DObject graphics = new Graphics3DObject();
       graphics.identity();
       graphics.rotate(rotationMatrix3d);
       graphics.addEllipsoid(rx, ry, rz, appearance);

       return graphics;
   }
}
