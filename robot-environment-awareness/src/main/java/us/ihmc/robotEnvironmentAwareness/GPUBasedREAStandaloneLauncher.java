package us.ihmc.robotEnvironmentAwareness;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.javaFXToolkit.messager.SharedMemoryJavaFXMessager;
import us.ihmc.pubsub.DomainFactory;
import us.ihmc.robotEnvironmentAwareness.communication.GPUModuleAPI;
import us.ihmc.robotEnvironmentAwareness.ui.GPUBasedEnvironmentAwarenessUI;
import us.ihmc.robotEnvironmentAwareness.updaters.GPUBasedREAModule;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.utilities.ros.RosMainNode;

import java.net.URI;

public class GPUBasedREAStandaloneLauncher extends Application {

    private GPUBasedEnvironmentAwarenessUI ui;
    private GPUBasedREAModule module;

    private ROS2Node ros2Node;
    private RosMainNode rosMainNode;

    @Override
    public void start(Stage primaryStage) throws Exception {

        URI rosMasterURI = new URI("http://localhost:11311/");
        rosMainNode = new RosMainNode(rosMasterURI, "GPUPlanarRegionSubscriber");
        ros2Node = ROS2Tools.createROS2Node(DomainFactory.PubSubImplementation.FAST_RTPS, ROS2Tools.REA_NODE_NAME);

        SharedMemoryJavaFXMessager messager = new SharedMemoryJavaFXMessager(GPUModuleAPI.API);
        ui = GPUBasedEnvironmentAwarenessUI.createIntraprocessUI(messager, primaryStage);
        module = GPUBasedREAModule.createIntraprocess(messager, ros2Node, rosMainNode);


        rosMainNode.execute();
        ui.show();
        module.start();
    }

    @Override
    public void stop() throws Exception {

        super.stop();
        rosMainNode.shutdown();
        ros2Node.destroy();
        ui.stop();
        module.stop();
        Platform.exit();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
