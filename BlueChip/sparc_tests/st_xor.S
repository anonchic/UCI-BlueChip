/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: xor.s,v 1.2 91/01/15 10:23:43 rudi Exp $"
.align 8
.seg "text"

#define ERR_RET %r6
#define ERR_RET_NUM 6

#include "err_incr.h"
#include "err_macro.h"

new_init(xor)
#include "err_subr.S"


/*********************************************************/
#include "crueltt.S"


start:		mov	0x0e0, %psr
		mov	0, %wim

		set 	address_0, %g1
		mov	%g1, %tbr
		nop

		mov	0xa, %l0
		mov	0x5, %l1
		xorcc	%l0, %l0, %g0

		be	stop
		nop

not_equal:	check_error(NEXTE,ba)


stop: b testok; nop
		nop

