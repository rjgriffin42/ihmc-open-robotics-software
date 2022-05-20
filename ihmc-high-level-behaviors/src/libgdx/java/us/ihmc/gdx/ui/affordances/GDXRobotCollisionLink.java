package us.ihmc.gdx.ui.affordances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import us.ihmc.euclid.geometry.interfaces.Line3DReadOnly;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.shape.primitives.interfaces.*;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.UnitVector3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.gdx.input.ImGui3DViewInput;
import us.ihmc.gdx.input.ImGui3DViewPickResult;
import us.ihmc.gdx.simulation.environment.GDXModelInstance;
import us.ihmc.gdx.tools.GDXModelPrimitives;
import us.ihmc.gdx.tools.GDXTools;
import us.ihmc.gdx.ui.gizmo.BoxRayIntersection;
import us.ihmc.gdx.ui.gizmo.CapsuleRayIntersection;
import us.ihmc.gdx.ui.gizmo.SphereRayIntersection;
import us.ihmc.log.LogTools;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.robotics.physics.Collidable;

public class GDXRobotCollisionLink implements RenderableProvider
{
   private final GDXModelInstance modelInstance;
   private final RigidBodyTransform transformToJoint;
   private final ReferenceFrame collisionMeshFrame;
   private final FramePose3D boxPose = new FramePose3D();
   private final RigidBodyTransform boxCenterToWorldTransform = new RigidBodyTransform();
   private final Shape3DReadOnly shape;
   private final MovingReferenceFrame frameAfterJoint;
   private final ImGui3DViewPickResult pickResult = new ImGui3DViewPickResult();
   private SphereRayIntersection sphereRayIntersection;
   private CapsuleRayIntersection capsuleIntersection;
   private BoxRayIntersection boxRayIntersection;
   private GDXModelInstance coordinateFrame;
   private boolean useOverrideTransform = false;
   private final RigidBodyTransform overrideTransform = new RigidBodyTransform();
   private final ReferenceFrame overrideFrame;
   private final ReferenceFrame overrideMeshFrame;
   private boolean pickSelected = false;

   public GDXRobotCollisionLink(us.ihmc.scs2.simulation.collision.Collidable collidable, Color color)
   {
      this(collidable.getShape(),
           collidable.getShape().getReferenceFrame(),
           collidable.getRigidBody().getParentJoint().getFrameAfterJoint(),
           collidable.getRigidBody().getName(),
           color);
   }

   public GDXRobotCollisionLink(Collidable collidable, Color color)
   {
      this(collidable.getShape(),
           collidable.getShape().getReferenceFrame(),
           collidable.getRigidBody().getParentJoint().getFrameAfterJoint(),
           collidable.getRigidBody().getName(),
           color);
   }

   public GDXRobotCollisionLink(Shape3DReadOnly shape,
                                ReferenceFrame shapeFrame,
                                MovingReferenceFrame frameAfterJoint,
                                String rigidBodyName,
                                Color color)
   {
      this.shape = shape;
      this.frameAfterJoint = frameAfterJoint;
      // TODO update every frame
      transformToJoint = new RigidBodyTransform(shapeFrame.getTransformToDesiredFrame(frameAfterJoint));
      collisionMeshFrame = ReferenceFrameTools.constructFrameWithChangingTransformToParent("collisionMeshFrame" + rigidBodyName,
                                                                                           frameAfterJoint,
                                                                                           transformToJoint);
      overrideFrame = ReferenceFrameTools.constructFrameWithChangingTransformToParent("overrideFrame" + rigidBodyName,
                                                                                      ReferenceFrame.getWorldFrame(),
                                                                                      overrideTransform);
      overrideMeshFrame = ReferenceFrameTools.constructFrameWithChangingTransformToParent("overrideMeshFrame" + rigidBodyName, overrideFrame, transformToJoint);

      modelInstance = new GDXModelInstance(GDXModelPrimitives.buildModel(meshBuilder ->
      {
         if (shape instanceof Sphere3DReadOnly)
         {
            Sphere3DReadOnly sphere = (Sphere3DReadOnly) shape;
            meshBuilder.addSphere((float) sphere.getRadius(), sphere.getPosition(), color);
            sphereRayIntersection = new SphereRayIntersection();
         }
         else if (shape instanceof Capsule3DReadOnly)
         {
            Capsule3DReadOnly capsule = (Capsule3DReadOnly) shape;
            Quaternion orientation = new Quaternion();
            EuclidGeometryTools.orientation3DFromZUpToVector3D(capsule.getAxis(), orientation);
            transformToJoint.appendTranslation(capsule.getPosition());
            transformToJoint.appendOrientation(orientation);
            meshBuilder.addCapsule(capsule.getLength(),
                                   capsule.getRadius(),
                                   capsule.getRadius(),
                                   capsule.getRadius(),
                                   50,
                                   50,
                                   color);
            capsuleIntersection = new CapsuleRayIntersection();
         }
         else if (shape instanceof Box3DReadOnly)
         {
            Box3DReadOnly box = (Box3DReadOnly) shape;
            transformToJoint.appendTranslation(box.getPosition());
            transformToJoint.appendOrientation(box.getOrientation());
            meshBuilder.addBox(box.getSizeX(),
                               box.getSizeY(),
                               box.getSizeZ(),
                               color);
            boxRayIntersection = new BoxRayIntersection();
         }
         else if (shape instanceof PointShape3DReadOnly)
         {
            PointShape3DReadOnly pointShape = (PointShape3DReadOnly) shape;
            meshBuilder.addSphere((float) 0.01, pointShape, color);
         }
         else
         {
            LogTools.warn("Shape not handled: {}", shape);
         }
      }, rigidBodyName));
      GDXTools.setTransparency(modelInstance, color.a);

      coordinateFrame = new GDXModelInstance(GDXModelPrimitives.createCoordinateFrame(0.15));
   }

   public void update()
   {
      if (useOverrideTransform)
      {
         overrideFrame.update();
         overrideMeshFrame.update();
         modelInstance.setTransformToReferenceFrame(overrideMeshFrame);
         coordinateFrame.setTransformToReferenceFrame(overrideMeshFrame);
      }
      else
      {
         collisionMeshFrame.update();
         modelInstance.setTransformToReferenceFrame(collisionMeshFrame);
         coordinateFrame.setTransformToReferenceFrame(collisionMeshFrame);
      }
   }

   public void calculatePick(ImGui3DViewInput input)
   {
      Line3DReadOnly pickRayInWorld = input.getPickRayInWorld();
      ReferenceFrame frameAfterJointToUse = useOverrideTransform ? overrideMeshFrame : frameAfterJoint;
      pickResult.reset();
      if (shape instanceof Sphere3DReadOnly)
      {
         Sphere3DReadOnly sphere = (Sphere3DReadOnly) shape;
         Point3DReadOnly position = sphere.getPosition();
         // TODO: Implement
      }
      else if (shape instanceof Capsule3DReadOnly)
      {
         Capsule3DReadOnly capsule = (Capsule3DReadOnly) shape;
         UnitVector3DReadOnly axis = capsule.getAxis();
         Point3DReadOnly position = capsule.getPosition();
         double length = capsule.getLength();
         double radius = capsule.getRadius();
         capsuleIntersection.setup(radius, length, position, axis, frameAfterJointToUse);
         if (capsuleIntersection.intersect(pickRayInWorld))
         {
            pickResult.addPickCollision(capsuleIntersection.getDistanceToCollision(input.getPickRayInWorld()));
         }
      }
      else if (shape instanceof Box3DReadOnly)
      {
         Box3DReadOnly box = (Box3DReadOnly) shape;
         boxPose.setToZero(frameAfterJointToUse);
         if (!useOverrideTransform)
            boxPose.set(box.getPose());
         boxPose.changeFrame(ReferenceFrame.getWorldFrame());
         boxPose.get(boxCenterToWorldTransform);
         double sizeX = box.getSizeX();
         double sizeY = box.getSizeY();
         double sizeZ = box.getSizeZ();
         if (boxRayIntersection.intersect(sizeX, sizeY, sizeZ, boxCenterToWorldTransform, pickRayInWorld))
         {
            Point3DReadOnly boxIntersection = boxRayIntersection.getFirstIntersectionToPack();
            Point3DReadOnly cameraPosition = input.getPickRayInWorld().getPoint();
            pickResult.addPickCollision(boxIntersection.distance(cameraPosition));
         }
      }
      else if (shape instanceof PointShape3DReadOnly)
      {
         PointShape3DReadOnly pointShape = (PointShape3DReadOnly) shape;
      }
      else
      {
         LogTools.warn("Shape not handled: {}", shape);
      }

      input.addPickResult(pickResult);
   }

   // Happens after update
   public void process3DViewInput(ImGui3DViewInput input)
   {
      pickSelected = input.getClosestPick() == pickResult;
      GDXTools.setTransparency(modelInstance, pickSelected ? 1.0f : 0.4f);
   }

   @Override
   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool)
   {
      modelInstance.getRenderables(renderables, pool);
      coordinateFrame.getRenderables(renderables, pool);
   }

   public RigidBodyTransform setOverrideTransform(boolean useOverrideTransform)
   {
      this.useOverrideTransform = useOverrideTransform;
      return overrideTransform;
   }

   public boolean getPickSelected()
   {
      return pickSelected;
   }
}