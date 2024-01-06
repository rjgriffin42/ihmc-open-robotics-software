package us.ihmc.perception;

import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D32;
import us.ihmc.perception.sceneGraph.rigidBody.primitive.PrimitiveRigidBodyShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IterativeClosestPointTools
{
   public static List<Point3D32> createICPObjectPointCloud(PrimitiveRigidBodyShape shape,
                                                     Pose3DReadOnly shapePose,
                                                     float xLength,
                                                     float yLength,
                                                     float zLength,
                                                     float xRadius,
                                                     float yRadius,
                                                     float zRadius,
                                                     int numberOfICPObjectPoints,
                                                           Random random)
   {
      List<Point3D32> objectPointCloud;

      switch (shape)
      {
         case BOX -> objectPointCloud = IterativeClosestPointTools.createBoxPointCloud(shapePose, xLength, yLength, zLength, numberOfICPObjectPoints, random);
         case PRISM -> objectPointCloud = IterativeClosestPointTools.createPrismPointCloud(shapePose, xLength, yLength, zLength, numberOfICPObjectPoints, random);
         case CYLINDER -> objectPointCloud = IterativeClosestPointTools.createCylinderPointCloud(shapePose, zLength, xRadius, numberOfICPObjectPoints, random);
         case ELLIPSOID -> objectPointCloud = IterativeClosestPointTools.createEllipsoidPointCloud(shapePose, xRadius, yRadius, zRadius, numberOfICPObjectPoints, random);
         case CONE -> objectPointCloud = IterativeClosestPointTools.createConePointCloud(shapePose, zLength, xRadius, numberOfICPObjectPoints, random);
         default -> objectPointCloud = IterativeClosestPointTools.createDefaultBoxPointCloud(shapePose, numberOfICPObjectPoints, random);
      }

      return objectPointCloud;
   }

   public static List<Point3D32> createBoxPointCloud(Pose3DReadOnly boxPose, float xLength, float yLength, float zLength, int numberOfPoints, Random random)
   {
      List<Point3D32> boxObjectPointCloud = new ArrayList<>();
      Pose3D boxPointPose = new Pose3D();

      float halfBoxDepth = xLength / 2.0f;
      float halfBoxWidth = yLength / 2.0f;
      float halfBoxHeight = zLength / 2.0f;
      for (int i = 0; i < numberOfPoints; i++)
      {
         int j = random.nextInt(6);
         float x = random.nextFloat(-halfBoxDepth, halfBoxDepth);
         float y = random.nextFloat(-halfBoxWidth, halfBoxWidth);
         float z = random.nextFloat(-halfBoxHeight, halfBoxHeight);
         if (j == 0 | j == 1)
         {
            x = (-(j & 1) * halfBoxDepth * 2.0f) + halfBoxDepth;
         }
         if (j == 2 | j == 3)
         {
            y = (-(j & 1) * halfBoxWidth * 2.0f) + halfBoxWidth;
         }
         if (j == 4 | j == 5)
         {
            z = (-(j & 1) * halfBoxHeight * 2.0f) + halfBoxHeight;
         }

         boxPointPose.set(boxPose);
         boxPointPose.appendTranslation(x, y, z);

         boxObjectPointCloud.add(new Point3D32(boxPointPose.getPosition()));
      }

      return boxObjectPointCloud;
   }

   public static List<Point3D32> createPrismPointCloud(Pose3DReadOnly prismPose, float xLength, float yLength, float zLength, int numberOfPoints, Random random)
   {
      List<Point3D32> prismObjectPointCloud = new ArrayList<>();
      Pose3D prismPointPose = new Pose3D();

      float halfPrismDepth = xLength / 2.0f;
      float halfPrismWidth = yLength / 2.0f;

      for (int i = 0; i < numberOfPoints; i++)
      {
         int side = random.nextInt(0, 4);
         float x = random.nextFloat(-halfPrismDepth, halfPrismDepth);
         float y = random.nextFloat(-halfPrismWidth, halfPrismWidth);
         float z = random.nextFloat(0, zLength);
         if (side == 0 || side == 1) // triangular faces
         {
            x = (1.0f - (z / zLength)) * x;
            y = (-(side & 1) * halfPrismWidth * 2.0f) + halfPrismWidth;
         }
         else if (side == 2 || side == 3) // rectangular faces
         {
            x = (1.0f - (z / zLength)) * ((-(side & 1) * halfPrismDepth * 2.0f) + halfPrismDepth);
         }

         prismPointPose.set(prismPose);
         prismPointPose.appendTranslation(x, y, z);

         prismObjectPointCloud.add(new Point3D32(prismPointPose.getPosition()));
      }

      return prismObjectPointCloud;
   }

   public static List<Point3D32> createCylinderPointCloud(Pose3DReadOnly cylinderPose, float zLength, float xRadius, int numberOfPoints, Random random)
   {
      List<Point3D32> cylinderObjectPointCloud = new ArrayList<>();
      Pose3D cylinderPointPose = new Pose3D();

      for (int i = 0; i < numberOfPoints; i++)
      {
         int j = random.nextInt(6);
         float z = random.nextFloat(0, zLength);
         float r = xRadius;
         if (j == 0)
         {
            z = 0;
            r = random.nextFloat(0, xRadius);
         }
         if (j == 1)
         {
            z = zLength;
            r = random.nextFloat(0, xRadius);
         }
         double phi = random.nextDouble(0, 2 * Math.PI);
         float x = (float) Math.cos(phi) * r;
         float y = (float) Math.sin(phi) * r;

         cylinderPointPose.set(cylinderPose);
         cylinderPointPose.appendTranslation(x, y, z);

         cylinderObjectPointCloud.add(new Point3D32(cylinderPointPose.getPosition()));
      }

      return cylinderObjectPointCloud;
   }

   public static List<Point3D32> createEllipsoidPointCloud(Pose3DReadOnly ellipsePose,
                                                           float xRadius,
                                                           float yRadius,
                                                           float zRadius,
                                                           int numberOfPoints,
                                                           Random random)
   {
      List<Point3D32> ellipsoidObjectPointCloud = new ArrayList<>();
      Pose3D ellipsoidPointPose = new Pose3D();

      for (int i = 0; i < numberOfPoints; i++)
      {
         double phi = random.nextDouble(0, 2.0 * Math.PI);
         double theta = random.nextDouble(0, 2.0 * Math.PI);
         float x = (float) (Math.sin(phi) * Math.cos(theta) * xRadius);
         float y = (float) (Math.sin(phi) * Math.sin(theta) * yRadius);
         float z = (float) Math.cos(phi) * zRadius;

         ellipsoidPointPose.set(ellipsePose);
         ellipsoidPointPose.appendTranslation(x, y, z);

         ellipsoidObjectPointCloud.add(new Point3D32(ellipsoidPointPose.getPosition()));
      }

      return ellipsoidObjectPointCloud;
   }

   public static List<Point3D32> createConePointCloud(Pose3DReadOnly conePose, float zLength, float xRadius, int numberOfPoints, Random random)
   {
      List<Point3D32> coneObjectPointCloud = new ArrayList<>();
      Pose3D conePointPose = new Pose3D();

      for (int i = 0; i < numberOfPoints; i++)
      {
         float z = random.nextFloat(0, zLength);
         double phi = random.nextDouble(0, 2.0 * Math.PI);
         float x = (float) Math.cos(phi) * (zLength - z) * (xRadius / zLength);
         float y = (float) Math.sin(phi) * (zLength - z) * (xRadius / zLength);

         conePointPose.set(conePose);
         conePointPose.appendTranslation(x, y, z);

         coneObjectPointCloud.add(new Point3D32(conePointPose.getPosition()));
      }

      return coneObjectPointCloud;
   }

   public static List<Point3D32> createDefaultBoxPointCloud(Pose3DReadOnly boxPose, int numberOfPoints, Random random)
   {
      List<Point3D32> boxObjectPointCloud = new ArrayList<>();
      Pose3D boxPointPose = new Pose3D();

      float halfBoxWidth = 0.405f / 2.0f;
      float halfBoxDepth = 0.31f / 2.0f;
      float halfBoxHeight = 0.19f / 2.0f;
      for (int i = 0; i < numberOfPoints; i++)
      {
         int j = random.nextInt(6);
         float x = random.nextFloat(-halfBoxDepth, halfBoxDepth);
         float y = random.nextFloat(-halfBoxWidth, halfBoxWidth);
         float z = random.nextFloat(-halfBoxHeight, halfBoxHeight);
         if (j == 0 | j == 1)
         {
            x = (-(j & 1) * halfBoxDepth * 2.0f) + halfBoxDepth;
         }
         if (j == 2 | j == 3)
         {
            y = (-(j & 1) * halfBoxWidth * 2.0f) + halfBoxWidth;
         }
         if (j == 4 | j == 5)
         {
            z = (-(j & 1) * halfBoxHeight * 2.0f) + halfBoxHeight;
         }

         boxPointPose.set(boxPose);
         boxPointPose.appendTranslation(x, y, z);

         boxObjectPointCloud.add(new Point3D32(boxPointPose.getPosition()));
      }

      return boxObjectPointCloud;
   }
}
