package us.ihmc.gdx.input.editor;

import us.ihmc.euclid.orientation.interfaces.Orientation3DReadOnly;

public interface OrientationEditable extends GDXUIEditableGraphic
{
   void setOrientation(Orientation3DReadOnly orientationPoint);
}