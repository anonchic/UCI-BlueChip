/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: intldst.s,v 1.2 91/01/10 17:27:52 rudi Exp $"
.align 8
.seg "text"

#define ERR_RET %r6
#define ERR_RET_NUM 6

#include "err_incr.h"
#include "err_macro.h"

#include "assy_macros.h"
new_init(intldst)

#include "crueltt.S"

#include "err_subr.S"

.seg	"text"
start:
		mov	0xa0, %psr		! CWP = 0 S = 1
		mov	0, %wim			! WIM = 0
	set address_0, %g1
	mov %g1, %tbr
		set	flag, %g2
		mov	0, %l0
		mov	9, %l1
		mov	18, %l2
		mov	27, %l3
		/* under the Cruel system model:
			all of the following get data_access_exception
			all of the following must correctly restore
			the destination register upon data_access_exception
			to correctly work.
		*/
		ldstub	[%g2 + %l0], %l0
		ldstub	[%g2 + %l1], %l1
		ldstub	[%g2 + %l2], %l2
		ldstub	[%g2 + %l3], %l3

		/* one more ldstb which clobbers  Rs1 */

		ldstub [%g2 + 1], %g2

! error checking code

		cmp	%l0,%g0
		check_error	(NEXTE,bne)

		set	0x00000001,%g4
		cmp	%g4,%l1
		check_error     (NEXTE,bne)

		set	0x00000002,%g4
		cmp	%g4,%l2
		check_error     (NEXTE,bne)
		
		set	0x00000003,%g4
		cmp	%g4,%l3
		check_error     (NEXTE,bne)

		set	0x00000001,%g4
		cmp	%g4,%g2
		check_error     (NEXTE,bne)


stop: b testok; nop
		nop

.seg "data"
.align	8
flag:		.word 0x00010203
		.word 0x00010203

		.word 0x00010203
		.word 0x00010203

		.word 0x00010203
		.word 0x00010203

		.word 0x00010203
		.word 0x00010203
.seg "text"


