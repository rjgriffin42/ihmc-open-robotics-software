package us.ihmc.perception.sceneGraph.yolo;

import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformBasics;
import us.ihmc.euclid.tuple3D.Point3D32;
import us.ihmc.perception.YOLOv8.YOLOv8DetectionClass;
import us.ihmc.perception.sceneGraph.DetectableSceneNode;

import java.util.List;

public class YOLOv8Node extends DetectableSceneNode
{
   // Read from RDX node, write to YOLO Manager
   private int maskErosionKernelRadius;
   private double outlierFilterThreshold;
   private float detectionAcceptanceThreshold;

   // Read from YOLO manager, write to RDX
   private YOLOv8DetectionClass detectionClass;
   private List<Point3D32> objectPointCloud;
   private Point3D32 objectCentroid;

   // Set this somewhere
   private final RigidBodyTransform centroidToObjectTransform = new RigidBodyTransform();
   private Pose3D objectPose;
   private Pose3D filteredObjectPose;
   private final RigidBodyTransform visualTransformToObjectPose = new RigidBodyTransform();

   private double alpha = 0.15;

   public YOLOv8Node(long id, String name, YOLOv8DetectionClass detectionClass, List<Point3D32> objectPointCloud, Point3D32 objectCentroid)
   {
      this(id,
           name,
           2,
           2.0,
           0.2f,
           detectionClass,
           objectPointCloud,
           objectCentroid,
           new RigidBodyTransform(),
           new Pose3D(objectCentroid, new RotationMatrix()),
           new Pose3D(objectCentroid, new RotationMatrix()),
           new RigidBodyTransform());
   }

   public YOLOv8Node(long id,
                     String name,
                     int maskErosionKernelRadius,
                     double outlierFilterThreshold,
                     float detectionAcceptanceThreshold,
                     YOLOv8DetectionClass detectionClass,
                     List<Point3D32> objectPointCloud,
                     Point3D32 objectCentroid,
                     RigidBodyTransformBasics centroidToObjectTransform,
                     Pose3D objectPose,
                     Pose3D filteredObjectPose,
                     RigidBodyTransformBasics visualTransformToObjectPose)
   {
      super(id, name);

      this.maskErosionKernelRadius = maskErosionKernelRadius;
      this.outlierFilterThreshold = outlierFilterThreshold;
      this.detectionAcceptanceThreshold = detectionAcceptanceThreshold;
      this.detectionClass = detectionClass;
      this.objectPointCloud = objectPointCloud;
      this.objectCentroid = objectCentroid;
      this.centroidToObjectTransform.set(centroidToObjectTransform);
      this.objectPose = objectPose;
      this.filteredObjectPose = filteredObjectPose;
      this.visualTransformToObjectPose.set(visualTransformToObjectPose);
   }

   public void update()
   {
      objectPose.getTranslation().set(objectCentroid);
      objectPose.appendTransform(centroidToObjectTransform);

      if (!filteredObjectPose.hasRotation())
         filteredObjectPose.getRotation().set(objectPose.getRotation());

      filteredObjectPose.interpolate(objectPose, alpha);
      getNodeToParentFrameTransform().set(filteredObjectPose);
      getNodeFrame().update();
   }


   public int getMaskErosionKernelRadius()
   {
      return maskErosionKernelRadius;
   }

   public void setMaskErosionKernelRadius(int maskErosionKernelRadius)
   {
      this.maskErosionKernelRadius = maskErosionKernelRadius;
   }

   public double getOutlierFilterThreshold()
   {
      return outlierFilterThreshold;
   }

   public void setOutlierFilterThreshold(double outlierFilterThreshold)
   {
      this.outlierFilterThreshold = outlierFilterThreshold;
   }

   public float getDetectionAcceptanceThreshold()
   {
      return detectionAcceptanceThreshold;
   }

   public void setDetectionAcceptanceThreshold(float detectionAcceptanceThreshold)
   {
      this.detectionAcceptanceThreshold = detectionAcceptanceThreshold;
   }

   public YOLOv8DetectionClass getDetectionClass()
   {
      return detectionClass;
   }

   public void setDetectionClass(YOLOv8DetectionClass detectionClass)
   {
      this.detectionClass = detectionClass;
   }

   public List<Point3D32> getObjectPointCloud()
   {
      return objectPointCloud;
   }

   public void setObjectPointCloud(List<Point3D32> objectPointCloud)
   {
      this.objectPointCloud = objectPointCloud;
   }

   public Point3D32 getObjectCentroid()
   {
      return objectCentroid;
   }

   public void setObjectCentroid(Point3D32 objectCentroid)
   {
      this.objectCentroid = objectCentroid;
   }

   public RigidBodyTransform getCentroidToObjectTransform()
   {
      return centroidToObjectTransform;
   }

   public void setCentroidToObjectTransform(RigidBodyTransformBasics centroidToObjectTransform)
   {
      this.centroidToObjectTransform.set(centroidToObjectTransform);
   }

   public Pose3D getObjectPose()
   {
      return objectPose;
   }

   public void setObjectPose(Pose3D objectPose)
   {
      this.objectPose = objectPose;
   }

   public Pose3D getFilteredObjectPose()
   {
      return filteredObjectPose;
   }

   public void setFilteredObjectPose(Pose3D filteredObjectPose)
   {
      this.filteredObjectPose = filteredObjectPose;
   }

   public RigidBodyTransform getVisualTransformToObjectPose()
   {
      return visualTransformToObjectPose;
   }

   public void setVisualTransformToObjectPose(RigidBodyTransformBasics visualTransformToObjectPose)
   {
      this.visualTransformToObjectPose.set(visualTransformToObjectPose);
   }

   public double getAlpha()
   {
      return alpha;
   }

   public void setAlpha(double alpha)
   {
      this.alpha = alpha;
   }

}
