package us.ihmc.robotics.dataStructures;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class DecayingResolutionFilter
{
   private double resolution;
   private long decayMillis;
   int capacity = 100000;

   private final TLongObjectHashMap<TimestampedPoint> map;
   //   private final HashMap<Long, TimestampedPoint> map;
   private final LinkedList<TimestampedPoint> list;

   public DecayingResolutionFilter(double resolution, long decayMillis, int maxSizeAllowed)
   {
      this.resolution = resolution;
      this.decayMillis = decayMillis;
      this.capacity = maxSizeAllowed;

      map = new TLongObjectHashMap<TimestampedPoint>();
      list = new LinkedList<TimestampedPoint>();
   }

   public boolean add(Point3d p)
   {
      return add(p.x, p.y, p.z);
   }

   public boolean add(double x, double y, double z)
   {
      return add((float) x, (float) y, (float) z);
   }

   public boolean add(float x, float y, float z)
   {
      long hash = hash(x, y, z, resolution);

      removeDecayedPoints();
      
      if (!map.containsKey(hash))
      {
         TimestampedPoint point = new TimestampedPoint(x, y, z, System.currentTimeMillis());
         synchronized (list)
         {
            atomicAddToMapAndList(hash, point);
         }
         return true;
      }

      return false;
   }

   private synchronized void atomicAddToMapAndList(long hash, TimestampedPoint point)
   {
      map.put(hash, point);
      list.addLast(point);
   }

   private void removeDecayedPoints()
   {
      TimestampedPoint timePoint;

      synchronized (list)
      {
         if (list.size() >= capacity && capacity > 0)
         {
            atomicRemoveFromListAndMap();
         }
      }

      long time = System.currentTimeMillis();
      while ((timePoint = list.peekFirst()) != null)
      {
         if (timePoint.timestamp + decayMillis > time || decayMillis<0)
         {
            return;
         }

         synchronized (list)
         {
            atomicRemoveFromListAndMap();
         }
      }
   }

   private synchronized void atomicRemoveFromListAndMap()
   {
      TimestampedPoint timePoint;
      timePoint = list.removeFirst();
      map.remove(hash(timePoint));
   }

   //================================================================================
   // Private Utility Functions
   //================================================================================

   private long hash(TimestampedPoint point)
   {
      return hash(point.x, point.y, point.z, resolution);
   }

   private static final int bits = 20;
   private static final int mask = 0xFFFFFFFF >>> bits;
   private static final int offset = 1 << (bits - 1);

   private long hash(float x, float y, float z, double res)
   {
      long hash = 0;
      hash += (((int) (x / res) + offset) & mask) << 2 * bits;
      hash += (((int) (y / res) + offset) & mask) << 1 * bits;
      hash += (((int) (z / res) + offset) & mask) << 0 * bits;

      return hash;
   }

   //================================================================================
   // Setters
   //================================================================================

   public void clear()
   {
      synchronized (list)
      {
         map.clear();
         list.clear();
      }
   }

   public void setResolution(double resolution)
   {
      this.resolution = resolution;
   }

   public void setDecay(long decayMillis)
   {
      this.decayMillis = decayMillis;
   }

   public void setCapacity(int capacity)
   {
      this.capacity = capacity;
   }

   //================================================================================
   // Getters
   //================================================================================

   /**
    * Get the points in a new array. The points them self are not copied.
    * @return new Point3f array
    */
   public Point3f[] getPoints3f()
   {
      synchronized (list)
      {
         Point3f[] points = new Point3f[list.size()];

         for (int i = 0; i < list.size(); i++)
         {
            points[i] = list.get(i);
         }
         
         return points;
      }
   }

   public ArrayList<TimestampedPoint> getPointsCopy()
   {
      // Thread-safely copies points into a new array
      ArrayList<TimestampedPoint> copy;

      synchronized (list)
      {
         copy = new ArrayList<TimestampedPoint>(list);
      }

      return copy;
   }

   public Point3f getNearestIntersection(Point3f origin, Vector3f direction)
   {
      ArrayList<TimestampedPoint> points = getPointsCopy();
      direction.normalize();
      float dx, dy, dz, dot;
      double distanceToLine, distance;

      double nearestDistance = Double.POSITIVE_INFINITY;
      Point3f nearestPoint = null;

      for (Point3f p : points)
      {
         dx = origin.x - p.x;
         dy = origin.y - p.y;
         dz = origin.z - p.z;

         dot = dx * direction.x + dy * direction.y + dz * direction.z;

         dx = dx - dot * direction.x;
         dy = dy - dot * direction.y;
         dz = dz - dot * direction.z;

         distanceToLine = Math.sqrt(dx * dx + dy * dy + dz * dz);

         if (distanceToLine < resolution / 2)
         {
            distance = origin.distance(p);
            if (distance < nearestDistance)
            {
               nearestDistance = distance;
               nearestPoint = p;
            }
         }
      }

      return nearestPoint;
   }
}