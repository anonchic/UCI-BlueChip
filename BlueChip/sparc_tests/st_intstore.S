/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: intstore.s,v 1.2 91/01/11 10:47:22 rudi Exp $"
.align 8
.seg "text"

#define ERR_RET %r6
#define ERR_RET_NUM 6

#include "err_incr.h"
#include "err_macro.h"

new_init(intstore)



#include "crueltt.S"

#include "err_subr.S"
	
.seg	"text"
start:
		mov	0xa0, %psr		! CWP = 0 S = 1
		mov	0, %wim			! WIM = 0
		set	address_0, %g1
		mov	%g1, %tbr		! TBR = 0
		set	0x12345678, %g2		! data to store
		set	area, %g3		! store address base

		st	%g2, [%g3]		! 0x12345678

		sth	%g2, [%g3 + 4]		! 0x5678
		sth	%g2, [%g3 + 6]		! 0x5678

		stb	%g2, [%g3 + 8]
		stb	%g2, [%g3 + 9]
		stb	%g2, [%g3 + 10]
		stb	%g2, [%g3 + 11]

!check errors

		set     area, %g1

		ld	[%g1],%g2
		set	0x12345678,%g3
		cmp     %g2,%g3
		check_error(NEXTE,bne)

		ld      [%g1+4],%g2
		set	0x56785678,%g3
		cmp	%g2,%g3
		check_error(NEXTE,bne)

		ld      [%g1+8],%g2
		set	0x78787878,%g3
		cmp	%g2,%g3
		check_error(NEXTE,bne)

stop: b testok; nop
		xnorcc	%g0, %g0, %g2	! ANNULED

.seg "data"
area:	.skip 16
.seg "text"

