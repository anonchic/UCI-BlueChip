/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: arithmetic.s,v 1.2 91/01/10 13:08:02 rudi Exp $"
.align 8
.seg "text"

#define ERR_RET %l0
#define ERR_RET_NUM 16

#include "err_incr.h"
#include "err_macro.h"
new_init(arithmetic)
#include "err_subr.S"


.seg	"text"
start:
		add	%g0, 1, %g1
		sub	%g0, 2, %g2
		or	%g0, 3, %g3
		orn	%g0, 4, %g4
		and	%g0, 5, %g5
		andn	%g0, 6, %g6
		xor	%g0, 7, %g7

		add	%g0, 1, %g1
		sub	%g0, 2, %g2
		or	%g0, 3, %g3
		orn	%g0, 4, %g4
		and	%g0, 5, %g5
		andn	%g0, 6, %g6
		xor	%g0, 7, %g7

		add	%g0, 1, %g1
		sub	%g0, 2, %g2
		or	%g0, 3, %g3
		orn	%g0, 4, %g4
		and	%g0, 5, %g5
		andn	%g0, 6, %g6
		xor	%g0, 7, %g7

! check for errors :
		set 	chk_data, %l0
		ld	[%l0],%l1
		ld      [%l0+4],%l2
		ld      [%l0+8],%l3
		ld      [%l0+12],%l4
		ld      [%l0+16],%l5
		ld      [%l0+20],%l6
		ld      [%l0+24],%l7

		cmp	%l1,%g1
		check_error(NEXTE,bne)
		cmp	%l2,%g2
		check_error(NEXTE,bne)
		cmp	%l3,%g3
		check_error(NEXTE,bne)
		cmp	%l4,%g4
		check_error(NEXTE,bne)
		cmp	%l5,%g5
		check_error(NEXTE,bne)
		cmp	%l6,%g6
		check_error(NEXTE,bne)
		cmp	%l7,%g7
		check_error(NEXTE,bne)

stop: b testok; nop
		nop

.seg "data"
.align	8
flag:		.word	0x12345678
flag2:		.word	0x92b4d6f8

		.word	0x12345678
		.word	0x92b4d6f8

chk_data:	.word	0x00000001
		.word	0xfffffffe
		.word	0x00000003
		.word	0xfffffffb
		.word	0x00000000
		.word	0x00000000
		.word	0x00000007

.seg "text"
