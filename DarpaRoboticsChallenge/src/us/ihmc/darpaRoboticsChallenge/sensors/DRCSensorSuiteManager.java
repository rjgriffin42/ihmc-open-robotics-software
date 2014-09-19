package us.ihmc.darpaRoboticsChallenge.sensors;

import java.net.URI;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.producers.RobotPoseBuffer;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPhysicalProperties;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.depthData.DepthDataFilter;
import us.ihmc.darpaRoboticsChallenge.networking.DRCNetworkProcessorNetworkingManager;
import us.ihmc.utilities.net.LocalObjectCommunicator;
import us.ihmc.utilities.net.ObjectCommunicator;

public interface DRCSensorSuiteManager
{
   public void initializeSimulatedSensors(LocalObjectCommunicator scsCommunicator, ObjectCommunicator fieldComputerClient, RobotPoseBuffer robotPoseBuffer,
                                          DRCNetworkProcessorNetworkingManager networkingManager, SDFFullRobotModel sdfFullRobotModel, DepthDataFilter lidarDataFilter, URI sensorURI, DRCRobotPhysicalProperties physicalProperties);

   public void initializePhysicalSensors(RobotPoseBuffer robotPoseBuffer, DRCNetworkProcessorNetworkingManager networkingManager,
                                         SDFFullRobotModel sdfFullRobotModel, ObjectCommunicator objectCommunicator, DepthDataFilter lidarDataFilter, URI sensorURI, DRCRobotPhysicalProperties physicalProperties);

}
