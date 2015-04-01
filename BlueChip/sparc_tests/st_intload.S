/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: intload.s,v 1.2 91/01/11 10:24:19 rudi Exp $"
.align 8
.seg "text"
#define ERR_RET %r31
#define ERR_RET_NUM 31

#include "err_incr.h"
#include "err_macro.h"

new_init(intload)

#include "crueltt.S"

#include "err_subr.S"

.seg	"text"
start:
		mov	0xa0, %psr		! CWP = 0 S = 1 ET = 1
		mov	0, %wim			! WIM = 0
		set     address_0, %g1
		mov	%g1, %tbr			! TBR = 0
		nop
		set	flag, %i7

		ld	[%i7], %i0		! 0x12345678
		ld	[%i7 + 8], %l0		! 0x12345678
		ld	[%i7 + 4], %o0		! 0x92b4d6f8
		ld	[%i7 + 12], %g7		! 0x92b4d6f8

		lduh	[%i7], %i1		! 0x00001234
		ldsh	[%i7 + 4], %l1		! 0x00001234
		ldsh	[%i7 + 8], %o1		! 0xffff92b4
		lduh	[%i7 + 12], %g1		! 0xffff92b4

		ldub	[%i7], %i2		! 0x00000012
		ldsb	[%i7 + 8], %l2		! 0x00000012
		ldsb	[%i7 + 4], %o2		! 0xffffff92
		ldub	[%i7 + 12], %g2		! 0xffffff92

		lduh	[%i7 + 2], %i3		! 0x00005678
		ldsh	[%i7 + 10], %l3		! 0x00005678
		ldsh	[%i7 + 6], %o3		! 0xffffd6f8
		lduh	[%i7 + 14], %g3		! 0xffffd6f8

		ldub	[%i7 + 1], %i4		! 0x00000034
		ldsb	[%i7 + 9], %l4		! 0x00000034
		ldsb	[%i7 + 5], %o4		! 0xffffffb4
		ldub	[%i7 + 13], %g4		! 0xffffffb4

		ldub	[%i7 + 2], %i5		! 0x00000056
		ldsb	[%i7 + 10], %l5		! 0x00000056
		ldsb	[%i7 + 6], %o5		! 0xffffffd6
		ldub	[%i7 + 14], %g5		! 0xffffffd6

		ldub	[%i7 + 3], %i6		! 0x00000078
		ldsb	[%i7 + 11], %l6		! 0x00000078
		ldsb	[%i7 + 7], %o6		! 0xfffffff8
		ldub	[%i7 + 15], %g6		! 0xfffffff8

! check for errors

		set	0x12345678,%i7
		cmp	%i0,%i7
		check_error(NEXTE,bne)

		set	0x00001234,%i7
		cmp	%i1,%i7
		check_error(NEXTE,bne)

		set	0x00000012,%i7
		cmp	%i2,%i7
		check_error(NEXTE,bne)

		set	0x00005678,%i7
		cmp	%i3,%i7
		check_error(NEXTE,bne)

		set	0x00000034,%i7
		cmp	%i4,%i7
		check_error(NEXTE,bne)

		set	0x00000056,%i7
		cmp	%i5,%i7
		check_error(NEXTE,bne)

		set	0x00000078,%i7
		cmp	%i6,%i7
		check_error(NEXTE,bne)

		set	0x12345678,%i7
		cmp	%l0,%i7
		check_error(NEXTE,bne)

		set	0xffff92b4,%i7
		cmp	%l1,%i7
		check_error(NEXTE,bne)

		set	0x00000012,%i7
		cmp	%l2,%i7
		check_error(NEXTE,bne)

		set	0x00005678,%i7
		cmp	%l3,%i7
		check_error(NEXTE,bne)

		set	0x00000034,%i7
		cmp	%l4,%i7
		check_error(NEXTE,bne)

		set	0x00000056,%i7
		cmp	%l5,%i7
		check_error(NEXTE,bne)

		set	0x00000078,%i7
		cmp	%l6,%i7
		check_error(NEXTE,bne)

		set	0x92b4d6f8,%i7
		cmp	%o0,%i7
		check_error(NEXTE,bne)

		set	0x00001234,%i7
		cmp	%o1,%i7
		check_error(NEXTE,bne)

		set	0xffffff92,%i7
		cmp	%o2,%i7
		check_error(NEXTE,bne)

		set	0xffffd6f8,%i7
		cmp	%o3,%i7
		check_error(NEXTE,bne)

		set	0xffffffb4,%i7
		cmp	%o4,%i7
		check_error(NEXTE,bne)

		set	0xffffffd6,%i7
		cmp	%o5,%i7
		check_error(NEXTE,bne)

		set	0xfffffff8,%i7
		cmp	%o6,%i7
		check_error(NEXTE,bne)

		set	0x000092b4,%i7
		cmp	%g1,%i7
		check_error(NEXTE,bne)

		set	0x00000092,%i7
		cmp	%g2,%i7
		check_error(NEXTE,bne)

		set	0x0000d6f8,%i7
		cmp	%g3,%i7
		check_error(NEXTE,bne)

		set	0x000000b4,%i7
		cmp	%g4,%i7
		check_error(NEXTE,bne)

		set	0x000000d6,%i7
		cmp	%g5,%i7
		check_error(NEXTE,bne)

		set	0x000000f8,%i7
		cmp	%g6,%i7
		check_error(NEXTE,bne)

		set	0x92b4d6f8,%i7
		cmp	%g7,%i7
		check_error(NEXTE,bne)

stop: b testok; nop
		mov	%i6, %i7

.seg "data"
.balign	8
flag:		.word	0x12345678
		.word	0x92b4d6f8

		.word	0x12345678
		.word	0x92b4d6f8

		.word	0xa5a5a5a5
		.word	0xa5a5a5a5
		.word	0xa5a5a5a5
		.word	0xa5a5a5a5
.seg "text"
