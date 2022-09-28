// Targeted by JavaCPP version 1.5.7: DO NOT EDIT THIS FILE

package us.ihmc.promp;

import java.nio.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

import static us.ihmc.promp.global.promp.*;

@NoOffset @Name("std::pair<Eigen::VectorXd,Eigen::VectorXd>") @Properties(inherit = us.ihmc.promp.presets.PrompInfoMapper.class)
public class VectorVectorPair extends Pointer {
    static { Loader.load(); }
    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
    public VectorVectorPair(Pointer p) { super(p); }
    public VectorVectorPair(EigenVectorXd firstValue, EigenVectorXd secondValue) { this(); put(firstValue, secondValue); }
    public VectorVectorPair()       { allocate();  }
    private native void allocate();
    public native @Name("operator =") @ByRef VectorVectorPair put(@ByRef VectorVectorPair x);


    @MemberGetter public native @ByRef EigenVectorXd first(); public native VectorVectorPair first(EigenVectorXd first);
    @MemberGetter public native @ByRef EigenVectorXd second();  public native VectorVectorPair second(EigenVectorXd second);

    public VectorVectorPair put(EigenVectorXd firstValue, EigenVectorXd secondValue) {
        first(firstValue);
        second(secondValue);
        return this;
    }
}

