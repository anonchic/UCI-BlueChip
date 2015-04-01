

-- VHDL-AMS model of Van Der Pol Oscillator equation 
-- (c) Southampton University 1997 
-- Southampton VHDL-AMS Validation Suite - example 1 
-- author: Tom Kazmierski 
-- Department of Electronics and Computer Science, University of Southampton 
-- Highfield, Southampton SO17 1BJ, United Kingdom 
-- Tel. +44 (01703) 593520   Fax +44 (01703) 592901 
-- e-mail: tjk@ecs.soton.ac.uk 
-- Created: 28 May 1997 
-- Last revised: 30 May 1997 
-- 

library IEEE; 
use IEEE.math_real.all; 

entity VanDerPol is 
    generic (m: real := 1.0); 
    port (quantity x: out real); -- unknown x(t) 
end entity VanDerPol; 

architecture SecondOrderODE of VanDerPol is 
    quantity xdot: real; -- x'(t) 
begin 
    -- unconditional break statement sets the initial conditions 
    break x => 0.0, xdot => 0.1; 

    -- two simultaneous equations are required for two unknown quantities: 
    --   - interface quantity with mode out (x) 
    --   - free quantity declared in architecture (xdot) 
    xdot == x'dot; 
    xdot'dot + m*(x*x - 1.0)* xdot + x == 0.0; 
end architecture SecondOrderODE; 

