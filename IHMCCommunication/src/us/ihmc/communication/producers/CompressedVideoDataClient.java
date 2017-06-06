package us.ihmc.communication.producers;

import boofcv.struct.calib.IntrinsicParameters;
import us.ihmc.communication.net.ConnectionStateListener;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;

public interface CompressedVideoDataClient extends ConnectionStateListener
{
   public abstract void consumeObject(VideoSource videoSource, byte[] data, long timestamp, Point3DReadOnly position, QuaternionReadOnly orientation, IntrinsicParameters intrinsicParameters);
}
