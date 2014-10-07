package us.ihmc.darpaRoboticsChallenge.controllers;

import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;

public interface PathGeneratorManager
{
   public FullRobotModel getFullRobotModel();
   public ReferenceFrames getReferenceFrames();
   public SideDependentList<ContactablePlaneBody> getBipedFeet();
}
