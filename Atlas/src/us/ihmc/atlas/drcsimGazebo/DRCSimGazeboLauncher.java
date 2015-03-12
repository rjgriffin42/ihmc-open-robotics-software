package us.ihmc.atlas.drcsimGazebo;

import us.ihmc.utilities.ThreadTools;

public class DRCSimGazeboLauncher
{
   public static final boolean RUN_CATKIN_MAKE = true;
   
   public static void main(String[] args)
   {
      new DRCSimGazeboLauncher();
   }

   public DRCSimGazeboLauncher()
   {      
      runProcess();
   }
   
   private void runProcess()
   {
      //Build plugin
      if (RUN_CATKIN_MAKE)
      {
         String commandline1 = "source ~/.bashrc;" +
               "source /opt/ros/indigo/setup.bash;" +
               "cd ihmc_gazebo_catkin_ws;"+
               "catkin_make";
         ThreadTools.runCommandLine(commandline1);
      }

      //Launch gazebo with ihmc_plugin
      String commandline2 = "source ~/.bashrc;" +
            "source /opt/ros/indigo/setup.bash;" +
            "source /usr/share/drcsim/setup.sh;" +
            "source $PWD/ihmc_gazebo_catkin_ws/devel/setup.bash;" +
            "roslaunch ihmc_gazebo ihmc_atlas_standing.launch";
      ThreadTools.runCommandLine(commandline2);
   }
}
