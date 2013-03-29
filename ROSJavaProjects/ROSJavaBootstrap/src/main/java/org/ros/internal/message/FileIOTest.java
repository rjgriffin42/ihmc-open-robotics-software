package org.ros.internal.message;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class FileIOTest
{

   @Test
   public void test() throws IOException
   {
      File rootDirectory = new File("/home/graythomas/workspace/ROSJavaProjects/ROSJavaBootstrap/ROSMessagesAndServices");
      System.out.println(rootDirectory.getAbsoluteFile().getCanonicalFile());
      System.out.println(rootDirectory.isDirectory());
      assert(rootDirectory.isDirectory());
   }

}
