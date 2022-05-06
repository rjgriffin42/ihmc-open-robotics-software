package us.ihmc.avatar.reachabilityMap;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.avatar.reachabilityMap.voxelPrimitiveShapes.SphereVoxelShape;
import us.ihmc.avatar.reachabilityMap.voxelPrimitiveShapes.SphereVoxelShape.SphereVoxelType;
import us.ihmc.euclid.geometry.BoundingBox3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameTuple3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.ReferenceFrameHolder;
import us.ihmc.euclid.tools.EuclidCoreIOTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointReadOnly;
import us.ihmc.robotics.linearAlgebra.PrincipalComponentAnalysis3D;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;

public class Voxel3DGrid implements ReferenceFrameHolder
{
   public static final int MAX_GRID_SIZE_VOXELS = (int) Math.pow(Integer.MAX_VALUE, 1.0 / 3.0);

   private final BoundingBox3D boundingBox;
   private final SphereVoxelShape sphereVoxelShape;
   private final double voxelSize;
   private final double gridSizeMeters;
   private final int gridSizeVoxels;
   private final int numberOfVoxels;
   private final Voxel3DData[] voxels;

   private final PoseReferenceFrame referenceFrame;

   public static Voxel3DGrid newVoxel3DGrid(int gridSizeInNumberOfVoxels, double voxelSize, int numberOfRays, int numberOfRotationsAroundRay)
   {
      return newVoxel3DGrid(ReferenceFrame.getWorldFrame(), gridSizeInNumberOfVoxels, voxelSize, numberOfRays, numberOfRotationsAroundRay);
   }

   public static Voxel3DGrid newVoxel3DGrid(ReferenceFrame parentFrame,
                                            int gridSizeInNumberOfVoxels,
                                            double voxelSize,
                                            int numberOfRays,
                                            int numberOfRotationsAroundRay)
   {
      SphereVoxelShape sphereVoxelShape = new SphereVoxelShape(voxelSize, numberOfRays, numberOfRotationsAroundRay, SphereVoxelType.graspOrigin);
      return new Voxel3DGrid(parentFrame, sphereVoxelShape, gridSizeInNumberOfVoxels, voxelSize);
   }

   public Voxel3DGrid(SphereVoxelShape sphereVoxelShape, int gridSizeInNumberOfVoxels, double voxelSize)
   {
      this(ReferenceFrame.getWorldFrame(), sphereVoxelShape, gridSizeInNumberOfVoxels, voxelSize);
   }

   public Voxel3DGrid(ReferenceFrame parentFrame, SphereVoxelShape sphereVoxelShape, int gridSizeInNumberOfVoxels, double voxelSize)
   {
      if (gridSizeInNumberOfVoxels > MAX_GRID_SIZE_VOXELS)
         throw new IllegalArgumentException("Grid size is too big: " + gridSizeInNumberOfVoxels + " [max=" + MAX_GRID_SIZE_VOXELS + "]");

      this.sphereVoxelShape = sphereVoxelShape;
      this.voxelSize = voxelSize;
      gridSizeVoxels = gridSizeInNumberOfVoxels;

      numberOfVoxels = gridSizeVoxels * gridSizeVoxels * gridSizeVoxels;

      gridSizeMeters = voxelSize * gridSizeInNumberOfVoxels;
      double halfSize = gridSizeMeters / 2.0;
      boundingBox = new BoundingBox3D(-halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize);
      voxels = new Voxel3DData[numberOfVoxels];
      referenceFrame = new PoseReferenceFrame("voxel3DGridFrame", parentFrame);
      sphereVoxelShape.setReferenceFrame(referenceFrame);
   }

   public Voxel3DData getVoxel(FrameTuple3DReadOnly query)
   {
      checkReferenceFrameMatch(query);
      return getVoxel((Tuple3DReadOnly) query);
   }

   public Voxel3DData getVoxel(Tuple3DReadOnly query)
   {
      return getVoxel(query.getX(), query.getY(), query.getZ());
   }

   public Voxel3DData getVoxel(double x, double y, double z)
   {
      if (!boundingBox.isInsideInclusive(x, y, z))
         throw new IllegalArgumentException("The given point is outside the grid");
      return getVoxel(toIndex(x), toIndex(y), toIndex(z));
   }

   public Voxel3DData getVoxel(int xIndex, int yIndex, int zIndex)
   {
      return voxels[Voxel3DKey.toArrayIndex(xIndex, yIndex, zIndex, gridSizeVoxels)];
   }

   public Voxel3DData getVoxel(int index)
   {
      return voxels[index];
   }

   public Voxel3DData getOrCreateVoxel(int index)
   {
      Voxel3DData voxel = voxels[index];

      if (voxel == null)
      {
         voxel = new Voxel3DData(new Voxel3DKey(index, gridSizeVoxels));
         voxels[index] = voxel;
      }

      return voxel;
   }

   public Voxel3DData getOrCreateVoxel(int xIndex, int yIndex, int zIndex)
   {
      int index = Voxel3DKey.toArrayIndex(xIndex, yIndex, zIndex, gridSizeVoxels);
      Voxel3DData voxel = voxels[index];
      if (voxel == null)
      {
         voxel = new Voxel3DData(new Voxel3DKey(xIndex, yIndex, zIndex, gridSizeVoxels));
         voxels[index] = voxel;
      }
      return voxel;
   }

   public void destroy(Voxel3DData voxel)
   {
      voxels[voxel.getKey().getIndex()] = null;
   }

   private double toCoordinate(int index)
   {
      return (index + 0.5) * voxelSize - 0.5 * gridSizeMeters;
   }

   private int toIndex(double coordinate)
   {
      return (int) (coordinate / voxelSize + gridSizeVoxels / 2 - 1);
   }

   private final PrincipalComponentAnalysis3D pca = new PrincipalComponentAnalysis3D();

   // FIXME Still in development
   private void fitCone(int xIndex, int yIndex, int zIndex)
   {
      Voxel3DData voxel = getVoxel(xIndex, yIndex, zIndex);

      List<Point3D> reachablePointsOnly = new ArrayList<>();
      for (int i = 0; i < sphereVoxelShape.getNumberOfRays(); i++)
      {
         if (voxel.isRayReachable(i))
            reachablePointsOnly.add(sphereVoxelShape.getPointsOnSphere()[i]);
      }

      pca.setPointCloud(reachablePointsOnly);
      pca.compute();
      RotationMatrix coneRotation = new RotationMatrix();
      pca.getPrincipalFrameRotationMatrix(coneRotation);

      Vector3D sphereOriginToAverage = new Vector3D();
      Vector3D thirdAxis = new Vector3D();
      coneRotation.getColumn(2, thirdAxis);

      FramePoint3D voxelLocation = new FramePoint3D(voxel.getPosition());
      Vector3D mean = new Vector3D();
      sphereOriginToAverage.sub(mean, voxelLocation);

      if (sphereOriginToAverage.dot(thirdAxis) < 0.0)
      {
         // Rotate the frame of PI around the principal axis, such that the third axis is pointing towards the point cloud.
         RotationMatrix invertThirdAxis = new RotationMatrix();
         invertThirdAxis.setToRollOrientation(Math.PI);
         coneRotation.multiply(invertThirdAxis);
      }

      // Build the cone
      double smallestDotProduct = Double.POSITIVE_INFINITY;
      coneRotation.getColumn(2, thirdAxis);
      Vector3D testedRay = new Vector3D();
      Vector3D mostOpenedRay = new Vector3D();

      // Find the point that is the farthest from the 
      for (Point3D point : reachablePointsOnly)
      {
         testedRay.sub(point, voxelLocation);
         double absDotProduct = Math.abs(testedRay.dot(thirdAxis));
         if (absDotProduct < smallestDotProduct)
         {
            smallestDotProduct = absDotProduct;
            mostOpenedRay.set(testedRay);
         }
      }

      Vector3D standardDeviation = new Vector3D();
      pca.getStandardDeviation(standardDeviation);
      standardDeviation.scale(1.3); // Because the points are uniformly distributed

      double coneBaseRadius = Math.sqrt(standardDeviation.getX() * standardDeviation.getX() + standardDeviation.getY() * standardDeviation.getY());//radiusVector.length();
      double coneHeight = mostOpenedRay.dot(thirdAxis);

      RigidBodyTransform coneTransform = new RigidBodyTransform();

      coneTransform.getRotation().set(coneRotation);
      coneTransform.getTranslation().set(voxelLocation);
   }

   public void setGridPose(RigidBodyTransformReadOnly pose)
   {
      referenceFrame.setPoseAndUpdate(pose);
   }

   public void setGridPose(Pose3DReadOnly pose)
   {
      referenceFrame.setPoseAndUpdate(pose);
   }

   @Override
   public PoseReferenceFrame getReferenceFrame()
   {
      return referenceFrame;
   }

   public SphereVoxelShape getSphereVoxelShape()
   {
      return sphereVoxelShape;
   }

   public double getVoxelSize()
   {
      return voxelSize;
   }

   public double getGridSizeMeters()
   {
      return gridSizeMeters;
   }

   public int getGridSizeVoxels()
   {
      return gridSizeVoxels;
   }

   public int getNumberOfVoxels()
   {
      return numberOfVoxels;
   }

   public FramePoint3D getMinPoint()
   {
      return new FramePoint3D(referenceFrame, boundingBox.getMinPoint());
   }

   public FramePoint3D getMaxPoint()
   {
      return new FramePoint3D(referenceFrame, boundingBox.getMaxPoint());
   }

   public class Voxel3DData
   {
      private final Voxel3DKey key;
      private final FramePoint3DReadOnly position;

      private VoxelJointData positionJointData;
      private VoxelJointData[] rayJointData;
      private VoxelJointData[] poseJointData;

      public Voxel3DData(Voxel3DKey key)
      {
         this.key = key;
         position = new FramePoint3D(getReferenceFrame(), toCoordinate(key.x), toCoordinate(key.y), toCoordinate(key.z));
      }

      public void registerReachablePosition(float[] jointPositions, float[] jointTorques)
      {
         if ((jointPositions == null || jointPositions.length == 0) && (jointTorques == null || jointTorques.length == 0))
            return;

         positionJointData = new VoxelJointData();
         positionJointData.jointPositions = jointPositions;
         positionJointData.jointTorques = jointTorques;
      }

      public void registerReachablePosition(OneDoFJointReadOnly[] joints)
      {
         positionJointData = new VoxelJointData();
         positionJointData.setJointPositions(joints);
         positionJointData.setJointTorques(joints);
      }

      public void registerReachableRay(int rayIndex, float[] jointPositions, float[] jointTorques)
      {
         if ((jointPositions == null || jointPositions.length == 0) && (jointTorques == null || jointTorques.length == 0))
            return;

         if (rayJointData == null)
            rayJointData = new VoxelJointData[sphereVoxelShape.getNumberOfRays()];

         VoxelJointData jointData = new VoxelJointData();
         jointData.jointPositions = jointPositions;
         jointData.jointTorques = jointTorques;
         rayJointData[rayIndex] = jointData;
      }

      public void registerReachableRay(int rayIndex, OneDoFJointReadOnly[] joints)
      {
         if (rayJointData == null)
            rayJointData = new VoxelJointData[sphereVoxelShape.getNumberOfRays()];

         VoxelJointData jointData = new VoxelJointData();
         jointData.setJointPositions(joints);
         jointData.setJointTorques(joints);
         rayJointData[rayIndex] = jointData;
      }

      public void registerReachablePose(int rayIndex, int rotationAroundRayIndex, float[] jointPositions, float[] jointTorques)
      {
         if ((jointPositions == null || jointPositions.length == 0) && (jointTorques == null || jointTorques.length == 0))
            return;

         if (poseJointData == null)
            poseJointData = new VoxelJointData[sphereVoxelShape.getNumberOfRays() * sphereVoxelShape.getNumberOfRotationsAroundRay()];

         VoxelJointData jointData = new VoxelJointData();
         jointData.jointPositions = jointPositions;
         jointData.jointTorques = jointTorques;
         poseJointData[rayIndex * sphereVoxelShape.getNumberOfRotationsAroundRay() + rotationAroundRayIndex] = jointData;
      }

      public void registerReachablePose(int rayIndex, int rotationAroundRayIndex, OneDoFJointReadOnly[] joints)
      {
         if (poseJointData == null)
            poseJointData = new VoxelJointData[sphereVoxelShape.getNumberOfRays() * sphereVoxelShape.getNumberOfRotationsAroundRay()];

         VoxelJointData jointData = new VoxelJointData();
         jointData.setJointPositions(joints);
         jointData.setJointTorques(joints);
         poseJointData[rayIndex * sphereVoxelShape.getNumberOfRotationsAroundRay() + rotationAroundRayIndex] = jointData;
      }

      /**
       * Return the D reachability value in percent for this voxel based on the number of the rays that
       * have been reached.
       * 
       * @return The D reachability
       */
      public double getD()
      {
         if (rayJointData == null)
            return 0;

         double d = 0;
         int numberOfRays = sphereVoxelShape.getNumberOfRays();

         for (int i = 0; i < numberOfRays; i++)
         {
            if (isRayReachable(i))
               d += 1.0;
         }

         d /= (double) numberOfRays;

         return d;
      }

      /**
       * Return the D0 reachability value in percent for this voxel based on the number of the
       * orientations (number of rays times number of rotations around rays) that have been reached.
       * 
       * @return The D0 reachability
       */
      public double getD0()
      {
         if (poseJointData == null)
            return 0;

         double d0 = 0;
         int numberOfRays = sphereVoxelShape.getNumberOfRays();
         int numberOfRotationsAroundRay = sphereVoxelShape.getNumberOfRotationsAroundRay();

         for (int i = 0; i < numberOfRays; i++)
         {
            for (int j = 0; j < numberOfRotationsAroundRay; j++)
            {
               if (isPoseReachable(i, j))
                  d0 += 1.0;
            }
         }

         d0 /= (double) numberOfRays;
         d0 /= (double) numberOfRotationsAroundRay;

         return d0;
      }

      public Voxel3DKey getKey()
      {
         return key;
      }

      public FramePoint3DReadOnly getPosition()
      {
         return position;
      }

      public int getNumberOfRays()
      {
         return sphereVoxelShape.getNumberOfRays();
      }

      public int getNumberOfRotationsAroundRay()
      {
         return sphereVoxelShape.getNumberOfRotationsAroundRay();
      }

      public boolean atLeastOneReachableRay()
      {
         if (rayJointData == null)
            return false;
         for (VoxelJointData jointData : rayJointData)
         {
            if (jointData != null)
               return true;
         }
         return false;
      }

      public boolean isRayReachable(int rayIndex)
      {
         return rayJointData == null ? false : rayJointData[rayIndex] != null;
      }

      public boolean atLeastOneReachablePose()
      {
         if (poseJointData == null)
            return false;
         for (VoxelJointData jointData : poseJointData)
         {
            if (jointData != null)
               return true;
         }
         return false;
      }

      public boolean isPoseReachable(int rayIndex, int rotationIndex)
      {
         return getPoseJointData(rayIndex, rotationIndex) != null;
      }

      public VoxelJointData getPositionJointData()
      {
         return positionJointData;
      }

      public VoxelJointData getRayJointData(int rayIndex)
      {
         return rayJointData[rayIndex];
      }

      public VoxelJointData getPoseJointData(int rayIndex, int rotationIndex)
      {
         if (poseJointData == null)
            return null;
         return poseJointData[rayIndex * sphereVoxelShape.getNumberOfRotationsAroundRay() + rotationIndex];
      }
   }

   public static class VoxelJointData
   {
      private float[] jointPositions;
      private float[] jointTorques;

      public VoxelJointData()
      {
      }

      public void setJointPositions(OneDoFJointReadOnly[] joints)
      {
         if (jointPositions == null)
            jointPositions = new float[joints.length];
         for (int i = 0; i < joints.length; i++)
         {
            jointPositions[i] = (float) joints[i].getQ();
         }
      }

      public void setJointTorques(OneDoFJointReadOnly[] joints)
      {
         if (jointTorques == null)
            jointTorques = new float[joints.length];
         for (int i = 0; i < joints.length; i++)
         {
            jointTorques[i] = (float) joints[i].getTau();
         }
      }

      public float[] getJointPositions()
      {
         return jointPositions;
      }

      public float[] getJointTorques()
      {
         return jointTorques;
      }
   }

   public static class Voxel3DKey
   {
      private int x, y, z;
      private int index;

      public Voxel3DKey(int x, int y, int z, int gridSizeVoxels)
      {
         if (x >= gridSizeVoxels)
            throw new ArrayIndexOutOfBoundsException(x);
         if (y >= gridSizeVoxels)
            throw new ArrayIndexOutOfBoundsException(y);
         if (z >= gridSizeVoxels)
            throw new ArrayIndexOutOfBoundsException(z);

         this.x = x;
         this.y = y;
         this.z = z;
         index = toArrayIndex(x, y, z, gridSizeVoxels);
      }

      public Voxel3DKey(int index, int gridSizeVoxels)
      {
         this.index = index;
         x = toXindex(index, gridSizeVoxels);
         y = toYindex(index, gridSizeVoxels);
         z = toZindex(index, gridSizeVoxels);

         if (x >= gridSizeVoxels)
            throw new ArrayIndexOutOfBoundsException(x);
         if (y >= gridSizeVoxels)
            throw new ArrayIndexOutOfBoundsException(y);
         if (z >= gridSizeVoxels)
            throw new ArrayIndexOutOfBoundsException(z);
      }

      public static int toArrayIndex(int x, int y, int z, int gridSizeVoxels)
      {
         return (x * gridSizeVoxels + y) * gridSizeVoxels + z;
      }

      public static int toXindex(int arrayIndex, int gridSizeVoxels)
      {
         return arrayIndex / gridSizeVoxels / gridSizeVoxels;
      }

      public static int toYindex(int arrayIndex, int gridSizeVoxels)
      {
         return (arrayIndex / gridSizeVoxels) % gridSizeVoxels;
      }

      public static int toZindex(int arrayIndex, int gridSizeVoxels)
      {
         return arrayIndex % gridSizeVoxels;
      }

      @Override
      public int hashCode()
      {
         return index;
      }

      @Override
      public boolean equals(Object object)
      {
         if (object instanceof Voxel3DKey)
         {
            Voxel3DKey other = (Voxel3DKey) object;
            return x == other.x && y == other.y && z == other.z;
         }
         else
         {
            return false;
         }
      }

      public int getX()
      {
         return x;
      }

      public int getY()
      {
         return y;
      }

      public int getZ()
      {
         return z;
      }

      public int getIndex()
      {
         return index;
      }

      @Override
      public String toString()
      {
         return EuclidCoreIOTools.getStringOf("(", ")", ", ", x, y, z);
      }
   }
}
