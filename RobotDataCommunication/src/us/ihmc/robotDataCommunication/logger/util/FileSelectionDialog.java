package us.ihmc.robotDataCommunication.logger.util;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;

public class FileSelectionDialog
{
   public static File loadDirectoryWithFileNamed(final String acceptedFileName)
   {
      final FileDialog fileDialog = new FileDialog((Frame) null, "Choose logging directory");
      fileDialog.setMode(FileDialog.LOAD);
      
      if(acceptedFileName != null)
      {
         fileDialog.setFilenameFilter(new FilenameFilter()
         {
            
            @Override
            public boolean accept(File dir, String name)
            {
               return (acceptedFileName.equals(name));
            }
         });        
      }
      
      fileDialog.setVisible(true);         
      String filename = fileDialog.getDirectory();
      if(filename != null)
      {
         return new File(filename);
      }
      else
      {
         return null;
      }

   }
}
