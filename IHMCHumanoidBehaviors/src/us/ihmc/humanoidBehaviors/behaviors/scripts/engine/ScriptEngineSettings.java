package us.ihmc.humanoidBehaviors.behaviors.scripts.engine;

import java.io.File;

public class ScriptEngineSettings
{
   public static final String scriptLoadingDirectory = "scripts" + File.separator;

   //For now, make directories the same
   public static final String scriptSavingDirectory = scriptLoadingDirectory; //"scriptsSaved" + File.separator;

   public static final String extension = ".xml";
}
