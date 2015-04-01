/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: multicycleCombo.s,v 1.3 91/01/14 13:25:15 rudi Exp $"
.align 8
.seg "text"

#define ERR_RET %r7
#define ERR_RET_NUM 7

#include "err_incr.h"
#include "err_macro.h"

#include "assy_macros.h"

new_init(multicycleCombo)	
#include "crueltt.S"

#include "err_subr.S"

#include "chk_reg.h"

/*
	An uninterrupted sequence of instructions which is design
	so that each of the following instructions is followed
	by any other sometime during the sequence.

		ld
		st
		ldstub
		ldd
		std
		jmp
*/

.seg	"text"
start:
		mov	0xa0, %psr		! CWP = 0 S = 1
		mov	0, %wim			! WIM = 0
		set	address_0, %g1	!added by murph
		mov	%g1, %tbr		! TBR = 0
		nop;nop;nop !and nops by murph

		clr	%l0 ! and clears added by murph
		clr	%l1
		clr	%l2
		clr	%l3
		clr	%l4	
		clr	%l5		
		clr	%l6
		clr	%l7

		clr	%i0
		clr	%i1
		clr	%i2
		clr	%i3
		clr	%i4	
		clr	%i5		
		clr	%i6
		clr	%i7

		clr	%o0
		clr	%o1
		clr	%o2
		clr	%o3
		clr	%o4	
		clr	%o5		
		clr	%o6
		clr	%o7

		clr	%g1
		clr	%g2
		clr	%g3
		clr	%g4	
		clr	%g5		
		clr	%g6
		clr	%g7		
	
		set	starea, %g3

		set	targ1, %i1
		set	targ2, %i2
		set	targ3, %i3
		set	targ4, %i4
		set	targ5, %g5
		set	targ6, %g6

		set	ldarea, %g1

		ld	[%g1], %i7
		lduh	[%g1 + 2], %l0
		st	%i7, [%g3]
		sth	%l0, [%g3 + 4]
		ldstub	[%g1], %l1
		ldstub	[%g1 + 1], %l1
		ldd	[%g0 + %g1], %l2
		ldd	[%g1 + 8], %l4
		std	%l2, [%g3 + 8]
		std	%l4, [%g3 + 16]
		jmpl	%i1, %i1
		jmpl	%i2, %i2

targ1:		ldub	[%g1 + 2], %l5 	! should not be executed
		check_error(NEXTE,ba)

targ2:		ldstub	[%g1 + 3], %l6
		ldsh	[%g1 + 2], %l7
		ldd	[%g1], %o0
		ldsb	[%g1 + 5], %o2
		std	%o0, [%g3 + 24]
		ld	[%g1 + 8], %o3
		jmpl	%i3, %i3
		stb	%o2, [%g1 + 4]
					! should not be executed
		check_error(NEXTE,ba)

targ3:		lduh	[%g1], %o4
		st	%o2, [%g3 + 28]
		ldd	[%g1 + 16], %o6
		sth	%o6, [%g3 + 30]
		std	%o6, [%g3 + 32]
		stb	%i4, [%g3 + 33]
		jmpl	%g0 + %i4, %i4
		ldstub	[%g1 + 3], %o5
		check_error(NEXTE,ba)
targ4:		st	%i4, [%g3 + 36]
		save
		st	%g5, [%g3 + 40]
		ldstub	[%g3 + 40], %i0
		std	%g0, [%g3 + 48]
		ldstub	[%g3 + 52], %i1
		jmpl	%g5, %g5
		ldd	[%g1 + 8], %g0
					! should not be executed
		check_error(NEXTE,ba)

targ5:		ldstub	[%g3 + 48], %i3
		ldd	[%g3 + 32], %i4
		jmpl	%g6, %g6
		std	%i4, [%g3 + 56]
		check_error(NEXTE,ba)
targ6:		ldd	[%g3 + 56], %g2

! checking errors

		cmp	%g7,%g0
		check_error(NEXTE,bne)

! check(%i0,0x00000000)

check(%i1,0x00000000)

	b testok
	nop
	
	

check(%i2,0xffffffbc)
check(%i3,0x00000000)
check(%i4,0xed34a671)
check(%i5,0x00001708) 
check(%i6,0xed32a671)
check(%i7,0xe3218fda)


check(%l0,0)
check(%l1,0)
check(%l2,0)
check(%l3,0)
check(%l4,0)
check(%l5,0)
check(%l6,0)
check(%l7,0)
 
check(%o0,0x00000000)
check(%o1,0x00000000)
check(%o2,0x00000000)
check(%o3,0x00000000)
check(%o4,0x00000000)
check(%o5,0x00000000)
check(%o6,0x00000000)
check(%o7,0x00000000) 
 
check(%g1,0x76543210)
check(%g2,0xed34a671)
check(%g3,0x00001708)
check(%g4,0x00000000)
check(%g5,0x0000174c)
check(%g6,0x00001780)

stop: b testok; nop
		xnorcc	%g0, %g0, %g2	! ANNULED

end:		nop

.seg "data"
	.align	8
ldarea:		.word	0x12345678
		.word	0x9abcdef0
		.word	0xfedcba98
		.word	0x76543210
		.word	0xed32a671
		.word	0xe3218fda
		.word	0x23076adb
		.word	0xcba62589

.seg "bss"
	.align	8
starea:		.skip 64
.seg "text"


