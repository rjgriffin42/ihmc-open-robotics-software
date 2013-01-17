% Produced by CVXGEN, 2013-01-17 22:52:37 +0000.
% CVXGEN is Copyright (C) 2006-2012 Jacob Mattingley, jem@cvxgen.com.
% The code in this file is Copyright (C) 2006-2012 Jacob Mattingley.
% CVXGEN, or solvers produced by CVXGEN, cannot be used for commercial
% applications without prior written permission from Jacob Mattingley.

% Filename: cvxsolve.m.
% Description: Solution file, via cvx, for use with sample.m.
function [vars, status] = cvxsolve(params, settings)
Psi_k = params.Psi_k;
epsilon = params.epsilon;
eta_d = params.eta_d;
etamax = params.etamax;
etamin = params.etamin;
kappa_k = params.kappa_k;
cvx_begin
  % Caution: automatically generated by cvxgen. May be incorrect.
  variable eta(6, 1);

  minimize(quad_form(Psi_k*eta - kappa_k, eye(3)) + quad_form(eta - eta_d, epsilon));
  subject to
    etamin <= eta;
    eta <= etamax;
cvx_end
vars.eta = eta;
status.cvx_status = cvx_status;
% Provide a drop-in replacement for csolve.
status.optval = cvx_optval;
status.converged = strcmp(cvx_status, 'Solved');
