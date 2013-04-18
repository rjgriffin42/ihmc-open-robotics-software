package us.ihmc.darpaRoboticsChallenge.sensors;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import us.ihmc.utilities.dataStructures.hyperCubeTree.Octree;
import us.ihmc.utilities.dataStructures.hyperCubeTree.OneDimensionalBounds;
import us.ihmc.utilities.dataStructures.quadTree.Box;
import us.ihmc.utilities.dataStructures.quadTree.QuadTreeListener;

public class DRCOctree extends Octree implements QuadTreeListener
{

   public DRCOctree(OneDimensionalBounds[] bounds, double resolution)
   {
      super(bounds, resolution);
   }

   public void nodeAdded(String id, Box bounds, float x, float y, float height)
   {
      //ignore
   }

   public void RawPointAdded(float x, float y, float z)
   {
      //ignore
   }

   public void PopToOctree(Point3f location)
   {
      double[] locationArray = new double[3];
      new Point3d(location).get(locationArray);
      this.put(locationArray, true);
   }

   public void PopToOctree(Point3f location, Point3f LidarHeadLocation)
   {
      this.putLidarAtGraduallyMoreAccurateResolution(new Point3d(LidarHeadLocation), new Point3d(location));
   }

}
