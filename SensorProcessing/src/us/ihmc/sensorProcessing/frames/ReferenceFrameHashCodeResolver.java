package us.ihmc.sensorProcessing.frames;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import gnu.trove.map.hash.TLongObjectHashMap;
import us.ihmc.robotModels.FullRobotModel;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.robotSide.RobotSextant;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

/**
 * 
 * This class is Immutable, you shouldn't add reference frames to it except through the constructor or you will be fired... from a cannon.. into the sun
 * 
 * This class keeps track of all the reference frames from the full robot model and reference frames. 
 * It uses reflection to pull all the frames from the full robot model and the reference frames, it looks at all models
 * that return a reference frame or a RobotSide,  robotQuadrant, or RobotSextant. If we get more robot segments we should 
 * switch to using reflection and generics to pull them automatically.
 * 
 * @author bs
 *
 */
public class ReferenceFrameHashCodeResolver
{
   
   private final TLongObjectHashMap<ReferenceFrame> nameBasedHashCodeToReferenceFrameMap = new TLongObjectHashMap<ReferenceFrame>();

   public ReferenceFrameHashCodeResolver(FullRobotModel fullRobotModel, ReferenceFrames referenceFrames)
   {
      checkAndAddReferenceFrame(ReferenceFrame.getWorldFrame());

      try
      {
         referenceFrameExtractor(referenceFrames);
         referenceFrameExtractor(fullRobotModel);
      }
      catch (IllegalAccessException | InvocationTargetException e)
      {
         e.printStackTrace();
      }

      // a little repetitive, but better than recursion
      for (OneDoFJoint joint : fullRobotModel.getOneDoFJoints())
      {
         ReferenceFrame frameBeforeJoint = joint.getFrameBeforeJoint();
         ReferenceFrame frameAfterJoint = joint.getFrameAfterJoint();
         ReferenceFrame comLinkBefore = joint.getPredecessor().getBodyFixedFrame();
         ReferenceFrame comLinkAfter = joint.getSuccessor().getBodyFixedFrame();

         checkAndAddReferenceFrame(frameBeforeJoint);
         checkAndAddReferenceFrame(frameAfterJoint);
         checkAndAddReferenceFrame(comLinkBefore);
         checkAndAddReferenceFrame(comLinkAfter);
      }
   }

   /**
    * uses reflection to get the reference frames
    * @param obj the object to pull from
    * @param clazz the class tyo
    * @throws IllegalAccessException
    * @throws InvocationTargetException
    */
   private void referenceFrameExtractor(Object obj) throws IllegalAccessException, InvocationTargetException
   {
      Class clazz = obj.getClass();
      Method[] declaredMethods = clazz.getMethods();
      for (Method method : declaredMethods)
      {
         if (method.getReturnType() == ReferenceFrame.class)
         {
            if (method.getParameterCount() == 0)
            {
               ReferenceFrame referenceFrame = (ReferenceFrame) method.invoke(obj);
               checkAndAddReferenceFrame(referenceFrame);
            }
            else if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == RobotSide.class)
            {
               for (RobotSide robotSide : RobotSide.values)
               {
                  ReferenceFrame referenceFrame = (ReferenceFrame) method.invoke(obj, robotSide);
                  checkAndAddReferenceFrame(referenceFrame);
               }
            }
            else if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == RobotQuadrant.class)
            {
               for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
               {
                  ReferenceFrame referenceFrame = (ReferenceFrame) method.invoke(obj, robotQuadrant);
                  checkAndAddReferenceFrame(referenceFrame);
               }
            }
            else if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == RobotSextant.class)
            {
               for (RobotSextant robotSextant : RobotSextant.values)
               {
                  ReferenceFrame referenceFrame = (ReferenceFrame) method.invoke(obj, robotSextant);
                  checkAndAddReferenceFrame(referenceFrame);
               }
            }
         }
      }
   }
   
   /**
    * Check to see if the map already has a frame under the hashcode. If so check if the two frames are the same, if not throw an exception. 
    * @param referenceFrame
    */
   private void checkAndAddReferenceFrame(ReferenceFrame referenceFrame)
   {
      if(nameBasedHashCodeToReferenceFrameMap.containsKey(referenceFrame.getNameBasedHashCode()))
      {
         ReferenceFrame existingFrame = nameBasedHashCodeToReferenceFrameMap.get(referenceFrame.getNameBasedHashCode());
         if(!referenceFrame.equals(existingFrame))
         {
            throw new IllegalArgumentException("ReferenceFrameHashCodeResolver: Tried to put in a reference frame with the same name");
         }
         return;
      }
      nameBasedHashCodeToReferenceFrameMap.put(referenceFrame.getNameBasedHashCode(), referenceFrame);
   }

   public ReferenceFrame getReferenceFrameFromNameBaseHashCode(long nameBasedHashCode)
   {
      return nameBasedHashCodeToReferenceFrameMap.get(nameBasedHashCode);
   }

   public Collection<ReferenceFrame> getAllReferenceFrames()
   {
      return nameBasedHashCodeToReferenceFrameMap.valueCollection();
   }
}
