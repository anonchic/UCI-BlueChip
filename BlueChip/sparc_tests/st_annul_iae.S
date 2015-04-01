/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: annul_iae.s,v 1.2 91/01/10 13:07:05 rudi Exp $" 
.align 8
.seg "text"

#define ERR_RET %r6
#define ERR_RET_NUM 6

#include "err_incr.h"
#include "err_macro.h"
new_init(annul_iae)


#include "crueltt.S"

#include "err_subr.S"

.seg	"text"
start:
		mov	0xa0, %psr		! CWP = 0 S = 1
		mov	0, %wim			! WIM = 0
	        set	address_0, %g1
		mov	%g1, %tbr			! TBR = 0
		nop !nops added by Murph - otherwise the wrong l7 could be used. 
		nop
		nop
		sethi	%hi(place), %l7
		b	branch
		or	%l7, %lo(place), %l7
.align 8
branch:		st	%g0, [%l7]		! IAE on cruel model
		ba,a	label	
		mov	22, %l0			! should NOT get iae, or
						! be executed
		mov	-1, %l1			! NOT EXECUTED
	        check_error(NEXTE,ba)
label:		mov	-13, %l2

stop: b testok; nop
		nop

.seg "data"
place:		.word 5
.seg "text"
