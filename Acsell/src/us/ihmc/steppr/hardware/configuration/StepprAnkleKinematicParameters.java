package us.ihmc.steppr.hardware.configuration;

public final class StepprAnkleKinematicParameters {
   
      private static final double N = 6.0; 

//Original values. Not sure why some of these don't match what Matlab now gives for original linkage values.
//	   private static final double px[] = { 0.000000000011833, 1.269023941053808, -1.269023941048548, 0.020516020869627, -0.020516021471959, 0.000000000606667,  0.115495040111334, -0.523312217421704, 0.523312217621650, -0.115495040305941 };
//	   private static final double py[] = { 0.000022001174462, 0.463732921959109, 0.463732921783293, -0.048394601071517, -0.048394601065684, 0.111755077611979, -0.075022109801143, 0.057936370829754, 0.057936363252024, -0.075022107299457 };
//	   private static final double p_m_JitX[] = { 1.272997427777619, 0.036001701491556, 0.001270562468774, 0.411410640321290, 0.628599891388498, -1.233612302811653, -0.128100680179965, 0.209457856510320, 0.014033597275913, -0.080071351885635 };
//	   private static final double p_m_JitY[] = { 0.465442714987602, -0.079404498198185, 0.091522714637596, -0.239129834635615, 0.040327656123728, 0.141924052147264, -0.130148712431276, 0.342409832258836, -0.315199115178924, 0.111764662233772 };
//	   private static final double pM1[] = { 0.001637333497514, 2.363024415727514, 6.448426531047804, 0.209647991746190, -0.132578850323121, -0.102263276506052, -0.223803048871049, 0.764689039287297, 0.459191580065332, 0.349362854084994 };
//	   private static final double pM2[] = { 0.001637333371246, -2.363024413970044, 6.448426532362931, 0.209647993650533, -0.132578850320880, 0.102263276373644, 0.223803036060187, 0.764689036580238, -0.459191581057666, 0.349362851648649 };

//Latest Matlab values for original Linkage
//	   private static final double pM1[] = { 0.0006,  2.3783, 6.4668, 0.2029, -0.1218, -0.0908, -0.2655, 0.6796,  0.3860, 0.3071};
//	   private static final double pM2[] = { 0.0006, -2.3783, 6.4668, 0.2029, -0.1218,  0.0908,  0.2655, 0.6796, -0.3860, 0.3071};
//	   private static final double p_j_JitX[] = { 1.2632,  0.0151, 0.0416,  0.4441, -0.2038, -0.1688, -0.0319, -0.1953,  0.0397, 0.0267};
//	   private static final double p_j_JitY[] = { 0.4646, -0.0651, 0.0132, -0.0506, -0.0645, -0.2435, -0.0723, -0.0137, -0.0372, 0.0148};

//Wide bracket linkage
      private static final double px[] = { 0.0000,  0.9030, -0.9030, -0.0010,  0.0010, -0.0000, -0.0100, -0.1011,  0.1011,  0.0100};
      private static final double py[] = { 0.0005,  0.4636,  0.4636, -0.0260, -0.0260,  0.0541, -0.0714,  0.0540,  0.0540, -0.0714};
	   private static final double pM1[] = { -0.0054,  3.3165,  6.4421,  0.2435, -0.0085,  0.0131, -0.1054,  1.4838,  0.6777,  0.3746};
	   private static final double pM2[] = { -0.0054, -3.3165,  6.4421,  0.2435, -0.0085, -0.0131,  0.1054,  1.4838, -0.6777,  0.3746};
      private static final double p_m_JitX[] = { 0.9064,  0.0228, -0.0042, -0.0031,  0.1523, -0.2989, -0.1515,  0.2460, -0.0961, -0.0190};
      private static final double p_m_JitY[] = { 0.4651, -0.0357,  0.0409, -0.2213,  0.0411,  0.1234, -0.0712,  0.1660, -0.1495,  0.0511};
	   private static final double p_j_JitX[] = { 0.9060, 0.0149, 0.0221, 0.1463, -0.1834, -0.2054, -0.0819, -0.2685, -0.0467, -0.0326};
	   private static final double p_j_JitY[] = { 0.4661, -0.0382,  0.0064, -0.1044, -0.0740, -0.3509, -0.1060, -0.0376, -0.0597, -0.0058};

	   	   	   
	   public static double[] getXParams()
	   {
		   return px;
	   }
	   
	   public static double[] getYParams()
	   {
		   return py;
	   }
	   
	   public static double[] getJITX_FromMotorParams()
	   {
		   return p_m_JitX;
	   }
	   
	   public static double[] getJITY_FromMotorParams()
	   {
		   return p_m_JitY;
	   }
	   
	   public static double[] getJITX_FromJointParams()
	   {
		   return p_j_JitX;
	   }
	   
	   public static double[] getJITY_FromJointParams()
	   {
		   return p_j_JitY;
	   }
	   
	   public static double[] getM1Params()
	   {
		   return pM1;
	   }
	   
	   public static double[] getM2Params()
	   {
		   return pM2;
	   }
	   
	   public static double getN()
	   {
		   return N;
	   }
	   
}
