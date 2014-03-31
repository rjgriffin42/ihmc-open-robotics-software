package us.ihmc.atlas;

import java.io.InputStream;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactPointInformation;
import us.ihmc.darpaRoboticsChallenge.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.DRCLocalConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotContactPointParamaters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPhysicalProperties;
import us.ihmc.darpaRoboticsChallenge.handControl.DRCHandModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCSimDRCRobotInitialSetup;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;

import com.jme3.math.Transform;

public class AtlasRobotModel implements DRCRobotModel
{
   private final AtlasRobotVersion selectedVersion;
   
   private final boolean runningOnRealRobot;

   public AtlasRobotModel()
   {
      this(AtlasRobotVersion.DRC_NO_HANDS);
   }

   public AtlasRobotModel(AtlasRobotVersion atlasVersion)
   {
      this(atlasVersion, DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT);
   }

   public AtlasRobotModel(AtlasRobotVersion atlasVersion, boolean runningOnRealRobot)
   {
      selectedVersion = atlasVersion;
      
      this.runningOnRealRobot = runningOnRealRobot;
   }

   public ArmControllerParameters getArmControllerParameters()
   {
      return new AtlasArmControllerParameters(runningOnRealRobot);
   }

   public WalkingControllerParameters getWalkingControlParamaters()
   {
      return new AtlasWalkingControllerParameters(runningOnRealRobot);
   }

   public StateEstimatorParameters getStateEstimatorParameters(double estimatorDT)
   {
      return new AtlasStateEstimatorParameters(runningOnRealRobot, estimatorDT);
   }

   public DRCRobotPhysicalProperties getPhysicalProperties()
   {
      return new AtlasPhysicalProperties();
   }

   public DRCRobotJointMap getJointMap()
   {
      return new AtlasJointMap(this);
   }

   public boolean hasIRobotHands()
   {
      return selectedVersion.getHandModel() == DRCHandModel.IROBOT;
   }

   public boolean hasArmExtensions()
   {
      return selectedVersion.hasArmExtensions();
   }

   public boolean hasHookHands()
   {
      return selectedVersion.getHandModel() == DRCHandModel.HOOK;
   }

   public DRCHandModel getHandModel()
   {
      return selectedVersion.getHandModel();
   }

   public String getModelName()
   {
      return "atlas";
   }

   public AtlasRobotVersion getAtlasVersion()
   {
      return selectedVersion;
   }

   @Override
   public String getSdfFile()
   {
      return selectedVersion.getSdfFile();
   }

   @Override
   public String[] getResourceDirectories()
   {
      return selectedVersion.getResourceDirectories();
   }

   @Override
   public InputStream getSdfFileAsStream()
   {
      return selectedVersion.getSdfFileAsStream();
   }

   @Override
   public Transform getOffsetHandFromWrist(RobotSide side)
   {
      return selectedVersion.getOffsetFromWrist(side);
   }

   @Override
   public RobotType getType()
   {
      return RobotType.ATLAS;
   }

   public String toString()
   {
      return selectedVersion.toString();
   }

   public DRCRobotInitialSetup<SDFRobot> getDefaultRobotInitialSetup(double groundHeight, double initialYaw)
   {
      return new DRCSimDRCRobotInitialSetup(groundHeight, initialYaw);
   }

   @Override
   public WalkingControllerParameters getMultiContactControllerParameters()
   {
      return new AtlasRobotMultiContactControllerParameters();
   }

   @Override
   public DRCRobotContactPointParamaters getContactPointParamaters(boolean addLoadsOfContactPoints, boolean addLoadsOfContactPointsToFeetOnly)
   {
      return new AtlasContactPointParamaters(selectedVersion,getJointMap(),addLoadsOfContactPoints,addLoadsOfContactPointsToFeetOnly);
   }

   @Override
   public ContactPointInformation getContactPointInformation(boolean addLoadsOfContactPoints, boolean addLoadsOfContactPointsToFeetOnly)
   {
      return null;
   }
}
