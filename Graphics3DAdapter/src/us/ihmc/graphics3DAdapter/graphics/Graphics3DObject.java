package us.ihmc.graphics3DAdapter.graphics;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.HeightMap;
import us.ihmc.graphics3DAdapter.exceptions.ShapeNotSupportedException;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddExtrusionInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddHeightMapInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddMeshDataInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddModelFileInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DPrimitiveInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.primitives.Graphics3DIdentityInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.primitives.Graphics3DRotateInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.primitives.Graphics3DScaleInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.primitives.Graphics3DTranslateInstruction;
import us.ihmc.graphics3DAdapter.input.SelectedListener;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.robotics.Axis;
import us.ihmc.utilities.inputDevices.keyboard.ModifierKeyInterface;
import us.ihmc.robotics.geometry.InertiaTools;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.Shape3d;
import us.ihmc.robotics.geometry.shapes.Sphere3d;

public class Graphics3DObject
{
   private static final int RESOLUTION = 25;

   private ArrayList<Graphics3DPrimitiveInstruction> graphics3DInstructions;
   private ArrayList<SelectedListener> selectedListeners;
   private MeshDataHolderSharpener sharpener = null;

   private boolean changeable = false;

   public Graphics3DObject(Shape3d shape, AppearanceDefinition appearance)
   {
      this(shape, appearance, null);
   }

   public Graphics3DObject(Shape3d shape)
   {
      this(shape, null, null);
   }

   public Graphics3DObject(ArrayList<Graphics3DPrimitiveInstruction> graphics3DInstructions)
   {
      this(null, null, graphics3DInstructions);
   }

   private Graphics3DObject(Shape3d shape, AppearanceDefinition appearance, ArrayList<Graphics3DPrimitiveInstruction> graphics3DInstructions)
   {
      if (graphics3DInstructions != null)
      {
         this.graphics3DInstructions = graphics3DInstructions;
      }
      else
      {
         this.graphics3DInstructions = new ArrayList<Graphics3DPrimitiveInstruction>();
      }

      if (shape != null)
      {
         if (appearance == null)
         {
            add(shape);
         }
         else
         {
            add(shape, appearance);
         }
      }
   }
   
   /**
    * Default no-arg constructor.  This creates a new empty Graphics3DObject component.
    */
   public Graphics3DObject()
   {
      this(null, null, null);
   }

   public ArrayList<Graphics3DPrimitiveInstruction> getGraphics3DInstructions()
   {
      return graphics3DInstructions;
   }

   public Graphics3DObject(Graphics3DObject graphics3DObject)
   {
      this();
      combine(graphics3DObject);
   }

   /**
    * Merge this with the specified Graphics3DObject.
    *
    * @param graphics3DObject Graphics3DObject to combine with.
    */
   public void combine(Graphics3DObject graphics3DObject)
   {
      this.identity();
      this.graphics3DInstructions.addAll(graphics3DObject.getGraphics3DInstructions());
   }

   private MeshDataHolder sharpenMeshData(MeshDataHolder meshDataToSharpen)
   {
      if (sharpener == null) 
      {
         sharpener = new MeshDataHolderSharpener();
      }
      
//      return meshDataToSharpen;
      return sharpener.sharpenMeshData(meshDataToSharpen);
   }
   
   public void combine(Graphics3DObject Graphics3DObject, Vector3d offset)
   {
      this.identity();
      this.translate(offset);

      ArrayList<Graphics3DPrimitiveInstruction> graphics3dInstructionsToAdd = Graphics3DObject.getGraphics3DInstructions();

      for (Graphics3DPrimitiveInstruction graphics3dInstructionToAdd : graphics3dInstructionsToAdd)
      {
         this.graphics3DInstructions.add(graphics3dInstructionToAdd);

         // Somewhat hackish. Need to translate the offset after each identity instruction. Otherwise not.
         // But this won't do the correct thing for instructions that might do identity in them.
         // We really need a hiarchical, programmatic, tree like way for doing the instructions...
         if (graphics3dInstructionToAdd instanceof Graphics3DIdentityInstruction)
         {
            this.translate(offset);
         }
      }
   }

   public void addInstruction(Graphics3DPrimitiveInstruction instruction)
   {
      this.graphics3DInstructions.add(instruction);
   }

   public void transform(RigidBodyTransform transform)
   {
      Matrix3d rotation = new Matrix3d();
      Vector3d translation = new Vector3d();
      transform.get(rotation, translation);

      translate(translation);
      rotate(rotation);
   }

   /**
    * Translates from the current position by the specified distances.  Graphic
    * components added after translation will appear in the new location.  The coordinate
    * system for these translations is based on those that preceded it.  Each new
    * has its coordinates reset to the parent joint's origin.  {@link #identity Identity}
    * resets back to the joint origin.
    *
    * @param tx distance translated in the x direction
    * @param ty distance translated in the y direction
    * @param tz distance translated in the z direction
    */
   public void translate(double tx, double ty, double tz)
   {
      graphics3DInstructions.add(new Graphics3DTranslateInstruction(tx, ty, tz));
   }

   /**
    * Translates from the current position by the specified distances.  Graphic
    * components added after translation will appear in the new location.  The coordinate
    * system for these translations is based on those that preceded it.  Each new
    * has its coordinates reset to the parent joint's origin.  {@link #identity Identity}
    * resets back to the joint origin.
    *
    * @param translation Tuple3d representing the translation.
    */
   public void translate(Tuple3d translation)
   {
      graphics3DInstructions.add(new Graphics3DTranslateInstruction(translation));
   }

   /**
    * Rotates the coordinate system counter clockwise around the specified axis by the given
    * angle in radians.  This does not rotate existing graphics, instead it rotates a "cursor"
    * when another object is added it will be centered on the origin of the current system
    * as described by the translations and rotations applied since its creation at the joint
    * origin.
    *
    * @param rotationAngle the angle to rotate around the specified axis in radians.
    * @param rotationAxis Axis around which to rotate. Either Link.X, Link.Y or Link.Z
    */

   public void rotate(double rotationAngle, Axis rotationAxis)
   {
      Matrix3d rot = new Matrix3d();

      if (rotationAxis == Axis.X)
         rot.rotX(rotationAngle);
      else if (rotationAxis == Axis.Y)
         rot.rotY(rotationAngle);
      else if (rotationAxis == Axis.Z)
         rot.rotZ(rotationAngle);

      rotate(rot);
   }

   /**
    * Rotates the coordinate system counter clockwise around the specified axis by the given
    * angle in radians.  This does not rotate existing graphics, instead it rotates a "cursor"
    * When another object is added it will be centered on the origin of the current system
    * as described by the translations and rotations applied since its creation at the joint
    * origin.
    *
    * @param rotationAngle the angle to rotate around the specified axis in radians.
    * @param rotationAxis Vector3d describing the axis of rotation.
    */
   public void rotate(double rotationAngle, Vector3d rotationAxis)
   {
      AxisAngle4d rotationAxisAngle = new AxisAngle4d(rotationAxis, rotationAngle);

      rotate(rotationAxisAngle);
   }

   /**
    * Rotates the coordinate system as described by the given rotation matrix.
    * This does not rotate existing graphics, instead it rotates a "cursor".
    * When another object is added it will be centered on the origin of the current system
    * as described by the translations and rotations applied since its creation at the joint
    * origin.
    *
    * @param rotationMatrix Matrix3d describing the rotation to be applied.
    */
   public void rotate(Matrix3d rotationMatrix)
   {
      graphics3DInstructions.add(new Graphics3DRotateInstruction(rotationMatrix));
   }

   public void rotate(AxisAngle4d rotationAxisAngle)
   {
      Matrix3d rotation = new Matrix3d();
      rotation.set(rotationAxisAngle);
      rotate(rotation);
   }

   /**
    * Scales the coordinate system by the specified scale factor. This does not scale existing
    * graphics, instead it scales the "current" coordinate system.  When another object is added
    * it will be uniformly scaled by the specified factor.
    *
    * @param scaleFactor Factor by which the coordinate system is scaled.  For example, 0.5 would
    * reduce future objects size by 50% whereas 2 would double it.
    * @return
    */
   public Graphics3DScaleInstruction scale(double scaleFactor)
   {
      return scale(new Vector3d(scaleFactor, scaleFactor, scaleFactor));
   }

   /**
    * Scales the coordinate system by the specified scale factor. This does not scale existing
    * graphics, instead it scales the "current" coordinate system.  When another object is added
    * it will be uniformly scaled by the specified factor.  The components of the vector indicate
    * scale factors in each dimension.
    *
    * @param scaleFactors Vector3d describing the scaling factors in each dimension.
    * @return
    */
   public Graphics3DScaleInstruction scale(Vector3d scaleFactors)
   {
      Graphics3DScaleInstruction graphics3DScale = new Graphics3DScaleInstruction(scaleFactors);
      graphics3DInstructions.add(graphics3DScale);

      return graphics3DScale;
   }

   /**
    * Resets the coordinate system to the joint origin.  This clears all rotations, translations,
    * and scale factors.
    */
   public void identity()
   {
      graphics3DInstructions.add(new Graphics3DIdentityInstruction());
   }

   /**
    * Adds the specified 3DS Max file to the center of the current coordinate system
    * with a default appearance.  3DS Max is a 3D modeling program that allows the creation
    * of detailed models and animations.  This function
    * only imports the model allowing the use
    * of more complicated and detailed system representations in simulations.
    *
    * @param fileURL URL pointing to the desired 3ds file.
    */
   public void addModelFile(URL fileURL)
   {
      addModelFile(fileURL, null);
   }

   /**
    * Adds the specified 3DS Max file to the center of the current coordinate system
    * with the given appearance.  3DS Max is a 3D modeling program that allows the creation
    * of detailed models and animations.  This function only imports the model allowing the use
    * of more complicated and detailed system representations in simulations.
    *
    * @param fileURL URL pointing to the desired 3ds file.
    * @param yoAppearanceDefinition Appearance to use with the 3ds model once imported.
    */
   public void addModelFile(URL fileURL, AppearanceDefinition yoAppearanceDefinition)
   {
      if (fileURL == null)
      {
         System.err.println("fileURL == null in addModelFile");

         return;
      }

      String fileName = fileURL.getFile();

      // System.out.println("File name: " + fileName + " " + fileName.length());

      if ((fileName == null) || (fileName.equals("")))
      {
         System.out.println("Null File Name in add3DSFile");

         return;
      }

      addModelFile(fileName, yoAppearanceDefinition);
   }

   /**
    * Adds the specified model file to the center of the current coordinate system
    * with a default appearance.  Supported model files are .stl, .obj and .dae
    *
    * Appearances of model files can only be changed at runtime if an appearance is given while loading the model
    *
    * @param fileName File path of the desired 3ds file.
    * @return void
    */
   public void addModelFile(String fileName)
   {
      addModelFile(fileName, null);
   }

   /**
    * Adds the specified model file to the center of the current coordinate system
    * with the given appearance.    Supported model files are .stl, .obj and .dae.
    *
    * Appearances of model files can only be changed at runtime if an appearance is given while loading the model
    *
    * @param fileName File path to the desired 3ds file.
    * @param app Appearance to use with the model once imported.
    */

   /**
    * @param fileName
    * @param app
    * @return
    */
   public Graphics3DAddModelFileInstruction addModelFile(String fileName, AppearanceDefinition app)
   {
      Graphics3DAddModelFileInstruction graphics3dAddModelFileInstruction = new Graphics3DAddModelFileInstruction(fileName, app);
      graphics3DInstructions.add(graphics3dAddModelFileInstruction);

      return graphics3dAddModelFileInstruction;
   }

   public Graphics3DAddModelFileInstruction addModelFile(String fileName, String submesh, boolean centerSubmesh, List<String> resourceDirectories,
           AppearanceDefinition app)
   {
      Graphics3DAddModelFileInstruction graphics3dAddModelFileInstruction = new Graphics3DAddModelFileInstruction(fileName, submesh, centerSubmesh, app,
                                                                               resourceDirectories);
      graphics3DInstructions.add(graphics3dAddModelFileInstruction);

      return graphics3dAddModelFileInstruction;
   }

   public Graphics3DAddModelFileInstruction addModelFile(String fileName, ArrayList<String> resourceDirectories, AppearanceDefinition app)
   {
      return addModelFile(fileName, null, false, resourceDirectories, app);
   }

   public void addCoordinateSystem(double length)
   {
      addCoordinateSystem(length, YoAppearance.Gray());
   }

   public void addCoordinateSystem(double length, AppearanceDefinition arrowAppearance)
   {
      addCoordinateSystem(length, YoAppearance.Red(), YoAppearance.White(), YoAppearance.Blue(), arrowAppearance);
   }

   /**
    * Creates a graphical representation of the x, y, and z axis of the current coordinate
    * system centered at its origin.  In the image below red, white and blue represent the
    * x, y and z axies respectively.<br /><br />
    * <img src="doc-files/LinkGraphics.addCoordinateSystem.jpg">
    *
    * @param length the length in meters of each axis arrow.
    */
   public void addCoordinateSystem(double length, AppearanceDefinition xAxisAppearance, AppearanceDefinition yAxisAppearance,
                                   AppearanceDefinition zAxisAppearance, AppearanceDefinition arrowAppearance)
   {
      rotate(Math.PI / 2.0, Axis.Y);
      addArrow(length, YoAppearance.Red(), arrowAppearance);
      rotate(-Math.PI / 2.0, Axis.Y);
      rotate(-Math.PI / 2.0, Axis.X);
      addArrow(length, YoAppearance.White(), arrowAppearance);
      rotate(Math.PI / 2.0, Axis.X);
      addArrow(length, YoAppearance.Blue(), arrowAppearance);
   }

   public Graphics3DAddMeshDataInstruction add(Shape3d shape)
   {
      return add(shape, YoAppearance.Black());
   }

   public Graphics3DAddMeshDataInstruction add(Shape3d shape, AppearanceDefinition app)
   {
      if (shape instanceof Sphere3d)
      {
         return addSphere(((Sphere3d) shape).getRadius(), app);
      }
      else
      {
         try
         {
            throw new ShapeNotSupportedException(shape);
         }
         catch (ShapeNotSupportedException e)
         {
            e.printStackTrace();
         }

         return null;
      }
   }

   public void addArrow(double length, AppearanceDefinition baseAppearance, AppearanceDefinition headAppearance)
   {
      double coneHeight = 0.1 * length;
      double cylinderHeight = length - coneHeight;
      double radius = 0.02 * length;
      double coneRadius = 2.0 * radius;

      addCylinder(cylinderHeight, radius, baseAppearance);
      translate(0.0, 0.0, cylinderHeight);
      addCone(coneHeight, coneRadius, headAppearance);
      translate(0.0, 0.0, -cylinderHeight);
   }

   /**
    * Adds a solid black cube with the specified dimensions centered around the origin of the
    * current coordinate system.  All lengths are in meters.</ br></ br>
    * The image below demonstrates a 0.25 x 0.25 x 0.25 cube generated by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addCube(0.25, 0.25, 0.25);}<br /><br />
    *
    * As is show by the graphical representation the cube is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addCube1.jpg">
    *
    * @param lx length of the cube in the x direction.
    * @param ly length of the cube in the y direction.
    * @param lz length of the cube in the z direction.
    */
   public Graphics3DAddMeshDataInstruction addCube(double lx, double ly, double lz)
   {
      return addCube(lx, ly, lz, YoAppearance.Black());
   }

   /**
    * Adds a solid cube with the given dimensions and appearance centered on the origin of the
    * current coordinate system.  All lengths are in meters.</ br></ br>
    * The image below demonstrates a yellow 0.1 x 0.35 x 0.2 cube generated by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addCube(0.1, 0.35, 0.2, YoAppearance.Yellow());}<br /><br />
    *
    * As is show by the graphical representation the cube is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addCube2.jpg">
    *
    *
    * @param lx length of the cube in the x direction.
    * @param ly length of the cube in the y direction.
    * @param lz length of the cube in the z direction.
    * @param cubeApp Appearance of the cube.  See {@link YoAppearance YoAppearance} for implementations.
    * @return
    */
   public Graphics3DAddMeshDataInstruction addCube(double lx, double ly, double lz, AppearanceDefinition cubeApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.Cube(lx, ly, lz, false);
      boolean sharpenMeshData = true;
      return addMeshData(meshData, sharpenMeshData, cubeApp);
   }
   
   public Graphics3DAddMeshDataInstruction addCube(double lx, double ly, double lz, boolean centered, AppearanceDefinition cubeApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.Cube(lx, ly, lz, centered);

      return addMeshData(meshData, cubeApp);
   }

   /**
    * Adds a solid wedge with the given dimensions centered on the origin of the current
    * coordinate system.  The peak of the wedge is directly above the far edge of the cube
    * in the x direction.</ br></ br>
    * The image below demonstrates a 0.25 x 0.25 x 0.25 wedge generated by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addWedge(0.25, 0.25, 0.25);}<br /><br />
    *
    * As is show by the graphical representation the wedge is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addWedge1.jpg">
    *
    * @param lx length of the wedge in the x direction.
    * @param ly length of the wedge in the y direction.
    * @param lz length of the wedge in the z direction.
    */
   public Graphics3DAddMeshDataInstruction addWedge(double lx, double ly, double lz)
   {
      return addWedge(lx, ly, lz, YoAppearance.Black());
   }

   /**
    *
    * Adds a solid wedge with the given dimensions and appearance centered on the origin of the current
    * coordinate system.  The peak of the wedge is directly above the far edge of the cube
    * in the x direction.</ br></ br>
    * The image below demonstrates a green 0.35 x 0.3 x 0.1 wedge generated by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addWedge(0.35, 0.3, 0.1, YoAppearance.GREEN());}<br /><br />
    *
    * As is show by the graphical representation the wedge is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addWedge2.jpg">
    *
    * @param lx length of the wedge in the x direction.
    * @param ly length of the wedge in the y direction.
    * @param lz length of the wedge in the z direction.
    * @param wedgeApp Appearance of the wedge.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addWedge(double lx, double ly, double lz, AppearanceDefinition wedgeApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.Wedge(lx, ly, lz);
      boolean sharpenMeshData = true;
      return addMeshData(meshData, sharpenMeshData, wedgeApp);
   }

   /**
    * Adds a solid sphere with the given radius centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a sphere with a 0.25 meter radius generated by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addSphere(0.25);}<br /><br />
    *
    * As is show by the graphical representation the sphere is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addSphere1.jpg">
    *
    * @param radius radius of the new sphere in meters.
    */
   public Graphics3DAddMeshDataInstruction addSphere(double radius)
   {
      return addSphere(radius, YoAppearance.Black());
   }

   /**
    * Adds a solid sphere with the given radius and appearance centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a blue sphere with a 0.15 meter radius generated by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addSphere(0.15, YoAppearance.Blue());}<br /><br />
    *
    * As is show by the graphical representation the sphere is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addSphere2.jpg">
    *
    * @param radius radius of the new sphere in meters.
    * @param sphereApp Appearance to be used with the new sphere.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addSphere(double radius, AppearanceDefinition sphereApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.Sphere(radius, RESOLUTION, RESOLUTION);

      return addMeshData(meshData, sphereApp);
   }

   public Graphics3DAddMeshDataInstruction addMeshData(MeshDataHolder meshData, AppearanceDefinition meshAppearance)
   {
      return addMeshData(meshData, false, meshAppearance);
   }
   
   public Graphics3DAddMeshDataInstruction addMeshData(MeshDataHolder meshData, boolean sharpenMeshData, AppearanceDefinition meshAppearance)
   {
      if (sharpenMeshData)
      {
         meshData = sharpenMeshData(meshData);
      }
      
      Graphics3DAddMeshDataInstruction instruction = new Graphics3DAddMeshDataInstruction(meshData, meshAppearance);
      graphics3DInstructions.add(instruction);

      return instruction;
   }

   /**
    * Adds a solid ellipsoid with the given radii centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates an ellipsoid with radii of 0.3, 0.2 and 0.1 in the x, y and z directions respectively:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addEllipsoid(0.3, 0.2, 0.1);}<br /><br />
    *
    * As is show by the graphical representation the ellipsoid is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addEllipsoid1.jpg">
    *
    * @param xRadius x direction radius in meters
    * @param yRadius y direction radius in meters
    * @param zRadius z direction radius in meters
    */
   public Graphics3DAddMeshDataInstruction addEllipsoid(double xRadius, double yRadius, double zRadius)
   {
      return addEllipsoid(xRadius, yRadius, zRadius, YoAppearance.Black());
   }

   /**
    * Adds a solid ellipsoid with the given radii and appearance centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a red ellipsoid with radii of 0.2, 0.2 and 0.1 in the x, y and z directions respectively:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addEllipsoid(0.2, 0.2, 0.1, YoAppearance.Red());}<br /><br />
    *
    * As is show by the graphical representation the ellipsoid is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addEllipsoid2.jpg">
    *
    * @param xRadius x direction radius in meters
    * @param yRadius y direction radius in meters
    * @param zRadius z direction radius in meters
    * @param ellipsoidApp Appearance to be used with the new ellipsoid.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addEllipsoid(double xRadius, double yRadius, double zRadius, AppearanceDefinition ellipsoidApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.Ellipsoid(xRadius, yRadius, zRadius, RESOLUTION, RESOLUTION);

      return addMeshData(meshData, ellipsoidApp);
   }

   /**
    * Adds a soild cylinder with the given radius and height centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a cylinder with radius of 0.2 and a height of 0.4 as described by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addCylinder(0.4, 0.2);}<br /><br />
    *
    * As is show by the graphical representation the cylinder is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addCylinder1.jpg">
    *
    * @param height cylinder height in meters.
    * @param radius cylinder radius in meters.
    */
   public Graphics3DAddMeshDataInstruction addCylinder(double height, double radius)
   {
      return addCylinder(height, radius, YoAppearance.Black());
   }

   /**
    * Adds a soild cylinder with the given radius, height and appearance centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a maroon cylinder with radius of 0.3 and a height of 0.1 as described by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addCylinder(0.1, 0.3, YoAppearance.Maroon());}<br /><br />
    *
    * As is show by the graphical representation the cylinder is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addCylinder2.jpg">
    *
    * @param height cylinder height in meters.
    * @param radius cylinder radius in meters.
    * @param cylApp Appearance to be used with the new cylinder.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addCylinder(double height, double radius, AppearanceDefinition cylApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.Cylinder(radius, height, RESOLUTION);

      return addMeshData(meshData, cylApp);
   }

   /**
    * Adds a cone with the given height and radius centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a cone with radius of 0.15 and a height of 0.35 as described by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addCone(0.35, 0.15);}<br /><br />
    *
    * As is show by the graphical representation the cone is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addCone1.jpg">
    *
    * @param height cone height in meters.
    * @param radius cone radius in meters.
    */
   public Graphics3DAddMeshDataInstruction addCone(double height, double radius)
   {
      return addCone(height, radius, YoAppearance.Black());
   }

   /**
    * Adds a cone with the given height, radius and appearance centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a dark green cone with radius of 0.4 and a height of 0.2 as described by the following code:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addCone(0.2, 0.4, YoAppearance.DarkGreen());}<br /><br />
    *
    * As is show by the graphical representation the cone is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addCone2.jpg">
    *
    * @param height cone height in meters.
    * @param radius cone radius in meters.
    * @param coneApp Appearance to be used with the new cone.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addCone(double height, double radius, AppearanceDefinition coneApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.Cone(height, radius, RESOLUTION);

      return addMeshData(meshData, coneApp);
   }

   /**
    * Adds a truncated cone with the given height, base width x, base width y, top width x, and top width y centered
    * on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a truncated cone with a height of 0.3, a x base width of 0.25, a y base width of 0.2,
    * a x top width of 0.15, and a y top width of 0.1:<br /><br />
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addGenTruncatedCone(0.3, 0.25, 0.2, 0.15, 0.1);}<br /><br />
    *
    * As is show by the graphical representation the truncated cone is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addGenTruncatedCone1.jpg">
    *
    * @param height in meters
    * @param bx x direction width of the base in meters
    * @param by y direction width of the base in meters
    * @param tx x direction width of the top in meters
    * @param ty y direction width of the top in meters
    */
   public Graphics3DAddMeshDataInstruction addGenTruncatedCone(double height, double bx, double by, double tx, double ty)
   {
      return addGenTruncatedCone(height, bx, by, tx, ty, YoAppearance.Black());
   }

   /**
    * Adds a truncated cone with the given height, base width x, base width y, top width x,
    * top width y, and appearance centered on the origin of the current coordinate system.
    * </ br></ br>
    * The image below demonstrates a navy blue truncated cone with a height of 0.25, a x
    * base width of 0.15, a y base width of 0.15, a x top width of 0.05, and a y top width
    * of 0.1:<br /><br />
    *
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addGenTruncatedCone(0.25, 0.15, 0.15, 0.05, 0.1, YoAppearance.Navy());}<br /><br />
    *
    * As is show by the graphical representation the truncated cone is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addGenTruncatedCone2.jpg">
    *
    * @param height in meters
    * @param bx x direction width of the base in meters
    * @param by y direction width of the base in meters
    * @param tx x direction width of the top in meters
    * @param ty y direction width of the top in meters
    * @param coneApp Appearance to be used with the new truncated cone.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addGenTruncatedCone(double height, double bx, double by, double tx, double ty, AppearanceDefinition coneApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.GenTruncatedCone(height, bx, by, tx, ty, RESOLUTION);

      return addMeshData(meshData, coneApp);
   }

   /**
    * Adds a hemi ellipsoid with the given x, y and z radii centered on the current coordinate system.  Hemi ellipsoids
    * are essentially cut in half, in this case the missing half is below the xy plane.
    * </ br></ br>
    * The image below demonstrates a hemi ellipsoid with x, y and z radii of 0.25, 0.15 and 0.35 respectively:<br /><br />
    *
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addHemiEllipsoid(0.25, 0.15, 0.35);}<br /><br />
    *
    * As is show by the graphical representation the hemi ellipsoid is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addHemiEllipsoid1.jpg">
    *
    * @param xRad radius of the ellipsoid in the x direction.
    * @param yRad radius of the ellipsoid in the y direction.
    * @param zRad radius of the ellipsoid in the z direction.
    */
   public Graphics3DAddMeshDataInstruction addHemiEllipsoid(double xRad, double yRad, double zRad)
   {
      return addHemiEllipsoid(xRad, yRad, zRad, YoAppearance.Black());
   }

   /**
    * Adds a hemi ellipsoid with the given appearance and x, y and z radii centered on the current coordinate system.  Hemi ellipsoids
    * are essentially cut in half, in this case the missing half is below the xy plane.
    * </ br></ br>
    * The image below demonstrates a dark red hemi ellipsoid with x, y and z radii of 0.15, 0.2 and 0.4 respectively:<br /><br />
    *
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addHemiEllipsoid(0.15, 0.2, 0.4, YoAppearance.DarkRed());}<br /><br />
    *
    * As is show by the graphical representation the hemi ellipsoid is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addHemiEllipsoid2.jpg">
    *
    * @param xRad radius of the ellipsoid in the x direction.
    * @param yRad radius of the ellipsoid in the y direction.
    * @param zRad radius of the ellipsoid in the z direction.
    * @param hEApp Appearance to be used with the new hemi ellipsoid.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addHemiEllipsoid(double xRad, double yRad, double zRad, AppearanceDefinition hEApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.HemiEllipsoid(xRad, yRad, zRad, RESOLUTION, RESOLUTION);

      return addMeshData(meshData, hEApp);
   }

   /**
    * Creates an ArcTorus centered on the current coordinate system.  An ArcTorus is a toroid shape beginning at the start
    * angle and wrapping around to the end angle.  Ensure that the angles don't overlap as the shape will appear inverted; the
    * start angle must be smaller than the end angle.  All angles are measured in radians.  The shape is also defined by its
    * major and minor radii.
    * </ br></ br>
    * The image below demonstrates a arctorus beginning at 3/2 pi and ending at 5/2 pi with major and minor radii of
    * 0.25 and 0.15 respectively:<br /><br />
    *
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addArcTorus(3*Math.PI/2, 5*Math.PI/2, 0.25, 0.15);}<br /><br />
    *
    * As is show by the graphical representation the arctorus is centered on the coordinate system.  Note that the ends
    * of the torus have no texture or surface, they are transparent.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addArcTorus1.jpg">
    *
    * @param startAngle Angle in radians at which the torus begins.
    * @param endAngle Angle in radians at which the torus ends
    * @param majorRadius Distance from the origin to the center of the torus
    * @param minorRadius Distance from the center of the torus to the walls on either side.
    */
   public Graphics3DAddMeshDataInstruction addArcTorus(double startAngle, double endAngle, double majorRadius, double minorRadius)
   {
      return addArcTorus(startAngle, endAngle, majorRadius, minorRadius, YoAppearance.Black());
   }

   /**
    * Creates an ArcTorus centered on the current coordinate system with the specified appearance.  An ArcTorus is a toroid
    * shape beginning at the start angle and wrapping around to the end angle.  Ensure that the angles don't overlap as the
    * shape will appear inverted; the start angle must be smaller than the end angle.  All angles are measured in radians.
    * The shape is also defined by its major and minor radii.  The minor radius is from the origin to the inner wall of the
    * torus while the major is from the origin to the outer wall.
    * </ br></ br>
    * The image below demonstrates an aqua arctorus beginning at 0 and ending at 3/2 pi with major and minor radii of
    * 0.30 and 0.05 respectively:<br /><br />
    *
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addArcTorus(0, 3*Math.PI/2, 0.30, 0.05);}<br /><br />
    *
    * As is show by the graphical representation the arctorus is centered on the coordinate system.  Note that the ends
    * of the torus have no texture or surface, they are transparent.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addArcTorus2.jpg">
    *
    * @param startAngle Angle in radians at which the torus begins.
    * @param endAngle Angle in radians at which the torus ends.
    * @param majorRadius Distance from the origin to the center of the torus.
    * @param minorRadius Distance from the center of the torus to the walls on either side.
    * @param arcTorusApp Appearance to be used with the new arctorus.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addArcTorus(double startAngle, double endAngle, double majorRadius, double minorRadius,
           AppearanceDefinition arcTorusApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.ArcTorus(startAngle, endAngle, majorRadius, minorRadius, RESOLUTION);

      return addMeshData(meshData, arcTorusApp);
   }

   /**
    * Creates a pyramid cube centered on the origin of the current coordinate system.  A pyramid cube is nothing more than
    * a standard cube of the given length, width and height with a square base pyramid of the specified height on the top and
    * bottom.
    * </ br></ br>
    * The image below demonstrates a pyramid cube beginning with dimensions of 0.2 by 0.2 by 0.2 and a pyramid height of 0.2:<br /><br />
    *
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addPyramidCube(0.2, 0.2, 0.2, 0.2);}<br /><br />
    *
    * As is show by the graphical representation the pyramid cube is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addPyramidCube1.jpg">
    *
    * @param lx Length in meters of the cube. (x direction)
    * @param ly Width in meters of the cube. (y direction)
    * @param lz Height of the cube in meters. (z direction)
    * @param lh Height of the pyramids in meters.
    */
   public Graphics3DAddMeshDataInstruction addPyramidCube(double lx, double ly, double lz, double lh)
   {
      return addPyramidCube(lx, ly, lz, lh, YoAppearance.Black());
   }

   /**
    * Creates a pyramid cube with the specified appearance centered on the origin of the current coordinate system.
    * A pyramid cube is nothing more than a standard cube of the given length, width and height with a square base
    * pyramid of the specified height on the top and bottom.
    * </ br></ br>
    * The image below demonstrates a alluminum pyramid cube beginning with dimensions of 0.4 by 0.2 by 0.1 and a pyramid height of 0.3:<br /><br />
    *
    * {@code linkGraphics.addCoordinateSystem(0.5);}<br />
    * {@code linkGraphics.addPyramidCube(0.2, 0.2, 0.2, 0.2);}<br /><br />
    *
    * As is show by the graphical representation the pyramid cube is centered on the coordinate system.
    * Again, x, y and z are red, white and blue.
    * <br /><br /><img src="doc-files/LinkGraphics.addPyramidCube2.jpg">
    *
    * @param lx Length in meters of the cube. (x direction)
    * @param ly Width in meters of the cube. (y direction)O
    * @param lz Height of the cube in meters. (z direction)
    * @param lh Height of the pyramids in meters.
    * @param cubeApp Appearance to be used with the new pyramid cube.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addPyramidCube(double lx, double ly, double lz, double lh, AppearanceDefinition cubeApp)
   {
      MeshDataHolder meshData = MeshDataGenerator.PyramidCube(lx, ly, lz, lh);
      boolean sharpenMeshData = true;
      return addMeshData(meshData, sharpenMeshData, cubeApp);
   }

   public Graphics3DAddMeshDataInstruction addPolygon(ArrayList<Point3d> polygonPoints)
   {
      return addPolygon(polygonPoints, YoAppearance.Black());
   }

   /**
    * Creates a polygon centered at the current coordinate system with the given vertices.
    * The points this shape is composed of must be coplanar and the order matters.  Randomly
    * inserting points will produce unpredictable results, clockwise direction determines the
    * side that is drawn.
    *
    * @param polygonPoints ArrayList containing the points.
    * @param yoAppearance Appearance to be used with the new polygon.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addPolygon(ArrayList<Point3d> polygonPoints, AppearanceDefinition yoAppearance)
   {
      MeshDataHolder meshData = MeshDataGenerator.Polygon(polygonPoints);

      return addMeshData(meshData, yoAppearance);
   }

   /**
    * Creates a polygon centered at the current coordinate system with the given vertices.
    * The points this shape is composed of must be coplanar and the order matters.  Randomly
    * inserting points will produce unpredictable results, clockwise direction determines the
    * side that is drawn.
    * @param convexPolygon2d ConvexPolygon2d containing the points.
    * @param yoAppearance Appearance to be used with the new polygon.  See {@link YoAppearance YoAppearance} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addPolygon(ConvexPolygon2d convexPolygon2d, AppearanceDefinition yoAppearance)
   {
      MeshDataHolder meshData = MeshDataGenerator.Polygon(convexPolygon2d);

      return addMeshData(meshData, yoAppearance);
   }

   public Graphics3DAddMeshDataInstruction addPolygon(ConvexPolygon2d convexPolygon2d)
   {
      MeshDataHolder meshData = MeshDataGenerator.Polygon(convexPolygon2d);

      return addMeshData(meshData, YoAppearance.Black());
   }

   /**
    * Creates a polygon centered at the current coordinate system with the given vertices.
    * The points this shape is composed of must be coplanar and in a logical order.  Randomly
    * inserting points will produce unpredictable results, clockwise direction determines the
    * side that is drawn.
    *
    * @param polygonPoint Array containing Point3d's to be used when generating the shape.
    */
   public Graphics3DAddMeshDataInstruction addPolygon(Point3d[] polygonPoint)
   {
      return addPolygon(polygonPoint, YoAppearance.Black());
   }

   /**
    * Creates a polygon centered at the current coordinate system with the given vertices.
    * The points this shape is composed of must be coplanar and the order matters.  Randomly
    * inserting points will produce unpredictable results, clockwise direction determines the
    * side that is drawn.
    *
    * @param polygonPoints Array containing the points
    * @param yoAppearance Appearance to be used with the new polygon.  See {@link AppearanceDefinition} for implementations.
    */
   public Graphics3DAddMeshDataInstruction addPolygon(Point3d[] polygonPoints, AppearanceDefinition yoAppearance)
   {
      MeshDataHolder meshData = MeshDataGenerator.Polygon(polygonPoints);

      return addMeshData(meshData, yoAppearance);
   }

   public Graphics3DAddMeshDataInstruction addPolygon(AppearanceDefinition yoAppearance, Point3d... polygonPoints)
   {
      return addPolygon(polygonPoints, yoAppearance);
   }


   public Graphics3DAddMeshDataInstruction addExtrudedPolygon(ConvexPolygon2d convexPolygon2d, double height)
   {
      return addExtrudedPolygon(convexPolygon2d, height, YoAppearance.Black());
   }

   public Graphics3DAddMeshDataInstruction addExtrudedPolygon(ConvexPolygon2d convexPolygon2d, double height, AppearanceDefinition appearance)
   {
      MeshDataHolder meshData = MeshDataGenerator.ExtrudedPolygon(convexPolygon2d, height);
      return addMeshData(meshData, true, appearance);
   }

   public Graphics3DAddMeshDataInstruction addExtrudedPolygon(List<Point2d> polygonPoints, double height)
   {
      return addExtrudedPolygon(polygonPoints, height, YoAppearance.Black());
   }

   public Graphics3DAddMeshDataInstruction addExtrudedPolygon(List<Point2d> polygonPoints, double height, AppearanceDefinition appearance)
   {
      MeshDataHolder meshData = MeshDataGenerator.ExtrudedPolygon(polygonPoints, height);
      return addMeshData(meshData, true, appearance);
   }

   /**
    * Create an extrusion of a BufferedImage. Black pixels of the image are extruded.
    * A pixel is considered black when (red+green+blue)/3 < 60
    *
    * @param bufferedImageToExtrude    BufferedImage to extrude
    * @param thickness Thinkness of extrusion
    * @param appearance Appearance
    */
   public Graphics3DAddExtrusionInstruction addExtrusion(BufferedImage bufferedImageToExtrude, double thickness, AppearanceDefinition appearance)
   {
      Graphics3DAddExtrusionInstruction instruction = new Graphics3DAddExtrusionInstruction(bufferedImageToExtrude, thickness, appearance);
      graphics3DInstructions.add(instruction);

      return instruction;
   }

   public Graphics3DAddExtrusionInstruction addText(String text, double thickness, AppearanceDefinition yoAppearance)
   {
      Graphics3DAddExtrusionInstruction instruction = new Graphics3DAddExtrusionInstruction(text, thickness, yoAppearance);
      graphics3DInstructions.add(instruction);

      return instruction;
   }

   public void createInertiaEllipsoid(Matrix3d momentOfInertia, Vector3d comOffset, double mass, AppearanceDefinition appearance)
   {
      Vector3d principalMomentsOfInertia = new Vector3d(momentOfInertia.m00, momentOfInertia.m11, momentOfInertia.m22);
      Vector3d ellipsoidRadii = InertiaTools.getInertiaEllipsoidRadii(principalMomentsOfInertia, mass);

      this.translate(comOffset);
      this.addEllipsoid(ellipsoidRadii.x, ellipsoidRadii.y, ellipsoidRadii.z, appearance);
      this.identity();
   }


   public Graphics3DInstruction addTeaPot(AppearanceDefinition appearance)
   {
      return addModelFile("models/Teapot.obj", appearance);
   }

   public Graphics3DInstruction addHeightMap(HeightMap heightMap, int xPointsPerSide, int yPointsPerSide, AppearanceDefinition appearance)
   {
      Graphics3DAddHeightMapInstruction instruction = new Graphics3DAddHeightMapInstruction(heightMap, xPointsPerSide, yPointsPerSide, appearance);
      graphics3DInstructions.add(instruction);

      return instruction;
   }

   public void notifySelectedListeners(Graphics3DNode graphics3dNode, ModifierKeyInterface modifierKeyHolder, Point3d location, Point3d cameraPosition,
           Quat4d cameraRotation)
   {
      if (selectedListeners != null)
      {
         for (SelectedListener selectedListener : selectedListeners)
         {
            selectedListener.selected(graphics3dNode, modifierKeyHolder, location, cameraPosition, cameraRotation);
         }
      }
   }

   public void registerSelectedListener(SelectedListener selectedListener)
   {
      if (selectedListeners == null)
      {
         selectedListeners = new ArrayList<SelectedListener>();
      }
      
      selectedListeners.add(selectedListener);
   }

   public boolean isChangeable()
   {
      return changeable;
   }

   public void setChangeable(boolean changeable)
   {
      this.changeable = changeable;
   }
}
