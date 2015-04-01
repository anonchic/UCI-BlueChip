/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: mem_memCombo.s,v 1.2 91/01/11 12:20:08 rudi Exp $"
.align 8
.seg "text"
/*********************************************************************************


	PURPOSE:
	This diagnostic tests the BIT bypass 3 and load data bypass paths.
	Data bypass is required when the instruction immediately following
	a load instruction (LD,LDUB,LDSB,LDUH,LDSH, and the alternate space
	variants) uses as an operand the destination register of the load.
	Bypass path 3 is used to pass a result from a R stage to the D stage
	in the pipeline.

	Registers %g1, %g2, and %g3 are used to accumulate errors.  When
	the label "stop" is reached, these registers should contain 0.  At
	"stop" an infinite loop is entered.

	Register %g4 is used to indicate completion of the diagnostic.
	If EVERY test was run (with or without errors) this register will
	contain -1.  Otherwise it will be zero.

	Some errors (such as traps) cannot be recovered from so the diagnostic
	is terminated early, and %g4 will not be set.

	If %g1, %g2, or %g3 does not contain zero, each bit set corresponds to a
	detected error, with the bits in %g2 representing errors 32 to 63,
	and bits in %g3 representing errors 64-95.
	For each set bit (n), find the call to:
	
		check_errors(n ...)

	in this source.  This is the point at which the error was detected.


*********************************************************************************/

/* registers used for recording errors */

#define ERR_RET %r1
#define ERR_RET_NUM 1

#include "err_incr.h"
#include "err_macro.h"

new_init(mem_memCombo)


/* constants used to check operation */

#define	C1		0x0001ffff
#define C1_1		(C1 >> 16)			/* 1st half-word of C1 */
#define C2		0x0100f000
#define C2_1		(C2 >> 24)			/* 1st byte of C2 */
#define C2_3ext		((C2 >> 8) | 0xffffff00)
#define C3		0x000001ff
#define C4		0x00000111
#define C5		0x00000222
#define C0		0xa1


/********************************************************************************

	TRAP TABLE

********************************************************************************/

#include "crueltt.S"

#include "err_subr.S"

/********************************************************************************

	STARTUP ROUTINE

********************************************************************************/

start:
		set	0x10e0,	%g1	! EF, S, PS, ET
		mov	%g1, %psr
		mov	0, %wim
	set address_0, %g1
		mov	%g1, %tbr; nop; nop; nop

/*******************************************************************************

	MAIN PROGRAM

*******************************************************************************/


		! initialize address space pointer to point to data
		!   and store program data

		set	Dspace, %l7
		clr	%l6


/*******************************************************************************
*******************************************************************************/

/*******************************************************************************

	Check bypass path 3 without data bypass

*******************************************************************************/

		mov	0xf, %l1			! set up register val

/* bypass 3 to src1 with no data bypass */
bp3_1_ndb:
		add	%l1, %g0, %l2			! source of bypass 3
		nop
		nop					! nops cause W/D xfer
		add	%l2, %g0, %l3			! target. %l2 is src1

		cmp	%l3, %l1			! %l3 should have val %l1
		check_error(NEXTE, bne)

/* bypass 3 to src2 with no data bypass */
bp3_2_ndb:
		add	%l1, %g0, %l2			! source of bypass 3
		nop
		nop					! nops cause W/D xfer
		add	%g0, %l2, %l3			! target. %l2 is src2

		cmp	%l3, %l1			! %l3 should have val %l1
		check_error(NEXTE, bne)

/* bypass 3 to both src1 and src2 with no data bypass */
bp3_b_ndb:
		add	%g0, C0, %l2			! source of bypass 3
		nop
		nop					! nops cause W/D xfer
		add	%l2, %l2, %l3			! target. %l2 is both

		cmp	%l3, (C0 + C0)
		check_error(NEXTE, bne)


/*******************************************************************************

	Check data bypass without bypass 3
	 - %l7 points to top of a memory block where data has
	   been stored (Dspace).  Use %l6 to index into block.
	 - interlocks (if add nop between LOAD and ADD, will prevent interlock)

*******************************************************************************/

		set	Dspace, %l7			! point to data space

/* Data bypass to src1 with no bypass 3 */
db_1_nb3:
		clr	%l6				! zero indexer

		ldsh	[%l7 + %l6], %l0		! LOAD_HERE
							! INT_OR_NOP_HERE
		add	%l0, %g0, %l2			! target.  %l0 is src1
							! %l0 has C1_1
		cmp	%l2,  (C1_1)			! should be C1_1
		check_error(NEXTE, bne)

/* Data bypass to src2 with no bypass 3 */
db_2_nb3:
		set	0x8, %l6			! point to 3rd word
		set	0x1, %l1			! set for add to increment

		ld	[%l7 + %l6], %l0		! LOAD_HERE
							! INT_OR_NOP_HERE
		add	%l1, %l0, %l2			! target. %l0 is src2
							! %l0 has C3
		cmp	%l2, (C3 + 1)			! should be C3 + 1
		check_error(NEXTE, bne)

/* Data bypass to both src1 and src2 with no bypass 3 */
db_b_nb3:
		set	0x4, %l6			! point to 2nd word

		ldub	[%l7 + %l6], %l0		! LOAD_HERE
							! INT_OR_NOP_HERE
		add	%l0, %l0, %l2			! target.  %l0 is both
							! %l0 has C2_1
		cmp	%l2,  (C2_1 + C2_1)		! should be C2_1 + C2_1
		check_error(NEXTE, bne)


/*******************************************************************************

	Simultaneous data bypass and bypass 3.
	 - %l7 points to top of memory block reserved for data.  Use %l6 to 
	   index into it.
	 - interlock (can prevent interlocks by adding NOP between LOAD and ADD)

*******************************************************************************/

		set 	0xa, %l1			! use for add (=10)
		set	0x5, %l2			! ditto (=5)
		set	0x6, %l6			! use to index 7th byte
		set	Dspace, %l7			! point to data

/* data bypass to src1 and bypass 3 to src2 */
db1_b2:
		add	%l1, %l2, %l3			! source of bp3 (=F hex)
		ldsb	[%l7 + %l6], %l4		! LOAD_HERE
							! INT_OR_NOP_HERE
		add	%l4, %l3, %l5			! target. BP3 is src2
							!  data bypass is src1
		cmp	%l5,  (C2_3ext + 0xf)
		check_error(NEXTE, bne)

/* data bypass to src2 and bypass 3 to src1 */
db2_b1:
		add	%l1, %l2, %l3			! source of bp3 (=F hex)
		ldsb	[%l7 + %l6], %l4		! LOAD_HERE
							! INT_OR_NOP_HERE
		add	%l3, %l4, %l5			! target. BP3 is src1
							!  data bypass is src2
		cmp	%l5,  (C2_3ext + 0xf)
		check_error(NEXTE, bne)


/*******************************************************************************

	LDD can cause the need for bypass 3 and data bypass if the odd and/or
	even register of the LDD is used as an operand before the LDD is
	through the pipe.  Diagnostic ldd_check.s does a fuller test of the
	LDD dependencies.

******************************************************************************/

		
		set	Double_sp, %l7			! point to double data
		clr	%l6				! point to 1st entry

/* ldd followed by odd register reference followed by another odd reg ref */
/* causes an interlock, then data is passed via data bypass and bypass 3  */

ldd_o_o:
		ldd	[%l7 + %l6], %l0		! LOAD_HERE
		add	%l1, %l1, %l2			! odd, interlock,DB
		sub	%l2, %l1, %l3			! odd, BP3

		cmp	%l2, (C5 + C5)
		check_error(NEXTE, bne)

		cmp	%l3, C5
		check_error(NEXTE, bne)

/* ldd followed by even reg ref followed by another even reg ref causes */
/* a data bypass and a bypass 3 without interlocking		  	*/

ldd_e_e:
		ldd	[%l7 + %l6], %l0		! LOAD_HERE
		add	%l0, %l0, %l2			! data bypass
		sub	%l2, %l0, %l3			! bypass 3

		cmp	%l2, (C4 + C4)
		check_error(NEXTE, bne)

		cmp	%l3, C4
		check_error(NEXTE, bne)


/*******************************************************************************

	TEST BYPASS 3 WITH SIMULTANEOUS BYPASS 1 AND BYPASS 2

*******************************************************************************/

/* Simultaneous bypas 2 and 3 */

simul_23:
		add	%g0, C4, %l0			! source of bypass 3
		add	%g0, C5, %l1			! source of bypass 2
		nop					! to align for bypass
		sub	%l1, %l0, %l2			! target of bypasses

		cmp	%l2, (C5 - C4)
		check_error(NEXTE, bne)

/* simultaneous bypass 1 and 3 */

simul_13:
		add	%g0, C4, %l0			! source of bypass 3
		nop					! align for bypass
		add	%g0, C5, %l1			! source of bypass 1
		sub	%l1, %l0, %l2			! target of bypasses

		cmp	%l2, (C5 - C4)
		check_error(NEXTE, bne)


/*******************************************************************************

	TEST SIMULTANEOUS DATA BYPASS AND BYPASS 1

*******************************************************************************/

/* simultaneous bypass 1 and data bypass with ld */

		set	Double_sp, %l7			! point to data
		set	0x2, %l6			! index to 2nd half wd

simul_ld1:						! source of data byp
		lduh	[%l7 + %l6], %l3		! LOAD_HERE
		add	%g0, C5, %l4			! source of bypass 1
		sub	%l4, %l3, %l5			! target of bypasses

		cmp	%l5, (C5 - C4)
		check_error(NEXTE, bne)

/* simultaneous bypass 1 and data bypass with ldd */

		clr	%l6
simul_ldd1:
		ldd	[%l7 + %l6], %l0		! source of data byp
							! and bypass 1
		add	%l1, %l0, %l2			! target of bypasses

		cmp	%l2, (C4 + C5)
		check_error(NEXTE, bne)


/*******************************************************************************

	ALL DONE

*******************************************************************************/


stop: b testok; nop
		b	stop
		b	stop



/*******************************************************************************
*******************************************************************************/


		.seg	"data"

		.align	4				! word align

Dspace:		.word C1, C2, C3


		.align	8

Double_sp:	.word C4, C5


/*******************************************************************************
*******************************************************************************/
