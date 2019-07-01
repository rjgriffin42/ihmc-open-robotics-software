package us.ihmc.tools.property;

public class BooleanStoredPropertyKey extends StoredPropertyKey<Boolean>
{
   public BooleanStoredPropertyKey(int id, String titleCasedName)
   {
      super(Boolean.class, id, titleCasedName);
   }
}
