package us.ihmc.tools.io;

import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.commons.nio.PathTools;
import us.ihmc.log.LogTools;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class WorkspacePathTools
{
   /**
    * Use this method when applications are being run from source and need to access a project file.
    *
    * This method will find the directoryNameToAssumePresent and then traverse that path to find
    * the file system path to a resource.
    *
    * @param directoryNameToAssumePresent i.e. ihmc-open-robotics-software
    * @param subsequentPathToResourceFolder i.e. src/main/resources, or ihmc-java-toolkit/src/main/resources
    * @param resourcePathString i.e. us/ihmc/someResource.txt
    * @return absolute, normalized path to that directory, or null if fails
    */
   public static Path findPathToResource(String directoryNameToAssumePresent, String subsequentPathToResourceFolder, String resourcePathString)
   {
      Path directoryInline = PathTools.findDirectoryInline(directoryNameToAssumePresent);
      if (directoryInline != null)
         return directoryInline.resolve(subsequentPathToResourceFolder).resolve(resourcePathString).toAbsolutePath().normalize();
      return null;
   }

   /**
    * Use this method when applications are being run from source and need to access a project file.
    *
    * This method searches for a directory in the path parts of the current working directory and
    * will also find it if it is a child of the current working directory.
    *
    * For example, if directoryNameToFind is ihmc-open-robotics-software and the current working directory is
    * ihmc-open-robotics-software/ihmc-java-toolkit/src, this will return the absolute path to ihmc-open-robotics-software.
    *
    * TODO: IHMC Commons's PathTools#findDirectoryInline is supposed to do this. Switch to that.
    *
    * @param directoryNameToFind
    * @return absolute path to that directory, or null if fails
    */
   public static Path handleWorkingDirectoryFuzziness(String directoryNameToFind)
   {
      Path absoluteWorkingDirectory = getWorkingDirectory();
      Path pathBuiltFromSystemRoot = Paths.get("/").toAbsolutePath().normalize(); // start with system root

      boolean directoryFound = false;
      for (Path path : absoluteWorkingDirectory)
      {
         pathBuiltFromSystemRoot = pathBuiltFromSystemRoot.resolve(path); // building up the path

         if (path.toString().equals(directoryNameToFind))
         {
            directoryFound = true;
            break;
         }
      }

      if (!directoryFound && Files.exists(pathBuiltFromSystemRoot.resolve(directoryNameToFind))) // working directory is workspace
      {
         pathBuiltFromSystemRoot = pathBuiltFromSystemRoot.resolve(directoryNameToFind);
         directoryFound = true;
      }

      if (!directoryFound)
      {
         LogTools.warn("{} directory could not be found. Working directory: {} Search stopped at: {}",
                       directoryNameToFind,
                       absoluteWorkingDirectory,
                       pathBuiltFromSystemRoot);
         return null;
      }

      return pathBuiltFromSystemRoot;
   }

   /**
    * Get the working directory, absolute and normalized.
    */
   public static Path getWorkingDirectory()
   {
      return Paths.get(".").toAbsolutePath().normalize();
   }

   /**
    * Uses Java's security functionality to find the source code path and
    * use it, along with the current working directory, to figure out where
    * the resources directory is, if possible. This method probably works
    * for most of our use cases, which is just to save configuration files
    * to version control from applications as we are developing them.
    */
   public static WorkingDirectoryPathComponents inferWorkingDirectoryPathComponents(Class<?> classForLoading)
   {
      WorkingDirectoryPathComponents inferredPathComponents = null;
      ProtectionDomain protectionDomain;
      try
      {
         protectionDomain = classForLoading.getProtectionDomain();
         CodeSource codeSource = protectionDomain.getCodeSource();
         URL location = codeSource == null ? null : codeSource.getLocation();
         // Going through URI is required to support Windows
         URI locationURI = location == null ? null : ExceptionTools.handle(location::toURI, DefaultExceptionHandler.MESSAGE_AND_STACKTRACE);
         if (locationURI != null && !location.getPath().isEmpty())
         {
            Path codeSourceDirectory = Paths.get(locationURI);
            LogTools.debug("Code source directory: {}", codeSourceDirectory);
            Path workingDirectory = WorkspacePathTools.getWorkingDirectory();
            LogTools.debug("Working directory: {}", workingDirectory);

            if (codeSourceDirectory.getNameCount() < 1 || workingDirectory.getNameCount() < 1
             || !codeSourceDirectory.getName(0).toString().equals(workingDirectory.getName(0).toString()))
            {
               throw new RuntimeException("""
               The code source directory and working directory need to share a common root.
                  Code source path: %s
                  Working directory: %s
               """.formatted(codeSourceDirectory, workingDirectory));
            }

            int lastIndexOfSrc = -1;
            for (int nameElementIndex = 0; nameElementIndex < codeSourceDirectory.getNameCount(); nameElementIndex++)
            {
               if (codeSourceDirectory.getName(nameElementIndex).toString().equals("src"))
               {
                  lastIndexOfSrc = nameElementIndex;
               }
            }

            if (lastIndexOfSrc >= 0)
            {
               // Add 2 to keep 'src' and the source set part after 'src' i.e. ../src/main
               // i.e This removes out/production/classes from [...]/src/main/out/production/classes
               Path pathBeforeResources = codeSourceDirectory.subpath(0, lastIndexOfSrc + 2);
               LogTools.debug("Path before resources: {}", pathBeforeResources);

               Path pathWithResources = pathBeforeResources.resolve("resources").normalize();
               LogTools.debug("Path with resources: {}", pathWithResources);

               // We go through both working directory and code source directory to find the common part
               int afterLastCommonPathElement = -1;
               for (int pathNameElement = 0; pathNameElement < pathWithResources.getNameCount()
                                          && pathNameElement < workingDirectory.getNameCount(); pathNameElement++)
               {
                  if (pathWithResources.getName(pathNameElement).toString().equals(workingDirectory.getName(pathNameElement).toString()))
                  {
                     // We add 1 here, to make sure the directory to assume present
                     // is closer to the 'src' folder, which is what the other tools assume right now.
                     afterLastCommonPathElement = pathNameElement + 1;
                  }
               }

               // Sometimes the working directory is set to within the src folder heirarchy.
               // In that case, let's back out and make the parent of 'src' our directory to assume present.
               if (afterLastCommonPathElement >= lastIndexOfSrc)
               {
                  afterLastCommonPathElement = lastIndexOfSrc - 1;
               }

               String directoryNameToAssumePresent = pathWithResources.getName(afterLastCommonPathElement).toString();
               int firstElementOfSubsequentPath = afterLastCommonPathElement + 1;
               String subsequentPathToResourceFolder
                     = pathWithResources.subpath(firstElementOfSubsequentPath, pathWithResources.getNameCount()).toString();
               inferredPathComponents = new WorkingDirectoryPathComponents(directoryNameToAssumePresent, subsequentPathToResourceFolder);

               LogTools.info("Inferred workspace directory components:\n Directory name to assume present: {}\n Subsequent path to resource folder: {}",
                             directoryNameToAssumePresent, subsequentPathToResourceFolder);
            }
            else
            {
               LogTools.warn("No src folder found.");
            }
         }
         else
         {
            LogTools.warn("This class is not normally compiled or JAR not normally built.");
         }
      }
      catch (SecurityException securityException) // We never seal JARs or apply security, so this will probably never happen
      {
         LogTools.error(securityException.getMessage());
      }

      return inferredPathComponents;
   }
}