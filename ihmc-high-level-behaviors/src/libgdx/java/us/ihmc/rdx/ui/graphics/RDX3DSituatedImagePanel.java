package us.ihmc.rdx.ui.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import org.lwjgl.opengl.GL41;
import us.ihmc.euclid.geometry.interfaces.Line3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.shape.primitives.Box3D;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.rdx.sceneManager.RDXSceneLevel;
import us.ihmc.rdx.tools.LibGDXTools;
import us.ihmc.rdx.ui.vr.RDX3DSituatedVideoPanelMode;
import us.ihmc.rdx.ui.vr.RDXVRModeManager;
import us.ihmc.rdx.vr.RDXVRContext;
import us.ihmc.robotics.interaction.BoxRayIntersection;
import us.ihmc.robotics.referenceFrames.ModifiableReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

import java.util.Set;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage.*;
import static com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates;
import static us.ihmc.rdx.ui.vr.RDX3DSituatedVideoPanelMode.FOLLOW_HEADSET;
import static us.ihmc.rdx.ui.vr.RDX3DSituatedVideoPanelMode.MANUAL_PLACEMENT;

public class RDX3DSituatedImagePanel
{
   private static final double FOLLOW_HEADSET_OFFSET_Y = 0.0;
   private static final double FOLLOW_HEADSET_OFFSET_Z = 0.17;

   private ModelInstance modelInstance;
   private Texture texture;
   private final RigidBodyTransform tempTransform = new RigidBodyTransform();
   private final FramePoint3D tempFramePoint = new FramePoint3D();
   private final Vector3 topLeftPosition = new Vector3();
   private final Vector3 bottomLeftPosition = new Vector3();
   private final Vector3 bottomRightPosition = new Vector3();
   private final Vector3 topRightPosition = new Vector3();
   private final Vector3 topLeftNormal = new Vector3(0.0f, 0.0f, 1.0f);
   private final Vector3 bottomLeftNormal = new Vector3(0.0f, 0.0f, 1.0f);
   private final Vector3 bottomRightNormal = new Vector3(0.0f, 0.0f, 1.0f);
   private final Vector3 topRightNormal = new Vector3(0.0f, 0.0f, 1.0f);
   private final Vector2 topLeftUV = new Vector2();
   private final Vector2 bottomLeftUV = new Vector2();
   private final Vector2 bottomRightUV = new Vector2();
   private final Vector2 topRightUV = new Vector2();

   private final RDXVRModeManager vrModeManager;
   private final ModifiableReferenceFrame floatingPanelFrame = new ModifiableReferenceFrame(ReferenceFrame.getWorldFrame());
   private final FramePose3D floatingPanelFramePose = new FramePose3D();
   private final RigidBodyTransform gripOffsetTransform = new RigidBodyTransform();
   private double lastTouchpadY = Double.NaN;
   private double panelZoom = 0;
   private double panelDistanceFromHeadset = 0.5;
   private SideDependentList<Boolean> grippedLastTime = new SideDependentList<>(false, false);
   private boolean justShown;
   private boolean isShowing;
   private Box3D frameOfVideo = new Box3D();
   private BoxRayIntersection frameOfVideoIntersection = new BoxRayIntersection();

   /**
    * Create for a programmatically placed panel.
    */
   public RDX3DSituatedImagePanel()
   {
      vrModeManager = null;
      isShowing = true;
   }

   /**
    * Create and enable VR interaction.
    * @param vrModeManager TODO: Remove this and replace with context based manipulation
    */
   public RDX3DSituatedImagePanel(RDXVRContext context, RDXVRModeManager vrModeManager)
   {
      this.vrModeManager = vrModeManager;
      context.addVRInputProcessor(this::addVRInputProcessor);
   }

   public void create(Texture texture, Frustum frustum, ReferenceFrame referenceFrame, boolean flipY)
   {
      // Counter clockwise order
      // Draw so thumb faces away and index right
      Vector3[] planePoints = frustum.planePoints;
      topLeftPosition.set(planePoints[7]);
      bottomLeftPosition.set(planePoints[4]);
      bottomRightPosition.set(planePoints[5]);
      topRightPosition.set(planePoints[6]);
      create(texture, referenceFrame, flipY);
   }

   public void create(Texture texture, Vector3[] points, ReferenceFrame referenceFrame, boolean flipY)
   {
      topLeftPosition.set(points[0]);
      bottomLeftPosition.set(points[1]);
      bottomRightPosition.set(points[2]);
      topRightPosition.set(points[3]);
      create(texture, referenceFrame, flipY);
   }

   public void create(Texture texture, double panelWidth, double panelHeight, ReferenceFrame referenceFrame, boolean flipY)
   {
      float halfPanelHeight = (float) panelHeight / 2.0f;
      float halfPanelWidth = (float) panelWidth / 2.0f;
      topLeftPosition.set(halfPanelHeight, halfPanelWidth, 0.0f);
      bottomLeftPosition.set(-halfPanelHeight, halfPanelWidth, 0.0f);
      bottomRightPosition.set(-halfPanelHeight, -halfPanelWidth, 0.0f);
      topRightPosition.set(halfPanelHeight, -halfPanelWidth, 0.0f);
      create(texture, referenceFrame, flipY);
   }

   private void create(Texture texture, ReferenceFrame referenceFrame, boolean flipY)
   {
      this.texture = texture;
      ModelBuilder modelBuilder = new ModelBuilder();
      modelBuilder.begin();

      MeshBuilder meshBuilder = new MeshBuilder();
      meshBuilder.begin(Position | Normal | ColorUnpacked | TextureCoordinates, GL41.GL_TRIANGLES);

      // Counter clockwise order
      // Draw so thumb faces away and index right
      topLeftUV.set(0.0f, flipY ? 1.0f : 0.0f);
      bottomLeftUV.set(0.0f, flipY ? 0.0f : 1.0f);
      bottomRightUV.set(1.0f, flipY ? 0.0f : 1.0f);
      topRightUV.set(1.0f, flipY ? 1.0f : 0.0f);
      tempFramePoint.setToZero(ReferenceFrame.getWorldFrame());
      LibGDXTools.toEuclid(topLeftPosition, tempFramePoint);
      tempFramePoint.changeFrame(referenceFrame);
      LibGDXTools.toLibGDX(tempFramePoint, topLeftPosition);
      meshBuilder.vertex(topLeftPosition, topLeftNormal, Color.WHITE, topLeftUV);
      tempFramePoint.setToZero(ReferenceFrame.getWorldFrame());
      LibGDXTools.toEuclid(bottomLeftPosition, tempFramePoint);
      tempFramePoint.changeFrame(referenceFrame);
      LibGDXTools.toLibGDX(tempFramePoint, bottomLeftPosition);
      meshBuilder.vertex(bottomLeftPosition, bottomLeftNormal, Color.WHITE, bottomLeftUV);
      tempFramePoint.setToZero(ReferenceFrame.getWorldFrame());
      LibGDXTools.toEuclid(bottomRightPosition, tempFramePoint);
      tempFramePoint.changeFrame(referenceFrame);
      LibGDXTools.toLibGDX(tempFramePoint, bottomRightPosition);
      meshBuilder.vertex(bottomRightPosition, bottomRightNormal, Color.WHITE, bottomRightUV);
      tempFramePoint.setToZero(ReferenceFrame.getWorldFrame());
      LibGDXTools.toEuclid(topRightPosition, tempFramePoint);
      tempFramePoint.changeFrame(referenceFrame);
      LibGDXTools.toLibGDX(tempFramePoint, topRightPosition);
      meshBuilder.vertex(topRightPosition, topRightNormal, Color.WHITE, topRightUV);
      meshBuilder.triangle((short) 3, (short) 0, (short) 1);
      meshBuilder.triangle((short) 1, (short) 2, (short) 3);
      Mesh mesh = meshBuilder.end();

      MeshPart meshPart = new MeshPart("xyz", mesh, 0, mesh.getNumIndices(), GL41.GL_TRIANGLES);
      Material material = new Material();

      material.set(TextureAttribute.createDiffuse(texture));
      material.set(ColorAttribute.createDiffuse(new Color(0.68235f, 0.688235f, 0.688235f, 1.0f)));
      modelBuilder.part(meshPart, material);

      // TODO: Rebuild the model if the camera parameters change.
      Model model = modelBuilder.end();
      modelInstance = new ModelInstance(model);
   }

   public void update(Texture imageTexture)
   {
      if (vrModeManager != null)
      {
         isShowing = vrModeManager.getShowFloatingVideoPanel().get();
         justShown = vrModeManager.getShowFloatVideoPanelNotification().poll();
      }

      if (isShowing && imageTexture != null && imageTexture != texture)
      {
         boolean flipY = false;
         float multiplier = 2.0f;
         float halfWidth = imageTexture.getWidth() / 10000.0f * multiplier;
         float halfHeight = imageTexture.getHeight() / 10000.0f * multiplier;
         create(imageTexture,
                new Vector3[] {new Vector3(0.0f, halfWidth, halfHeight),
                               new Vector3(0.0f, halfWidth, -halfHeight),
                               new Vector3(0.0f, -halfWidth, -halfHeight),
                               new Vector3(0.0f, -halfWidth, halfHeight)},
                floatingPanelFrame.getReferenceFrame(),
                flipY);
      }

      setPoseToReferenceFrame(floatingPanelFrame.getReferenceFrame());
   }

   public void addVRInputProcessor(RDXVRContext context)
   {
      RDX3DSituatedVideoPanelMode placementMode = vrModeManager.getVideoPanelPlacementMode();

      context.getHeadset().runIfConnected(headset ->
      {
         if (placementMode == FOLLOW_HEADSET || (placementMode == MANUAL_PLACEMENT && justShown))
         {
            if (modelInstance != null)
            {
               floatingPanelFramePose.setToZero(headset.getXForwardZUpHeadsetFrame());
               floatingPanelFramePose.getPosition().set(panelDistanceFromHeadset, FOLLOW_HEADSET_OFFSET_Y, FOLLOW_HEADSET_OFFSET_Z);
               floatingPanelFramePose.changeFrame(ReferenceFrame.getWorldFrame());
               floatingPanelFramePose.get(floatingPanelFrame.getTransformToParent());
               floatingPanelFrame.getReferenceFrame().update();
            }
         }
      });
      frameOfVideo.set(floatingPanelFramePose, 0.15,Math.abs(topRightPosition.y-topLeftPosition.y), Math.abs(topRightPosition.y - bottomLeftPosition.y));
      for (RobotSide side : RobotSide.values)
      {
         context.getController(side).runIfConnected(controller ->
         {
            Line3DReadOnly pickRay = controller.getPickRay();
            boolean intersecting = frameOfVideoIntersection.intersect(frameOfVideo.getSizeX(), frameOfVideo.getSizeY(), frameOfVideo.getSizeZ(),
                                                                      floatingPanelFrame.getReferenceFrame().getTransformToWorldFrame(), pickRay);
            if (placementMode == MANUAL_PLACEMENT)
            {
               if (modelInstance != null)
               {
                  floatingPanelFramePose.setToZero(floatingPanelFrame.getReferenceFrame());
                  floatingPanelFramePose.changeFrame(ReferenceFrame.getWorldFrame());
                  boolean intersectVideo = frameOfVideo.isPointInside(controller.getPickPointPose().getPosition());

                  boolean isGripping = controller.getGripActionData().x() > 0.9;
                  if ((grippedLastTime.get(side) || intersectVideo) && isGripping)
                  {
                     if (!grippedLastTime.get(side)) // set up offset
                     {
                        floatingPanelFramePose.changeFrame(controller.getXForwardZUpControllerFrame());
                        floatingPanelFramePose.get(gripOffsetTransform);
                        floatingPanelFramePose.changeFrame(ReferenceFrame.getWorldFrame());
                     }
                     floatingPanelFrame.getTransformToParent().set(gripOffsetTransform);
                     controller.getXForwardZUpControllerFrame().getTransformToWorldFrame().transform(floatingPanelFrame.getTransformToParent());
                     floatingPanelFrame.getReferenceFrame().update();

                     grippedLastTime.put(side, true);
                  }
                  else
                  {
                     grippedLastTime.put(side, false);
                  }
               }
            }
            else if (intersecting)
            {
               if (controller.getTouchpadTouchedActionData().bState())
               {
                  double y = controller.getTouchpadActionData().y();
                  if (!Double.isNaN(lastTouchpadY))
                  {
                     panelZoom = y - lastTouchpadY;
                  }
                  lastTouchpadY = y;
                  panelDistanceFromHeadset = panelDistanceFromHeadset + panelZoom;
               }
               else

               {
                  lastTouchpadY = Double.NaN;
               }
            }
         });
      }
   }

   public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool, Set<RDXSceneLevel> sceneLevels)
   {
      if (modelInstance != null && isShowing && sceneLevels.contains(RDXSceneLevel.VIRTUAL))
         modelInstance.getRenderables(renderables, pool);
   }

   public void setPoseToReferenceFrame(ReferenceFrame referenceFrame)
   {
      if (modelInstance != null)
      {
         referenceFrame.getTransformToDesiredFrame(tempTransform, ReferenceFrame.getWorldFrame());
         LibGDXTools.toLibGDX(tempTransform, modelInstance.transform);
      }
   }

   public ModelInstance getModelInstance()
   {
      return modelInstance;
   }

   public Texture getTexture()
   {
      return texture;
   }
}
