package us.ihmc.robotics.lists;

import java.util.List;

import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameTuple;
import us.ihmc.robotics.geometry.FrameVector;

public class FrameTupleArrayList<T extends FrameTuple<?>> extends RecyclingArrayList<T>
{
   private FrameTupleArrayList(Class<T> clazz)
   {
      super(clazz);
   }

   private FrameTupleArrayList(int initialSize, Class<T> clazz)
   {
      super(initialSize, clazz);
   }

   public void setOrCreate(int i, FrameTuple<?> frameTuple)
   {
      if (i >= size)
      {
         size = i + 1;
         ensureCapacity(size);
      }
      unsafeGet(i).setIncludingFrame(frameTuple);
   }

   public void set(int i, FrameTuple<?> frameTuple)
   {
      get(i).setIncludingFrame(frameTuple);
   }

   private void unsafeSet(int i, FrameTuple<?> frameTuple)
   {
      unsafeGet(i).setIncludingFrame(frameTuple);
   }

   public void copyFromListAndTrimSize(FrameTupleArrayList<?> otherList)
   {
      ensureCapacity(otherList.size());
      size = otherList.size;

      for (int i = 0; i < size; i++)
      {
         unsafeSet(i, otherList.unsafeGet(i));
      }
   }

   public void copyFromListAndTrimSize(List<? extends FrameTuple<?>> otherList)
   {
      ensureCapacity(otherList.size());
      size = otherList.size();

      for (int i = 0; i < size; i++)
      {
         unsafeSet(i, otherList.get(i));
      }
   }

   public static FrameTupleArrayList<FramePoint> createFramePointArrayList()
   {
      return new FrameTupleArrayList<>(FramePoint.class);
   }

   public static FrameTupleArrayList<FrameVector> createFrameVectorArrayList()
   {
      return new FrameTupleArrayList<>(FrameVector.class);
   }

   public static FrameTupleArrayList<FramePoint> createFramePointArrayList(int initialSize)
   {
      return new FrameTupleArrayList<>(initialSize, FramePoint.class);
   }

   public static FrameTupleArrayList<FrameVector> createFrameVectorArrayList(int initialSize)
   {
      return new FrameTupleArrayList<>(initialSize, FrameVector.class);
   }
}
