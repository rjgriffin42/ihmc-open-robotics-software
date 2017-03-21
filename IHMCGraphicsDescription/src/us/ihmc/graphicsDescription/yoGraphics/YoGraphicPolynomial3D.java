package us.ihmc.graphicsDescription.yoGraphics;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.euclid.transform.AffineTransform;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.PointCloud3DMeshGenerator;
import us.ihmc.graphicsDescription.SegmentedLine3DMeshDataGenerator;
import us.ihmc.graphicsDescription.appearance.AppearanceDefinition;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.instructions.Graphics3DAddMeshDataInstruction;
import us.ihmc.graphicsDescription.plotting.artifact.Artifact;
import us.ihmc.robotics.dataStructures.listener.VariableChangedListener;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.dataStructures.variable.YoVariable;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoseUsingQuaternions;
import us.ihmc.robotics.math.frames.YoFrameQuaternion;
import us.ihmc.robotics.math.trajectories.YoPolynomial;
import us.ihmc.robotics.math.trajectories.YoPolynomial3D;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.gui.GraphicsUpdatable;

public class YoGraphicPolynomial3D extends YoGraphic implements RemoteYoGraphic, GraphicsUpdatable
{
   private final static AppearanceDefinition BLACK_APPEARANCE = YoAppearance.Black();

   public enum TrajectoryGraphicType
   {
      HIDE, SHOW_AS_LINE, SHOW_AS_POINTS;
      public static TrajectoryGraphicType[] values = values();
   };

   public enum TrajectoryColorType
   {
      BLACK, VELOCITY_BASED, ACCELERATION_BASED;
      public static TrajectoryColorType[] values = values();
   };

   private final YoGraphicJob yoGraphicJob;

   private final double radius;
   private final int resolution;
   private final int radialResolution;

   private final Graphics3DObject graphics3dObject = new Graphics3DObject();
   private final AppearanceDefinition[] colorPalette = createColorPalette(128);
   private final SegmentedLine3DMeshDataGenerator segmentedLine3DMeshGenerator;
   private final PointCloud3DMeshGenerator pointCloud3DMeshGenerator;
   private final Graphics3DAddMeshDataInstruction[] graphics3DAddMeshDataInstructions;
   private final Point3D[] intermediatePositions;
   private final Vector3D[] intermediateVelocities;
   private final Vector3D[] intermediateAccelerations;

   private final boolean hasPoseDefined;
   private final YoFramePoseUsingQuaternions poseToPolynomialFrame;

   private final int numberOfPolynomials;
   private final YoPolynomial3D[] yoPolynomial3Ds;
   /**
    * This array is used to store the total number of YoVariables used for each
    * {@code YoPolynomial}. It has a length equal to {@code 3 * yoPolynomial3Ds.length}, and the
    * information is stored as follows:
    * <p>
    * {@code yoPolynomialSizes[3 * i + 0] = yoPolynomial3Ds[i].getYoPolynomialX().getMaximumNumberOfCoefficients() + 1;}
    * <br>
    * {@code yoPolynomialSizes[3 * i + 1] = yoPolynomial3Ds[i].getYoPolynomialY().getMaximumNumberOfCoefficients() + 1;}
    * <br>
    * {@code yoPolynomialSizes[3 * i + 2] = yoPolynomial3Ds[i].getYoPolynomialZ().getMaximumNumberOfCoefficients() + 1;}
    * </p>
    */
   private final int[] yoPolynomialSizes;
   private final DoubleYoVariable[] waypointTimes;

   /** Notification for this YoGraphic of what task should be fulfilled see {@link CurrentTask}. */
   private final EnumYoVariable<?> currentGraphicType;
   private final EnumYoVariable<?> currentColorType;
   /**
    * When this is created as a {@link RemoteYoGraphic}, it is consider as a READER and thus turns
    * on this flag to let the WRITER know that it has to synchronize.
    */
   private final BooleanYoVariable readExists;

   private final AtomicBoolean dirtyGraphic = new AtomicBoolean(false);

   public YoGraphicPolynomial3D(String name, List<YoPolynomial3D> yoPolynomial3Ds, List<DoubleYoVariable> waypointTimes, double radius, int resolution,
                                int radialResolution, YoVariableRegistry registry)
   {
      this(name, null, yoPolynomial3Ds, waypointTimes, radius, resolution, radialResolution, registry);
   }

   public YoGraphicPolynomial3D(String name, YoPolynomial3D[] yoPolynomial3Ds, DoubleYoVariable[] waypointTimes, double radius, int resolution,
                                int radialResolution, YoVariableRegistry registry)
   {
      this(name, null, yoPolynomial3Ds, waypointTimes, radius, resolution, radialResolution, registry);
   }

   public YoGraphicPolynomial3D(String name, YoFramePoseUsingQuaternions poseToPolynomialFrame, List<YoPolynomial3D> yoPolynomial3Ds,
                                List<DoubleYoVariable> waypointTimes, double radius, int resolution, int radialResolution, YoVariableRegistry registry)
   {
      this(name, poseToPolynomialFrame, yoPolynomial3Ds.toArray(new YoPolynomial3D[0]), toArray(waypointTimes), radius, resolution, radialResolution, registry);
   }

   public YoGraphicPolynomial3D(String name, YoFramePoseUsingQuaternions poseToPolynomialFrame, YoPolynomial3D[] yoPolynomial3Ds,
                                DoubleYoVariable[] waypointTimes, double radius, int resolution, int radialResolution, YoVariableRegistry registry)
   {
      super(name);

      yoGraphicJob = YoGraphicJob.WRITER;

      if (yoPolynomial3Ds.length != waypointTimes.length)
         throw new RuntimeException("Inconsistent number of YoPolynomial3Ds ( " + yoPolynomial3Ds.length + " ) and waypoint times ( " + waypointTimes.length
               + " ).");

      this.radius = radius;
      this.resolution = resolution;
      this.radialResolution = radialResolution;
      this.yoPolynomial3Ds = yoPolynomial3Ds;
      this.waypointTimes = waypointTimes;

      hasPoseDefined = poseToPolynomialFrame != null;
      this.poseToPolynomialFrame = poseToPolynomialFrame;

      numberOfPolynomials = yoPolynomial3Ds.length;

      yoPolynomialSizes = new int[3 * numberOfPolynomials];
      for (int i = 0; i < numberOfPolynomials; i++)
      {
         yoPolynomialSizes[3 * i + 0] = yoPolynomial3Ds[i].getYoPolynomialX().getMaximumNumberOfCoefficients() + 1;
         yoPolynomialSizes[3 * i + 1] = yoPolynomial3Ds[i].getYoPolynomialY().getMaximumNumberOfCoefficients() + 1;
         yoPolynomialSizes[3 * i + 2] = yoPolynomial3Ds[i].getYoPolynomialZ().getMaximumNumberOfCoefficients() + 1;
      }

      currentGraphicType = new EnumYoVariable<>(name + "CurrentGraphicType", registry, TrajectoryGraphicType.class, false);
      currentColorType = new EnumYoVariable<>(name + "CurrentColorType", registry, TrajectoryColorType.class, false);
      readExists = new BooleanYoVariable(name + "ReaderExists", registry);

      intermediatePositions = new Point3D[resolution];
      intermediateVelocities = new Vector3D[resolution];
      intermediateAccelerations = new Vector3D[resolution];

      for (int i = 0; i < resolution; i++)
      {
         intermediatePositions[i] = new Point3D();
         intermediateVelocities[i] = new Vector3D();
         intermediateAccelerations[i] = new Vector3D();
      }

      segmentedLine3DMeshGenerator = new SegmentedLine3DMeshDataGenerator(resolution, radialResolution, radius);
      pointCloud3DMeshGenerator = new PointCloud3DMeshGenerator(resolution, radialResolution, radius);
      graphics3DAddMeshDataInstructions = new Graphics3DAddMeshDataInstruction[resolution - 1];

      graphics3dObject.setChangeable(true);
      for (int i = 0; i < resolution - 1; i++)
         graphics3DAddMeshDataInstructions[i] = graphics3dObject.addMeshData(segmentedLine3DMeshGenerator.getMeshDataHolders()[i], YoAppearance.AliceBlue());

      setupDirtyGraphicListener();
   }

   /**
    * Create a YoGraphic for remote visualization.
    * 
    * @param name name of this YoGraphic.
    * @param yoVariables the list of YoVariables needed for this YoGraphic expected to be in the
    *           same order as packed in {@link #getVariables()}.
    * @param constants the list of constants (variables that will never change) needed for this
    *           YoGraphic expected to be in the same order as packed in {@link #getConstants()}.
    * @return a YoGraphic setup for remote visualization.
    */
   static YoGraphicPolynomial3D createAsRemoteYoGraphic(String name, YoVariable<?>[] yoVariables, Double[] constants)
   {
      return new YoGraphicPolynomial3D(name, yoVariables, constants);
   }

   private YoGraphicPolynomial3D(String name, YoVariable<?>[] yoVariables, Double[] constants)
   {
      super(name);

      yoGraphicJob = YoGraphicJob.READER;

      int index = 0;
      radius = constants[index++];
      resolution = constants[index++].intValue();
      radialResolution = constants[index++].intValue();
      hasPoseDefined = constants[index++].intValue() == 1;
      numberOfPolynomials = constants[index++].intValue();

      yoPolynomialSizes = subArray(constants, index, 3 * numberOfPolynomials);

      index = 0;

      if (hasPoseDefined)
      {
         DoubleYoVariable xVariable = (DoubleYoVariable) yoVariables[index++];
         DoubleYoVariable yVariable = (DoubleYoVariable) yoVariables[index++];
         DoubleYoVariable zVariable = (DoubleYoVariable) yoVariables[index++];
         YoFramePoint position = new YoFramePoint(xVariable, yVariable, zVariable, ReferenceFrame.getWorldFrame());
         DoubleYoVariable qx = (DoubleYoVariable) yoVariables[index++];
         DoubleYoVariable qy = (DoubleYoVariable) yoVariables[index++];
         DoubleYoVariable qz = (DoubleYoVariable) yoVariables[index++];
         DoubleYoVariable qs = (DoubleYoVariable) yoVariables[index++];
         YoFrameQuaternion orientation = new YoFrameQuaternion(qx, qy, qz, qs, ReferenceFrame.getWorldFrame());
         poseToPolynomialFrame = new YoFramePoseUsingQuaternions(position, orientation);
      }
      else
      {
         poseToPolynomialFrame = null;
      }

      yoPolynomial3Ds = new YoPolynomial3D[numberOfPolynomials];

      for (int i = 0; i < numberOfPolynomials; i++)
      {
         int xSize = yoPolynomialSizes[3 * i + 0];
         int ySize = yoPolynomialSizes[3 * i + 1];
         int zSize = yoPolynomialSizes[3 * i + 2];

         YoPolynomial xPolynomial = new YoPolynomial(subArray(yoVariables, index + 1, xSize - 1), (IntegerYoVariable) yoVariables[index]);
         index += xSize;

         YoPolynomial yPolynomial = new YoPolynomial(subArray(yoVariables, index + 1, ySize - 1), (IntegerYoVariable) yoVariables[index]);
         index += ySize;

         YoPolynomial zPolynomial = new YoPolynomial(subArray(yoVariables, index + 1, zSize - 1), (IntegerYoVariable) yoVariables[index]);
         index += zSize;

         yoPolynomial3Ds[i] = new YoPolynomial3D(xPolynomial, yPolynomial, zPolynomial);
      }

      waypointTimes = subArray(yoVariables, index, numberOfPolynomials);

      currentGraphicType = (EnumYoVariable<?>) yoVariables[index++];
      currentColorType = (EnumYoVariable<?>) yoVariables[index++];
      readExists = (BooleanYoVariable) yoVariables[index++];

      intermediatePositions = new Point3D[resolution];
      intermediateVelocities = new Vector3D[resolution];
      intermediateAccelerations = new Vector3D[resolution];

      for (int i = 0; i < resolution; i++)
      {
         intermediatePositions[i] = new Point3D();
         intermediateVelocities[i] = new Vector3D();
         intermediateAccelerations[i] = new Vector3D();
      }

      segmentedLine3DMeshGenerator = new SegmentedLine3DMeshDataGenerator(resolution, radialResolution, radius);
      pointCloud3DMeshGenerator = new PointCloud3DMeshGenerator(resolution, radialResolution);
      graphics3DAddMeshDataInstructions = new Graphics3DAddMeshDataInstruction[resolution - 1];

      graphics3dObject.setChangeable(true);
      for (int i = 0; i < resolution - 1; i++)
         graphics3DAddMeshDataInstructions[i] = graphics3dObject.addMeshData(segmentedLine3DMeshGenerator.getMeshDataHolders()[i], YoAppearance.AliceBlue());

      setupDirtyGraphicListener();
   }

   private void setupDirtyGraphicListener()
   {
      VariableChangedListener listener = v -> dirtyGraphic.set(true);
      getVariablesDefiningGraphic().forEach(variable -> variable.addVariableChangedListener(listener));
      currentGraphicType.addVariableChangedListener(listener);
      currentColorType.addVariableChangedListener(listener);
   }

   private static int[] subArray(Double[] source, int start, int length)
   {
      int[] subArray = new int[length];
      for (int i = 0; i < length; i++)
         subArray[i] = source[i + start].intValue();
      return subArray;
   }

   private static DoubleYoVariable[] toArray(List<DoubleYoVariable> list)
   {
      return list.toArray(new DoubleYoVariable[0]);
   }

   private static DoubleYoVariable[] subArray(YoVariable<?>[] source, int start, int length)
   {
      DoubleYoVariable[] subArray = new DoubleYoVariable[length];
      System.arraycopy((DoubleYoVariable[]) source, start, subArray, 0, length);
      return subArray;
   }

   private static AppearanceDefinition[] createColorPalette(int size)
   {
      AppearanceDefinition[] colorPalette = new AppearanceDefinition[size];

      for (int i = 0; i < size; i++)
      {
         float hue = 240.0f * (1.0f - i / (size - 1.0f)) / 360.0f;
         colorPalette[i] = YoAppearance.Color(Color.getHSBColor(hue, 0.9f, 0.9f));
      }

      return colorPalette;
   }

   public void setColorType(TrajectoryColorType colorType)
   {
      setCurrentColorType(colorType);
   }

   public void showGraphic()
   {
      setGraphicType(TrajectoryGraphicType.SHOW_AS_LINE);
   }

   public void hideGraphic()
   {
      setGraphicType(TrajectoryGraphicType.HIDE);
   }

   public void setGraphicType(TrajectoryGraphicType graphicType)
   {
      setCurrentGraphicType(graphicType);

      if (graphicType != TrajectoryGraphicType.HIDE)
      {
         dirtyGraphic.set(true);
         update();
      }
   }

   @Override
   public void update()
   {
      if (yoGraphicJob == YoGraphicJob.READER)
      {
         // Notify the writer that a reader exists and the writer does not have to compute the meshes.
         readExists.set(true);
      }

      switch (yoGraphicJob)
      {
      case READER:
         computeTrajectoryMesh();
         break;
      case WRITER:
         if (!readExists.getBooleanValue())
            computeTrajectoryMesh();
      default:
         break;
      }
   }

   private void computeTrajectoryMesh()
   {
      if (!dirtyGraphic.get())
         return;

      if (getCurrentGraphicType() == TrajectoryGraphicType.HIDE)
      {
         for (Graphics3DAddMeshDataInstruction meshDataInstruction : graphics3DAddMeshDataInstructions)
            meshDataInstruction.setMesh(null);
         dirtyGraphic.set(false);
         return;
      }

      for (Point3D position : intermediatePositions)
         position.setToZero();
      for (Vector3D velocity : intermediateVelocities)
         velocity.setToZero();
      for (Vector3D acceleration : intermediateAccelerations)
         acceleration.setToZero();

      int index = 0;
      double trajectoryTime = 0.0;

      while (index < waypointTimes.length && trajectoryTime < waypointTimes[index].getDoubleValue())
         trajectoryTime = waypointTimes[index++].getDoubleValue();

      int polynomialIndex = 0;

      double maxVelocity = 0.0;
      double maxAcceleration = 0.0;

      for (int i = 0; i < resolution; i++)
      {
         double t = i / (resolution - 1.0) * trajectoryTime;

         while (t > waypointTimes[polynomialIndex].getDoubleValue())
            polynomialIndex++;

         YoPolynomial3D activePolynomial3D = yoPolynomial3Ds[polynomialIndex];
         activePolynomial3D.compute(t);
         intermediatePositions[i].set(activePolynomial3D.getPosition());
         intermediateVelocities[i].set(activePolynomial3D.getVelocity());
         intermediateAccelerations[i].set(activePolynomial3D.getAcceleration());

         maxVelocity = Math.max(maxVelocity, activePolynomial3D.getVelocity().lengthSquared());
         maxAcceleration = Math.max(maxAcceleration, activePolynomial3D.getAcceleration().lengthSquared());
      }

      maxVelocity = Math.sqrt(maxVelocity);
      maxAcceleration = Math.sqrt(maxAcceleration);

      switch (getCurrentColorType())
      {
      case BLACK:
         for (Graphics3DAddMeshDataInstruction meshDataInstruction : graphics3DAddMeshDataInstructions)
            meshDataInstruction.setAppearance(BLACK_APPEARANCE);
         break;
      case VELOCITY_BASED:
         for (int i = 0; i < resolution - 1; i++)
         {
            double velocity = intermediateVelocities[i].length();
            int colorIndex = (int) Math.round((colorPalette.length - 1.0) * (velocity / maxVelocity));
            graphics3DAddMeshDataInstructions[i].setAppearance(colorPalette[colorIndex]);
         }
         break;
      case ACCELERATION_BASED:
         for (int i = 0; i < resolution - 1; i++)
         {

            double acceleration = intermediateAccelerations[i].length();
            int colorIndex = (int) Math.round((colorPalette.length - 1.0) * (acceleration / maxAcceleration));
            graphics3DAddMeshDataInstructions[i].setAppearance(colorPalette[colorIndex]);
         }
         break;
      default:
         break;
      }

      switch (getCurrentGraphicType())
      {
      case SHOW_AS_LINE:
         segmentedLine3DMeshGenerator.compute(intermediatePositions, intermediateVelocities);
         for (int i = 0; i < resolution - 1; i++)
            graphics3DAddMeshDataInstructions[i].setMesh(segmentedLine3DMeshGenerator.getMeshDataHolders()[i]);
         break;

      case SHOW_AS_POINTS:
         pointCloud3DMeshGenerator.compute(intermediatePositions);
         for (int i = 0; i < resolution - 1; i++)
            graphics3DAddMeshDataInstructions[i].setMesh(pointCloud3DMeshGenerator.getMeshDataHolders()[i]);
         break;
      default:
         throw new RuntimeException("Unexpected state: " + getCurrentGraphicType());
      }

      dirtyGraphic.set(false);
   }

   private void setCurrentGraphicType(TrajectoryGraphicType graphicType)
   {
      currentGraphicType.set(graphicType.ordinal());
   }

   private TrajectoryGraphicType getCurrentGraphicType()
   {
      return TrajectoryGraphicType.values[currentGraphicType.getOrdinal()];
   }

   private void setCurrentColorType(TrajectoryColorType colorType)
   {
      currentColorType.set(colorType.ordinal());
   }

   private TrajectoryColorType getCurrentColorType()
   {
      return TrajectoryColorType.values[currentColorType.getOrdinal()];
   }

   @Override
   public RemoteGraphicType getRemoteGraphicType()
   {
      return RemoteGraphicType.POLYNOMIAL_3D_DGO;
   }

   @Override
   public YoVariable<?>[] getVariables()
   {
      List<YoVariable<?>> allVariables = new ArrayList<>();
      allVariables.addAll(getVariablesDefiningGraphic());
      allVariables.add(currentGraphicType);
      allVariables.add(readExists);

      return allVariables.toArray(new YoVariable[0]);
   }

   private List<YoVariable<?>> getVariablesDefiningGraphic()
   {
      List<YoVariable<?>> graphicVariables = new ArrayList<>();

      if (poseToPolynomialFrame != null)
      {
         graphicVariables.add(poseToPolynomialFrame.getYoX());
         graphicVariables.add(poseToPolynomialFrame.getYoY());
         graphicVariables.add(poseToPolynomialFrame.getYoZ());
         graphicVariables.add(poseToPolynomialFrame.getYoQx());
         graphicVariables.add(poseToPolynomialFrame.getYoQy());
         graphicVariables.add(poseToPolynomialFrame.getYoQz());
         graphicVariables.add(poseToPolynomialFrame.getYoQs());
      }

      addPolynomialVariablesToList(yoPolynomial3Ds, numberOfPolynomials, graphicVariables);

      for (DoubleYoVariable waypointTime : waypointTimes)
         graphicVariables.add(waypointTime);

      return graphicVariables;
   }

   private static void addPolynomialVariablesToList(YoPolynomial3D[] yoPolynomial3Ds, int numberOfPolynomials, List<YoVariable<?>> allVariables)
   {
      for (int i = 0; i < numberOfPolynomials; i++)
      {
         for (int index = 0; index < 3; index++)
         {
            YoPolynomial yoPolynomial = yoPolynomial3Ds[i].getYoPolynomial(index);

            allVariables.add(yoPolynomial.getYoNumberOfCoefficients());
            for (YoVariable<?> coefficient : yoPolynomial.getYoCoefficients())
               allVariables.add(coefficient);
         }
      }
   }

   @Override
   public double[] getConstants()
   {
      TDoubleArrayList allConstants = new TDoubleArrayList();
      allConstants.add(radius);
      allConstants.add(resolution);
      allConstants.add(radialResolution);

      allConstants.add(hasPoseDefined ? 1 : 0);

      allConstants.add(numberOfPolynomials);

      for (int i = 0; i < numberOfPolynomials; i++)
         allConstants.add(yoPolynomialSizes[i]);

      return allConstants.toArray();
   }

   @Override
   public AppearanceDefinition getAppearance()
   {
      // Does not matter as the appearance is generated internally
      return YoAppearance.AliceBlue();
   }

   @Override
   public Graphics3DObject getLinkGraphics()
   {
      return graphics3dObject;
   }

   private final RigidBodyTransform rigidBodyTransform = new RigidBodyTransform();

   @Override
   protected void computeRotationTranslation(AffineTransform transform)
   {
      if (getCurrentGraphicType() == TrajectoryGraphicType.HIDE)
         return;

      if (poseToPolynomialFrame != null)
      {
         poseToPolynomialFrame.getPose(rigidBodyTransform);
         transform.set(rigidBodyTransform);
      }
      else
      {
         transform.setIdentity();
      }

      update();
   }

   @Override
   protected boolean containsNaN()
   { // Only used to determine if the graphics from this object is valid, and whether to display or hide.
      return getCurrentGraphicType() == TrajectoryGraphicType.HIDE;
   }

   @Override
   public Artifact createArtifact()
   {
      throw new RuntimeException("Implement Me!");
   }
}
