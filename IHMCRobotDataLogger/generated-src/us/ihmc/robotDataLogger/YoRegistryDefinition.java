package us.ihmc.robotDataLogger;
import us.ihmc.idl.IDLSequence;
import java.util.Arrays;

/**
* 
* Definition of the class "YoRegistryDefinition" defined in Handshake.idl. 
*
* This file was automatically generated from Handshake.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit Handshake.idl instead.
*
*/
public class YoRegistryDefinition
{
    public YoRegistryDefinition()
    {
        	name_ = new StringBuilder(255); 
        
        
    }

    public void set(YoRegistryDefinition other)
    {
        	parent_ = other.parent_;
        	name_.setLength(0);
        	name_.append(other.name_);

    }

    public void setParent(int parent)
    {
        parent_ = parent;
    }

    public int getParent()
    {
        return parent_;
    }

        
        public void setName(String name)
        {
        	name_.setLength(0);
        	name_.append(name);
        }
        
        public String getNameAsString()
        {
        	return getName().toString();
        }

    public StringBuilder getName()
    {
        return name_;
    }

        




    @Override
    public boolean equals(Object other)
    {
        if(other == null) return false;
        if(other == this) return true;
        if(!(other instanceof YoRegistryDefinition)) return false;
        YoRegistryDefinition otherMyClass = (YoRegistryDefinition)other;
        boolean returnedValue = true;

        returnedValue &= this.parent_ == otherMyClass.parent_;

                
        returnedValue &= us.ihmc.idl.IDLTools.equals(this.name_, otherMyClass.name_);
                

        return returnedValue;
    }
    
     @Override
    public String toString()
    {
		StringBuilder builder = new StringBuilder();
		
      	builder.append("YoRegistryDefinition {");
        builder.append("parent=");
        builder.append(this.parent_);

                builder.append(", ");
        builder.append("name=");
        builder.append(this.name_);

                
        builder.append("}");
		return builder.toString();
    }

    private int parent_; 
    private StringBuilder name_; 

}