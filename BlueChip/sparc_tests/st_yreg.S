/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: yreg.s,v 1.2 91/01/15 10:31:17 rudi Exp $"
.align 8
.seg "text"

#define ERR_RET %r6
#define ERR_RET_NUM 6

#include "err_incr.h"
#include "err_macro.h"

new_init(yreg)
	
#include "crueltt.S"

#include "err_subr.S"


.seg	"text"
start:
	mov	0, %wim			! WIM = 0
	set address_0, %g1
	mov	%g1, %tbr			! TBR = 0

	mov	0xa6, %psr		! CWP = 7 S = 1

		mov	21, %y			! multiplicand
		nop
		nop
		nop

		andcc   %r0, %r0, %l0

		mov	25, %l1			! multiplier
		mulscc	%l0, %l1, %l0
		mulscc	%l0, %l1, %l0
		mulscc	%l0, %l1, %l0
		mulscc	%l0, %l1, %l0
		mov	%y, %l2			! RDY
		mov	%l2, %l3		! must interlock

! check errors
		nop;nop;nop
		set	0xa0000001,%g1
		mov	%y,%g4
		cmp	%g1,%g4
		check_error(NEXTE,bne)
		cmp	%g1,%l2
		check_error(NEXTE,bne)
		cmp	%g1,%l3
		check_error(NEXTE,bne)

stop: b testok; nop
		b	stop

