/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: dmove.s,v 1.2 91/01/10 15:42:07 rudi Exp $"
.align 8
.seg "text"

#define ERR_RET %r6
#define ERR_RET_NUM 6

#include "err_incr.h"
#include "err_macro.h"
new_init(dmove)
	
 
#include "crueltt.S"

#include "err_subr.S"

.seg	"text"
start:
		mov	0xa0, %psr		! CWP = 6 S = 1
		mov	0, %wim			! WIM = 0
		mov	0, %tbr			! TBR = 0
		nop

		set	ldarea, %l0		! base of load data area
		set	starea, %l2

		set	0, %l1			! load area index
		set	0, %l3			! store area index
		ld	[%l0 + %l1], %i0	! ld - st case
		st	%i0, [%l2 + %l3]

		ld      [%l0 + %l1], %g1
		ld      [%l2 + %l3], %g2
		cmp	%g1,%g2
		check_error(NEXTE,bne)

		set	8, %l1
		set	4, %l3
		ldd	[%l0 + %l1], %i2	! ldd - st-even case
		st	%i2, [%l2 + %l3]

		ldd     [%l0 + %l1], %g4
		ld      [%l2 + %l3], %g2
		cmp	%g4,%g2
		check_error(NEXTE,bne)


		set	16, %l1
		set	8, %l3
		ldd	[%l0 + %l1], %i4	! ldd - st-odd case
		st	%i5, [%l2 + %l3]

		ldd     [%l0 + %l1], %g4
		ld      [%l2 + %l3], %g2
		cmp	%g5,%g2
		check_error(NEXTE,bne)

		set	24, %l1
		set	16, %l3
		mov	5, %i7
		ld	[%l0 + %l1], %i6	! ld-even - std case
		std	%i6, [%l2 + %l3]

		ld      [%l0 + %l1], %g1
		ldd     [%l2 + %l3], %g2
		cmp	%g1,%g2
		check_error(NEXTE,bne)

		save	%l0, 0, %l0		! new window
		set	starea, %l2

		set	28, %l1
		set	24, %l3
		mov	27, %i0
		ld	[%l0 + %l1], %i1	! ld-odd - std case
		std	%i0, [%l2 + %l3]

		ld      [%l0 + %l1], %g1
		ldd     [%l2 + %l3], %g2
		cmp	%g1,%g3
		check_error(NEXTE,bne)

		set	32, %l1
		set	32, %l3
		ldd	[%l0 + %l1], %i2	! ldd - std case
		std	%i2, [%l2 + %l3]

		ldd     [%l0 + %l1], %g2
		ldd     [%l2 + %l3], %g4
		cmp	%g4,%g2
		check_error(NEXTE,bne)

		set	40, %l3
		mov	45, %i3			! other cases
		st	%i3, [%l2 + %l3]

		set	45,%g1
		ld      [%l2 + %l3], %g2
		cmp	%g1,%g2
		check_error(NEXTE,bne)

		set	48, %l3
		mov	-45, %i4
		mov	-5, %i5
		std	%i4, [%l2 + %l3]

		mov     -45, %g1
		ldd     [%l2 + %l3], %g2
		cmp     %g1,%g2
                check_error(NEXTE,bne)

		set	56, %l3
		mov	10, %i7
		mov	-11, %i6
		std	%i6, [%l2 + %l3]

		mov     -11, %g1 
                ldd     [%l2 + %l3], %g2
                cmp     %g1,%g2
                check_error(NEXTE,bne)

stop: b testok; nop
		xnorcc	%g0, %g0, %g2	! ANNULED

.seg "data"
.align 8
ldarea:
		.word	0x12345678
		.word	0x9abcdef0
		.word	0xfedcba98
		.word	0x76543210
		.word	0x13457685
		.word	0xffff23ec
		.word	0x80120014
		.word	0x76503110
		.word	0x12315678
		.word	0x9ab1def0
		.word	0xfed1ba98
		.word	0x76513210
		.word	0x13417685
		.word	0xfff123ec
		.word	0x80110014
		.word	0x76513110

.align 8
starea:	.skip 128
.seg "text"

