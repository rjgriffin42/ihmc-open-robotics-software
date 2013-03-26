#!/bin/sh

modelPath=src/us/ihmc/darpaRoboticsChallenge/models/GFE/
cp -r /usr/share/drcsim-2.2/gazebo_models/ $modelPath
cp -r /usr/share/drcsim-2.2/worlds $modelPath
cp -r ~/.gazebo/models $modelPath

cp -r /usr/share/gazebo-1.5/media $modelPath/gazebo
cp -r /usr/share/drcsim-2.2/media $modelPath/drcsim

cp $modelPath/gazebo/media/materials/textures/road1.jpg ../SDFLoader/src/us/ihmc/SdfLoader/models

gzsdf print $modelPath/gazebo_models/atlas_description/atlas/atlas.urdf > $modelPath/atlas.sdf
gzsdf print $modelPath/gazebo_models/atlas_description/atlas_sandia_hands/atlas_sandia_hands.urdf > $modelPath/atlas_sandia_hands.sdf
gzsdf print $modelPath/gazebo_models/atlas_description/atlas_irobot_hands/atlas_irobot_hands.urdf > $modelPath/atlas_irobot_hands.sdf

gzsdf print $modelPath/worlds/vrc_task_1.world > $modelPath/vrcTask1World.sdf
gzsdf print $modelPath/worlds/vrc_task_2.world > $modelPath/vrcTask2World.sdf
gzsdf print $modelPath/worlds/vrc_task_3.world > $modelPath/vrcTask3World.sdf

gzsdf print $modelPath/worlds/qual_task_1.world > $modelPath/qualTask1World.sdf
gzsdf print $modelPath/worlds/qual_task_2.world > $modelPath/qualTask2World.sdf
gzsdf print $modelPath/worlds/qual_task_3.world > $modelPath/qualTask3World.sdf
gzsdf print $modelPath/worlds/qual_task_4.world > $modelPath/qualTask4World.sdf

./createRobotHands.sh
