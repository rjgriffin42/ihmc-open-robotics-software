package us.ihmc.atlas;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.posePlayback.PoseSequenceEditorGUI;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public class AtlasPoseSequenceEditorGUI
{
   public static void main(String[] args)
   {
      // Flag to set robot model
      JSAP jsap = new JSAP();
      FlaggedOption robotModel = new FlaggedOption("robotModel").setLongFlag("model").setShortFlag('m').setRequired(true).setStringParser(JSAP.STRING_PARSER);
      robotModel.setHelp("Robot models: " + AtlasRobotModelFactory.robotModelsToString());

      DRCRobotModel model;
      try
      {
         jsap.registerParameter(robotModel);

         JSAPResult config = jsap.parse(args);

         if (config.success())
         {
            model = AtlasRobotModelFactory.createDRCRobotModel(config.getString("robotModel"), AtlasRobotModel.AtlasTarget.SIM, false);
         }
         else
         {
            System.out.println("Enter a robot model.");

            return;
         }
      }
      catch (Exception e)
      {
         System.out.println("Robot model not found");
         e.printStackTrace();

         return;
      }

      PoseSequenceEditorGUI scriptedEditorGUI = new PoseSequenceEditorGUI(model);
      scriptedEditorGUI.setDefaultCloseOperation(PoseSequenceEditorGUI.EXIT_ON_CLOSE);
      scriptedEditorGUI.setVisible(true);
   }
}
