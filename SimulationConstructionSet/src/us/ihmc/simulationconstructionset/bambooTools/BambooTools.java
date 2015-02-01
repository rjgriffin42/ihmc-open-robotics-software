package us.ihmc.simulationconstructionset.bambooTools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.utilities.DateTools;
import us.ihmc.utilities.code.unitTesting.youtube.YouTubeCredentials;
import us.ihmc.utilities.code.unitTesting.youtube.YouTubeUploader;
import us.ihmc.utilities.gui.GUIMessageFrame;
import us.ihmc.utilities.operatingSystem.OperatingSystemTools;

public class BambooTools
{
   private final static String[] possibleRootDirectoriesForBambooDataAndVideos = new String[]{"C:/BambooDataAndVideos/", "D:/BambooDataAndVideos/",
         "../BambooDataAndVideos/"};

   private final static String eraseableBambooDataAndVideosDirectoryLinux = "/media/GoFlex/EraseableBambooDataAndVideos/";
   private final static String eraseableBambooDataAndVideosDirectoryWindows = "X:/EraseableBambooDataAndVideos/";

   private static final String UPLOADED_VIDEOS_LOG = "uploaded-videos.log";

   private static boolean WRITE_LOG_FILE_ON_SUCCESS = false;

   public static boolean isRunningOnBamboo()
   {
      String buildType = System.getProperty("build.type");
      if (buildType != null)
         return true;

      return false;
   }

   public static boolean isNightlyBuild()
   {
      String buildType = System.getProperty("build.type");

      return ((buildType != null) && (buildType.equals("nightly")));
   }

   public static boolean doMovieCreation()
   {
      return Boolean.parseBoolean(System.getProperty("create.movies"));
   }
   
   public static boolean getShowSCSWindows()
   {
      return Boolean.parseBoolean(System.getProperty("show.scs.windows"));
   }

   /**
    * If you set {@code upload.movies} to {@code true} you must also set the system properties
    * defined in {@link YouTubeCredentials}.
    */
   public static boolean doMovieUpload()
   {
      return Boolean.parseBoolean(System.getProperty("upload.movies"));
   }

   public static boolean getCheckNothingChanged()
   {
      return Boolean.parseBoolean(System.getProperty("check.nothing.changed"));
   }

   public static boolean isEveryCommitBuild()
   {
      String buildType = System.getProperty("build.type");

      return ((buildType != null) && (buildType.equals("everyCommit")));
   }

   public static boolean isIsolatedBuild()
   {
      String buildType = System.getProperty("build.type");

      return ((buildType != null) && (buildType.equals("isolated")));
   }

   public static void createMovieAndDataWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(String simplifiedRobotModelName, SimulationConstructionSet scs)
   {
      createMovieAndDataWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(simplifiedRobotModelName, scs, 1);
   }

   public static void createMovieAndDataWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(String simplifiedRobotModelName, SimulationConstructionSet scs,
                                                                                                int additionalStackDepthForRelevantCallingMethod)
   {
      try
      {
         String rootDirectoryToUse = determineEraseableBambooDataAndVideosRootDirectoryToUse();

         if (rootDirectoryToUse == null)
         {
            reportErrorMessage("Couldn't find a BambooDataAndVideos directory (for share drive)!");

            return;
         }

         reportOutMessage("Automatically creating video and data and saving to " + rootDirectoryToUse);

         createMovieAndDataWithDateTimeClassMethod(rootDirectoryToUse, simplifiedRobotModelName, scs, additionalStackDepthForRelevantCallingMethod + 2);
      } catch (Throwable t)
      {
         reportErrorMessage("createMovieAndData failed with " + t.toString());
         t.printStackTrace();
         System.err.flush();
         if (t instanceof Error)
         {
            throw (Error) t;
         } else
         {
            throw (RuntimeException) t;
         }
      }
   }

   public static void createMovieAndDataWithDateTimeClassMethodAndCheckIntoSVN(String simplifiedRobotModelName, SimulationConstructionSet scs)
   {
      String rootDirectoryToUse = determineBambooDataAndVideosRootDirectoryToUse();

      if (rootDirectoryToUse == null)
      {
         reportErrorMessage("Couldn't find a BambooDataAndVideos directory (for SVN)!");

         return;
      }

      reportOutMessage("Automatically creating video and data and saving to " + rootDirectoryToUse);

      File[] files = createMovieAndDataWithDateTimeClassMethod(rootDirectoryToUse, simplifiedRobotModelName, scs, 2);

      File rootDirectoryFile = new File(rootDirectoryToUse);
      updateSVN(rootDirectoryFile);

      for (File file : files)
      {
         addToSVN(file);
      }

      updateSVN(rootDirectoryFile);

      commitToSVN(files);
   }

   private static String determineBambooDataAndVideosRootDirectoryToUse()
   {
      String rootDirectoryToUse = null;

      for (String rootDirectoryToTry : possibleRootDirectoriesForBambooDataAndVideos)
      {
         File rootFile = new File(rootDirectoryToTry);
         if (rootFile.exists())
         {
            rootDirectoryToUse = rootDirectoryToTry;

            break;
         }
      }

      return rootDirectoryToUse;
   }

   private static String determineEraseableBambooDataAndVideosRootDirectoryToUse()
   {
      String rootDirectoryToTry = System.getProperty("create.movies.dir");
      if (rootDirectoryToTry == null)
      {
         if (OperatingSystemTools.isWindows())
         {
            rootDirectoryToTry = eraseableBambooDataAndVideosDirectoryWindows;
         } else
         {
            rootDirectoryToTry = eraseableBambooDataAndVideosDirectoryLinux;
         }
      }

      if (new File(rootDirectoryToTry).exists())
      {
         return rootDirectoryToTry;
      } 
      else if (doMovieUpload())
      {
         // if we're going to upload the movies, we can just use the tmp dir if
         // the other specified directories don't exist
         System.out.println("Saving movies to tmp dir before uploading..");

         String tmpDir = System.getProperty("java.io.tmpdir");
         File movieDir = new File(tmpDir, "atlas-movies");
         if (movieDir.exists() || movieDir.mkdirs())
         {
            System.out.println("Using " + movieDir.getAbsolutePath());
            return movieDir.getAbsolutePath();
         }
         System.err.println("Couldn't create directory: " + movieDir.getAbsolutePath());
      }
      
      return determineBambooDataAndVideosRootDirectoryToUse();
   }

   public static File getSVNDirectoryWithMostRecentBambooDataAndVideos()
   {
      String rootDirectory = determineBambooDataAndVideosRootDirectoryToUse();

      return getDirectoryWithMostRecentBambooDataAndVideos(rootDirectory);
   }

   public static File getEraseableDirectoryWithMostRecentBambooDataAndVideos()
   {
      String rootDirectory = determineEraseableBambooDataAndVideosRootDirectoryToUse();

      return getDirectoryWithMostRecentBambooDataAndVideos(rootDirectory);
   }

   public static File getDirectoryWithMostRecentBambooDataAndVideos(String rootDirectory)
   {
      if (rootDirectory == null)
         return null;

      File file = new File(rootDirectory);

      FileFilter fileFilter = new FileFilter()
      {
         public boolean accept(File file)
         {
            if (!file.isDirectory())
               return false;
            String fileName = file.getName();

            String regex = "\\d\\d\\d\\d\\d\\d\\d\\d";
            if (fileName.matches(regex))
               return true;

            return false;
         }

      };

      File[] timeStampedDirectories = file.listFiles(fileFilter);
      if (timeStampedDirectories == null)
         return null;
      if (timeStampedDirectories.length == 0)
         return null;

      Comparator<File> fileAlphabeticalComparator = createFileAlphabeticalComparator();
      Arrays.sort(timeStampedDirectories, fileAlphabeticalComparator);

      return timeStampedDirectories[0];
   }

   public static Comparator<File> createFileAlphabeticalComparator()
   {
      Comparator<File> fileAlphabeticalComparator = new Comparator<File>()
      {
         public int compare(File file1, File file2)
         {
            String name1 = file1.getName();
            String name2 = file2.getName();

            return name2.compareTo(name1);
         }

      };

      return fileAlphabeticalComparator;
   }

   public static long updateLocalBambooDataAndVideos()
   {
      String rootDirectoryToUse = determineBambooDataAndVideosRootDirectoryToUse();

      if (rootDirectoryToUse == null)
      {
         System.err.println("Couldn't find a BambooDataAndVideos directory (for updating)!");

         return -1;
      }

      File rootDirectoryFile = new File(rootDirectoryToUse);

      long updateReturnValue = updateSVN(rootDirectoryFile);

      return updateReturnValue;
   }

   private static File[] createMovieAndDataWithDateTimeClassMethod(String rootDirectory, String simplifiedRobotModelName, SimulationConstructionSet scs)
   {
      return createMovieAndDataWithDateTimeClassMethod(rootDirectory, simplifiedRobotModelName, scs, 3);
   }

   private static File[] createMovieAndDataWithDateTimeClassMethod(String rootDirectory, String simplifiedRobotModelName, SimulationConstructionSet scs, int stackDepthForRelevantCallingMethod)
   {
      String dateString = DateTools.getDateString();
      String directoryName = rootDirectory + dateString + "/";

      File directory = new File(directoryName);
      if (!directory.exists())
      {
         directory.mkdir();
      }

      String classAndMethodName = getClassAndMethodName(stackDepthForRelevantCallingMethod);

      String timeString = DateTools.getTimeString();
      String filenameStart = dateString + "_" + timeString;
      if (!simplifiedRobotModelName.equals(""))
      {
         filenameStart += "_" + simplifiedRobotModelName;
      }
      filenameStart += "_" + classAndMethodName;
      String movieFilename = filenameStart + ".mp4";

//    String movieFilename = filenameStart + ".mov";
      File movieFile = scs.createMovie(directoryName + movieFilename);

      String dataFilename = directoryName + filenameStart + ".data.gz";

      File dataFile = new File(dataFilename);

      try
      {
         scs.writeData(dataFile);
      } 
      catch (Exception e)
      {
         System.err.println("Error in writing data file in BambooTools.createMovieAndDataWithDateTimeClassMethod()");
         e.printStackTrace();
      }

      if (doMovieUpload())
      {
         uploadMovieToYouTube(movieFile, "video/mp4", movieFilename);
      }

      scs.gotoOutPointNow();

      return new File[]{directory, movieFile, dataFile};
   }

   private static void uploadMovieToYouTube(File movieFile, String mimeType, String movieFilename)
   {
      try
      {
         YouTubeCredentials credentials = new YouTubeCredentials();
         YouTubeUploader youTubeUploader = new YouTubeUploader(credentials.getUsername(), credentials.getPassword(), credentials.getDeveloperKey());
         String videoUrl = youTubeUploader.uploadVideo(movieFile, mimeType, movieFilename);
         writeVideoUrlToVideoLog(movieFilename, videoUrl);
      } catch (UnknownHostException e)
      {
         System.err.println("Failed to resolve host for YouTube, is the internet down?");
         throw new RuntimeException(e);
      } catch (Exception e)
      {
         throw new RuntimeException("Failed to upload video to YouTube!", e);
      }
   }

   private static void writeVideoUrlToVideoLog(String fileName, String videoUrl)
   {
      String artifactsOut = System.getProperty("artifacts.out");
      if (artifactsOut == null)
      {
         System.err.println("No artifacts.out system property set, not logging video url.");
         return;
      }

      // create artifacts dir if it doesn't already exist
      new File(artifactsOut).mkdirs();

      File file = new File(artifactsOut, UPLOADED_VIDEOS_LOG);
      try
      {
         OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file, true));
         out.write(videoUrl + " (" + fileName + ")\r\n");
         out.flush();
         out.close();
      } catch (IOException e)
      {
         System.err.println("Failed to write video url to " + file.getAbsolutePath() + "\n" + e.toString());
      }
   }

   public static String getClassAndMethodName()
   {
      return getClassAndMethodName(1);
   }

   public static String getClassAndMethodName(int stackDepthZeroIfCallingMethod)
   {
      int stackDepthForRelevantCallingMethod = stackDepthZeroIfCallingMethod + 2;

      StackTraceElement[] elements = Thread.currentThread().getStackTrace();
      String methodName = elements[stackDepthForRelevantCallingMethod].getMethodName();
      String className = elements[stackDepthForRelevantCallingMethod].getClassName();
      className = className.substring(className.lastIndexOf('.') + 1);

      String classAndMethodName = className + "." + methodName;

      return classAndMethodName;
   }

   private static long updateSVN(File directoryToUpdate)
   {
      reportOutMessage("Updating SVN for " + directoryToUpdate);
      DAVRepositoryFactory.setup();
      SVNClientManager clientManager = SVNClientManager.newInstance();
      SVNUpdateClient updateClient = clientManager.getUpdateClient();

      try
      {
         long updateReturnValue = updateClient.doUpdate(directoryToUpdate, SVNRevision.HEAD, SVNDepth.INFINITY, true, false);

         return updateReturnValue;
      } catch (SVNException e)
      {
         return -1;
      }
   }

   private static void addToSVN(File fileToAdd)
   {
      try
      {
         boolean force = false;
         boolean mkdir = false;    // this must be false to work
         boolean climbUnversionedParents = false;
         boolean includeIgnored = false;
         boolean makeParents = true;

         DAVRepositoryFactory.setup();
         SVNClientManager clientManager = SVNClientManager.newInstance();
         SVNWCClient wcClient = clientManager.getWCClient();

         wcClient.doAdd(fileToAdd, force, mkdir, climbUnversionedParents, SVNDepth.EMPTY, includeIgnored, makeParents);

         String logMessage = "Successfully Added (I think) " + fileToAdd.getPath() + " to SVN";
         reportOutMessage(logMessage);

         String logFilename = fileToAdd.getPath() + ".successAddingToSVNLog";
         writeSuccessLogFile(logMessage, logFilename);
      } catch (SVNException exception)
      {
         // Will get exceptions on the doAdd above if it already exists, but not sure how to check first, so I'm just adding it every time.

         reportErrorMessage("Exception adding " + fileToAdd.getPath() + " to SVN! \n" + exception);
         String logFilename = fileToAdd.getPath() + ".errorAddingToSVNLog";
         writeErrorLogFile(exception, logFilename);
      }
   }

   private static void commitToSVN(File[] filesToCommit)
   {
      try
      {
         boolean keepLocks = false;
         String commitMessage = "Auto generated from Bamboo run.";
         SVNProperties revisionProperties = null;
         String[] changeLists = null;
         boolean keepChangeList = false;
         boolean force = false;

         DAVRepositoryFactory.setup();
         SVNClientManager clientManager = SVNClientManager.newInstance();

         SVNCommitClient commitClient = clientManager.getCommitClient();
         SVNCommitInfo commitInfo = commitClient.doCommit(filesToCommit, keepLocks, commitMessage, revisionProperties, changeLists, keepChangeList, force,
               SVNDepth.EMPTY);

         reportOutMessage(commitInfo.toString() + ": Successfully commited to SVN!");

         String logFilename = filesToCommit[0].getPath() + ".successCommittingToSVNLog";
         writeSuccessLogFile(commitInfo.toString(), logFilename);
      } catch (SVNException exception)
      {
         reportErrorMessage(filesToCommit[0].getPath() + " Couldn't commit to SVN! \n " + exception);

         // Will get exceptions on the doCommit above things aren't set up correctly. Log a file if so.
         String logFilename = filesToCommit[0].getPath() + ".errorCommittingToSVNLog";
         writeErrorLogFile(exception, logFilename);
      }
   }

   private static void writeSuccessLogFile(String successString, String logFilename)
   {
      if (!WRITE_LOG_FILE_ON_SUCCESS)
         return;

      System.out.println("Writing " + successString + " to log file " + logFilename);
      File logFile = new File(logFilename);

      try
      {
         FileOutputStream logStream = new FileOutputStream(logFile);
         PrintStream printStream = new PrintStream(logStream);

         printStream.println(successString);
         printStream.close();

         System.out.println("Done writing " + successString + " to log file " + logFilename);

      } catch (FileNotFoundException e1)
      {
         System.out.println("FileNotFoundException! File = " + logFile);
      }
   }

   private static void writeErrorLogFile(Exception exception, String logFilename)
   {
      System.out.println("Writing error log to log file " + logFilename + ". Exception was " + exception);

      File logFile = new File(logFilename);

      try
      {
         FileOutputStream logStream = new FileOutputStream(logFile);
         PrintStream printStream = new PrintStream(logStream);

         exception.printStackTrace(printStream);
         printStream.close();
      } catch (FileNotFoundException e1)
      {
      }
   }

   public static int garbageCollectAndGetUsedMemoryInMB()
   {
      Runtime runtime = Runtime.getRuntime();

      System.gc();
      sleep(100);
      System.gc();

      long freeMemory = runtime.freeMemory();
      long totalMemory = runtime.totalMemory();
      long usedMemory = totalMemory - freeMemory;

      int usedMemoryMB = (int) (usedMemory / 1000000);

      return usedMemoryMB;
   }

   private static void sleep(long sleepMillis)
   {
      try
      {
         Thread.sleep(sleepMillis);
      } catch (InterruptedException e)
      {
      }
   }

   private static GUIMessageFrame guiMessageFrame;
   private static int junitTestCasesIndex;

   public static void logMessagesToFile(String filename)
   {
      if (guiMessageFrame == null)
         return;

      File file = new File(filename);
      logMessagesToFile(file);
   }

   public static void logMessagesToFile(File file)
   {
      if (guiMessageFrame == null)
         return;

      guiMessageFrame.save(file);
   }

   public static void logMessagesAndCheckInToBambooDataAndVideos(String filename)
   {
      String rootDirectoryToUse = determineBambooDataAndVideosRootDirectoryToUse();

      String logFilename = rootDirectoryToUse + DateTools.getDateString() + "_" + DateTools.getTimeString() + "_" + filename;
      File file = new File(logFilename);
      logMessagesToFile(file);

      addToSVN(file);
      commitToSVN(new File[]{file});
   }

   public static void logMessagesAndShareOnSharedDriveIfAvailable(String filename)
   {
      String rootDirectoryToUse = determineEraseableBambooDataAndVideosRootDirectoryToUse();

      String logFilename = rootDirectoryToUse + DateTools.getDateString() + "_" + DateTools.getTimeString() + "_" + filename;
      File file = new File(logFilename);
      logMessagesToFile(file);
   }

   public static void reportErrorMessage(String errorMessage)
   {
      System.err.println(errorMessage);

      createGUIMessageFrame();
      guiMessageFrame.appendErrorMessage(errorMessage);
   }

   public static void reportOutMessage(String outMessage)
   {
      System.out.println(outMessage);

      createGUIMessageFrame();
      guiMessageFrame.appendOutMessage(outMessage);
   }

   public static void reportParameterMessage(String parameterMessage)
   {
      System.out.println(parameterMessage);

      createGUIMessageFrame();
      guiMessageFrame.appendParameterMessage(parameterMessage);
   }

   public static void reportTestStartedMessage()
   {
      int usedMemoryInMB = garbageCollectAndGetUsedMemoryInMB();

      createGUIMessageFrame();
      guiMessageFrame.appendMessageToPanel(junitTestCasesIndex, BambooTools.getClassAndMethodName(1) + " started. Used Memory = " + usedMemoryInMB + " MB.");
   }

   public static void reportTestFinishedMessage()
   {
      int usedMemoryInMB = garbageCollectAndGetUsedMemoryInMB();

      createGUIMessageFrame();
      guiMessageFrame.appendMessageToPanel(junitTestCasesIndex, BambooTools.getClassAndMethodName(1) + " finished. Used Memory = " + usedMemoryInMB + " MB.");
   }

   private static void createGUIMessageFrame()
   {
      if (guiMessageFrame == null)
      {
         guiMessageFrame = GUIMessageFrame.getInstance();
         junitTestCasesIndex = guiMessageFrame.createGUIMessagePanel("JUnit Test Cases");
      }
   }

   public static void sleepForever()
   {
      while (true)
      {
         sleep(1.00);
      }

   }

   public static void sleep(double secondsToSleep)
   {
      try
      {
         Thread.sleep((long) (secondsToSleep * 1000));
      } catch (InterruptedException e)
      {
      }
   }

   public static String getFullFilenameUsingClassRelativeURL(Class<?> class1, String relativeFileName)
   {
      URL resource = class1.getResource(relativeFileName);
      if (resource == null) throw new RuntimeException("resource " + relativeFileName + " == null");
      String fileName = resource.getFile();

      return fileName;
   }

   public static String getSimpleRobotNameFor(SimpleRobotNameKeys key)
   {
      switch(key)
      {
         case M2V2:
            return "M2V2";
         case R2:
            return "R2";
         case VALKYRIE:
            return "Valkyrie";
         case ATLAS:
            return "Atlas";
         case BONO:
            return "Bono";
         case SPOKED_RUNNER:
            return "SpokedRunner";
         default:
            return "";
      }
   }

   public static enum SimpleRobotNameKeys
   {
      M2V2, R2, VALKYRIE, ATLAS, BONO, SPOKED_RUNNER
   }



}
