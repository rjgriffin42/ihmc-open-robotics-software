% Produced by CVXGEN, 2013-01-22 10:35:58 -0500.
% CVXGEN is Copyright (C) 2006-2012 Jacob Mattingley, jem@cvxgen.com.
% The code in this file is Copyright (C) 2006-2012 Jacob Mattingley.
% CVXGEN, or solvers produced by CVXGEN, cannot be used for commercial
% applications without prior written permission from Jacob Mattingley.

% Filename: cvxsolve.m.
% Description: Solution file, via cvx, for use with sample.m.
function [vars, status] = cvxsolve(params, settings)
A = params.A;
B = params.B;
C = params.C;
W = params.W;
epsilon = params.epsilon;
fmin = params.fmin;
cvx_begin
  % Caution: automatically generated by cvxgen. May be incorrect.
  variable rho(32, 1);

  minimize(quad_form(A*rho - W, C) + epsilon*quad_form(rho, eye(32)));
  subject to
    rho >= 0;
    B*rho >= fmin;
cvx_end
vars.rho = rho;
status.cvx_status = cvx_status;
% Provide a drop-in replacement for csolve.
status.optval = cvx_optval;
status.converged = strcmp(cvx_status, 'Solved');
