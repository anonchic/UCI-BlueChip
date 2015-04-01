/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: mem_brCombo.s,v 1.2 91/01/11 11:59:08 rudi Exp $"
.align 8
.seg "text"

/****************************************************************/
/*								*/
/* 	Test Items :						*/
/*		LD , ST , LDST             	 		*/
/*		followed by  Bicc                  	 	*/
/*	Test results :						*/
/*	   	%i3,%i4 and %i7 are used to flag errors		*/
/*	if ( %i3==0 and %i4==0 and %i7==0 )then no errors	*/
/*         							*/
/****************************************************************/
/* SIGNIFICANT REGISTERS AT LABEL "stop"			*/
/* EXPECTED VALUES (FROM SAS)					*/
/*         							*/
/*window:  0							*/
/*           INS          LOCALS            OUTS         GLOBALS*/
/*0:  0x00001000      0x00000059      0x22222222      0x00000000*/
/*1:  0x00002000      0x00000022      0x44444444      0x00040000*/
/*2:  0x00004000      0x000000ab      0xabcedf01      0x000000ee*/
/*3:  0x00000000      0x000000ee      0x22222222      0x22222222*/
/*4:  0x00000000      0x00000000      0x44444444      0x00000000*/
/*5:  0x00000258      0xffffffff      0x00000555      0x44444444*/
/*6:  0x00008000      0xffffffff      0x00000666      0x000000ab*/
/*7:  0x00000000      0x00001790      0x00000777      0x87654321*/
/* 	%r20 (%l4) is used as temp reg for error checking on the 
	end of this test and may not be 0x00000000		*/
/****************************************************************/

/******************************************************************************

This program modified September 9, 1987.  Previously, this diagnostic used
all of the registers as writable locations and addressed memory as a
constant offset (no register base).  Because of this limitation, a trap
table was not included, which meant that it could not be run on the
simulator.  In order to add the necessary trap table, the register %l7
is used as a base address to point to a storage location in memory.  The
instructions that used %l7 as a destination now use %l5 as a destination.
No other changes were made.

******************************************************************************/

#define ERR_RET %r20
#define ERR_RET_NUM 20

#include "err_incr.h"
#include "err_macro.h"


/******************************************************************************

	TRAP TABLE

******************************************************************************/

new_init(mem_brCombo)
#include "crueltt.S"

#include "err_subr.S"


/******************************************************************************

	MAIN PROGRAM

******************************************************************************/

start:
! initialize PSR, WIM, and TBR
! Originally this diagnostic set ET=0.  However, in memory disable mode, this
! causes the simulator to reset, so it has been changed to 1.

	mov	0xa0,%psr			! SET PSR :  S=1, pS=0,
	                		        !            ET=1,CWP=0
	mov	%r0,%wim
	set	address_0, %g1
	mov	%g1,%tbr

/*  SET CONSTANT to REGISTERS */


	set	4096,%i0
	set	4096*2,%i1
	set	4096*4,%i2
	set	4096*8,%i6
	set	0x0,%i3
	set	0x0,%i4
	set	0x0,%i7

	set	0xfff,%o0
	set	0x111,%o1
	set	0x222,%o2
	set	0x333,%o3
	set	0x444,%o4
	set	0x555,%o5
	set	0x666,%o6
	set	0x777,%o7

	set	0x0,%l0
	set	0x1,%l1
	set	0x2,%l2
	set	0x3,%l3
	set	0x4,%l4
	set	0x5,%l5
	set	0x6,%l6

	set	4096*16,%g1
	set	0x22222222,%g2
	set	0x33333333,%g3
	set	0x44444444,%g4
	set	0x55555555,%g5
	set	0x66666666,%g6
	set	0x77777777,%g7

/******************************************************************************

	Set address of data space into %l7 for use as pointer so no longer
	use low 4K for memory addressing.

******************************************************************************/

	set	data_area, %l7


/******************************************************************************

	GO WILD!!!

******************************************************************************/

/*	test st-bicc not taken	*/

       	add	%i4,1,%i4
	add	%r0,300,%i5
	subcc	%r0,%r0,%r0
     	st	%g2,[%l7 + 0]			!               (1)
	bne	branch1				! Bicc not taken
	add	%r0,400,%i5
       	sub	%i4,1,%i4
branch1:
	ld	[%l7 + 0],%g3
	subcc	%g3,%g2,%r0			! test memory result
	be	branch2
	nop
	add	%i4,2,%i4
branch2:
	subcc	%i5,400,%i5			! test delay taken
	be	branch3
	nop
	add	%i4,4,%i4
branch3:
	

/*	test st-bicc,a not taken	*/

	add	%r0,600,%i5
	addcc	%r0,1,%r0
     	st	%g4,[%l7 + 0]			!               (1)
	bneg,a	branch4				! Bicc,a not taken
	add	%r0,500,%i5
	nop
branch4:
	ld	[%l7 + 0],%g5
	subcc	%g5,%g4,%r0			! test memory result
	be	branch5
	nop
	add	%i4,8,%i4
branch5:
	subcc	%i5,600,%r0			! test delay not taken
	be	branch6
	nop
	add	%i4,16,%i4
branch6:

/*	test st-bicc taken	*/

	add	%r0,777,%i5
	addcc	%r0,1,%r0
     	st	%g6,[%l7 + 0]			!               (1)
	bpos	branch7				! Bicc taken
	add	%r0,0xffffffff,%i5
	add	%i4,32,%i4
branch7:
	ld	[%l7 + 0],%g7
	subcc	%g7,%g6,%r0			! test memory result
	be	branch8
	nop
	add	%i4,64,%i4
branch8:
	subcc	%i5,0xffffffff,%r0		! test delay taken
	be	branch9
	nop
	add	%i4,128,%i4
branch9:


/*	test st-bicc,a taken	*/

	set	0x87654321,%g6
	add	%r0,777,%i5
	addcc	%r0,1,%r0
     	st	%g6,[%l7 + 0]			!               (1)
	bpos,a	branch10			! Bicc taken
	add	%r0,0xffffffff,%i5
	add	%i4,256,%i4
branch10:
	ld	[%l7 + 0],%g7
	subcc	%g7,%g6,%r0			! test memory result
	be	branch11
	nop
	add	%i4,512,%i4
branch11:
	subcc	%i5,0xffffffff,%r0		! test delay taken
	be	branch12
	nop
	add	%i4,1024,%i4
branch12:

/*	test st-ba	*/

	add	%r0,300,%i5
	subcc	%r0,%r0,%r0
     	st	%g2,[%l7 + 0]			!               (1)
	ba	brnch1				! Bicc not taken
	add	%r0,400,%i5
       	add	%i4,2048,%i4
brnch1:
	ld	[%l7 + 0],%g3
	subcc	%g3,%g2,%r0			! test memory result
	be	brnch2
	nop
	add	%i4,%i0,%i4			! %i0 = 4096
brnch2:
	subcc	%i5,400,%i5			! test delay taken
	be	brnch3
	nop
	add	%i4,%i1,%i4
brnch3:
	

/*	test st-ba,a	*/

	add	%r0,600,%i5
	addcc	%r0,1,%r0
     	st	%g4,[%l7 + 0]			!               (1)
	ba,a	brnch4				! Bicc,a not taken
	add	%r0,500,%i5
	nop
brnch4:
	ld	[%l7 + 0],%g5
	subcc	%g5,%g4,%r0			! test memory result
	be	brnch5
	nop
	add	%i4,%i2,%i4			! %i2 = 4096*2
brnch5:
	subcc	%i5,600,%r0			! test delay not taken
	be	brnch6
	nop
	add	%i4,%i6,%i4			! %i2 = 4096*4
brnch6:

/*	test ld-bicc not taken	*/

       	add	%i3,1,%i3
	st	%g2,[%l7 + 0]
	add	%r0,300,%i5
	subcc	%r0,%r0,%r0
     	ld	[%l7 + 0],%o0			!               (1)
	bne	branch13			! Bicc not taken
	add	%r0,400,%i5
       	sub	%i3,1,%i3
branch13:
	subcc	%o0,%g2,%r0			! test memory result
	be	branch14
	nop
	add	%i3,2,%i3
branch14:
	subcc	%i5,400,%i5			! test delay taken
	be	branch15
	nop
	add	%i3,4,%i3
branch15:
	

/*	test ld-bicc,a not taken	*/

	st	%g4,[%l7 + 0]
	add	%r0,600,%i5
	addcc	%r0,1,%r0
	ld	[%l7 + 0],%o1
	bneg,a	branch16			! Bicc,a not taken
	add	%r0,500,%i5
	nop
branch16:
	subcc	%o1,%g4,%r0			! test memory result
	be	branch155
	nop
	add	%i3,8,%i3
branch155:
	subcc	%i5,600,%r0			! test delay not taken
	be	branch166
	nop
	add	%i3,16,%i3
branch166:

/*	test ld-bicc taken	*/

     	st	%g6,[%l7 + 0]			!               (1)
	add	%r0,777,%i5
	addcc	%r0,1,%r0
	ld	[%l7 + 0],%o2
	bpos	branch17				! Bicc taken
	add	%r0,0xffffffff,%i5
	add	%i3,32,%i3
branch17:
	subcc	%o2,%g6,%r0			! test memory result
	be	branch19
	nop
	add	%i3,64,%i3
branch19:
	subcc	%i5,0xffffffff,%r0		! test delay taken
	be	branch110
	nop
	add	%i3,128,%i3
branch110:

/*	test ld-bicc,a taken	*/

	set	0xabcedf01,%g6
     	st	%g6,[%l7 + 0]			!               (1)
	add	%r0,777,%i5
	addcc	%r0,1,%r0
	ld	[%l7 + 0],%o2
	bpos,a	branch217				! Bicc taken
	add	%r0,0xffffffff,%i5
	add	%i3,32,%i3
branch217:
	subcc	%o2,%g6,%r0			! test memory result
	be	branch219
	nop
	add	%i3,64,%i3
branch219:
	subcc	%i5,0xffffffff,%r0		! test delay taken
	be	branch2110
	nop
	add	%i3,128,%i3
branch2110:

/*	test ld-ba	*/

	add	%r0,300,%i5
     	st	%g2,[%l7 + 0]			!               (1)
	subcc	%r0,%r0,%r0
     	ld	[%l7 + 0],%o3			!               (1)
	ba	brnch11				! Bicc not taken
	add	%r0,400,%i5
       	add	%i3,2048,%i3
brnch11:
	subcc	%o3,%g2,%r0			! test memory result
	be	brnch12
	nop
	add	%i3,%i0,%i3			! %i0 = 4096
brnch12:
	subcc	%i5,400,%i5			! test delay taken
	be	brnch13
	nop
	add	%i3,%i1,%i3
brnch13:
	

/*	test ld-ba,a	*/

	add	%r0,600,%i5
     	st	%g4,[%l7 + 0]			!               (1)
	addcc	%r0,1,%r0
	ld	[%l7 + 0],%o4
	ba,a	brnch14				! Bicc,a not taken
	add	%r0,500,%i5
	nop
brnch14:
	subcc	%o4,%g4,%r0			! test memory result
	be	brnch15
	nop
	add	%i3,%i2,%i3			! %i2 = 4096*2
brnch15:
	subcc	%i5,600,%r0			! test delay not taken
	be	brnch16
	nop
	add	%i3,%i6,%i3			! %i2 = 4096*4
brnch16:

/*	test ldst-bicc not taken	*/

       	add	%i7,1,%i7
	mov	89, %g2
	stb	%g2,[%l7 + 3]
	add	%r0,300,%i5
	subcc	%r0,%r0,%r0
     	ldstub	[%l7 + 3],%l0			!               (1)
	bne	branh13				! Bicc not taken
	add	%r0,400,%i5
       	sub	%i7,1,%i7
branh13:
	cmp	%l0,%g2				! test memory result
	be	branh14
	nop
	add	%i7,2,%i7
branh14:
	subcc	%i5,400,%i5			! test delay taken
	be	branh15
	nop
	add	%i7,4,%i7
branh15:
	ldsb	[%l7 + 3],%l5
	cmp	%l5, -1
	be	branh95
	nop
	add	%i7,8,%i7
branh95:
	

/*	test ldst-bicc,a not taken	*/

	mov	34, %g4
	stb	%g4,[%l7 + 0]
	add	%r0,600,%i5
	addcc	%r0,1,%r0
	ldstub	[%l7 + 0],%l1
	bneg,a	branh16				! Bicc,a not taken
	add	%r0,500,%i5
	nop
branh16:
	subcc	%l1,%g4,%r0			! test memory result
	be	branh155
	nop
	add	%i7,16,%i7
branh155:
	subcc	%i5,600,%r0			! test delay not taken
	be	branh166
	nop
	add	%i7,32,%i7
branh166:
	ldsb	[%l7 + 0],%l6
	cmp	%l6,-1
	be	branh85
	nop
	add	%i7,64,%i7
branh85:

/*	test ldst-bicc taken	*/

	mov	0xee, %g6
     	stb	%g6,[%l7 + 0]			!               (1)
	add	%r0,777,%i5
	addcc	%r0,1,%r0
	ldstub	[%l7 + 0],%l2
	bpos	branh17				! Bicc taken
	add	%r0,0xffffffff,%i5
	add	%i7,128,%i7
branh17:
	subcc	%l2,%g6,%r0			! test memory result
	be	branh19
	nop
	add	%i7,256,%i7
branh19:
	subcc	%i5,0xffffffff,%r0		! test delay taken
	be	branh110
	nop
	add	%i7,512,%i7
branh110:
	ldsb	[%l7 + 0],%l5
	cmp	%l5, -1
	be	branh75
	nop
	add	%i7,1024,%i7
branh75:

/*	test ldst-bicc,a taken	*/

	set	0xab,%g6
     	stb	%g6,[%l7 + 0]			!               (1)
	add	%r0,777,%i5
	addcc	%r0,1,%r0
	ldstub	[%l7 + 0],%l2
	bpos,a	branh217				! Bicc taken
	add	%r0,0xffffffff,%i5
	add	%i7,2048,%i7
branh217:
	subcc	%l2,%g6,%r0			! test memory result
	be	branh219
	nop
	add	%i7,%i0,%i7
branh219:
	subcc	%i5,0xffffffff,%r0		! test delay taken
	be	branh2110
	nop
	add	%i7,%i1,%i7
branh2110:

/*	test ldst-ba	*/

	add	%r0,300,%i5
	mov	0xee, %g2
     	stb	%g2,[%l7 + 0]			!               (1)
	subcc	%r0,%r0,%r0
     	ldstub	[%l7 + 0],%l3			!               (1)
	ba	brnh11				! Bicc not taken
	add	%r0,400,%i5
       	add	%i7,%i2,%i7
brnh11:
	subcc	%l3,%g2,%r0			! test memory result
	be	brnh12
	nop
	add	%i7,%i6,%i7			!
brnh12:
	subcc	%i5,400,%i5			! test delay taken
	be	brnh13
	nop
	add	%i7,%g1,%i7
brnh13:
	

/*	test ldst-ba,a	*/

	sll	%g1,1,%g1
	add	%r0,600,%i5
	mov	0, %g4
     	stb	%g4,[%l7 + 0]			!               (1)
	addcc	%r0,1,%r0
	ldstub	[%l7 + 0],%l4
	ba,a	brnh14				! Bicc,a not taken
	add	%r0,500,%i5
	nop
brnh14:
	subcc	%l4,%g4,%r0			! test memory result
	be	brnh15
	nop
	add	%i7,%g1,%i7			! 
brnh15:
	sll	%g1,1,%g1
	subcc	%i5,600,%r0			! test delay not taken
	be	brnh16
	nop
	add	%i7,%g1,%i7			!
brnh16:
	st	%g0,[%l7 + 0]
	st	%g1,[%l7 + 4]
	st	%g2,[%l7 + 8]
	st	%g3,[%l7 + 12]
	st	%g4,[%l7 + 16]
	st	%g5,[%l7 + 20]
	st	%g6,[%l7 + 24]
	st	%g7,[%l7 + 28]
	st	%o0,[%l7 + 32]
	st	%o1,[%l7 + 36]
	st	%o2,[%l7 + 40]
	st	%o3,[%l7 + 44]
	st	%o4,[%l7 + 48]
	st	%o5,[%l7 + 52]
	st	%o6,[%l7 + 56]
	st	%o7,[%l7 + 60]
	st	%l0,[%l7 + 64]
	st	%l1,[%l7 + 68]
	st	%l2,[%l7 + 72]
	st	%l3,[%l7 + 76]
	st	%l4,[%l7 + 80]
	st	%l5,[%l7 + 84]
	st	%l6,[%l7 + 88]
	st	%l7,[%l7 + 92]
	st	%i0,[%l7 + 96]
	st	%i1,[%l7 + 100]
	st	%i2,[%l7 + 104]
	st	%i3,[%l7 + 108]
	st	%i4,[%l7 + 112]
	st	%i5,[%l7 + 116]
	st	%i6,[%l7 + 120]
	st	%i7,[%l7 + 124]

! error checking

	cmp	%i3,%g0
	check_error(NEXTE,bne)

	cmp	%i4,%g0
	check_error(NEXTE,bne)

	cmp	%i7,%g0
	check_error(NEXTE,bne)

	nop
stop: b testok; nop
	nop
	nop
	nop

	.seg	"data"
	.align	8
data_area:
		.word	0xaaaaaaaa
		.word	0xbbbbbbbb
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
		.word	0xcccccccc
	.seg	"text"
