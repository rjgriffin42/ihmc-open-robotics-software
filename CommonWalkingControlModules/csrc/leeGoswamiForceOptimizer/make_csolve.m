% Produced by CVXGEN, 2013-01-17 15:52:18 +0000.
% CVXGEN is Copyright (C) 2006-2012 Jacob Mattingley, jem@cvxgen.com.
% The code in this file is Copyright (C) 2006-2012 Jacob Mattingley.
% CVXGEN, or solvers produced by CVXGEN, cannot be used for commercial
% applications without prior written permission from Jacob Mattingley.

% Filename: make_csolve.m.
% Description: Calls mex to generate the csolve mex file.
%mex -v csolve.c ldl.c matrix_support.c solver.c util.c
mex csolve.c ldl.c matrix_support.c solver.c util.c
