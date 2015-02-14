package us.ihmc.darpaRoboticsChallenge.controllers;

import java.util.ArrayList;

import us.ihmc.userInterface.modifiableObjects.PathActivationLevel;
import us.ihmc.utilities.math.dataStructures.HeightMapWithPoints;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.utilities.humanoidRobot.footstep.Footstep;

public interface Path
{
   public ArrayList<Footstep> generateFootsteps(HeightMapWithPoints heightMap, SideDependentList<ContactablePlaneBody> contactableFeet);

   public void cancel();

   public void activateGraphics(PathActivationLevel activationLevel);

   public void removeFootStep(Footstep footstep);

}
