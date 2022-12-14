package us.ihmc.utilities.ros;

import static us.ihmc.robotics.Assert.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import us.ihmc.communication.ros.generators.RosMessagePacket;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import us.ihmc.utilities.ros.msgToPacket.converter.GenericROSTranslationTools;

/**
 * <p>
 * Notes to future refactorers: There is a reason that this test is not in IHMCROSTools even though it is testing
 * a class that lives there.
 * </p>
 *
 * <p>
 * Because this test works by gathering information from the working java classpath, it must live at a higher level than
 * the packets it is testing.
 * </p>
 *
 * @author Doug Stephen <a href="mailto:dstephen@ihmc.us">(dstephen@ihmc.us)</a>
 */
public class ROSMessageFileCreatorTest
{
   @Test
   public void testAllExportedPacketsWithTopicsAreFormattedCorrectly()
   {
      Set<Class<?>> rosMessagePacketAnnotatedClasses = GenericROSTranslationTools.getIHMCCoreRosMessagePacketAnnotatedClasses();
      for (Class<?> rosMessagePacketAnnotatedClass : rosMessagePacketAnnotatedClasses)
      {
         RosMessagePacket annotation = rosMessagePacketAnnotatedClass.getAnnotation(RosMessagePacket.class);
         String topicString = annotation.topic();
         switch (topicString)
         {
         case RosMessagePacket.NO_CORRESPONDING_TOPIC_STRING:
            break;
         default:
            if (!(topicString.startsWith("/") && topicString.length() > 1))
            {
               fail("Topic string for packet " + rosMessagePacketAnnotatedClass.getSimpleName()
                     + " is not formatted correctly! Topics must start with \"/\" and must be at least one letter!\nIf this packet does not have a topic associated with it, do not set the topic.");
            }
         }
      }
   }
}