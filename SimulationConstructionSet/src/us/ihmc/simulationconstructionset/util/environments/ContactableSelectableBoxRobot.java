package us.ihmc.simulationconstructionset.util.environments;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceRGBColor;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddMeshDataInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DInstruction;
import us.ihmc.graphics3DAdapter.input.Key;
import us.ihmc.graphics3DAdapter.input.ModifierKeyInterface;
import us.ihmc.graphics3DAdapter.input.SelectedListener;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.GroundContactPointGroup;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.utilities.containers.EnumTools;
import us.ihmc.utilities.math.RotationalInertiaCalculator;
import us.ihmc.utilities.math.geometry.Box3d;
import us.ihmc.utilities.math.geometry.Box3d.FaceName;
import us.ihmc.utilities.math.geometry.Direction;
import us.ihmc.utilities.math.geometry.FrameBox3d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicVector;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;

public class ContactableSelectableBoxRobot extends ContactableRobot implements SelectableObject, SelectedListener
{
   private static final double DEFAULT_LENGTH = 1.0;
   private static final double DEFAULT_WIDTH = 0.6;
   private static final double DEFAULT_HEIGHT = 1.2;

   private static final double DEFAULT_MASS = 10.0;

   private final FrameBox3d frameBox;

   private final FloatingJoint floatingJoint;
   private final Link boxLink;
   private final Graphics3DObject linkGraphics;

   // graphics
   private final EnumMap<FaceName, Graphics3DInstruction> faceGraphics = new EnumMap<FaceName, Graphics3DInstruction>(FaceName.class);
   private final EnumYoVariable<Direction> selectedDirection = EnumYoVariable.create("selectedDirection", "", Direction.class, yoVariableRegistry, true);
   private static final Color defaultColor = Color.BLUE;
   private static final Color selectedColor = Color.RED;

   private double selectTransparency = 1.0;
   private double unselectTransparency = 1.0;

   private final ArrayList<SelectableObjectListener> selectedListeners = new ArrayList<SelectableObjectListener>();

   public ContactableSelectableBoxRobot(String name, double length, double width, double height, double mass)
   {
      super(name);

      floatingJoint = new FloatingJoint(name + "Base", name, new Vector3d(0.0, 0.0, 0.0), this);
      linkGraphics = new Graphics3DObject();
      linkGraphics.setChangeable(true);
      boxLink = boxLink(linkGraphics, length, width, height, mass);
      floatingJoint.setLink(boxLink);
      this.addRootJoint(floatingJoint);

      frameBox = new FrameBox3d(ReferenceFrame.getWorldFrame(), length, width, height);

      createBoxGraphics(frameBox);
      setUpGroundContactPoints(frameBox);

      unSelect(true);

      linkGraphics.registerSelectedListener(this);
   }

   public static ContactableSelectableBoxRobot createContactableCardboardBoxRobot(String name, double length, double width, double height, double mass)
   {
      ContactableSelectableBoxRobot contactableBoxRobot = new ContactableSelectableBoxRobot(name, length, width, height, mass);
      contactableBoxRobot.createCardboardBoxGraphics(length, width, height);

      initializeDefaults(contactableBoxRobot);

      return contactableBoxRobot;
   }

   public static ContactableSelectableBoxRobot createContactableWoodBoxRobot(String name, double length, double width, double height, double mass)
   {
      ContactableSelectableBoxRobot contactableBoxRobot = new ContactableSelectableBoxRobot(name, length, width, height, mass);
      contactableBoxRobot.createWoodBoxGraphics(length, width, height);

      initializeDefaults(contactableBoxRobot);

      return contactableBoxRobot;
   }

   public static ContactableSelectableBoxRobot createContactable2By4Robot(String name, double length, double width, double height, double mass)
   {
      ContactableSelectableBoxRobot contactableBoxRobot = new ContactableSelectableBoxRobot(name, length, width, height, mass);
      contactableBoxRobot.create2By4Graphics(length, width, height);

      initializeDefaults(contactableBoxRobot);

      return contactableBoxRobot;
   }

   private static void initializeDefaults(ContactableSelectableBoxRobot contactableBoxRobot)
   {
      contactableBoxRobot.selectTransparency = 0.1;
      contactableBoxRobot.unselectTransparency = 0.9;

      contactableBoxRobot.unSelect(true);
   }

   public ContactableSelectableBoxRobot()
   {
      this("ContactableBoxRobot");
   }

   public ContactableSelectableBoxRobot(String name)
   {
      this(name, DEFAULT_LENGTH, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MASS);
   }

   public void addDynamicGraphicForceVectorsToGroundContactPoints(double forceVectorScale, AppearanceDefinition appearance,
         YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      addDynamicGraphicForceVectorsToGroundContactPoints(0, forceVectorScale, appearance, yoGraphicsListRegistry);
   }

   public void addDynamicGraphicForceVectorsToGroundContactPoints(int groupIdentifier, double forceVectorScale, AppearanceDefinition appearance,
         YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      if (yoGraphicsListRegistry == null)
         return;

      GroundContactPointGroup groundContactPointGroup = floatingJoint.physics.getGroundContactPointGroup(groupIdentifier);
      ArrayList<GroundContactPoint> groundContactPoints = groundContactPointGroup.getGroundContactPoints();

      for (GroundContactPoint groundContactPoint : groundContactPoints)
      {
         YoGraphicVector dynamicGraphicVector = new YoGraphicVector(groundContactPoint.getName(), groundContactPoint.getYoPosition(),
               groundContactPoint.getYoForce(), forceVectorScale, appearance);
         yoGraphicsListRegistry.registerYoGraphic("ContactableSelectableBoxRobot", dynamicGraphicVector);
      }
   }

   private void setUpGroundContactPoints(FrameBox3d frameBox)
   {
      String name = this.getName();

      Point3d[] vertices = new Point3d[Box3d.NUM_VERTICES];
      for (int i = 0; i < vertices.length; i++)
      {
         vertices[i] = new Point3d();
      }

      frameBox.getBox3d().computeVertices(vertices);

      for (int i = 0; i < vertices.length; i++)
      {
         Point3d vertex = vertices[i];
         GroundContactPoint groundContactPoint = new GroundContactPoint("gc_" + name + i, new Vector3d(vertex), this.getRobotsYoVariableRegistry());

         floatingJoint.addGroundContactPoint(groundContactPoint);
      }
   }

   @Override
   public FloatingJoint getFloatingJoint()
   {
      return floatingJoint;
   }

   private Link boxLink(Graphics3DObject linkGraphics, double length, double width, double height, double mass)
   {
      Link ret = new Link("box");

      ret.setMass(mass);

      ret.setMomentOfInertia(RotationalInertiaCalculator.getRotationalInertiaMatrixOfSolidBox(length, width, height, mass));
      ret.setComOffset(0.0, 0.0, 0.0);

      //      linkGraphics.translate(0.0, 0.0, -height / 2.0);
      //      linkGraphics.addCube(length, width, height, YoAppearance.EarthTexture(null));
      ret.setLinkGraphics(linkGraphics);

      return ret;
   }

   @Override
   public synchronized boolean isPointOnOrInside(Point3d pointInWorldToCheck)
   {
      return frameBox.getBox3d().isInsideOrOnSurface(pointInWorldToCheck);
   }

   public synchronized void getCurrentBox3d(FrameBox3d frameBoxToPack)
   {
      frameBoxToPack.setAndChangeFrame(frameBox);
   }

   @Override
   public boolean isClose(Point3d pointInWorldToCheck)
   {
      return isPointOnOrInside(pointInWorldToCheck);
   }

   @Override
   public synchronized void closestIntersectionAndNormalAt(Point3d intersectionToPack, Vector3d normalToPack, Point3d pointInWorldToCheck)
   {
      frameBox.getBox3d().checkIfInside(pointInWorldToCheck, intersectionToPack, normalToPack);
   }

   @Override
   public void setMass(double mass)
   {
      boxLink.setMass(mass);
   }

   @Override
   public void setMomentOfInertia(double Ixx, double Iyy, double Izz)
   {
      boxLink.setMomentOfInertia(Ixx, Iyy, Izz);
   }

   @Override
   public void select()
   {
      unSelect(false);
      if (selectedDirection.getEnumValue() == null)
         selectedDirection.set(Direction.Y);
      else
         selectedDirection.set(EnumTools.getNext(selectedDirection.getEnumValue()));

      for (boolean positive : new boolean[] { true, false })
      {
         FaceName faceName = FaceName.get(positive, selectedDirection.getEnumValue());
         faceGraphics.get(faceName).setAppearance(new YoAppearanceRGBColor(selectedColor, selectTransparency));
      }

      notifySelectedListenersThisWasSelected(this);
   }

   @Override
   public void unSelect(boolean reset)
   {
      if (reset)
         selectedDirection.set(null);

      for (FaceName faceName : FaceName.values())
      {
         faceGraphics.get(faceName).setAppearance(new YoAppearanceRGBColor(defaultColor, unselectTransparency));
      }

   }

   @Override
   public void addSelectedListeners(SelectableObjectListener listener)
   {
      selectedListeners.add(listener);
   }

   private void notifySelectedListenersThisWasSelected(Object selectedInformation)
   {
      for (SelectableObjectListener listener : selectedListeners)
      {
         listener.wasSelected(this, selectedInformation);
      }
   }

   private void createBoxGraphics(FrameBox3d frameBox)
   {
      for (FaceName faceName : FaceName.values())
      {
         int nVerticesPerFace = Box3d.NUM_VERTICES_PER_FACE;
         Point3d[] vertices = new Point3d[nVerticesPerFace];
         for (int i = 0; i < vertices.length; i++)
         {
            vertices[i] = new Point3d();
         }

         frameBox.getBox3d().computeVertices(vertices, faceName);
         Graphics3DAddMeshDataInstruction faceGraphic = linkGraphics.addPolygon(vertices, YoAppearance.Red());
         faceGraphics.put(faceName, faceGraphic);
      }
   }

   public Direction getSelectedDirection()
   {
      return selectedDirection.getEnumValue();
   }

   private void createCardboardBoxGraphics(double sizeX, double sizeY, double sizeZ)
   {
      add1x13DObject("models/cardboardBox.obj", sizeX, sizeY, sizeZ);
   }

   private void createWoodBoxGraphics(double sizeX, double sizeY, double sizeZ)
   {
      add1x13DObject("models/woodBox2.obj", sizeX, sizeY, sizeZ);
   }

   // TODO Create some graphics for the 2-by-4 debris
   private void create2By4Graphics(double sizeX, double sizeY, double sizeZ)
   {
      add1x13DObject("models/woodBox2.obj", sizeX, sizeY, sizeZ);
   }

   protected void add1x13DObject(String fileName, double length, double width, double height)
   {
      Graphics3DObject graphics = new Graphics3DObject();
      graphics.translate(0.0, 0.0, -height / 2.0); // TODO: Center the 3ds files so we don't have to do this translate.
      graphics.scale(new Vector3d(length, width, height));
      graphics.addModelFile(fileName);

      linkGraphics.combine(graphics);
   }

   @Override
   public void update()
   {
      super.update();
      updateCurrentBox3d();
   }

   private final RigidBodyTransform temporaryTransform3D = new RigidBodyTransform();

   private synchronized void updateCurrentBox3d()
   {
      floatingJoint.getTransformToWorld(temporaryTransform3D);
      frameBox.setTransform(temporaryTransform3D);
   }

   public double getHalfHeight()
   {
      return frameBox.getBox3d().getHeight() * 0.5;
   }

   @Override
   public void selected(Graphics3DNode graphics3dNode, ModifierKeyInterface modifierKeyHolder, Point3d location, Point3d cameraLocation, Quat4d cameraRotation)
   {
      if (!modifierKeyHolder.isKeyPressed(Key.P))
         return;
      //      System.out.println("Selected box " + this.getName());
      select();
   }
}
