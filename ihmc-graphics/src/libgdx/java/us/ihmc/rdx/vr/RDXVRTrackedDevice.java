package us.ihmc.rdx.vr;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.BufferUtils;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.TrackedDevicePose;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRSystem;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.rdx.tools.LibGDXTools;
import us.ihmc.rdx.tools.RDXModelLoader;

import java.nio.IntBuffer;

public abstract class RDXVRTrackedDevice
{
   private int deviceIndex;
   private boolean isConnected;
   private final IntBuffer errorCode = BufferUtils.newIntBuffer(1);
   private final RigidBodyTransform deviceToPlayAreaTransform = new RigidBodyTransform();
   private final ReferenceFrame deviceYUpZBackFrame;
   private final RigidBodyTransform tempOpenVRToWorldTransform = new RigidBodyTransform();
   private ModelInstance modelInstance = null;

   public RDXVRTrackedDevice(ReferenceFrame vrPlayAreaYUpZBackFrame)
   {
      deviceYUpZBackFrame = ReferenceFrameTools.constructFrameWithChangingTransformToParent("device" + deviceIndex + "YUpZBackFrame",
                                                                                            vrPlayAreaYUpZBackFrame,
                                                                                            deviceToPlayAreaTransform);
   }

   protected void update(TrackedDevicePose.Buffer trackedDevicePoses)
   {
      if (isConnected)
      {
         HmdMatrix34 openVRRigidBodyTransform = trackedDevicePoses.get(deviceIndex).mDeviceToAbsoluteTracking();
         if (OpenVRTools.containsNaN(openVRRigidBodyTransform))
         {
            isConnected = false;
         }
         else
         {
            LibGDXTools.toEuclid(openVRRigidBodyTransform, deviceToPlayAreaTransform);
            deviceYUpZBackFrame.update();

            if (modelInstance == null)
            {
               String renderModelName = VRSystem.VRSystem_GetStringTrackedDeviceProperty(deviceIndex,
                                                                                         VR.ETrackedDeviceProperty_Prop_RenderModelName_String,
                                                                                         errorCode);
               Model model = new Model();
               if (renderModelName.contains("{htc_business_streaming}")) // vive focus 3 controller render models are not supported in open vr
               {
                  String modelFile = "vr/controllers/";
                  if (renderModelName.contains("left"))
                     modelFile += "vive_focus3_left/Focus3_controller_left.g3dj";
                  else if (renderModelName.contains("right"))
                     modelFile += "vive_focus3_right/Focus3_controller_right.g3dj";
                  model = RDXModelLoader.load(modelFile);
               }
               else
                  model = RDXVRModelLoader.loadRenderModel(renderModelName);
               modelInstance = model != null ? new ModelInstance(model) : null;
            }

            deviceYUpZBackFrame.getTransformToDesiredFrame(tempOpenVRToWorldTransform, ReferenceFrame.getWorldFrame());
            LibGDXTools.toLibGDX(tempOpenVRToWorldTransform, modelInstance.transform);
         }
      }
   }

   public ReferenceFrame getDeviceYUpZBackFrame()
   {
      return deviceYUpZBackFrame;
   }

   protected void setDeviceIndex(int deviceIndex)
   {
      this.deviceIndex = deviceIndex;
   }

   protected void setConnected(boolean isConnected)
   {
      this.isConnected = isConnected;
   }

   public ModelInstance getModelInstance()
   {
      return modelInstance;
   }

   public boolean isConnected()
   {
      return isConnected;
   }

   public int getDeviceIndex()
   {
      return deviceIndex;
   }
}
