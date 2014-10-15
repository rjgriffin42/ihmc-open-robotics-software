package us.ihmc.utilities.ros;



import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packetAnnotations.ClassAnnotation;
import us.ihmc.communication.packetAnnotations.FieldAnnotation;
import us.ihmc.communication.packets.dataobjects.RobotConfigurationData;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.communication.packets.sensing.LookAtPacket;
import us.ihmc.communication.packets.walking.ChestOrientationPacket;
import us.ihmc.communication.packets.walking.ComHeightPacket;
import us.ihmc.communication.packets.walking.FootPosePacket;
import us.ihmc.communication.packets.walking.FootStatePacket;
import us.ihmc.communication.packets.walking.FootstepData;
import us.ihmc.communication.packets.walking.FootstepDataList;
import us.ihmc.communication.packets.walking.FootstepStatus;
import us.ihmc.communication.packets.walking.HeadOrientationPacket;
import us.ihmc.communication.packets.walking.PauseCommand;
import us.ihmc.communication.packets.walking.PelvisPosePacket;
import us.ihmc.utilities.ros.msgToPacket.IHMCMessageMap;

public class ROSMessageGenerator
{
   private static String messageFolder = ("../ROSJavaBootstrap/ROSMessagesAndServices/ihmc_msgs/msg/").replace("/", File.separator);
   boolean overwriteSubMessages;

   public ROSMessageGenerator(Boolean overwriteSubMessages)
   {
      this.overwriteSubMessages = overwriteSubMessages;
   }

   public static void main(String... args)
   {
      ROSMessageGenerator messageGenerator = new ROSMessageGenerator(true);
      for (Class clazz : IHMCMessageMap.PACKET_LIST)
      {
         messageGenerator.createNewRosMessage(clazz, true);
      }
   }

   public String createNewRosMessage(Class clazz, boolean overwrite)
   {
      if (clazz == null)
      {
         return "";
      }

      File file = new File(messageFolder);
      if (!file.exists())
      {
         file.mkdirs();
      }

      String messageName = clazz.getSimpleName() + "Message";
      File messageFile = new File((messageFolder + File.separator + messageName + ".msg"));

      if (overwrite ||!messageFile.exists())
      {
         messageFile.delete();

         try
         {
            messageFile.createNewFile();
            System.out.println("Message Created: " + messageFile.getName());
            PrintStream fileStream = new PrintStream(messageFile);

            String outBuffer = "## " + messageName + System.lineSeparator();
            ClassAnnotation annotation = (ClassAnnotation) clazz.getAnnotation(ClassAnnotation.class);
            if (annotation != null)
            {
            	String[] annotationString = annotation.documentation().split("\r?\n|\r");
         	   for(String line : annotationString)
         	   {
         		   outBuffer += "# " + line + System.lineSeparator();
         	   }
            }
            else
            {
               outBuffer += "# No Documentation Annotation Found" + System.lineSeparator();
            }

            outBuffer += System.lineSeparator();

            Field[] fields = clazz.getFields();
            for (Field field : fields)
            {
               outBuffer += printType(field);
               outBuffer += " " + field.getName() + System.lineSeparator();
            }

            fileStream.println(outBuffer);
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }

      return messageName;
   }

   private String printType(Field field)
   {
	   String buffer = "";
	   FieldAnnotation fieldAnnotation = field.getAnnotation(FieldAnnotation.class);
	   if(fieldAnnotation != null)
	   {
		   String[] annotationString = fieldAnnotation.fieldDocumentation().split("\r?\n|\r");
		   for(String line : annotationString)
		   {
			   buffer += "# " + line + System.lineSeparator();
		   }
	   }
	   
      if (List.class.isAssignableFrom(field.getType()))
      {
         Class genericType = ((Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
         buffer += printType(genericType);
         buffer += "[]";
      }
      else
      {
         buffer += printType(field.getType());
      }

      return buffer;
   }

   private String printType(Class clazz)
   {
      String buffer = "";
      if (clazz == null)
      {
         return buffer;
      }

      if (clazz.isArray())
      {
         buffer += printType(clazz.getComponentType());
         buffer += "[]";
      }
      else if (clazz.isEnum())
      {
         Object[] enumList = clazz.getEnumConstants();
         buffer += "#Options for enum" + System.lineSeparator();

         for (int i = 0; i < enumList.length; i++)
         {
            buffer += "# uint8";
            buffer += " " + enumList[i];
            buffer += " = " + i + System.lineSeparator();
         }

         buffer += "uint8";
      }
      else if (clazz.equals(byte.class) || clazz.equals(Byte.class))
      {
         buffer += "int8";
      }
      else if (clazz.equals(short.class) || clazz.equals(Short.class))
      {
         buffer += "int16";
      }
      else if (clazz.equals(int.class) || clazz.equals(Integer.class))
      {
         buffer += "int32";
      }
      else if (clazz.equals(long.class) || clazz.equals(Long.class))
      {
         buffer += "int64";
      }
      else if (clazz.equals(float.class) || clazz.equals(Float.class))
      {
         buffer += "float32";
      }
      else if (clazz.equals(double.class) || clazz.equals(Double.class))
      {
         buffer += "float64";
      }
      else if (clazz.equals(boolean.class) || clazz.equals(Boolean.class))
      {
         buffer += "bool";
      }
      else if (clazz.equals(char.class) || clazz.equals(Character.class))
      {
         buffer += "uint8";
      }
      else if (clazz.equals(String.class))
      {
         buffer += "string";
      }

      else if (clazz.equals(Quat4d.class))
      {
         buffer += "geometry_msgs/Quaternion";
      }
      else if (clazz.equals(Point3d.class) || (clazz.equals(Vector3d.class)))
      {
         buffer += "geometry_msgs/Vector3";
      }
      else
      {
         buffer += createNewRosMessage(clazz, overwriteSubMessages);
      }

      return buffer;
   }
   
//   private void printAnnotation(String annotaionString, String printBuffer)
//   {
//	   String[] annotationString = annotaionString.split("\r?\n|\r");
//	   for(String line : annotationString)
//	   {
//		   printBuffer += "# " + line + System.lineSeparator();
//	   }
//   }
}
